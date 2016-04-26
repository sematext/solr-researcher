/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.junit.Test;

import com.sematext.solr.handler.component.ReSearcherRequestContext;

public class TestAbstractReSearcherComponent {

  /*
   * Tests different input query strings
   */
  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testPreProcessOriginalQuery() {
    DummyAbstractReSearcherComponent comp = new DummyAbstractReSearcherComponent();
    
    NamedList args = new NamedList();
    args.add("commonMisspellingsFile", "../solr/collection1/conf/common_misspellings_en.txt");
    comp.init(args);
    
    SolrQueryRequestBase req = new LocalSolrQueryRequest(null, (SolrParams) null);
    ResponseBuilder rb = new ResponseBuilder(req, null, null);
    ReSearcherRequestContext ctx = new ReSearcherRequestContext(rb);

    rb.setQueryString("some \"acadamy query\" for test");
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNotNull(ctx.getCorrectedQuery());
    assertEquals(ctx.getCorrectedQuery(), "some \"academy query\" for test");

    rb.setQueryString("acadamy some \" acadamy query \" for test");
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNotNull(ctx.getCorrectedQuery());
    assertEquals(ctx.getCorrectedQuery(), "academy some \" academy query \" for test");

    rb.setQueryString("\"acadamy\" some \" acadamy query \" for \"test\"");
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNotNull(ctx.getCorrectedQuery());
    assertEquals(ctx.getCorrectedQuery(), "\"academy\" some \" academy query \" for \"test\"");

    rb.setQueryString("\" acadamy\"");
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNotNull(ctx.getCorrectedQuery());
    assertEquals(ctx.getCorrectedQuery(), "\" academy\"");

    rb.setQueryString("\"acadamy \" ");
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNotNull(ctx.getCorrectedQuery());
    assertEquals(ctx.getCorrectedQuery(), "\"academy \"");

    // OK original query
    rb.setQueryString("some \"academy query\" for test");
    ctx.setCorrectedQuery(null);
    try {
      // we expect this invocation to fail (since Solr context isn't prepared) and we just check if misspelling
      // correction was set into response
      comp.preProcessOriginalQuery(ctx, rb);
    }
    catch (Throwable thr) {
    }
    assertNull(ctx.getCorrectedQuery());
  }
}