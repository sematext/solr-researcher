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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class ReSearcherDemoServlet extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(ReSearcherDemoServlet.class.toString());

  private SolrServer server = null;


  /*
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init();
    
    try {
      LOG.info("Initializing server ...");
      server = new CommonsHttpSolrServer(config.getInitParameter("localSolrUrl"));
      LOG.info("Server initialized ...");
    } catch (MalformedURLException e) {
      LOG.info("Initializing ERROR : " + e.toString());
      e.printStackTrace();
      throw new ServletException(e);
    }

    LOG.info("ReSearcherDemoServlet loaded successfully");
  }
  */

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String query = request.getParameter("query");
    String type = request.getParameter("type");

    SolrQuery solrQuery = new SolrQuery();

    solrQuery.setQueryType("standard" + type);
    solrQuery.set("spellcheck", "true");
    solrQuery.set("spellcheck.collate", "true");
    solrQuery.set("spellcheck.extendedResults", "true");
    solrQuery.set("spellcheck.onlyMorePopular", "true");
    // solrQuery.set("spellcheck.dictionary", language);

    solrQuery.setQuery(query);
    solrQuery.setRows(10);

    LOG.info("Query : " + solrQuery);
    
    QueryResponse queryResponse;

    try {
      server = new HttpSolrServer("http://localhost:8080/solr");
      queryResponse = server.query(solrQuery);

    } catch (SolrServerException e) {
      throw new RuntimeException(e);
    }

    Results results = extractResults(queryResponse);
    results.setQuery(query);
    SpellcheckerResults scResults = extractSpellcheckerResults(queryResponse, type);

    request.setAttribute("results", results);
    request.setAttribute("scResults", scResults);
    request.setAttribute("type", type);
    
    String nextJsp = "/WEB-INF/jsp/resultSc.jsp";
    if (type.equals("Res")) {
      nextJsp = "/WEB-INF/jsp/resultRes.jsp";
    }

    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(nextJsp);
    dispatcher.forward(request,response);
  }

  private SpellcheckerResults extractSpellcheckerResults(QueryResponse queryResponse, String type) {
    SpellcheckerResults r = null;
    
    if (type.equals("Res")) {
      List<String> sugs = (List<String>) queryResponse.getResponse().get("extended_spellchecker_suggestions");
      SolrDocumentList docs = (SolrDocumentList) queryResponse.getResponse().get("spellchecked_response");
      
      if (sugs != null && sugs.size() != 0) {
        r = new SpellcheckerResults();
        r.setSuggestions(sugs);
        r.setSpellcheckedHits(docs.getNumFound());
        r.setSuggestionRows(convertResponseToResultRows(docs));
        r.setBestSuggestion(sugs.get(0));
      }
    }
    else {
      SpellCheckResponse scRes = queryResponse.getSpellCheckResponse();
      
      if (scRes != null) {
        r = new SpellcheckerResults();
        r.setSuggestions(new ArrayList<String>());
        r.getSuggestions().add(scRes.getCollatedResult());
        r.setBestSuggestion(scRes.getCollatedResult());
      }
    }
    
    return r;
  }

  private Results extractResults(QueryResponse queryResponse) {
    SolrDocumentList documentList = queryResponse.getResults();
    Results results = new Results();

    results.setQTime(queryResponse.getQTime());
    results.setNumFound(documentList.getNumFound());
    results.setRows(convertResponseToResultRows(documentList));

    return results;
  }

  private List<ResultRow> convertResponseToResultRows(SolrDocumentList documentList) {
    List <ResultRow> rows  = new ArrayList<ResultRow>();
    Iterator<SolrDocument> iterator = documentList.iterator();

    while (iterator.hasNext()) {
        SolrDocument solrDocument = iterator.next();
        String id = (String) solrDocument.getFieldValue("id");
        String foo = (String) solrDocument.getFieldValue("foo");
        String bar = (String) solrDocument.getFieldValue("bar");
        
        rows.add(new ResultRow(id, foo, bar));
    }
    
    return rows;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  public SolrServer getServer() {
    return server;
  }

  public void setServer(SolrServer server) {
    this.server = server;
  }
};
