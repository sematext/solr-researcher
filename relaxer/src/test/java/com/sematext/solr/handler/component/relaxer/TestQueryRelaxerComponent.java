/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.handler.component.FacetComponent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import com.sematext.solr.handler.component.AbstractReSearcherComponent;
import com.sematext.solr.handler.component.relaxer.query.RelaxerParams;

@SolrTestCaseJ4.SuppressSSL
public class TestQueryRelaxerComponent extends SolrTestCaseJ4 {
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
    assertU(adoc("id", "12", "foo", "apple ipad ipod"));
    assertU(adoc("id", "13", "cjk", "messiあい ronaldo"));
    
    assertU("commit", commit());
  }
  
  @AfterClass
  public static void afterClass() throws IOException {
    h.getCore().getSearcher().get().close();
  }
  
  @Test
  public void testGroup2() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, 
        "+_query_:\"{!type=edismax qf='foo^10.0' v='apple google' mm=100% relax=on}\" +_query_:\"{!type=edismax qf='foo^10.0' v='apple google' mm=0% relax=on}\"",
        QueryRelaxerComponent.COMPONENT_NAME, "true",
        GroupParams.GROUP, "true", 
        GroupParams.GROUP_FIELD, "id",
        GroupParams.GROUP_LIMIT, "3",
        GroupParams.GROUP_TOTAL_COUNT, "true")
              ,"*[count(//result[@name='response'])=0]"
              ,"//arr[@name='relaxer_suggestions']/lst[1]//result[@numFound='1']"
            );
  }
  
  @Test
  public void testRelaxMM() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, 
        "{!edismax qf=foo v='bono and bob marley 4' mm='100%' relax='on'}",
        QueryRelaxerComponent.COMPONENT_NAME, "true",
        RelaxerParams.QUERY_RELAXER_LONG_QUERY_MM, "-1")
              ,"//result[@name='response'][@numFound='0']"
//              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedFullQuery'][.='{!edismax qf=foo v=&apos;bono and bob marley 4&apos; mm=&apos;-1&apos; relax=&apos;on&apos;}']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedType'][.='mm']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='3']"
            );
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, 
        "{!edismax qf=foo v='bono and bob marley 4 6' mm='100%' relax='on'}",
        QueryRelaxerComponent.COMPONENT_NAME, "true",
        RelaxerParams.QUERY_RELAXER_LONG_QUERY_MM, "-1")
              ,"//result[@name='response'][@numFound='0']"
              ,"*[count(//arr[@name='relaxer_suggestions']/lst)=0]"
            );
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, 
        "{!edismax qf=foo v='bono and bob marley 4 6' mm='100%' relax='on'}",
        QueryRelaxerComponent.COMPONENT_NAME, "true",
        RelaxerParams.QUERY_RELAXER_LONG_QUERY_MM, "-2")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedType'][.='mm']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='3']"
            );
  }
  
  @Test
  public void testHighlighting() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bobo marley ",
        QueryRelaxerComponent.COMPONENT_NAME, "true", HighlightParams.HIGHLIGHT, "true", HighlightParams.FIELDS, "foo")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedType'][.='query']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/lst[@name='relaxer_highlighting']/lst[@name='2']/arr[@name='foo']/str[1]"
            );
  }
  
  public void testHlQuery() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bobo marley",
        QueryRelaxerComponent.COMPONENT_NAME, "true", HighlightParams.HIGHLIGHT, "true",  HighlightParams.Q, "bobo marley", HighlightParams.FIELDS, "foo")
              ,"//result[@name='response'][@numFound='0']"
            );
  }
  
  @Test
  public void testHlQuery2() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "{!edismax qf=foo v='google AND apple' relax=on} OR {!edismax qf=foo v='facebook AND yahoo'} ",
        QueryRelaxerComponent.COMPONENT_NAME, "true", HighlightParams.HIGHLIGHT, "true",  HighlightParams.Q, "{!edismax qf=foo v='google AND apple' relax=on}", HighlightParams.FIELDS, "foo")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple']"
            );
  }
  
  @Test
  public void testGroup() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bi marley ",
        QueryRelaxerComponent.COMPONENT_NAME, "true", GroupParams.GROUP, "true", GroupParams.GROUP_FIELD, "id")
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/lst[@name='relaxer_grouped']/lst[@name='id']/int[@name='matches'][.='7']"
            );
  }
  
  @Test
  public void testFacet() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bi marley ",
        QueryRelaxerComponent.COMPONENT_NAME, "true", FacetComponent.COMPONENT_NAME, "true", FacetParams.FACET_FIELD, "id")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/lst[@name='relaxer_facet_counts']/lst[@name='facet_fields']/lst[@name='id']"
            );
  }
  
  @Test
  public void testLimit() {
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "elvis AND marley",
        QueryRelaxerComponent.COMPONENT_NAME, "true", RelaxerParams.QUERY_RELAXER_MAX_QUERIES, "1")
              ,"//result[@name='response'][@numFound='0']"
              ,"*[count(//arr[@name='relaxer_suggestions']/lst)=1]"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
            );
    
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "elvis AND marley",
        QueryRelaxerComponent.COMPONENT_NAME, "true", RelaxerParams.QUERY_RELAXER_MAX_QUERIES, "2")
              ,"//result[@name='response'][@numFound='0']"
              ,"*[count(//arr[@name='relaxer_suggestions']/lst)=2]"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='elvis']"
            );
  }
  
  @Test
  public void testRows() {
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "bi marley ",
        QueryRelaxerComponent.COMPONENT_NAME, "true", RelaxerParams.QUERY_RELAXER_ROWS_PER_QUERY, "5")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@name='relaxer_response'][@numFound='7']"
              ,"*[count(//arr[@name='relaxer_suggestions']/lst[1]/result[@name='relaxer_response']/doc)=5]"
            );
  }
  
  @Test
  public void testCheckCorrectedQuery() {
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "aple",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//result[@name='spellchecked_response'][@numFound='1']"
            );
  }
  
  @Test
  public void testQueryAND() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "google AND apple",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple']"
            );
  }
  
  @Test
  public void testQueryNOT() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "apple NOT ipad ipod",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple ipod']"
            );
  }
  
  @Test
  public void testQueryNOT2() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "apple -ipad ipod",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple ipod']"
            );
  }
  
  @Test
  public void testQueryField() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "foo:apple AND id:10",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='foo:apple']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='id:10']"
            );
  }
  
  @Test
  public void testQueryCJK() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "cjk:xaviあい",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='cjk:あい']"
            );
  }
  
  @Test
  public void testQuerySubQuery() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "{!edismax qf=foo v='google AND apple' relax=on}",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple']"
            );
  }
  
  @Test
  public void testQuerySubQuery2() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "{!edismax qf=foo v='google AND apple' relax=on} OR {!edismax qf=foo v='facebook AND yahoo'} ",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple']"
            );
  }
  
  @Test
  public void testQuerySubQuery3() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "{!edismax qf=foo v='google AND apple'} OR {!edismax qf=cjk v='facebook AND yahoo'}",
        RelaxerParams.QUERY_RELAXER_FIELD, "foo",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='1']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='apple']"
            );
  }
  
  @Test
  public void testMoreVsLess() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bono AND wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='4']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='bono']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='wailers']"
              );
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bono AND wailers",
        RelaxerParams.QUERY_RELAXER_PREFER_FEWER_MATCHES, "true",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='3']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='wailers']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='bono']"
              );
  }
  
  @Test
  public void testGoodResult() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bono AND wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='4']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='bono']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='wailers']"
              );
    assertQ(req(CommonParams.QT, "dismax_relaxer_min_good_result", CommonParams.Q, "bono AND wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true" )
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='3']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='wailers']"
              );
  }
  
  @Test
  public void testTopN() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bono AND wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='4']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='bono']"
              ,"//arr[@name='relaxer_suggestions']/lst[2]/str[@name='relaxedQuery'][.='wailers']"
              );
  }

  @Test
  public void testRegularQuery() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bobo marley wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/result[@numFound='3']"
              ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley wailers']"
              );
  }
  
  @Test
  public void testSingleWord() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bon",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='0']"
              );
  }
  
  @Test
  public void testPhraseQuery() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "\"bob wailers\"",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
            ,"//result[@name='response'][@numFound='0']"
            );
  }

  @Test
  public void testQueryCorrectlySpelled() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bob marley",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
              ,"//result[@name='response'][@numFound='7']"
              );
  }
  
  @Test
  public void testPhraseQueryWithCommonMisspellings_checkZeroHitsWithCommonMisspellings() {
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "\"apple ipod ipad\"",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
            ,"//result[@name='response'][@numFound='0']"
            );
  }

  @Test
  public void testPhraseQueryWithCommonMisspellings() {
    assertQ(req(CommonParams.QT, "dismax_relaxer_common_misspellings", CommonParams.Q, "bobo marley",
        QueryRelaxerComponent.COMPONENT_NAME, "true")
            ,"//result[@name='spellchecked_response'][@numFound='3']"
            ,"//result[@name='response'][@numFound='0']"
            );
  }
  
  @Test
  public void testRegularQueryWithCorrectionHighlighting() {
    assertQ(req(CommonParams.QT, "dismax_relaxer", CommonParams.Q, "bobo marley wailers",
        QueryRelaxerComponent.COMPONENT_NAME, "true", AbstractReSearcherComponent.RES_HIGHLIGHT_REPLACED_TAG_PARAM_NAME, "b",
        AbstractReSearcherComponent.RES_HIGHLIGHT_REMOVED_TAG_PARAM_NAME, "i")
            ,"//result[@name='relaxer_response'][@numFound='3']"
            ,"//result[@name='response'][@numFound='0']"
            ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='relaxedQuery'][.='marley wailers']"
            ,"//arr[@name='relaxer_suggestions']/lst[1]/str[@name='highlightRelaxedQuery'][.='<i>bobo</i> marley wailers']"
            );
  }
}