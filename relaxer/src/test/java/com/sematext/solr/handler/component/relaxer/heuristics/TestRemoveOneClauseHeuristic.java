/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.heuristics;

import static com.sematext.solr.handler.component.relaxer.heuristics.Utils.verify;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sematext.solr.handler.component.relaxer.QueryRelaxerHeuristic;
import com.sematext.solr.handler.component.relaxer.RelaxerSuggestion;

@SolrTestCaseJ4.SuppressSSL
public class TestRemoveOneClauseHeuristic extends SolrTestCaseJ4 {
  private QueryRelaxerHeuristic heuristic = null;

  @BeforeClass
  public static void beforeTests() throws Exception {
    // to run from IDE:
    // initCore("solr/collection1/conf/solrconfig.xml","solr/collection1/conf/schema.xml");

    // to build with maven
    initCore("solrconfig.xml", "schema.xml");
  }

  @Before
  public void setup() {
    heuristic = new RemoveOneClauseHeuristic();
  }

  @Test
  public void testCreateSuggestions() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "foo AND bar"));
    verify(sugs, "bar", "foo");
  }

  @Test
  public void testCreateSuggestions2() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "foo OR bar"));
    verify(sugs, "bar", "foo");
  }

  @Test
  public void testCreateSuggestions3() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "foo NOT bar"));
    verify(sugs, "NOT bar", "foo");
  }

  @Test
  public void testCreateSuggestions4() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "some \"quoted query\""));
    verify(sugs, "\"quoted query\"", "some");
  }

  @Test
  public void testCreateSuggestionsCJK() {
    Map<Pattern, Analyzer> fieldAnalyzerMaps = createCJKAnalyzer();
    heuristic.setFieldAnalyzerMaps(fieldAnalyzerMaps);
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "fooあい", CommonParams.DF, "cjk"));
    verify(sugs, "あい", "foo");
  }

  @Test
  public void testCreateSuggestionsCJK2() {
    Map<Pattern, Analyzer> fieldAnalyzerMaps = createCJKAnalyzer();
    heuristic.setFieldAnalyzerMaps(fieldAnalyzerMaps);
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(
        req(CommonParams.Q, "fooあい OR bar", CommonParams.DF, "cjk"));
    verify(sugs, "あい OR bar", "foo OR bar", "fooあい");
  }

  @Test
  public void testCreateSuggestionsCJK3() {
    Map<Pattern, Analyzer> fieldAnalyzerMaps = createCJKAnalyzer();
    heuristic.setFieldAnalyzerMaps(fieldAnalyzerMaps);
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(
        req(CommonParams.Q, "+fooあい OR bar", CommonParams.DF, "cjk"));
    verify(sugs, "+あい OR bar", "+foo OR bar", "+fooあい");
  }

  @Test
  public void testCreateSuggestionsOneTerm() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "foo"));
    assertNull(sugs);
  }

  @Test
  public void testCreateSuggestionsOnePhrase() {
    Set<RelaxerSuggestion> sugs = heuristic.createSuggestions(req(CommonParams.Q, "\"foo bar\""));
    assertNull(sugs);
  }

  private Map<Pattern, Analyzer> createCJKAnalyzer() {
    Analyzer analyzer = new CJKAnalyzer();
    Map<Pattern, Analyzer> fieldAnalyzerMaps = new LinkedHashMap<Pattern, Analyzer>();
    Pattern fieldPattern = Pattern.compile("cjk");
    fieldAnalyzerMaps.put(fieldPattern, analyzer);
    return fieldAnalyzerMaps;
  }
}
