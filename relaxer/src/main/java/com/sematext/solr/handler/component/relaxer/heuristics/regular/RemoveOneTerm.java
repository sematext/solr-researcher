/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics.regular;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.request.SolrQueryRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sematext.solr.handler.component.ReSearcherUtils;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

public class RemoveOneTerm extends QueryRelaxerHeuristic {

  @Override
  public Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req) {
    List<String> words = new ArrayList<String>();

    String userQuery = req.getParams().get(CommonParams.Q);
    ReSearcherUtils.tokenizeQueryString(userQuery, words);

    if (words.size() <= 1) {
      // nothing to offer
      return null;
    } else {
      Set<RelaxerSuggestion> suggestions = new LinkedHashSet<RelaxerSuggestion>();

      for (int i = 0; i < words.size(); i++) {
        StringBuilder sug = new StringBuilder();

        for (int j = 0; j < words.size(); j++) {
          if (j != i) {
            sug.append(words.get(j).trim());
            sug.append(" ");
          }
        }

        String relaxedUserQuery = sug.toString().trim();
        suggestions.add(new QueryRelaxerSuggestion(relaxedUserQuery, userQuery, relaxedUserQuery));
      }

      return suggestions;
    }
  }
}
