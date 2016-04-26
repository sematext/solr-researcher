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

import com.sematext.solr.handler.component.SpellcheckerSuggestionProcessor;
import com.sematext.solr.handler.component.dym.SuggestionsFoundRatioCalculator;

public class FindMisspelingProcessor implements SpellcheckerSuggestionProcessor {
  private long originalQueryHits;
  private int spellcheckCount;
  private SuggestionsFoundRatioCalculator ratioCalculator;
  private Float highestRatio;
  private String misspellingWithHighestRatio;
  private String misspelling;
  private int countOfMisspellings;

  public FindMisspelingProcessor(long originalQueryHits, int spellcheckCount, SuggestionsFoundRatioCalculator ratioCalculator, Float highestRatio) {
    this.originalQueryHits = originalQueryHits;
    this.spellcheckCount = spellcheckCount;
    this.ratioCalculator = ratioCalculator;
    this.highestRatio = highestRatio;
  }

  @Override
  public void afterProcessingFinished() {
    // TODO Auto-generated method stub

  }

  @Override
  public void process(NamedList wordData, String wordName) {
    if (ratioCalculator.isRatioAcceptable(wordData, originalQueryHits, spellcheckCount)) {
      float thisRatio = SuggestionsFoundRatioCalculator.calculateRatio(wordData, spellcheckCount);
      // System.out.println("Suggestion, ratio :" + suggestions.getName(i) + ", " + thisRatio);
      
      if (thisRatio > highestRatio) {
        highestRatio = thisRatio;
        misspellingWithHighestRatio = wordName;
      }

      if (misspelling == null) {
        if (originalQueryHits == 0 && thisRatio >= ratioCalculator.getMinRequiredSuggestionRatioForZeroHits()) {
          // we want to remember just the first misspelling, but also loop through all possible suggestions
          misspelling = wordName;
        }
        else if (originalQueryHits > 0 && thisRatio >= ratioCalculator.getMinRequiredSuggestionRatio()) {
          // we want to remember just the first misspelling, but also loop through all possible suggestions
          misspelling = wordName;
        }
      }

      countOfMisspellings++;
    }
  }

  public String getMisspellingWithHighestRatio() {
    return misspellingWithHighestRatio;
  }

  public String getMisspelling() {
    return misspelling;
  }

  public int getCountOfMisspellings() {
    return countOfMisspellings;
  }

  public Float getHighestRatio() {
    return highestRatio;
  }
}