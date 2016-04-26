/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardHandlerFactory;

import java.util.List;

public class ReSearcherRequestContext {
  private ModifiableSolrParams params;
  private String originalQueryString;
  private long originalQueryHits;
  
  private String correctedQuery;
  private long correctedQueryHits;
  private ReSearcherHandler handler;
  
  private SolrCore core;
  private List<SearchComponent> queryOnlyComponents;
  private ShardHandlerFactory shardHandlerFactory;
  
  public ReSearcherRequestContext(ResponseBuilder rb) {
    params = new ModifiableSolrParams(rb.req.getParams());
    originalQueryString = rb.getQueryString();
    originalQueryHits = ReSearcherUtils.extractOriginalQueryHits(rb);
  }

  
  public void restoreContext(ResponseBuilder rb) {
    rb.req.setParams(params);
    rb.setQueryString(originalQueryString);
  }
  
  public String getCorrectedQuery() {
    return correctedQuery;
  }
  
  public void setCorrectedQuery(String correctedQuery) {
    this.correctedQuery = correctedQuery;
  }
  
  public String getOriginalQueryString() {
    return originalQueryString;
  }
  
  public long getOriginalQueryHits() {
    return originalQueryHits;
  }

  public long getCorrectedQueryHits() {
    return correctedQueryHits;
  }

  public void setCorrectedQueryHits(long correctedQueryHits) {
    this.correctedQueryHits = correctedQueryHits;
  }
  
  public ModifiableSolrParams getParams() {
    return params;
  }
  
  public ReSearcherHandler getHandler() {
    if (handler == null) {
      handler = new ReSearcherHandler();
    }
    return handler;
  }


  public SolrCore getCore() {
    return core;
  }


  public void setCore(SolrCore core) {
    this.core = core;
  }


  public List<SearchComponent> getQueryOnlyComponents() {
    return queryOnlyComponents;
  }


  public void setQueryOnlyComponents(List<SearchComponent> queryOnlyComponents) {
    this.queryOnlyComponents = queryOnlyComponents;
  }


  public ShardHandlerFactory getShardHandlerFactory() {
    return shardHandlerFactory;
  }


  public void setShardHandlerFactory(ShardHandlerFactory shardHandlerFactory) {
    this.shardHandlerFactory = shardHandlerFactory;
  }
}
