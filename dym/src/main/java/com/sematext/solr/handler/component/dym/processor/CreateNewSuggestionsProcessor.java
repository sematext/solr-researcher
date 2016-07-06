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

import java.util.LinkedHashSet;
import java.util.Set;

public class CreateNewSuggestionsProcessor implements SpellcheckerSuggestionProcessor {
  private String originalQuery;
  private int wordStartOffset;
  private int wordEndOffset;
  private Set<String> newSuggestions = new LinkedHashSet<String>();
  
  public CreateNewSuggestionsProcessor(String originalQueryParam, NamedList<Object> suggestionForWord) {
    originalQuery = originalQueryParam;
    wordEndOffset = (Integer) suggestionForWord.get("endOffset");
    wordStartOffset = (Integer) suggestionForWord.get("startOffset");    
  }

  @Override
  public void afterProcessingFinished() {
    // empty method, nothing to do here
  }

  @Override
  public void process(NamedList<Object> wordData, String wordName) {
    String word = (String) wordData.get("word");

    StringBuilder collation = new StringBuilder(originalQuery);
    collation.replace(wordStartOffset, wordEndOffset, word);

    String collVal = collation.toString();
    if (collVal.equals(originalQuery) == false) {
        newSuggestions.add(collVal);
    }
  }

  public Set<String> getNewSuggestions() {
    return newSuggestions;
  }
}