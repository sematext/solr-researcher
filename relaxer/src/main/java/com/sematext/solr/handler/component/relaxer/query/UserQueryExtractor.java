/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

import org.apache.solr.common.params.SolrParams;

/**
 * 
 * Extract user query from query. User query is textual query that user enter from search box. Query may have non-user
 * query that is generated automatically from user interface
 * 
 * @author sematext, http://www.sematext.com/
 */
public abstract class UserQueryExtractor {
  public abstract String extract(SolrParams params);

  public abstract String relaxHighlightQuery(String highlightQuery, SolrParams params, String userQuery, String relaxedUserQuery);
  
  public abstract String relaxQuery(SolrParams params, String userQuery, String relaxedUserQuery);
  
  public abstract String relaxMM(SolrParams params, String relaxedMM);
}
