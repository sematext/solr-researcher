/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

/**
 * 
 * Relaxer parameters
 * 
 * @author sematext, http://www.sematext.com/
 */
public interface RelaxerParams {
  static final String QUERY_RELAXER_PREFIX = "queryRelaxer.";

  /**
   * Use the value for this parameter as the query to relaxer.
   * <p/>
   * This parameter is <b>optional</b>. If absent, then the q parameter is used.
   */
  static final String QUERY_RELAXER_Q = QUERY_RELAXER_PREFIX + "q";

  static final String QUERY_RELAXER_FIELD = QUERY_RELAXER_PREFIX + "field";

  static final String QUERY_RELAXER_PREFER_FEWER_MATCHES = QUERY_RELAXER_PREFIX + "preferFewerMatches";
  
  /**
   * The number of results (documents) to return for each relaxed query. Defaults to 5.
   */
  static final String QUERY_RELAXER_ROWS_PER_QUERY = QUERY_RELAXER_PREFIX + "rowsPerQuery";
  
  /**
   * The number of relaxed query to return. Defaults to 5.
   */
  static final String QUERY_RELAXER_MAX_QUERIES = QUERY_RELAXER_PREFIX + "maxQueries";
  
  /**
   * The threshold of long query (count by clause)
   */
  static final String QUERY_RELAXER_LONG_QUERY_TERMS = QUERY_RELAXER_PREFIX + "longQueryTerms";
  
  /**
   * The user specified mm used for relaxing original mm param for long query
   */
  static final String QUERY_RELAXER_LONG_QUERY_MM = QUERY_RELAXER_PREFIX + "longQueryMM";
}
