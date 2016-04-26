/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

public class SortableSuggestion implements Comparable<SortableSuggestion> {
  private String suggestion;
  private long numHits;
  private float distance;
  private int sortableScore;
  
  public SortableSuggestion(String suggestion, long numHits, float distance) {
    this.suggestion = suggestion;
    this.numHits = numHits;
    this.distance = distance;
    
    this.sortableScore = (int) ( (Math.log((double) numHits)) * Math.pow(distance, 2.0d));
  }
  
  @Override
  public int compareTo(SortableSuggestion that) {
      int scoreDifference = that.sortableScore - this.sortableScore;
      
      if (scoreDifference == 0) {
        // if they have equal score, we still have to order them (so one of them is not overriden)
        return this.getSuggestion().compareTo(that.getSuggestion());
      }
    
      return scoreDifference;
  }

  public String getSuggestion() {
      return suggestion;
  }

  public long getNumHits() {
      return numHits;
  }
}