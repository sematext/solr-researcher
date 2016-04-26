package com.sematext.solr.web;

import java.util.ArrayList;
import java.util.List;

public class Results {
    private long numFound;
    private float qTime;
    private List<ResultRow> rows;
    private String query;

    public long getNumFound() {
        return numFound;
    }

    public void setNumFound(long numFound) {
        this.numFound = numFound;
    }

    public float getQTime() {
        return qTime;
    }

    public void setQTime(float time) {
        qTime = time;
    }

    public List<ResultRow> getRows() {
        return rows;
    }

    public void setRows(List<ResultRow> rows) {
        this.rows = rows;
    }

    public void addResultRow(ResultRow resultRow) {

        if (rows == null) {
            rows = new ArrayList<ResultRow>();
        }

        rows.add(resultRow);
    }

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }

}
