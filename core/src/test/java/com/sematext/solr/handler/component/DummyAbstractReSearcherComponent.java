/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.solr.handler.component.ResponseBuilder;

import java.io.IOException;

import com.sematext.solr.handler.component.AbstractReSearcherComponent;
import com.sematext.solr.handler.component.ReSearcherRequestContext;

public class DummyAbstractReSearcherComponent extends AbstractReSearcherComponent {

  @Override
  protected boolean checkComponentShouldProcess(ResponseBuilder rb) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected void doProcess(ReSearcherRequestContext ctx, ResponseBuilder rb) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  protected String getBestSuggestionResultsTagName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getSuggestionsTagName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected String getComponentName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

}