/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer;


public abstract class RelaxerSuggestion {
  private long numHits;
  private String relaxedQuery;

  public long getNumHits() {
    return numHits;
  }

  public void setNumHits(long numHits) {
    this.numHits = numHits;
  }

  public String getRelaxedQuery() {
    return relaxedQuery;
  }

  public void setRelaxedQuery(String relaxedQuery) {
    this.relaxedQuery = relaxedQuery;
  }

}
