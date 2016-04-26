/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer;

import java.util.Comparator;

/**
 * 
 * Comparator for suggestion using number of hit.
 * 
 * @author sematext, http://www.sematext.com/
 */
public class RelaxerSuggestionComparator implements Comparator<RelaxerSuggestion> {
  private boolean asc = true;

  public RelaxerSuggestionComparator() {
  }

  public RelaxerSuggestionComparator(boolean asc) {
    this.asc = asc;
  }

  @Override
  public int compare(RelaxerSuggestion s1, RelaxerSuggestion s2) {
    long result = s2.getNumHits() - s1.getNumHits();

    if (result == 0) {
      // if they have equal number of hits, we still have to order them (so one of them is not overridden)
      result = s1.getRelaxedQuery().compareTo(s2.getRelaxedQuery());
    }

    if (asc) {
      return (result > 0 ? -1 : (result < 0 ? 1 : 0));
    } else {
      return (int)result;
    }
  }

}

