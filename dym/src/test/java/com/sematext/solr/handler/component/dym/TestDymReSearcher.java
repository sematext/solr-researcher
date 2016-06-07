/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import com.sematext.solr.handler.component.AbstractReSearcherComponent;

@SolrTestCaseJ4.SuppressSSL
public class TestDymReSearcher extends SolrTestCaseJ4 {
  @BeforeClass
  public static void beforeTests() throws Exception {
    // to run from IDE:
    // initCore("solr/collection1/conf/solrconfig.xml","solr/collection1/conf/schema.xml");
    
    // to build with maven
    initCore("solrconfig.xml","schema.xml");

    assertU(adoc("id", "1", "foo", "elvis presley"));
    assertU(adoc("id", "2", "foo", "bob marley"));
    assertU(adoc("id", "3", "foo", "bob dylan"));
    assertU(adoc("id", "4", "foo", "the doors"));
    assertU(adoc("id", "5", "foo", "bob marley & the wailers"));
    assertU(adoc("id", "6", "foo", "bono"));
    assertU(adoc("id", "7", "foo", "bob marley & the wailers 2"));
    assertU(adoc("id", "8", "foo", "bob marley & the wailers 3"));
    assertU(adoc("id", "9", "foo", "bono and bob marley 1"));
    assertU(adoc("id", "10", "foo", "bono and bob marley 2"));
    assertU(adoc("id", "11", "foo", "bono and bob marley 3"));
    assertU(adoc("id", "12", "foo", "elvis"));
    assertU(adoc("id", "13", "foo", "elvis 2"));
    
    assertU("commit", commit());
  }
  
  @AfterClass
  public static void afterClass() throws IOException {
    h.getCore().getSearcher().get().close();
  }
  
  @Test
  public void testPhrase() throws IOException, Exception {
    assertQ(req(CommonParams.QT, "standard", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true", 
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='7']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bob AND foo:marley']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[2][.='foo:bono AND foo:marley']"
        );
  }
  
  @Test
  public void testSingleWord() {
    assertQ(req(CommonParams.QT, "standard", 
        CommonParams.Q, "foo:bon",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='8']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bob']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[2][.='foo:bono']"
        ,"//lst[@name='extended_spellchecker_suggestions_hit_counts']/long[@name='foo:bob'][.='8']"
        ,"//lst[@name='extended_spellchecker_suggestions_hit_counts']/long[@name='foo:bono'][.='4']"
        ,"//result[@name='spellchecked_response']/doc[1]/str[@name='id'][.='2']"
        ,"//result[@name='spellchecked_response']/doc[2]/str[@name='id'][.='3']"
        ,"//result[@name='spellchecked_response']/doc[3]/str[@name='id'][.='5']"
        ,"//result[@name='spellchecked_response']/doc[4]/str[@name='id'][.='7']"
        ,"//result[@name='spellchecked_response']/doc[5]/str[@name='id'][.='8']"
        ,"//result[@name='spellchecked_response']/doc[6]/str[@name='id'][.='9']"
        ,"//result[@name='spellchecked_response']/doc[7]/str[@name='id'][.='10']"
        ,"//result[@name='spellchecked_response']/doc[8]/str[@name='id'][.='11']");
  }
  
  @Test
  public void testQueryCorrectlySpelled() {
    assertQ(req(CommonParams.QT, "standard", 
        CommonParams.Q, "foo:bob AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='7']");
  }
  
  @Test
  public void testPhraseWithGoodSuggestionAlgorithm() {
    assertQ(req(CommonParams.QT, "standardGoodSuggestion", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='7']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bob AND foo:marley']");
  }
  
  @Test
  public void testFacetAndHighlight() {
    assertQ(req(CommonParams.QT, "standardGoodSuggestion", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        FacetParams.FACET, "true", 
        FacetParams.FACET_FIELD, "foo", 
        FacetParams.FACET_FIELD, "id", 
        FacetParams.FACET_FIELD, "bar", 
        FacetParams.FACET_QUERY, "id:[0 TO 20]", 
        FacetParams.FACET_QUERY, "id:[1 TO 100]", 
        HighlightParams.HIGHLIGHT, "true", 
        HighlightParams.FIELDS, "foo",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='7']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bob AND foo:marley']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='bob'][.='7']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='marley'][.='7']"
        ,"//lst[@name='spellchecked_highlighting']/lst[@name='2']/arr[@name='foo']/str[1]");
  }
  
  @Test
  public void testOriginalAndSpellcheckedFacet() {
    assertQ(req(CommonParams.QT, "standardGoodSuggestionAllowSomeOriginalResults", 
        CommonParams.Q, "foo:elvos OR foo:presley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        FacetParams.FACET, "true", 
        FacetParams.FACET_FIELD, "foo", 
        FacetParams.FACET_FIELD, "id", 
        FacetParams.FACET_FIELD, "bar", 
        FacetParams.FACET_QUERY, "id:[0 TO 20]", 
        FacetParams.FACET_QUERY, "id:[1 TO 100]", 
        HighlightParams.HIGHLIGHT, "true", 
        HighlightParams.FIELDS, "foo",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='3']"
        ,"//result[@name='response'][@numFound='1']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:elvis OR foo:presley']"
        // check spellchecked facets:
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='elvi'][.='3']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='presley'][.='1']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='2'][.='1']"
        // check original facets:
        ,"//lst[@name='facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='elvi'][.='1']"
        ,"//lst[@name='facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='presley'][.='1']"
        );
  }
  
  @Test
  public void testPhraseWithFgsAndCommonMisspellings() {
    assertQ(req(CommonParams.QT, "standardResWithCommonMisspellings", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true", 
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='3']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bono AND foo:marley']"
        );
  }
  
  @Test
  public void testFacetAndHighlightWithCommonMisspellings() {
    assertQ(req(CommonParams.QT, "standardResWithCommonMisspellings", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        FacetParams.FACET, "true", 
        FacetParams.FACET_FIELD, "foo", 
        FacetParams.FACET_FIELD, "id",
        FacetParams.FACET_FIELD, "bar", 
        FacetParams.FACET_QUERY, "id:[0 TO 20]", 
        FacetParams.FACET_QUERY, "id:[1 TO 100]",
        HighlightParams.HIGHLIGHT, "true", 
        HighlightParams.FIELDS, "foo",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='3']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bono AND foo:marley']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='bob'][.='3']"
        ,"//lst[@name='spellchecked_facet_counts']/lst[@name='facet_fields']/lst[@name='foo']/int[@name='marley'][.='3']"
        ,"//lst[@name='spellchecked_highlighting']/lst[@name='9']/arr[@name='foo']/str[1]"
        );
  }
  
  @Test
  public void testPhraseWithCorrectionHighlighting() {
    assertQ(req(CommonParams.QT, "standard", 
        CommonParams.Q, "foo:bobo AND foo:marley",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        AbstractReSearcherComponent.RES_HIGHLIGHT_REPLACED_TAG_PARAM_NAME, "b",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='spellchecked_response'][@numFound='7']"
        ,"//result[@name='response'][@numFound='0']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[1][.='foo:bob AND foo:marley']"
        ,"//arr[@name='extended_spellchecker_suggestions']/str[2][.='foo:bono AND foo:marley']"
        ,"//arr[@name='extended_spellchecker_suggestions_highlighted']/str[1][.='<b>foo:bob</b> AND foo:marley']"
        ,"//arr[@name='extended_spellchecker_suggestions_highlighted']/str[2][.='<b>foo:bono</b> AND foo:marley']"
        );
  }
 
  @Test
  public void testBugWithMultipleIncorrectWords() {
    // the bug occurs only when the second word is the one that should be changed
    assertQ(req(CommonParams.QT, "standardIgnoreCollation", 
        CommonParams.Q, "foo:marlex AND foo:bobo",
        SpellingParams.SPELLCHECK_COLLATE, "true", 
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10", 
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true",
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='0']"
        );
    
    // it is enough to see that exception didn't occur; since both words are incorrect and ReS currently handles only one
    // incorrect word, just one word will be fixed which still isn't enough to get any results (so suggestions wont be
    // returned either).
  }
}