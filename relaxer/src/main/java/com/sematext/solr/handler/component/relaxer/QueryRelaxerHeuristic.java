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
import org.apache.solr.request.SolrQueryRequest;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 
 * Abstract Query Relaxer Heuristic
 * 
 * @author sematext, http://www.sematext.com/
 */
public abstract class QueryRelaxerHeuristic {
  private Map<Pattern, Analyzer> fieldAnalyzerMaps = null;

  /**
   * Returns a set of suggestions. If no suggestions were created, it should return null or empty set.
   * 
   * @param req originalQueryHits
   * @return .
   */
  public abstract Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req);

  public Map<Pattern, Analyzer> getFieldAnalyzerMaps() {
    return fieldAnalyzerMaps;
  }

  public void setFieldAnalyzerMaps(Map<Pattern, Analyzer> fieldAnalyzerMaps) {
    this.fieldAnalyzerMaps = fieldAnalyzerMaps;
  }

}
