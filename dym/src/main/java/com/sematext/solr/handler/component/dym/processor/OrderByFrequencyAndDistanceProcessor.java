/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym.processor;

import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.StringDistance;
import org.apache.solr.common.util.NamedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sematext.solr.handler.component.SpellcheckerSuggestionProcessor;
import com.sematext.solr.handler.component.dym.ScoredSuggestion;

public class OrderByFrequencyAndDistanceProcessor implements SpellcheckerSuggestionProcessor {
  private List<ScoredSuggestion> scoredSugs = new ArrayList<ScoredSuggestion>();
  private Iterator<String> newSuggestionsIterator;
  private boolean ignoreCollation;
  private String misspelling;
  private double topFrequency;
  
  private static final StringDistance DISTANCE = new LevensteinDistance(); // FIXME: make a solrconfig parameter
  private float distCoef = 1.0f; // FIXME: overridable via URL for experimenting?
  private float freqCoef = 0.24f; // FIXME: overridable via URL for experimenting?
  
  public OrderByFrequencyAndDistanceProcessor(Iterator<String> newSuggestionsIteratorParam, boolean ignoreCollationParam, String misspellingParam, double topFrequencyParam) {
    newSuggestionsIterator = newSuggestionsIteratorParam;
    ignoreCollation = ignoreCollationParam;
    misspelling = misspellingParam;
    topFrequency = topFrequencyParam;
  }
  
  @Override
  @SuppressWarnings("rawtypes")
  public void process(NamedList wordData, String wordName) {
    String sugString = newSuggestionsIterator.next();
    double sugFreq = ((Integer) wordData.get("freq")).doubleValue();
    
    float distance = DISTANCE.getDistance(misspelling, (String) wordData.get("word"));
    double distScore = distCoef * distance;
    double freqScore = freqCoef * (Math.sqrt(sugFreq) / topFrequency);
    double score = distScore + freqScore;
    
    // create object with sugString + score and stick it in a collection for sorting
    scoredSugs.add(new ScoredSuggestion(sugString, score, distance));
  }

  @Override
  public void afterProcessingFinished() {
    // collation is suggestion at the end of new suggestions iterator (it wasn't processed with method process() since
    // new suggestions iterator contains one element more than spellchecker's list of suggestions for some word - that
    // suggestion being collation)
    if (newSuggestionsIterator.hasNext()) {
      String sugString = newSuggestionsIterator.next();
      if (sugString == null) return;
      
      if (ignoreCollation == false) {
        scoredSugs.add(new ScoredSuggestion(sugString, 0.35f, DISTANCE.getDistance(misspelling, sugString)));
      }
    }
  }

  public List<ScoredSuggestion> getScoredSugs() {
    return scoredSugs;
  }
}