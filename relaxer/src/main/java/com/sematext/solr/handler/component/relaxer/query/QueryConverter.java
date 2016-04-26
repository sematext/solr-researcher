/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.request.SolrQueryRequest;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 
 * FIXME: add descriptive class javadoc
 * 
 * @author sematext, http://www.sematext.com/
 */
public abstract class QueryConverter {
  private Map<Pattern, Analyzer> fieldAnalyzerMaps = null;

  public abstract List<Clause> convert(String query, SolrQueryRequest req);

  public Map<Pattern, Analyzer> getFieldAnalyzerMaps() {
    return fieldAnalyzerMaps;
  }

  public void setFieldAnalyzerMaps(Map<Pattern, Analyzer> fieldAnalyzerMaps) {
    this.fieldAnalyzerMaps = fieldAnalyzerMaps;
  }

}
