/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym.processor;

import org.apache.solr.common.util.NamedList;

import com.sematext.solr.handler.component.SpellcheckerSuggestionProcessor;
import com.sematext.solr.handler.component.dym.SuggestionsFoundRatioCalculator;

public class SuggestionRatioProcessor implements SpellcheckerSuggestionProcessor {
  private long originalQueryHits;
  private int spellcheckCount;
  private SuggestionsFoundRatioCalculator ratioCalculator;
  private Float highestRatio;
  private NamedList<Object> suggestionWithHighestRatio;
  private boolean stopIterating = false;
  
  public SuggestionRatioProcessor(long originalQueryHits, int spellcheckCount, SuggestionsFoundRatioCalculator ratioCalculator, Float highestRatio) {
    this.originalQueryHits = originalQueryHits;
    this.spellcheckCount = spellcheckCount;
    this.ratioCalculator = ratioCalculator;
    this.highestRatio = highestRatio;
  }
  
  @Override
  public void afterProcessingFinished() {
    // empty
  }

  @Override
  public void process(NamedList<Object> wordData, String wordName) {
    if (stopIterating) {
      return;
    }
    
    if (ratioCalculator.isRatioAcceptable(wordData, originalQueryHits, spellcheckCount)) {
      if (highestRatio != null) {
        // if we have it, use highestRatio criterion
        if (SuggestionsFoundRatioCalculator.calculateRatio(wordData, spellcheckCount) >= highestRatio.floatValue()) {
          suggestionWithHighestRatio = wordData;
          stopIterating = true;
          return;
        }
        else {
        }
      }
      else {
        suggestionWithHighestRatio = wordData;
        stopIterating = true;
        return;
      }
    }
  }

  public NamedList<Object> getSuggestionWithHighestRatio() {
    return suggestionWithHighestRatio;
  }
}