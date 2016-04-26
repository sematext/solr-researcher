/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

import org.apache.solr.BaseDistributedSearchTestCase;
import org.apache.solr.SolrTestCaseJ4.SuppressSSL;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.junit.Test;

@SuppressSSL
public class TestDistributedDymReSearcher extends BaseDistributedSearchTestCase {
  
  @Override
  public String getSolrHome() {
    return getFile("solr/collection1").getParent();
  }
  
  @Test
  public void test() throws Exception {
    del("*:*");
    index("id", "1", "foo", "elvis presley");
    index("id", "2", "foo", "bob marley");
    index("id", "3", "foo", "bob dylan");
    index("id", "4", "foo", "the doors");
    index("id", "5", "foo", "bob marley & the wailers");
    index("id", "6", "foo", "bono");
    index("id", "7", "foo", "bob marley & the wailers 2");
    index("id", "8", "foo", "bob marley & the wailers 3");
    index("id", "9", "foo", "bono and bob marley 1");
    index("id", "10", "foo", "bono and bob marley 2");
    index("id", "11", "foo", "bono and bob marley 3");
    index("id", "12", "foo", "elvis");
    index("id", "13", "foo", "elvis 2");
    commit();

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
