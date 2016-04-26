/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

public class ScoredSuggestion implements Comparable<ScoredSuggestion> {
  String sug;
  double score;
  float distance;
  
  public ScoredSuggestion(String sug, double score, float distance) {
      this.sug = sug;
      this.score = score;
      this.distance = distance;
  }
  
  public int hashCode() {
      return sug.hashCode();
  }
  
  public boolean equals(Object obj) {
      if (obj == null) return false;
      if (obj instanceof ScoredSuggestion == false) return false;
      ScoredSuggestion otherSug = (ScoredSuggestion) obj;
      return this.sug.equals(otherSug.sug);
  }
  
  public int compareTo(ScoredSuggestion otherSS) {
    double diff = this.score - otherSS.score; 
      if (diff > 0)
          return -1;
      else if (diff < 0)
          return 1;
      else
          return 0;
  }
  
  public String toString() { return "(" + sug + "," + score + ")"; }
}