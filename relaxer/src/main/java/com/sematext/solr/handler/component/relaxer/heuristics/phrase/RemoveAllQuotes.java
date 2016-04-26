/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics.phrase;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.request.SolrQueryRequest;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

public class RemoveAllQuotes extends QueryRelaxerHeuristic {

  @Override
  public Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req) {
    Set<RelaxerSuggestion> suggestions = new LinkedHashSet<RelaxerSuggestion>();

    String userQuery = req.getParams().get(CommonParams.Q);
    String relaxedUserQuery = userQuery.replace('"', ' ').trim();
    suggestions.add(new QueryRelaxerSuggestion(relaxedUserQuery, userQuery, relaxedUserQuery));

    return suggestions;
  }
}
