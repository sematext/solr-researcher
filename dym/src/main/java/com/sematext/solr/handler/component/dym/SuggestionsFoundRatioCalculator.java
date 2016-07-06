/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

import org.apache.solr.common.util.NamedList;

/**
 * Contains logic and data for "(suggestion found) / (suggestions requested) ratio" logic.
 * 
 */
public class SuggestionsFoundRatioCalculator {
  private float minRequiredSuggestionRatio;
  private float minRequiredSuggestionRatioForZeroHits;
  
  public SuggestionsFoundRatioCalculator(float minRequiredSuggestionRatioForZeroHitsParam, float minRequiredSuggestionRatioParam) {
    minRequiredSuggestionRatio = minRequiredSuggestionRatioParam;
    minRequiredSuggestionRatioForZeroHits = minRequiredSuggestionRatioForZeroHitsParam;
    
  }
  
  public boolean isRatioAcceptable(NamedList<Object> sug, long originalQueryHits, int spellcheckCount) {
    float ratio = calculateRatio(sug, spellcheckCount);
    float ratioToCheck = 0.0f;
    
    if (originalQueryHits == 0) {
      ratioToCheck = minRequiredSuggestionRatioForZeroHits;
    }
    else {
      ratioToCheck = minRequiredSuggestionRatio;
    }
    
    if (ratio >= ratioToCheck) {
      return true;
    }
    
    return false;
  }
  
  public static float calculateRatio(NamedList<Object> sug, int spellcheckCount) {
    int numFound = ((Integer) sug.get("numFound")).intValue();
    return ((float) numFound) / ((float) spellcheckCount);
  }

  public float getMinRequiredSuggestionRatio() {
    return minRequiredSuggestionRatio;
  }

  public float getMinRequiredSuggestionRatioForZeroHits() {
    return minRequiredSuggestionRatioForZeroHits;
  }
}