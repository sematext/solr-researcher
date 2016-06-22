/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package org.apache.solr.handler.component;

import static org.apache.solr.common.params.CommonParams.PATH;

import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrQueryTimeoutImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sematext.solr.handler.component.ReSearcherRequestContext;
import com.sematext.solr.handler.component.ReSearcherUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ReSearcherHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SolrCore.class);
  
  public long handleSuggestionHitsRequest(ReSearcherRequestContext ctx, String query, Set<String> componentNames) throws Exception {
    ModifiableSolrParams params = new ModifiableSolrParams(ctx.getParams());
    params.set(CommonParams.ROWS, "0");
    for(String componentName : componentNames) {
      params.set(componentName, "false");
    }
    params.set(CommonParams.Q, query);

    SolrQueryRequest req = new SolrQueryRequestBase(ctx.getCore(), params) {};
    SolrQueryResponse rsp = new SolrQueryResponse();
    ResponseBuilder rb = new ResponseBuilder(req, rsp, ctx.getQueryOnlyComponents());
    
    try {
      handleSuggestionRequest(ctx, rb, ctx.getQueryOnlyComponents(), true);
    } finally {
      req.close();
    }
    
    return ReSearcherUtils.extractOriginalQueryHits(rb);
  }

  public SolrQueryResponse handleSuggestionResponseRequest(ReSearcherRequestContext ctx, String query, String componentName, ResponseBuilder originalRb) throws Exception {
    ModifiableSolrParams newParams = new ModifiableSolrParams(originalRb.req.getParams());
    newParams.set(CommonParams.Q, query);
    newParams.set(componentName, "false");

    SolrQueryRequest req = new SolrQueryRequestBase(ctx.getCore(), newParams) {};
    SolrQueryResponse rsp = new SolrQueryResponse();
    ResponseBuilder rb = new ResponseBuilder(req, rsp, originalRb.components);
    
    try {
      handleSuggestionRequest(ctx, rb, originalRb.components, false);
    } finally {
      req.close();
    }
    
    return rsp;
  }
  
  public SolrQueryResponse handleSuggestionResponseRequest(ReSearcherRequestContext ctx, ModifiableSolrParams params, String componentName, ResponseBuilder rb) throws Exception {
    params.set(componentName, "false");
    rb.req.setParams(params);

    handleSuggestionRequest(ctx, rb, ctx.getQueryOnlyComponents(), false);
    
    return rb.rsp;
  }
  
  public SolrQueryResponse handleSuggestionResponseRequest(ReSearcherRequestContext ctx, ModifiableSolrParams params, String componentName, List<SearchComponent> components) throws Exception {
    params.set(componentName, "false");

    SolrQueryRequest req = new SolrQueryRequestBase(ctx.getCore(), params) {};
    SolrQueryResponse rsp = new SolrQueryResponse();
    ResponseBuilder rb = new ResponseBuilder(req, rsp, components);
    
    try {
      handleSuggestionRequest(ctx, rb, components, false);
    } finally {
      req.close();
    }
    
    return rsp;
  }
  
  private ShardHandler getAndPrepShardHandler(SolrQueryRequest req, ResponseBuilder rb, ShardHandlerFactory shardHandlerFactory) {
    ShardHandler shardHandler = null;

    CoreContainer cc = req.getCore().getCoreDescriptor().getCoreContainer();
    boolean isZkAware = cc.isZooKeeperAware();
    rb.isDistrib = req.getParams().getBool("distrib", isZkAware);
    if (!rb.isDistrib) {
      // for back compat, a shards param with URLs like localhost:8983/solr will mean that this
      // search is distributed.
      final String shards = req.getParams().get(ShardParams.SHARDS);
      rb.isDistrib = ((shards != null) && (shards.indexOf('/') > 0));
    }
    
    if (rb.isDistrib) {
      shardHandler = shardHandlerFactory.getShardHandler();
      shardHandler.prepDistributed(rb);
      if (!rb.isDistrib) {
        shardHandler = null; // request is not distributed after all and so the shard handler is not needed
      }
    }

    if(isZkAware) {
      ZkController zkController = cc.getZkController();
      NamedList<Object> headers = rb.rsp.getResponseHeader();
      if(headers != null) {
        headers.add("zkConnected", 
            zkController != null 
          ? !zkController.getZkClient().getConnectionManager().isLikelyExpired() 
          : false);
      }
      
    }

    return shardHandler;
  }
  
  private void handleSuggestionRequest(ReSearcherRequestContext ctx, ResponseBuilder rb, List<SearchComponent> components, boolean ignoreOutput) throws Exception {
    
    ShardHandler shardHandler1 = null;
    try {
      shardHandler1 = getAndPrepShardHandler(rb.req, rb, ctx.getShardHandlerFactory());
    } catch (Throwable e) {
      e.printStackTrace();
      Field field = ResponseBuilder.class.getDeclaredField("isDistrib");
      LOG.error("isDistrib = " + Modifier.toString(field.getModifiers()));
      LOG.error("Current Lucene version " + Version.LATEST.toString());
    }
    
    for (SearchComponent c : components) {
      c.prepare(rb);
    }

    if (!rb.isDistrib) {
      long timeAllowed = rb.req.getParams().getLong(CommonParams.TIME_ALLOWED, -1L);
      if (timeAllowed > 0L) {
        SolrQueryTimeoutImpl.set(timeAllowed);
      }
      try {
        for (SearchComponent c : components) {
          c.process(rb);
        }
      } finally {
        SolrQueryTimeoutImpl.reset();
      }
    } else {
      if (rb.outgoing == null) {
        rb.outgoing = new LinkedList<ShardRequest>();
      }
      rb.finished = new ArrayList<ShardRequest>();
      
      int nextStage = 0;
      do {
        rb.stage = nextStage; 
        nextStage = ResponseBuilder.STAGE_DONE;
        
        // the next stage is the minimum of what all components report
        for (SearchComponent c : components) {
          nextStage = Math.min(nextStage, c.distributedProcess(rb));
        }
        // check the outgoing queue and send requests
        while (rb.outgoing.size() > 0) {

          // submit all current request tasks at once
          while (rb.outgoing.size() > 0) {
            ShardRequest sreq = rb.outgoing.remove(0);
            sreq.actualShards = sreq.shards;
            if (sreq.actualShards == ShardRequest.ALL_SHARDS) {
              sreq.actualShards = rb.shards;
            }
            sreq.responses = new ArrayList<ShardResponse>(sreq.actualShards.length);

            for (String shard : sreq.actualShards) {
              ModifiableSolrParams params = new ModifiableSolrParams(sreq.params);
              params.remove(ShardParams.SHARDS); // not a top-level request
              params.set(CommonParams.DISTRIB, "false"); // not a top-level request
              params.remove("indent");
              params.remove(CommonParams.HEADER_ECHO_PARAMS);
              params.set(ShardParams.IS_SHARD, true); // a sub (shard) request
              params.set(ShardParams.SHARDS_PURPOSE, sreq.purpose);
              params.set(ShardParams.SHARD_URL, shard); // so the shard knows what was asked
              String shardQt = params.get(ShardParams.SHARDS_QT);
              if (shardQt == null) {
             // for distributed queries that don't include shards.qt, use the original path
                // as the default but operators need to update their luceneMatchVersion to enable
                // this behavior since it did not work this way prior to 5.1
                if (rb.req.getCore().getSolrConfig().luceneMatchVersion.onOrAfter(Version.LUCENE_5_1_0)) {
                  String reqPath = (String) rb.req.getContext().get(PATH);
                  if (!"/select".equals(reqPath)) {
                    params.set(CommonParams.QT, reqPath);
                  } // else if path is /select, then the qt gets passed thru if set
                } else {
                  // this is the pre-5.1 behavior, which translates to sending the shard request to /select
                  params.remove(CommonParams.QT);
                }
              } else {
                params.set(CommonParams.QT, shardQt);
              }
              shardHandler1.submit(sreq, shard, params, rb.preferredHostAddress);
            }
          }

          // now wait for replies, but if anyone puts more requests on
          // the outgoing queue, send them out immediately (by exiting
          // this loop)
          boolean tolerant = rb.req.getParams().getBool(ShardParams.SHARDS_TOLERANT, false);
          while (rb.outgoing.size() == 0) {
            ShardResponse srsp = tolerant ? shardHandler1.takeCompletedIncludingErrors() : shardHandler1
                .takeCompletedOrError();
            if (srsp == null)
              break; // no more requests to wait for

            // Was there an exception?
            if (srsp.getException() != null) {
              // If things are not tolerant, abort everything and rethrow
              if (!tolerant) {
                shardHandler1.cancelAll();
                if (srsp.getException() instanceof SolrException) {
                  throw (SolrException) srsp.getException();
                } else {
                  throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, srsp.getException());
                }
              } else {
                if(rb.rsp.getResponseHeader().get(SolrQueryResponse.RESPONSE_HEADER_PARTIAL_RESULTS_KEY) == null) {
                  rb.rsp.getResponseHeader().add(SolrQueryResponse.RESPONSE_HEADER_PARTIAL_RESULTS_KEY, Boolean.TRUE);
                }
              }
            }

            rb.finished.add(srsp.getShardRequest());

            // let the components see the responses to the request
            for (SearchComponent c : components) {
              c.handleResponses(rb, srsp.getShardRequest());
            }
          }
        }
        
        for (SearchComponent c : components) {
          c.finishStage(rb);
        }
        // we are done when the next stage is MAX_VALUE
      } while (nextStage != Integer.MAX_VALUE);
    }
    
 // SOLR-5550: still provide shards.info if requested even for a short circuited distrib request
    if(!rb.isDistrib && rb.req.getParams().getBool(ShardParams.SHARDS_INFO, false) && rb.shortCircuitedURL != null) {
      NamedList<Object> shardInfo = new SimpleOrderedMap<Object>();
      SimpleOrderedMap<Object> nl = new SimpleOrderedMap<Object>();        
      if (rb.rsp.getException() != null) {
        Throwable cause = rb.rsp.getException();
        if (cause instanceof SolrServerException) {
          cause = ((SolrServerException)cause).getRootCause();
        } else {
          if (cause.getCause() != null) {
            cause = cause.getCause();
          }          
        }
        nl.add("error", cause.toString() );
        StringWriter trace = new StringWriter();
        cause.printStackTrace(new PrintWriter(trace));
        nl.add("trace", trace.toString() );
      }
      else {
        nl.add("numFound", rb.getResults().docList.matches());
        nl.add("maxScore", rb.getResults().docList.maxScore());
      }
      nl.add("shardAddress", rb.shortCircuitedURL);
      nl.add("time", rb.req.getRequestTimer().getTime()); // elapsed time of this request so far
      
      int pos = rb.shortCircuitedURL.indexOf("://");        
      String shardInfoName = pos != -1 ? rb.shortCircuitedURL.substring(pos+3) : rb.shortCircuitedURL;
      shardInfo.add(shardInfoName, nl);   
      rb.rsp.getValues().add(ShardParams.SHARDS_INFO,shardInfo);
    }
  }
}
