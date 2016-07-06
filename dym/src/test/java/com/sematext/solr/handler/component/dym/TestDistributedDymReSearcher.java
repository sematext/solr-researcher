/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

import org.apache.lucene.util.LuceneTestCase.Slow;
import org.apache.solr.BaseDistributedSearchTestCase;
import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.junit.Test;

@SuppressSSL
@Slow
//@AwaitsFix(bugUrl = "https://issues.apache.org/jira/browse/SOLR-8447")
public class TestDistributedDymReSearcher extends BaseDistributedSearchTestCase {
  
  public TestDistributedDymReSearcher() {
    stress = 0;
  }
  
  @Override
  public String getSolrHome() {
    return getFile("solr/collection1").getParent();
  }
  
  public static void beforeTests() throws Exception {
    // to run from IDE:
    // initCore("solr/collection1/conf/solrconfig.xml","solr/collection1/conf/schema.xml");
    
    // to build with maven
    // initCore("solrconfig.xml","schema.xml");

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
    
    assertU("commit", BaseDistributedSearchTestCase.commit());
  }
  
  @Test
  @ShardsFixed(num = 2)
  public void test() throws Exception {
    handle.clear();
    handle.put("QTime", SKIPVAL);
    handle.put("timestamp", SKIPVAL);
    handle.put("maxScore", SKIPVAL);
    handle.put("responseHeader", SKIP);
    handle.put("spellchecked_response", UNORDERED);

    query(CommonParams.QT, "standardResWithCommonMisspellings", 
        ShardParams.SHARDS_QT, "standardResWithCommonMisspellings",
        CommonParams.Q, "foo:bobo AND foo:marley", 
        SpellingParams.SPELLCHECK_COLLATE, "true",
        SpellingParams.SPELLCHECK_BUILD, "true", 
        SpellingParams.SPELLCHECK_COUNT, "10",
        SpellingParams.SPELLCHECK_EXTENDED_RESULTS, "true", 
        DymReSearcher.COMPONENT_NAME, "true",
        SpellCheckComponent.COMPONENT_NAME, "true");
  }
}
