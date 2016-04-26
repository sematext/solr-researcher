/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * Extracts user query for Edismax query using regex. This class can handle subquery with pattern q={!type=dismax
 * qf=myfield v='solr rocks'}. If there are multiple subqueries we can select one subquery by adding localparam relax=on or
 * by specifying a field using URL param &relax.q=[field_name].
 * 
 * @author sematext, http://www.sematext.com/
 */
public class RegexExtractor extends UserQueryExtractor {
  private static final Pattern SUB_QUERY = Pattern.compile("\\{\\![^}]+\\}");
  private static final Pattern VALUE = Pattern.compile("v\\s*=\\s*'((?:[^']|(?<=\\\\)')+)'|v\\s*=\\s*(\\S+)[\\s|}]");
  private static final Pattern QUERY_FIELDS = Pattern.compile("qf\\s*=\\s*'([^']+)'|qf\\s*=\\s*(\\S+)[\\s|}]");
  private static final Pattern MM = Pattern.compile("mm\\s*=\\s*'([^']+)'|mm\\s*=\\s*(\\S+)[\\s|}]");
  private static final Pattern RELAX = Pattern.compile("relax\\s*=\\s*'([^']+)'|relax\\s*=\\s*(\\S+)[\\s|}]");
  private static final Pattern FIELDS = Pattern.compile("(\\w+)(~[0-9.,]+)?(\\^[0-9.,]+)?");
  private static final String RELAXER_QUERY = "&" + RelaxerParams.QUERY_RELAXER_Q + "=[^&]+";

  @Override
  public String extract(SolrParams params) {
    String q = params.get(RelaxerParams.QUERY_RELAXER_Q);
    if (q != null) {
      return q;
    }

    q = params.get(CommonParams.Q);
    String field = params.get(RelaxerParams.QUERY_RELAXER_FIELD);
    String userQuery = null;

    Matcher matcher = SUB_QUERY.matcher(q);
    while (matcher.find()) {
      String subQuery = matcher.group();

      String value = match(VALUE, subQuery);
      String relax = match(RELAX, subQuery);

      if (field == null) {
        if (value != null) {
          userQuery = value;
        }
      } else {
        String queryFields = match(QUERY_FIELDS, subQuery);
        if (queryFields != null) {
          Set<String> fields = matchAll(FIELDS, queryFields);
          if (fields.contains(field)) {
            return value;
          }
        }
      }
      if ("ON".equalsIgnoreCase(relax)) {
        return value;
      }
    }

    if (field == null && userQuery != null) {
      return userQuery;
    } else {
      return q;
    }
  }
  
  @Override
  public String relaxHighlightQuery(String highlightQuery, SolrParams params, String userQuery, String relaxedUserQuery) {
    return relax(params, userQuery, relaxedUserQuery, highlightQuery);
  }

  @Override
  public String relaxQuery(SolrParams params, String userQuery, String relaxedUserQuery) {
    String query = params.get(CommonParams.Q);
    String q = params.get(RelaxerParams.QUERY_RELAXER_Q);
    if (q != null) {
      return query.replaceFirst(RELAXER_QUERY, relaxedUserQuery);
    }

    return relax(params, userQuery, relaxedUserQuery, query);
  }

  private String relax(SolrParams params, String userQuery, String relaxedUserQuery, String query) {
    String field = params.get(RelaxerParams.QUERY_RELAXER_FIELD);
    String relaxedQuery = null;

    Matcher matcher = SUB_QUERY.matcher(query);
    while (matcher.find()) {
      String subQuery = matcher.group();

      String value = match(VALUE, subQuery);
      String relax = match(RELAX, subQuery);

      int start = matcher.start();

      if ("ON".equalsIgnoreCase(relax)) {
        return replace(VALUE, query, start, subQuery, relaxedUserQuery);
      }

      if (userQuery.equals(value)) {
        if (field == null) {
          relaxedQuery = replace(VALUE, query, start, subQuery, relaxedUserQuery);
        } else {
          String queryFields = match(QUERY_FIELDS, subQuery);
          if (queryFields != null) {
            Set<String> fields = matchAll(FIELDS, queryFields);
            if (fields.contains(field)) {
              relaxedQuery = replace(VALUE, query, start, subQuery, relaxedUserQuery);
            }
          }
        }
      }
    }

    if (relaxedQuery != null) {
      return relaxedQuery;
    } else {
      return relaxedUserQuery;
    }
  }
  
  @Override
  public String relaxMM(SolrParams params, String relaxedMM) {
    String query = params.get(CommonParams.Q);
    String field = params.get(RelaxerParams.QUERY_RELAXER_FIELD);

    Matcher matcher = SUB_QUERY.matcher(query);
    while (matcher.find()) {
      String subQuery = matcher.group();
      String relax = match(RELAX, subQuery);
      int start = matcher.start();

      if ("ON".equalsIgnoreCase(relax)) {
        return replaceOrInsert(MM, query, start, subQuery, relaxedMM);
      }

      String queryFields = match(QUERY_FIELDS, subQuery);
      if (queryFields != null) {
        Set<String> fields = matchAll(FIELDS, queryFields);
        if (fields.contains(field)) {
          return replaceOrInsert(MM, query, start, subQuery, relaxedMM);
        }
      }
    }
    
    return query;
  }

  private String match(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    String value = null;
    if (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        value = matcher.group(i);
        if (value != null)
          break;
      }
    }

    return value;
  }

  private String replace(Pattern pattern, String query, int start, String text, String replace) {
    Matcher matcher = pattern.matcher(text);
    String value = null;
    if (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        value = matcher.group(i);
        if (value != null) {
          StringBuilder sb = new StringBuilder();
          sb.append(query.substring(0, matcher.start(i) + start));
          sb.append(replace);
          sb.append(query.substring(matcher.end(i) + start));
          return sb.toString();
        }
      }
    }

    return value;
  }
  
  private String replaceOrInsert(Pattern pattern, String query, int start, String subQuery, String replace) {
    Matcher matcher = pattern.matcher(subQuery);
    String value = null;
    if (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        value = matcher.group(i);
        if (value != null) {
          StringBuilder sb = new StringBuilder();
          sb.append(query.substring(0, matcher.start(i) + start));
          sb.append(replace);
          sb.append(query.substring(matcher.end(i) + start));
          return sb.toString();
        }
      }
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(query.substring(0, start));
      sb.append(subQuery.substring(0, subQuery.length() - 1));
      sb.append(" mm='");
      sb.append(replace);
      sb.append("'}");
      sb.append(query.substring(subQuery.length() + start));
      return sb.toString();
    }

    return value;
  }

  private Set<String> matchAll(Pattern pattern, String text) {
    Set<String> values = new HashSet<String>();
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        String value = matcher.group(i);
        if (value != null) {
          values.add(value);
          break;
        }
      }
    }

    return values;
  }

}
