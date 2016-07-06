/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.solr.common.util.NamedList;

/**
 * Implement this class with code which should process original Solr spellchecker's suggestions. 
 * 
 * For instance, instance
 * of this class could have static properties which describe best match, frequency etc. Since method process will be
 * invoked for each suggestion, you can compare each suggestion with others and possibly create resulting list with
 * acceptable suggestions.
 * 
 * Instances of this class are intended to be used with method ReSearcherUtils.iterateOverSpellcheckerSuggestionsForWord().
 *
 * @author sematext, http://www.sematext.com/
 */
public interface SpellcheckerSuggestionProcessor {
  /**
   * Method which will be invoked on each suggestion.
   * 
   * @param wordData - data about some suggestion, for instance, by using wordData.get("frequency"), frequency information
   * @param nameValue - name of node for which data wordData is provided (for instance, when iterating over all
   *                    incorrect words, nameValue will contain name of incorrect word)
   * can be extracted
   */
  void process(NamedList<Object> wordData, String nameValue);
  
  /**
   * Provides a to perform some logic after processing of all suggestions was finished.
   */
  void afterProcessingFinished();
}
