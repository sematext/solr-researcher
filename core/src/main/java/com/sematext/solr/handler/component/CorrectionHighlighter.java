/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which knows how to mark (highlight) parts of queries as changed or unchanged.
 *
 * @author sematext, http://www.sematext.com/
 */
public class CorrectionHighlighter {

  /**
   * Returns highlighted version of corrected query. In case this method should highlight removed parts of original
   * query, parameter highlightingTagRemoved should be provided (with value equal to tag which should be used for
   * highlighting, for instance String "b"). The same applies for replaced words, but parameter for that is
   * highlightingTagReplaced. In case removed quotes shouldn't be highlighted, parameter ignoreRemovedQuotes should be
   * set to true (this is the most common case). Otherwise, set it to false.
   * 
   * @param originalQuery
   * @param correctedQuery
   * @param highlightingTagRemoved
   * @param highlightingTagReplaced
   * @param ignoreRemovedQuotes
   * @return
   */
  public static String highlightCorrections(String originalQuery, String correctedQuery, String highlightingTagRemoved, String highlightingTagReplaced, boolean ignoreRemovedQuotes) {
    if (originalQuery == null || correctedQuery == null || originalQuery.trim().equals("") || correctedQuery.trim().equals("")) {
      // this should never happen, but we don't want an exception in this code; incorrect higlighting is less of a problem than
      // runtime exceptions
      return correctedQuery;
    }
    
    List<String> originalQueryTokens = new ArrayList<String>();
    List<String> correctedQueryTokens = new ArrayList<String>();
    ReSearcherUtils.tokenizeQueryString(originalQuery, originalQueryTokens);
    ReSearcherUtils.tokenizeQueryString(correctedQuery, correctedQueryTokens);

    int j = 0;
    String highlightedQuery = "";
    
    for (int i = 0; i < correctedQueryTokens.size(); i++) {
      String nextTokenOrig = originalQueryTokens.get(j);
      String nextTokenCorr = correctedQueryTokens.get(i);
      
      while (nextTokenOrig.equals("\"") && ignoreRemovedQuotes) {
        j++;
        nextTokenOrig = originalQueryTokens.get(j);
      }
      
      if (nextTokenCorr.equalsIgnoreCase(nextTokenOrig)) {
        // this words are the same, so just write it to highlighted version
        // TODO what if it is a quote?
        highlightedQuery += nextTokenCorr + " ";
      }
      else {
        // TODO take care of the quotes!!! they can be the difference
        
        // check next words from orig, if any of them matches corr, then just highlight the words between
        int k = j + 1;
        boolean wordFound = false;
        for (; k < originalQueryTokens.size(); k++) {
          if (originalQueryTokens.get(k).equalsIgnoreCase(nextTokenCorr)) {
            // we found it
            wordFound = true;
            break;
          }
        }
        
        if (wordFound) {
          boolean tagOpened = false;
          boolean tagClosed = false;
          
          // if we have to mark removed words...
          if (highlightingTagRemoved != null) {
            for (int l = j; l < k; l++) {
              if (originalQueryTokens.get(l).equals("\"") && ignoreRemovedQuotes == true) {
                ;
              }
              else {
                if (tagOpened == false) {
                  highlightedQuery += "<" + highlightingTagRemoved + ">";
                  tagOpened = true;
                }
                
                // mark all words from orig query as removed until found word
                 highlightedQuery += originalQueryTokens.get(l);
                 
                 if (l == k - 1) {
                   // if this is the last word to be replaced
                   highlightedQuery += "</" + highlightingTagRemoved + "> ";
                   tagClosed = true;
                 }
                 else {
                   // if ending tag isn't needed yet, just put space
                   
                   // if this is a quote and we shouldn't ignore quote, then the quote was already added; blank
                   // after that quote isn't needed
                   if (originalQueryTokens.get(l).equals("\"") && ignoreRemovedQuotes == false) {
                     ;
                   }
                   else {
                     highlightedQuery += " ";
                   }
                 }
              }
            }
            
            if (tagOpened == true && tagClosed == false) {
              // there is a blank at the end, so remove it first
              highlightedQuery = highlightedQuery.substring(0, highlightedQuery.length() - 1);
              highlightedQuery += "</" + highlightingTagRemoved + "> ";
            }
          }
          
          // also add current word from corrected query, since we're moving to the next token
          highlightedQuery += nextTokenCorr + " ";

          // and move orig cursor
          j = k;
        }
        else {
          // if the word wasn't found, that means that current word from corr replaces current word from orig
          if (highlightingTagReplaced != null) {
            highlightedQuery += "<" + highlightingTagReplaced + ">" + nextTokenCorr + "</" + highlightingTagReplaced + "> ";
          }
        }
      }
      
      j++;
    }
    
    // we're finished with tokens from corrected query, but maybe there are some tokens left in the original query;
    // all such tokens should be marked as removed; but only if client wants them marked
    // BEWARE that j is now equal to i + 1 (because of j++ at the end of for loop)
    if (highlightingTagRemoved != null && (j < originalQueryTokens.size())) {
      boolean tagOpened = false;
      boolean tagClosed = false;

      for (int k = j; k < originalQueryTokens.size(); k++) {
        if (originalQueryTokens.get(k).equals("\"") && ignoreRemovedQuotes == true) {
          ;
        }
        else {
          if (tagOpened == false) {
            highlightedQuery += "<" + highlightingTagRemoved + ">";
            tagOpened = true;
          }

         highlightedQuery += originalQueryTokens.get(k);
          
          if (k == originalQueryTokens.size() - 1) {
            // if this is the last word to be replaced
            highlightedQuery += "</" + highlightingTagRemoved + "> ";
            tagClosed = true;
          }
          else {
            // if ending tag isn't needed yet, just put space
            highlightedQuery += " ";
          }
        }
      }
      
      if (tagOpened == true && tagClosed == false) {
        // there is a blank at the end, so remove it first
        highlightedQuery = highlightedQuery.substring(0, highlightedQuery.length() - 1);
        highlightedQuery += "</" + highlightingTagRemoved + "> ";
      }
    }
    
    return highlightedQuery.trim();
  }
}