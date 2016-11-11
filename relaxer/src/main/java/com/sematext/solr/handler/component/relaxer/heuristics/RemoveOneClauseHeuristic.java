/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics;

import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sematext.solr.handler.component.relaxer.MMRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.heuristics.regular.RemoveOneTerm;
import com.sematext.solr.handler.component.relaxer.query.Clause;
import com.sematext.solr.handler.component.relaxer.query.EdismaxQueryConverter;
import com.sematext.solr.handler.component.relaxer.query.QueryConverter;
import com.sematext.solr.handler.component.relaxer.query.RegexExtractor;
import com.sematext.solr.handler.component.relaxer.query.RelaxerParams;
import com.sematext.solr.handler.component.relaxer.query.UserQueryExtractor;

/**
 * 
 * Relaxer heuristic which works similar to {@link RemoveOneTerm} heuristic on regular queries. 
 * It will consider a phrase or term as a clause. This heuristic also understands Boolean operators
 * AND, OR, NOT.
 *
 * @author sematext, http://www.sematext.com/
 */
public class RemoveOneClauseHeuristic extends QueryRelaxerHeuristic {
  private QueryConverter queryConverter = new EdismaxQueryConverter();
  private UserQueryExtractor userQueryExtractor = new RegexExtractor();

  @Override
  public Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req) {
    SolrParams params = req.getParams();
    int longQueryLength = params.getInt(RelaxerParams.QUERY_RELAXER_LONG_QUERY_TERMS, 5);
    String userQuery = userQueryExtractor.extract(params);
    String highlightQuery = params.get(HighlightParams.Q);

    List<Clause> clauses = queryConverter.convert(userQuery, req);
    int clauseLength = clauseLength(clauses);
    if (clauseLength <= 1) {
      return null;
    } else if (clauseLength >= longQueryLength) {
      Set<RelaxerSuggestion> suggestions = new LinkedHashSet<RelaxerSuggestion>();
      String relaxedMM = req.getParams().get(RelaxerParams.QUERY_RELAXER_LONG_QUERY_MM);
      if(relaxedMM != null) {
        MMRelaxerSuggestion suggestion = new MMRelaxerSuggestion(relaxedMM);
        String relaxedQuery = userQueryExtractor.relaxMM(params, relaxedMM);
        suggestion.setRelaxedQuery(relaxedQuery);
        suggestions.add(suggestion);
      }
      return suggestions;
    } else {
      Set<RelaxerSuggestion> suggestions = new LinkedHashSet<RelaxerSuggestion>();

      for (int i = 0; i < clauses.size(); i++) {
        Clause clauseToRemove = clauses.get(i);
        boolean multipleTokenClause = clauseToRemove.getTokens() != null && clauseToRemove.getTokens().length > 1;

        String text = clauseToRemove.getRaw();
        if ("NOT".equals(text) || "AND".equals(text) || "OR".equals(text)) {
          continue;
        }

        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        
        for (int j = 0; j < i - 1; j++) {
          before.append(clauses.get(j).getRaw());
          before.append(" ");
        }

        if (i >= 1) {
          String raw = clauses.get(i - 1).getRaw();
          if (multipleTokenClause || (!"NOT".equals(raw) && !"AND".equals(raw) && !"OR".equals(raw))) {
            before.append(raw);
            before.append(" ");
          }
        }
        
        if (i < clauses.size() - 1) {
          String raw = clauses.get(i + 1).getRaw();
          if (multipleTokenClause || (!"AND".equals(raw) && !"OR".equals(raw))) {
            after.append(raw);
            after.append(" ");
          }
        }
        for (int j = i + 2; j < clauses.size(); j++) {
          after.append(clauses.get(j).getRaw());
          after.append(" ");
        }
        
        if (multipleTokenClause) {
          for (int k = 0; k < clauseToRemove.getTokens().length; k++ ) {
            StringBuilder sug = new StringBuilder(before.toString());
            if (clauseToRemove.getMust() == '+' || clauseToRemove.getMust() == '-') {
              sug.append(clauseToRemove.getMust());
            }
            if (clauseToRemove.getField() != null) {
              sug.append(clauseToRemove.getField());
              sug.append(":");
            }
            
            for (int t = 0; t < clauseToRemove.getTokens().length; t++) {
              if (t != k) {
                sug.append(clauseToRemove.getTokens()[t]);
              }
            }
            sug.append(" ");
            sug.append(after.toString());
            
            String relaxedUserQuery = sug.toString().trim();
            String relaxedQuery = userQueryExtractor.relaxQuery(params, userQuery, relaxedUserQuery);
            QueryRelaxerSuggestion suggestion = new QueryRelaxerSuggestion(relaxedQuery, userQuery, relaxedUserQuery);
            if (highlightQuery != null) {
              String relaxedHighlightQuery = userQueryExtractor.relaxHighlightQuery(highlightQuery, params, highlightQuery, relaxedUserQuery);
              suggestion.setRelaxedHighlightQuery(relaxedHighlightQuery);
            }
            suggestions.add(suggestion);
          }
        } else {
          String relaxedUserQuery = before.append(after.toString()).toString().trim();
          String relaxedQuery = userQueryExtractor.relaxQuery(params, userQuery, relaxedUserQuery);
          QueryRelaxerSuggestion suggestion = new QueryRelaxerSuggestion(relaxedQuery, userQuery, relaxedUserQuery);
          if (highlightQuery != null) {
            String relaxedHighlightQuery = userQueryExtractor.relaxHighlightQuery(highlightQuery, params, highlightQuery, relaxedUserQuery);
            suggestion.setRelaxedHighlightQuery(relaxedHighlightQuery);
          }
          suggestions.add(suggestion);
        }
        
      }

      return suggestions;
    }
  }
  
  private int clauseLength(List<Clause> clauses) {
    int length = 0;
    for(Clause clause : clauses) {
      if (clause.getTokens() == null || clause.isPhrase() ) {
        length++;
      } else {
        length += clause.getTokens().length;
      }
    }
    
    return length;
  }

  @Override
  public void setFieldAnalyzerMaps(Map<Pattern, Analyzer> fieldAnalyzerMaps) {
    super.setFieldAnalyzerMaps(fieldAnalyzerMaps);
    this.queryConverter.setFieldAnalyzerMaps(fieldAnalyzerMaps);
  }
  
}
