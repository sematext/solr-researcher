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

public class FindTopFrequencyProcessor implements SpellcheckerSuggestionProcessor {
  private double topFrequency = 0.0d;

  @Override
  @SuppressWarnings("rawtypes")
  public void process(NamedList wordData, String wordName) {
    double sugFreq = Math.sqrt(((Integer) wordData.get("freq")).doubleValue());
    if (sugFreq > topFrequency) {
      topFrequency = sugFreq;
    }
  }

  public double getTopFrequency() {
    return topFrequency;
  }

  @Override
  public void afterProcessingFinished() {
    // empty implementation since nothing needs to be done
  }
}