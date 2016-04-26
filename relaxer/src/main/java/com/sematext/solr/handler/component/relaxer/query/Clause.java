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
 * A clause in edismax query syntax. It can be a term, phrase or Boolean operator.
 * 
 * @author sematext, http://www.sematext.com/
 */
public class Clause {

  protected String field;
  protected String rawField; // if the clause is +(foo:bar) then rawField=(foo
  protected boolean isPhrase;
  protected boolean hasWhitespace;
  protected boolean hasSpecialSyntax;
  protected boolean syntaxError;
  protected char must; // + or -
  protected String val; // the field value (minus the field name, +/-, quotes)
  protected String[] tokens;
  protected String raw; // the raw clause w/o leading/trailing whitespace

  boolean isBareWord() {
    return must == 0 && !isPhrase;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getRawField() {
    return rawField;
  }

  public void setRawField(String rawField) {
    this.rawField = rawField;
  }

  public boolean isPhrase() {
    return isPhrase;
  }

  public void setPhrase(boolean isPhrase) {
    this.isPhrase = isPhrase;
  }

  public boolean isHasWhitespace() {
    return hasWhitespace;
  }

  public void setHasWhitespace(boolean hasWhitespace) {
    this.hasWhitespace = hasWhitespace;
  }

  public boolean isHasSpecialSyntax() {
    return hasSpecialSyntax;
  }

  public void setHasSpecialSyntax(boolean hasSpecialSyntax) {
    this.hasSpecialSyntax = hasSpecialSyntax;
  }

  public boolean isSyntaxError() {
    return syntaxError;
  }

  public void setSyntaxError(boolean syntaxError) {
    this.syntaxError = syntaxError;
  }

  public char getMust() {
    return must;
  }

  public void setMust(char must) {
    this.must = must;
  }

  public String getVal() {
    return val;
  }

  public void setVal(String val) {
    this.val = val;
  }

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public String[] getTokens() {
    return tokens;
  }

  public void setTokens(String[] tokens) {
    this.tokens = tokens;
  }

}
