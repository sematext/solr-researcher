/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics.phrase;

import static com.sematext.solr.handler.component.relaxer.heuristics.Utils.verify;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

@SolrTestCaseJ4.SuppressSSL
public class TestRemoveOneTokenFromPhrase extends SolrTestCaseJ4 {
  private QueryRelaxerHeuristic heuristic = null;

  @BeforeClass
  public static void beforeTests() throws Exception {
    // to run from IDE:
    // initCore("solr/collection1/conf/solrconfig.xml","solr/collection1/conf/schema.xml");
    
    // to build with maven
    initCore("solrconfig.xml","schema.xml");
  }
  
  @Before
  public void setup() {
    heuristic = new RemoveOneTokenFromPhrase();
  }

  @Test
  public void testCreateSuggestions1() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some \"quoted query\" for test"));
    verify(sugs, "\"quoted query\" for test", "some query for test", "some quoted for test",
        "some \"quoted query\" test", "some \"quoted query\" for");
  }

  @Test
  public void testCreateSuggestions2() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some \" quoted query \" for test"));
    verify(sugs, "\"quoted query\" for test", "some query for test", "some quoted for test",
        "some \"quoted query\" test", "some \"quoted query\" for");
  }

  @Test
  public void testCreateSuggestions3() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"some quoted query\" for test"));
    verify(sugs, "\"quoted query\" for test", "\"some query\" for test", "\"some quoted\" for test",
        "\"some quoted query\" test", "\"some quoted query\" for");
  }

  @Test
  public void testCreateSuggestions4() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some quoted query \"for\" test"));
    verify(sugs, "quoted query \"for\" test", "some query \"for\" test", "some quoted \"for\" test",
        "some quoted query test", "some quoted query \"for\"");
  }

  @Test
  public void testCreateSuggestions5() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some quoted query \" for test\""));
    verify(sugs, "quoted query \"for test\"", "some query \"for test\"", "some quoted \"for test\"",
        "some quoted query test", "some quoted query for");
  }

  @Test
  public void testCreateSuggestions6() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some quoted query for \"test\""));
    verify(sugs, "quoted query for \"test\"", "some query for \"test\"", "some quoted for \"test\"",
        "some quoted query \"test\"", "some quoted query for");
  }

  @Test
  public void testCreateSuggestions7() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"some\" quoted query for test"));
    verify(sugs, "quoted query for test", "\"some\" query for test", "\"some\" quoted for test",
        "\"some\" quoted query test", "\"some\" quoted query for");
  }

  @Test
  public void testCreateSuggestions8() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"some\""));
    assertNull(sugs);
  }

  @Test
  public void testCreateSuggestions9() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"some\" test \""));
    verify(sugs, "test \"", "\"some\" \"");
  }

  @Test
  public void testCreateSuggestions10() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"rate and feedback\""));
    verify(sugs, "\"and feedback\"", "\"rate feedback\"", "\"rate and\"");
  }
}
