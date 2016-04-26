/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer;


/**
 * 
 * Relaxed user query representation
 *
 * @author sematext, http://www.sematext.com/
 */
public class QueryRelaxerSuggestion extends RelaxerSuggestion {

  public static enum FewerMatchesType {
    TRUE(new RelaxerSuggestionComparator(true)), FALSE(new RelaxerSuggestionComparator(false));

    private RelaxerSuggestionComparator comparator;

    private FewerMatchesType(RelaxerSuggestionComparator comparator) {
      this.comparator = comparator;
    }

    public RelaxerSuggestionComparator getComparator() {
      return comparator;
    }

    public static FewerMatchesType fromString(String sort) {
      if ("true".equalsIgnoreCase(sort)) {
        return TRUE;
      } else {
        return FALSE;
      }
    }
  }

  private String userQuery;
  private String relaxedUserQuery;
  private String relaxedHighlightQuery;

  public QueryRelaxerSuggestion(String relaxedQuery, String userQuery, String relaxedUserQuery) {
    this.userQuery = userQuery;
    setRelaxedQuery(relaxedQuery);
    this.relaxedUserQuery = relaxedUserQuery;
  }
  
  public String getRelaxedUserQuery() {
    return relaxedUserQuery;
  }

  public String getUserQuery() {
    return userQuery;
  }
  
  public String getRelaxedHighlightQuery() {
    return relaxedHighlightQuery;
  }
  
  public void setRelaxedHighlightQuery(String relaxedHighlightQuery) {
    this.relaxedHighlightQuery = relaxedHighlightQuery;
  }
  
  public String toString() {
    return "(" + relaxedUserQuery + "," + getNumHits() + ")";
  }
}
