/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

import static org.junit.Assert.*;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;

import com.sematext.solr.handler.component.relaxer.query.RegexExtractor;
import com.sematext.solr.handler.component.relaxer.query.RelaxerParams;
import com.sematext.solr.handler.component.relaxer.query.UserQueryExtractor;

import java.util.HashMap;
import java.util.Map;

public class TestRegexExtract {
  private UserQueryExtractor extract = null;
  
  @Before
  public void setup() {
    extract = new RegexExtractor();
  }
  
  @Test
  public void testExtract() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("q", "query:\"{!type=edismax qf='title description body' v='large rectangle'}  \" +yearpub:2008");

    SolrParams params = new MapSolrParams(m);
    String userQuery = extract.extract(params);
    assertEquals("large rectangle", userQuery);

    m.clear();
    m.put("q", "query:\"{!type=edismax qf='title description body' v=large}  \" +yearpub:2008");
    userQuery = extract.extract(params);
    assertEquals("large", userQuery);

    m.clear();
    m.put("q", "+_query_:\"{!type=edismax qf='featured_k^1.0' v='true' mm=100%}\" +_query_:\"{!type=edismax qf='workflow_state_k^1.0 workflow_state_k^1.0 workflow_state_k^1.0 workflow_state_k^1.0' v='PUBLISHED' mm=100%}\" +_query_:\"{!type=edismax qf='locale_k^1.0' v='en-GB OR en' mm=100%}\" -(-_query_:\"{!type=edismax qf='entitlements_mk^1.0' v='AgentED:kmreviewer OR TeamED:kmreviewerteam OR TeamRoleED:kmreviewerteamrole OR PrivateContentEntitlement OR KMContentReviewer' mm=100%}\" AND +_query_:\"{!type=edismax qf='entitlements_mk^1.0' v='[* TO *]' mm=100%}\") -(+_query_:\"{!type=edismax qf='tags_mtg^1.0' v='product_iphone3' mm=100%}\" OR -_query_:\"{!type=edismax qf='tagsets_mk^1.0' v='product' mm=100%}\") -(+_query_:\"{!type=edismax qf='tags_");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "tags_mtg");
    userQuery = extract.extract(params);
    assertEquals("product_iphone3", userQuery);
    
    m.clear();
    m.put("q", "query:\"{!type=edismax qf='title description body'}large  \" +yearpub:2008");
    m.put(RelaxerParams.QUERY_RELAXER_Q, "large");
    userQuery = extract.extract(params);
    assertEquals("large", userQuery);

    m.clear();
    m.put("q", "query:\"{!type=edismax qf='title description body' v=large}  \" +yearpub:2008");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "title");
    userQuery = extract.extract(params);
    assertEquals("large", userQuery);

    m.clear();
    m.put("q", "query:\"{!type=edismax qf='title description body' v=large}  \" +yearpub:2008");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "text");
    userQuery = extract.extract(params);
    assertEquals("query:\"{!type=edismax qf='title description body' v=large}  \" +yearpub:2008", userQuery);
    
    m.clear();
    m.put("q", "+_query_:\"{!type=edismax qf='private_section_s_en^0.1 shared_contact_center_s_en^1.0 title_s_en^10.0 public_section_s_en^0.1 ' boost=boost_weight_f pf='private_section_s_en^1 shared_contact_center_s_en^1 public_section_s_en^1' ps=25 pf2='private_section_s_en^0.2 shared_contact_center_s_en^0.2 public_section_s_en^0.2' pf3='private_section_s_en^0.5 shared_contact_center_s_en^0.5 public_section_s_en^0.5' v='today rained dogs cats fad' mm=100% tie=1 relax=on synonym=true}\" +_query_:\"{!type=edismax qf='locale_k^0.1' boost=boost_weight_f v='en*' mm=100%}\" +_query_:\"{!type=edismax qf='content_category_tg^0.1' boost=boost_weight_f v='content_article OR content_decisiontree OR content_faq OR content_knowledgealert OR content_segment OR content_spidereddocument OR content_uploadeddocument' mm=100%}\" -(-_query_:\"{!type=edismax qf='workflow_state_k^0.1 ' boost=boost_weight_f v='AWAITING_PUBLICATION OR DRAFT OR PUBLISHED OR REQUEST_FOR_REWORK OR UNDER_REVIEW' mm=100%}\" AND +_query_:\"{!type=edismax qf='workflow_state_k^0.1 ' boost=boost_weight_f v='[* TO *]' mm=100%}\") -(-_query_:\"{!type=edismax qf='entitlements_mk^0.1' boost=boost_weight_f v='\"AgentED:kmmanager\" OR \"TeamED:kmmanagerteam\" OR \"TeamRoleED:Supervisor\" OR \"TeamED:kmmanagerteam\" OR \"TeamRoleED:kmmanagerteamrole\" OR \"KMContentManager\" OR \"Telephony0\" OR \"PageSetExecutionEntitlement0\" OR \"KnowledgeCentreAdmin\" OR \"PrivateContentEntitlement\" OR \"RegisteredUserContent\"' mm=100%}\" AND +_query_:\"{!type=edismax qf='entitlements_mk^0.1' boost=boost_weight_f v='[* TO *]' mm=100%}\")");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "text");
    userQuery = extract.extract(params);
    assertEquals("today rained dogs cats fad", userQuery);
    
    m.clear();
    m.put("q", "+_query_:\"{!type=edismax qf='title description body' v='Abstract mapping for don\\'t lola public section' relax='on'}\")");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "text");
    userQuery = extract.extract(params);
    assertEquals("Abstract mapping for don\\'t lola public section", userQuery);

    m.clear();
    m.put("q", "+(+_query_:\"{!type=kdismax qf='tags_mtg^0.1' boost=boost_weight_f v='product OR product_apple OR product_ipod OR product_ipodshuffle OR product_ipodnano OR product_ipodclassic OR product_ipodtouch OR product_ipodmini OR product_iphone OR product_iphone3 OR product_iphone4 OR product_iphone5 OR product_ipad OR product_ipad2 OR product_ipad3 OR product_ipad4 OR product_ipadmini' mm=100%}\" OR +_query_:\"{!type=kdismax qf='shared_self_service_s_en^1.0 title_self_service_s_en^10.0 public_section_s_en^0.1 ' boost=boost_weight_f pf='shared_self_service_s_en^1 public_section_s_en^1' ps=25 pf2='shared_self_service_s_en^0.2 public_section_s_en^0.2' pf3='shared_self_service_s_en^0.5 public_section_s_en^0.5' v='fast' mm=100% tie=1 relax=on synonym=true}\") +_query_:\"{!type=kdismax qf='locale_k^0.1' boost=boost_weight_f v='en*' mm=100%}\" -_query_:\"{!type=kdismax qf='content_category_tg^0.1' boost=boost_weight_f v='content_segment' mm=100%}\" +_query_:\"{!type=kdismax qf='workflow_state_k^0.1' boost=boost_weight_f v='PUBLISHED' mm=100%}\" -(-_query_:\"{!type=kdismax qf='entitlements_mk^0.1' boost=boost_weight_f v='' mm=100%}\" AND +_query_:\"{!type=kdismax qf='entitlements_mk^0.1' boost=boost_weight_f v='[* TO *]' mm=100%}\")");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "text");
    userQuery = extract.extract(params);
    assertEquals("fast", userQuery);

    m.clear();
    m.put("q", "+(+_query_:\"{!type=kdismax qf='tags_mtg^0.1' boost=boost_weight_f v='product OR product_apple OR product_ipod OR product_ipodshuffle OR product_ipodnano OR product_ipodclassic OR product_ipodtouch OR product_ipodmini OR product_iphone OR product_iphone3 OR product_iphone4 OR product_iphone5 OR product_ipad OR product_ipad2 OR product_ipad3 OR product_ipad4 OR product_ipadmini' mm=100%}\" OR +_query_:\"{!type=kdismax qf='shared_self_service_s_en^1.0 title_self_service_s_en^10.0 public_section_s_en^0.1 ' boost=boost_weight_f pf='shared_self_service_s_en^1 public_section_s_en^1' ps=25 pf2='shared_self_service_s_en^0.2 public_section_s_en^0.2' pf3='shared_self_service_s_en^0.5 public_section_s_en^0.5' v='kana' mm=100% tie=1 relax=on synonym=true}\") +_query_:\"{!type=kdismax qf='locale_k^0.1' boost=boost_weight_f v='en*' mm=100%}\" -_query_:\"{!type=kdismax qf='content_category_tg^0.1' boost=boost_weight_f v='content_segment' mm=100%}\" +_query_:\"{!type=kdismax qf='workflow_state_k^0.1' boost=boost_weight_f v='PUBLISHED' mm=100%}\" -(-_query_:\"{!type=kdismax qf='entitlements_mk^0.1' boost=boost_weight_f v='' mm=100%}\" AND +_query_:\"{!type=kdismax qf='entitlements_mk^0.1' boost=boost_weight_f v='[* TO *]' mm=100%}\")");
    m.put(RelaxerParams.QUERY_RELAXER_FIELD, "text");
    userQuery = extract.extract(params);
    assertEquals("kana", userQuery);
    
  }
  
  @Test
  public void testRelax() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("q", "query:\"{!type=edismax qf='title description body' v='large rectangle'}  \" +yearpub:2008");
    SolrParams params = new MapSolrParams(m);
    String userQuery = "large rectangle";
    String relaxedUserQuery = "rectangle";
    String relaxedQuery = extract.relaxQuery(params, userQuery, relaxedUserQuery);
    assertEquals("query:\"{!type=edismax qf='title description body' v='rectangle'}  \" +yearpub:2008", relaxedQuery);
  }
  
  @Test
  public void testRelaxMM() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("q", "query:\"{!type=edismax qf='title description body' v='large rectangle' relax='on'}\" +yearpub:2008");
    SolrParams params = new MapSolrParams(m);
    String relaxedQuery = extract.relaxMM(params, "-1");
    assertEquals("query:\"{!type=edismax qf='title description body' v='large rectangle' relax='on' mm='-1'}\" +yearpub:2008", relaxedQuery);
    
    m.put("q", "query:\"{!type=edismax qf='title description body' v='large rectangle' relax='on' mm='100%'}\" +yearpub:2008");
    relaxedQuery = extract.relaxMM(params, "-1");
    assertEquals("query:\"{!type=edismax qf='title description body' v='large rectangle' relax='on' mm='-1'}\" +yearpub:2008", relaxedQuery);
    
    m.put("q", "query:\"{!type=edismax qf='title description body' v='large rectangle' mm='100%'}\" +yearpub:2008");
    relaxedQuery = extract.relaxMM(params, "-1");
    assertEquals("query:\"{!type=edismax qf='title description body' v='large rectangle' mm='100%'}\" +yearpub:2008", relaxedQuery);
  }

}
