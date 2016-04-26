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
 * Relax mm param for long query
 *
 * @author sematext, http://www.sematext.com/
 */
public class MMRelaxerSuggestion extends RelaxerSuggestion{
  private String relaxedMM;
  
  public MMRelaxerSuggestion(String relaxedMM) {
    this.relaxedMM = relaxedMM;
  }
  
  public String getRelaxedMM() {
    return relaxedMM;
  }
  
  @Override
  public String toString() {
    return "MMRelaxerSuggestion(" + relaxedMM + ")";
  }
}
