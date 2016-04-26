/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardHandler;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ReSearcherHandler {

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
  
  private void handleSuggestionRequest(ReSearcherRequestContext ctx, ResponseBuilder rb, List<SearchComponent> components, boolean ignoreOutput) throws Exception {
    ShardHandler shardHandler1 = ctx.getShardHandlerFactory().getShardHandler();
    shardHandler1.checkDistributed(rb);
    boolean isDistributed = isDistributed(rb);
    if (rb.outgoing == null) {
      rb.outgoing = new LinkedList<ShardRequest>();
    }
    rb.finished = new ArrayList<ShardRequest>();

    for (SearchComponent c : components) {
      c.prepare(rb);
    }

    if (!isDistributed) {
      for (SearchComponent c : components) {
        c.process(rb);
      }
    } else {
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
            sreq.responses = new ArrayList<ShardResponse>();

            for (String shard : sreq.actualShards) {
              ModifiableSolrParams params = new ModifiableSolrParams(sreq.params);
              params.remove(ShardParams.SHARDS); // not a top-level request
              params.set("distrib", "false"); // not a top-level request
              params.remove("indent");
              params.remove(CommonParams.HEADER_ECHO_PARAMS);
              params.set(ShardParams.IS_SHARD, true); // a sub (shard) request
              params.set(ShardParams.SHARD_URL, shard); // so the shard knows what was asked
              String shardQt = params.get(ShardParams.SHARDS_QT);
              if (shardQt == null) {
                params.remove(CommonParams.QT);
              } else {
                params.set(CommonParams.QT, shardQt);
              }
              shardHandler1.submit(sreq, shard, params);
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
              }
            }

            rb.finished.add(srsp.getShardRequest());

            // let the components see the responses to the request
            for (SearchComponent c : components) {
              c.handleResponses(rb, srsp.getShardRequest());
            }
          }

          for (SearchComponent c : components) {
            c.finishStage(rb);
          }

          // at this stage we have final numFound
          if (ignoreOutput && rb.stage == ResponseBuilder.STAGE_GET_FIELDS) {
            nextStage = ResponseBuilder.STAGE_DONE;
            break;
          }
        }
        // we are done when the next stage is MAX_VALUE
      } while (nextStage != Integer.MAX_VALUE);
    }
  }

  private boolean isDistributed(ResponseBuilder rb) {
    SolrQueryRequest req = rb.req;
    SolrParams params = req.getParams();
    
    boolean isDistrib = params.getBool("distrib", req.getCore().getCoreDescriptor()
        .getCoreContainer().isZooKeeperAware());
    String shards = params.get(ShardParams.SHARDS);

    // for back compat, a shards param with URLs like localhost:8983/solr will mean that this
    // search is distributed.
    boolean hasShardURL = shards != null && shards.indexOf('/') > 0;
    isDistrib = hasShardURL | isDistrib;
    
    return isDistrib;
  }
  
}
