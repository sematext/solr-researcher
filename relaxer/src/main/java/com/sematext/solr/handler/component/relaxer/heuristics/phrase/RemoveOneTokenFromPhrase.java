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
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sematext.solr.handler.component.ReSearcherUtils;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

/**
 * Relaxer heuristic for phrase queries which works similar to RemoveOneToken heuristic on regular queries. It will produce
 * multiple suggestions from combinations where just one term was removed from original query (quotes will be left intact).
 * 
 *
 * @author sematext, http://www.sematext.com/
 */
public class RemoveOneTokenFromPhrase extends QueryRelaxerHeuristic {

  @Override
  public Set<RelaxerSuggestion> createSuggestions(SolrQueryRequest req) {
    List<String> tokens = new ArrayList<String>();
    int countOfQuotes = 0;
    
    SolrParams params = req.getParams();
    String userQuery = params.get(CommonParams.Q);
    countOfQuotes = ReSearcherUtils.tokenizeQueryString(userQuery, tokens);
    
    if ((tokens.size() - countOfQuotes) <= 1) {
      // nothing to offer, there were 1 or 0 terms in the query
      return null;
    } else {
      Set<RelaxerSuggestion> suggestions = new LinkedHashSet<RelaxerSuggestion>();
      
      for (int i = 0; i < tokens.size(); i++) {
        StringBuilder sug = new StringBuilder();
        
        if (tokens.get(i).equals("\"")) {
          // quotes will not be treated as a separate term
          continue;
        }
        
        int startingQuoteOpened = 0;
        boolean startingQuoteOmitted = false;

        for (int j = 0; j < tokens.size(); j++) {
          
          if (tokens.get(j).equals("\"")) {
            startingQuoteOpened++;
            
            if ((startingQuoteOpened % 2) == 1) {
              // if this was starting quote, we have to look for ending quote and compare

              if ((tokens.size() > (j + 3)) && (tokens.get(j + 3).equals("\"")) && ((j + 1 == i) || (j + 2 == i))) {
                // if there are just two terms under this quote, and we are currently removing one of them, no need to add quotes
                startingQuoteOmitted = true;
              }
              else if ((tokens.size() > (j + 2)) && (tokens.get(j + 2).equals("\"")) && ((j + 1 == i))) {
                // if there is just one terms under this quote, and we are currently removing it, no need to add quotes
                startingQuoteOmitted = true;
              }
              else {
                sug.append("\"");
              }
            }
            else {
              // this was ending quote, so we have to check if we should write it or not
              if (startingQuoteOmitted == false) {
                // since all tokens append blank to the end, if we add quote, it should replace that blank
                sug.setCharAt(sug.length() - 1, '\"');
                sug.append(" ");
              }
              
              // reset the flag in any case
              startingQuoteOmitted = false;
            }
          }
          else if (j != i) {
            sug.append(tokens.get(j).trim());
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