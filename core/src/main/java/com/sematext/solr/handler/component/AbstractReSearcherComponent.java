/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.handler.component.MoreLikeThisComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.handler.component.StatsComponent;
import org.apache.solr.handler.component.TermVectorComponent;
import org.apache.solr.handler.component.TermsComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractReSearcherComponent extends SearchComponent implements ReSearcherParams, SolrCoreAware, PluginInfoInitialized {
  private static final Logger LOG = LoggerFactory.getLogger(CommonMisspellings.class);

  private String commonMisspellingsFileLocation = null;
  private Map<String, String> commonMisspellingsMap = null;
  private Set<String> componentNames = new HashSet<String>();

  private int maxOriginalResults;
  
  private PluginInfo shfInfo;
  private List<SearchComponent> queryOnlyComponents;
  private ShardHandlerFactory shardHandlerFactory;
  private SolrCore core;


  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    maxOriginalResults = getInt(args, "maxOriginalResults");
    String tmp = (String) args.get("allComponentNames");
    if (tmp != null) {
      componentNames.addAll(Arrays.asList(tmp.split("\\s*,\\s*")));
    }
    componentNames.add(getComponentName());
    componentNames.add(FacetComponent.COMPONENT_NAME);
    componentNames.add(HighlightComponent.COMPONENT_NAME);
    componentNames.add(StatsComponent.COMPONENT_NAME);
    componentNames.add(TermsComponent.COMPONENT_NAME);
    componentNames.add(TermVectorComponent.COMPONENT_NAME);
    componentNames.add(SpellCheckComponent.COMPONENT_NAME);
    componentNames.add(MoreLikeThisComponent.COMPONENT_NAME);
    componentNames.add(GroupParams.GROUP);
    componentNames.add("queryRelaxer");
    componentNames.add("DymReSearcher");
    componentNames.add("autoComplete");
    commonMisspellingsFileLocation = (String) args.get("commonMisspellingsFile");
    commonMisspellingsMap = CommonMisspellings.loadCommonMisspellingsFile(commonMisspellingsFileLocation);
  }

  @Override
  public void init(PluginInfo info) {
    for (PluginInfo child : info.children) {
      if ("shardHandlerFactory".equals(child.type)) {
        this.shfInfo = child;
        break;
      }
    }
    init(info.initArgs);
  }

  @Override
  public void inform(SolrCore core) {
    this.core = core;
    queryOnlyComponents = new ArrayList<SearchComponent>();
    queryOnlyComponents.add(core.getSearchComponent("query"));
    if (shfInfo == null) {
      shardHandlerFactory = core.getCoreDescriptor().getCoreContainer().getShardHandlerFactory();
    } else {
      shardHandlerFactory = core.createInitInstance(shfInfo, ShardHandlerFactory.class, null, null);
      core.addCloseHook(new CloseHook() {
        @Override
        public void preClose(SolrCore core) {
          shardHandlerFactory.close();
        }

        @Override
        public void postClose(SolrCore core) {
        }
      });
    }
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
  }

  /**
   * Alway disable ReSearcher flag for distributed request
   * 
   * @param sreq
   */

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    if (!checkComponentShouldProcess(rb)) {
      return;
    }

    doProcess(rb);
  }

  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    if (!checkComponentShouldProcess(rb)) {
      return;
    }

    sreq.params.set(getComponentName(), "false");
  }

  @Override
  public void finishStage(ResponseBuilder rb) {
    if (!checkComponentShouldProcess(rb)) {
      return;
    }

    if (rb.stage == ResponseBuilder.STAGE_GET_FIELDS) {
      doProcess(rb);
    }
  }

  public void doProcess(ResponseBuilder rb) {
    long originalQueryHits = ReSearcherUtils.extractOriginalQueryHits(rb);
    if (originalQueryHits > maxOriginalResults) {
      // if there are enough results already, no need to run researcher
      return;
    }
    
    ReSearcherRequestContext ctx = new ReSearcherRequestContext(rb);
    ctx.setCore(core);
    ctx.setQueryOnlyComponents(queryOnlyComponents);
    ctx.setShardHandlerFactory(shardHandlerFactory);
    
    boolean flag = false;
    if (rb.req.getParams().getBool(ReSearcherUtils.INTERNAL_QUERY_MARKER_PARAM) == null) {
      flag = preProcessOriginalQuery(ctx, rb);
    }

    if (!flag) {
      try {
        doProcess(ctx, rb);
      } catch (Exception e) {
        String msg = "ReSearcher error";
        LOG.error(msg, e);
        throw new RuntimeException(msg, e);
      }
    }

    ctx.restoreContext(rb);
  }

  public boolean preProcessOriginalQuery(ReSearcherRequestContext ctx, ResponseBuilder rb) {
    if (commonMisspellingsFileLocation == null) {
      return false;
    }

    String originalQuery = rb.getQueryString();
    String correctedQuery = CommonMisspellings.correctedQuery(originalQuery, commonMisspellingsMap);
    if (correctedQuery != null) {
      LOG.info("Original query : '" + originalQuery + "' is corrected to '" + correctedQuery
          + "', checking its results...");
      ctx.setCorrectedQuery(correctedQuery);

      try {
        long hits = ctx.getHandler().handleSuggestionHitsRequest(ctx, correctedQuery, componentNames);
        LOG.info("New query : '" + correctedQuery + "' produced " + hits + " hits");

        if (hits > maxOriginalResults) {
          ctx.setCorrectedQueryHits(hits);
          List<String> suggestionsList = new ArrayList<String>();
          suggestionsList.add(correctedQuery);
          Map<String, Long> suggestionsHitCounts = new HashMap<String, Long>();
          suggestionsHitCounts.put(correctedQuery, hits);
          ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName(), suggestionsList);
          ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName() + "_hit_counts", suggestionsHitCounts);
          
          SolrQueryResponse rsp = ctx.getHandler().handleSuggestionResponseRequest(ctx, correctedQuery, getComponentName(), rb);
          rb.rsp.add("spellchecked_response", rsp.getValues().get("response"));
          rb.rsp.add("spellchecked_facet_counts", rsp.getValues().get("facet_counts"));
          rb.rsp.add("spellchecked_terms", rsp.getValues().get("terms"));
          rb.rsp.add("spellchecked_termVectors", rsp.getValues().get("termVectors"));
          rb.rsp.add("spellchecked_highlighting", rsp.getValues().get("highlighting"));
          rb.rsp.add("spellchecked_stats", rsp.getValues().get("stats"));
          rb.rsp.add("spellchecked_grouped", rsp.getValues().get("grouped"));
          return true;
        }
      } catch (Exception e) {
        String msg = "Error while correcting original query '" + originalQuery + "' to query '" + correctedQuery
            + "' and searching for results!";
        LOG.error(msg, e);
        throw new RuntimeException(msg, e);
      }
    }

    return false;
  }

  protected abstract String getComponentName();

  protected abstract boolean checkComponentShouldProcess(ResponseBuilder rb);

  protected abstract void doProcess(ReSearcherRequestContext ctx, ResponseBuilder rb) throws Exception;

  protected abstract String getBestSuggestionResultsTagName();

  protected abstract String getSuggestionsTagName();

  @SuppressWarnings("unchecked")
  protected void performHighlighting(ReSearcherRequestContext ctx, ResponseBuilder rb) {
    // check if highlighting is needed
    SolrParams params = rb.req.getParams();
    String highlightRemoved = params.get(RES_HIGHLIGHT_REMOVED_TAG_PARAM_NAME);
    String highlightReplaced = params.get(RES_HIGHLIGHT_REPLACED_TAG_PARAM_NAME);
    boolean ignoreQuotes = params.get(RES_HIGHLIGHT_IGNORE_QUOTES_PARAM_NAME, "true").equals("true") ? true : false;

    if (highlightRemoved == null && highlightReplaced == null) {
      // nothing to do, so just return;
      return;
    }

    // else, fetch suggestions produced by the component and perform highlighting
    List<String> sugs = (List<String>) rb.rsp.getValues().get(getSuggestionsTagName());
    if (sugs == null) {
      // if component didn't generate suggestions, there is nothing to highlight
      return;
    }

    List<String> sugsHighlighted = new ArrayList<String>();

    for (String sug : sugs) {
      sugsHighlighted.add(CorrectionHighlighter.highlightCorrections(ctx.getOriginalQueryString(), sug,
          highlightRemoved, highlightReplaced, ignoreQuotes));
    }

    rb.rsp.getValues().add(getSuggestionsTagName() + "_highlighted", sugsHighlighted);
  }

  @SuppressWarnings("rawtypes")
  protected int getInt(NamedList args, String name) {
    Integer value = (Integer) args.get(name);
    if (value != null) {
      return value.intValue();
    } else {
      return 0;
    }
  }

  @SuppressWarnings("rawtypes")
  protected float getFloat(NamedList args, String name) {
    Float value = (Float) args.get(name);
    if (value != null) {
      return value.floatValue();
    } else {
      return 0;
    }
  }

  @SuppressWarnings("rawtypes")
  protected boolean getBoolean(NamedList args, String name) {
    Boolean value = (Boolean) args.get(name);
    if (value != null) {
      return value.booleanValue();
    } else {
      return false;
    }
  }
  
  public SolrCore getCore() {
    return core;
  }
  
  public List<SearchComponent> getQueryOnlyComponents() {
    return queryOnlyComponents;
  }
  
  public ShardHandlerFactory getShardHandlerFactory() {
    return shardHandlerFactory;
  }
  
  public Set<String> getComponentNames() {
    return componentNames;
  }
  
  public int getMaxOriginalResults() {
    return maxOriginalResults;
  }
}
