/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.web;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

/**
 * Used to invoke ReSearcherDemoServlet as a standalone application so we can debug it without running it on server.
 * 
 *
 * @author sematext, http://www.sematext.com/
 */
public class StandaloneDemoServletInvoker {

  /**
   * @param args
   * @throws IOException 
   * @throws ServletException 
   */
  public static void main(String[] args) throws ServletException, IOException {
    ReSearcherDemoServlet s = new ReSearcherDemoServlet(); 
    s.setServer((SolrServer) new HttpSolrServer("http://localhost:8080/solr"));
    
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    request.setParameter("query", "amerik");
    request.setParameter("type", "Res");
    
    s.doGet(request, response);
    
    System.out.println(response.getStringWriter().getBuffer().toString());
    
  }
}