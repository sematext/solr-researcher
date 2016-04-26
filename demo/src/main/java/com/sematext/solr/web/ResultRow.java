package com.sematext.solr.web;

public class ResultRow {

    private String id;
    private String foo;
    private String bar;

    public ResultRow() {
    }

    public ResultRow(String id, String foo, String bar) {
        this.id = id;
        this.foo = foo;
        this.bar = bar;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

    public String getBar() {
      return bar;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }

}
