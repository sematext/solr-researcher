/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.sematext.solr.handler.component.AbstractReSearcherComponent;
import com.sematext.solr.handler.component.CorrectionHighlighter;
import com.sematext.solr.handler.component.ReSearcherRequestContext;
import com.sematext.solr.handler.component.ReSearcherUtils;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion.FewerMatchesType;
import com.sematext.solr.handler.component.relaxer.query.RelaxerParams;

/**
 * 
 * FIXME: add descriptive class javadoc
 * 
 * @author sematext, http://www.sematext.com/
 */
public final class QueryRelaxerComponent extends AbstractReSearcherComponent {
  private static final Logger LOG = LoggerFactory.getLogger(QueryRelaxerComponent.class);
  
  public static final String COMPONENT_NAME = "queryRelaxer";

  private static final String PHRASE_QUERY_HEURISTICS = "phraseQueryHeuristics";
  private static final String REGULAR_QUERY_HEURISTICS = "regularQueryHeuristics";
  private static final String FIELD_ANALYZER_MAPS = "fieldAnalyzerMaps";
  private static final String MIN_RESULTS_FOR_GOOD_FGS_SUGGESTION = "minResultsForGoodFgsSuggestion";

  private static final String DEFAULT_PHRASE_QUERY_HEURISTIC_CLASS = "com.sematext.solr.handler.component.relaxer.heuristics.phrase.RemoveAllQuotes";
  private static final String DEFAULT_REGULAR_QUERY_HEURISTIC_CLASS = "com.sematext.solr.handler.component.relaxer.heuristics.regular.RemoveOneTerm";
  
  /**
   * The minimal number of results some suggestion has to return to label it as a 'good' suggestion
   */
  private int minResultForGoodFgsSuggestion = 0;

  private List<QueryRelaxerHeuristic> phraseQueryHeuristics = new ArrayList<QueryRelaxerHeuristic>();
  private List<QueryRelaxerHeuristic> regularQueryHeuristics = new ArrayList<QueryRelaxerHeuristic>();;

  @SuppressWarnings("rawtypes")
  protected NamedList args;

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void init(NamedList args) {
    super.init(args);
    this.args = args;

    List<String> phraseHeur = (List<String>) args.get(PHRASE_QUERY_HEURISTICS);
    List<String> regularHeur = (List<String>) args.get(REGULAR_QUERY_HEURISTICS);

    minResultForGoodFgsSuggestion = getInt(args, MIN_RESULTS_FOR_GOOD_FGS_SUGGESTION);

    if (phraseHeur != null) {
      for (String heur : phraseHeur) {
        loadHeuristic(phraseQueryHeuristics, heur);
      }
    } else {
      loadHeuristic(phraseQueryHeuristics, DEFAULT_PHRASE_QUERY_HEURISTIC_CLASS);
    }

    if (regularHeur != null) {
      for (String heur : regularHeur) {
        loadHeuristic(regularQueryHeuristics, heur);
      }
    } else {
      loadHeuristic(regularQueryHeuristics, DEFAULT_REGULAR_QUERY_HEURISTIC_CLASS);
    }

  }
  
  @Override
  public boolean checkComponentShouldProcess(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, false)) {
      return false;
    }

    return true;
  }

  @Override
  public void doProcess(ReSearcherRequestContext ctx, ResponseBuilder rb) throws Exception {
    SolrParams params = rb.req.getParams();

    String loggingHeader = "Searching for suggestions for query : '" + rb.req.getParams().get(CommonParams.Q) + "'";
    LOG.info(loggingHeader + " - starting process");
 
    // remove query relaxer parameter because requests sent from this component aren't supposed to be relaxed
    String preferFewerMatches = params.get(RelaxerParams.QUERY_RELAXER_PREFER_FEWER_MATCHES);
    FewerMatchesType fewerMatchesType = FewerMatchesType.fromString(preferFewerMatches);
    SortedSet<RelaxerSuggestion> sortedSet = new TreeSet<RelaxerSuggestion>(fewerMatchesType.getComparator());
    
    int rows = params.getInt(CommonParams.ROWS, 10);
    rows = params.getInt(RelaxerParams.QUERY_RELAXER_ROWS_PER_QUERY, rows);
    int maxQueries = params.getInt(RelaxerParams.QUERY_RELAXER_MAX_QUERIES, 5);
    
    String highlightRemoved = rb.req.getParams().get(RES_HIGHLIGHT_REMOVED_TAG_PARAM_NAME);
    String highlightReplaced = rb.req.getParams().get(RES_HIGHLIGHT_REPLACED_TAG_PARAM_NAME);
    boolean ignoreQuotes = rb.req.getParams().get(RES_HIGHLIGHT_IGNORE_QUOTES_PARAM_NAME, "true").equals("true") ?
        true : false;
    
    Set<RelaxerSuggestion> newSuggestions = createSuggestions(rb.req);
    LOG.info(loggingHeader + " - created suggestions : " + newSuggestions);
    for (RelaxerSuggestion suggestion : newSuggestions) {
      long hits = ctx.getHandler().handleSuggestionHitsRequest(ctx, suggestion.getRelaxedQuery(), getComponentNames());
      
      if (hits > ctx.getOriginalQueryHits()) {
        suggestion.setNumHits(hits);
        sortedSet.add(suggestion);

        if (minResultForGoodFgsSuggestion > 0) {
          if ((fewerMatchesType == FewerMatchesType.TRUE && hits >= minResultForGoodFgsSuggestion)
              || (fewerMatchesType == FewerMatchesType.FALSE && hits <= minResultForGoodFgsSuggestion)) {
            break;
          }
        }
      }
    }

    // formating result
    if (sortedSet.isEmpty()) {
      LOG.info(loggingHeader + " - no suggestion with more hits than original query found");
    } else {
      int index = 0;
      List<SimpleOrderedMap<Object>> relaxedResponses = new ArrayList<SimpleOrderedMap<Object>>();
      for (RelaxerSuggestion suggestion : sortedSet) {
        QueryRelaxerSuggestion queryRelaxerSuggestion = null;
        MMRelaxerSuggestion mmRelaxerSuggestion = null;
        if (suggestion instanceof QueryRelaxerSuggestion) {
          queryRelaxerSuggestion = (QueryRelaxerSuggestion)suggestion;
        } else if (suggestion instanceof MMRelaxerSuggestion) {
          mmRelaxerSuggestion = (MMRelaxerSuggestion)suggestion;
        }

        ctx.getParams().set(CommonParams.ROWS, rows);
        rb.req.setParams(ctx.getParams());
        
        SolrQueryResponse rsp = ctx.getHandler().handleSuggestionResponseRequest(ctx, suggestion.getRelaxedQuery(), getComponentName(), rb);

        SimpleOrderedMap<Object> relaxedResponse = new SimpleOrderedMap<Object>();
        if (queryRelaxerSuggestion != null) {
          relaxedResponse.add("relaxedQuery", queryRelaxerSuggestion.getRelaxedUserQuery());
          relaxedResponse.add("relaxedType", "query");
        } else if (mmRelaxerSuggestion != null) {
          relaxedResponse.add("relaxedMM", mmRelaxerSuggestion.getRelaxedMM());
          relaxedResponse.add("relaxedType", "mm");
        }
        relaxedResponse.add("relaxedFullQuery", suggestion.getRelaxedQuery());
        
        relaxedResponse.add("relaxer_grouped", rsp.getValues().get("grouped"));
        relaxedResponse.add("relaxer_facet_counts", rsp.getValues().get("facet_counts"));
        relaxedResponse.add("relaxer_terms", rsp.getValues().get("terms"));
        relaxedResponse.add("relaxer_termVectors", rsp.getValues().get("termVectors"));
        relaxedResponse.add("relaxer_highlighting", rsp.getValues().get("highlighting"));
        relaxedResponse.add("relaxer_stats", rsp.getValues().get("stats"));
        
        if (queryRelaxerSuggestion != null) {
          if (highlightRemoved != null || highlightReplaced != null) {
             String highlightRelaxedQuery = CorrectionHighlighter.highlightCorrections(
                 queryRelaxerSuggestion.getUserQuery(), queryRelaxerSuggestion.getRelaxedUserQuery(), highlightRemoved, highlightReplaced, ignoreQuotes);
             relaxedResponse.add("highlightRelaxedQuery", highlightRelaxedQuery);
          }
        }
        
        relaxedResponse.add(getBestSuggestionResultsTagName(), rsp.getValues().get("response"));
        relaxedResponses.add(relaxedResponse);
        
        index++;
        if (index >= maxQueries) {
          break;
        }
      }
      ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName(), relaxedResponses);
    }
  }

  private Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req) {
    boolean isPhrase = false;

    String userQuery = req.getParams().get(CommonParams.Q);
    if (userQuery.contains("\"")) {
      isPhrase = true;
    }

    Set<RelaxerSuggestion> newSuggestions = new LinkedHashSet<RelaxerSuggestion>();
    List<QueryRelaxerHeuristic> heuristicsToIterate;

    if (isPhrase) {
      heuristicsToIterate = phraseQueryHeuristics;
    } else {
      heuristicsToIterate = regularQueryHeuristics;
    }

    for (QueryRelaxerHeuristic heur : heuristicsToIterate) {
      Set<RelaxerSuggestion> sugs = heur.createSuggestions(req);

      if (sugs != null) {
        newSuggestions.addAll(sugs);
      }
    }

    return newSuggestions;
  }

  @Override
  public void inform(SolrCore core) {
    super.inform(core);
    if (args != null) {
      LOG.info("Initializing relaxer");

      IndexSchema schema = core.getLatestSchema();
      @SuppressWarnings("unchecked")
      List<String> maps = (List<String>) args.get(FIELD_ANALYZER_MAPS);
      if (maps != null) {
        Map<Pattern, Analyzer> fieldAnalyzerMaps = new LinkedHashMap<Pattern, Analyzer>();
        for(String fieldAnalyzerMap : maps) {
          String[] strs = fieldAnalyzerMap.split("\\s+");
          if (strs.length < 2) {
            continue;
          }
          Pattern fieldPattern = Pattern.compile(strs[0]);
          FieldType fieldType = schema.getFieldTypes().get(strs[1]);
          Analyzer analyzer = fieldType == null ? new WhitespaceAnalyzer() : 
            fieldType.getQueryAnalyzer();
          fieldAnalyzerMaps.put(fieldPattern, analyzer);
        }
        for (QueryRelaxerHeuristic heuristic : phraseQueryHeuristics) {
          heuristic.setFieldAnalyzerMaps(fieldAnalyzerMaps);
        }
        for (QueryRelaxerHeuristic heuristic : regularQueryHeuristics) {
          heuristic.setFieldAnalyzerMaps(fieldAnalyzerMaps);
        }
      }
    }
  }
  
  @Override
  protected String getBestSuggestionResultsTagName() {
    return "relaxer_response";
  }

  @Override
  protected String getSuggestionsTagName() {
    return "relaxer_suggestions";
  }

  @Override
  public String getComponentName() {
    return QueryRelaxerComponent.COMPONENT_NAME;
  }

  @Override
  public String getDescription() {
    return "Relaxer";
  }

  @SuppressWarnings("rawtypes")
  private void loadHeuristic(List<QueryRelaxerHeuristic> heuristics, String heur) {
    try {
      Class heurClass = this.getClass().getClassLoader().loadClass(heur);
      heuristics.add((QueryRelaxerHeuristic) heurClass.newInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
