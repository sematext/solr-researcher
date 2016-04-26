/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics;

import static org.junit.Assert.assertEquals;
import java.util.Set;

import com.sematext.solr.handler.component.relaxer.QueryRelaxerSuggestion;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

/**
 * 
 * Unit test util share by Heuristic Unit Test
 * 
 * @author sematext, http://www.sematext.com/
 */
public final class Utils {
  private Utils() {
  }

  public static void verify(Set<RelaxerSuggestion> sugs, String... token) {
    // test suggestions are good
    Object[] suggestions = sugs.toArray();
    assertEquals(suggestions.length, token.length);
    for (int i = 0; i < suggestions.length; i++) {
      assertEquals(token[i], ((QueryRelaxerSuggestion) suggestions[i]).getRelaxedQuery());
    }
  }
}
