/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sematext.solr.handler.component.CorrectionHighlighter;

public class TestCorrectionHighlighter {

  @Test
  public void testHighlightCorrections() {
    assertEquals("some query", CorrectionHighlighter.highlightCorrections("some query", "some query", "i", "b", true));
    assertEquals("some <i>test</i> query", CorrectionHighlighter.highlightCorrections("some test query", "some query", "i", "b", true));
    assertEquals("some test <i>query</i>", CorrectionHighlighter.highlightCorrections("some test query", "some test", "i", "b", true));
    assertEquals("<i>some</i> test query", CorrectionHighlighter.highlightCorrections("some test query", "test query", "i", "b", true));
    assertEquals("some <b>text</b> query", CorrectionHighlighter.highlightCorrections("some test query", "some text query", "i", "b", true));
    assertEquals("some test <b>quest</b>", CorrectionHighlighter.highlightCorrections("some test query", "some test quest", "i", "b", true));
    assertEquals("<b>same</b> test query", CorrectionHighlighter.highlightCorrections("some test query", "same test query", "i", "b", true));
    
    // now more complex corrections
    assertEquals("some <i>test query</i>", CorrectionHighlighter.highlightCorrections("some test query", "some", "i", "b", true));
    assertEquals("some <i>test query</i> bla", CorrectionHighlighter.highlightCorrections("some test query bla", "some bla", "i", "b", true));  
    // assertEquals("some <i>test query</i> <b>good</b> solr", CorrectionHighlighter.highlightCorrections("some test query goot solr", "some good solr", "i", "b", true));
    
    // few phrase corrections
    assertEquals("some test query bla", CorrectionHighlighter.highlightCorrections("some \"test query\" bla", "some test query bla", "i", "b", true));
    assertEquals("some test query bla", CorrectionHighlighter.highlightCorrections("some test \"query\" bla", "some test query bla", "i", "b", true));
    assertEquals("some test query bla", CorrectionHighlighter.highlightCorrections("some test query \"bla\"", "some test query bla", "i", "b", true));
    assertEquals("some test query bla", CorrectionHighlighter.highlightCorrections("\"some\" test query bla", "some test query bla", "i", "b", true));

    // some queries with everything mixed : quotes, removed and replaced words
    assertEquals("some test query <b>blog</b>", CorrectionHighlighter.highlightCorrections("some \"test query\" bla", "some test query blog", "i", "b", true));
    assertEquals("some <i>test</i> query <b>blog</b>", CorrectionHighlighter.highlightCorrections("some \"test query\" bla", "some query blog", "i", "b", true));
    
    // with ignoreRemovedQuotes = false
    assertEquals("some <i>\"test</i> query <i>\"</i> bla", CorrectionHighlighter.highlightCorrections("some \"test query\" bla", "some query bla", "i", "b", false));
    assertEquals("some <i>\"test</i> query <i>\"</i> blog <b>blog</b>", CorrectionHighlighter.highlightCorrections("some \"test query\" blog bla", "some query blog blog", "i", "b", false));
  }  
}