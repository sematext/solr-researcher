/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.web;

import java.util.List;

public class SpellcheckerResults {
  private String bestSuggestion;
  private List<String> suggestions;
  private List<ResultRow> suggestionRows;
  private long spellcheckedHits;
  
  public List<String> getSuggestions() {
    return suggestions;
  }
  public void setSuggestions(List<String> suggestions) {
    this.suggestions = suggestions;
  }
  public List<ResultRow> getSuggestionRows() {
    return suggestionRows;
  }
  public void setSuggestionRows(List<ResultRow> suggestionRows) {
    this.suggestionRows = suggestionRows;
  }
  public long getSpellcheckedHits() {
    return spellcheckedHits;
  }
  public void setSpellcheckedHits(long spellcheckedHits) {
    this.spellcheckedHits = spellcheckedHits;
  }
  public String getBestSuggestion() {
    return bestSuggestion;
  }
  public void setBestSuggestion(String bestSuggestion) {
    this.bestSuggestion = bestSuggestion;
  }
}