/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.Version;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.response.SolrQueryResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class with useful utility methods.
 *
 * @author sematext, http://www.sematext.com/
 */
@SuppressWarnings({ "rawtypes" })
public class ReSearcherUtils {
  public static final String INTERNAL_QUERY_MARKER_PARAM = "internalQuery";

  public static long extractOriginalQueryHits(ResponseBuilder rb) {
    long hits = 0;
    if (rb.rsp != null) {
      Number hitsInteger = (Number) rb.rsp.getToLog().get("hits");
      if (hitsInteger == null) {
        hits = rb.getNumberDocumentsFound();
      } else {
        hits = hitsInteger.longValue();
      }
    }
    return hits;
  }
  
  /**
   * Returns spellchecker's suggestions from original response. In case there are no suggestions, returns null.
   * 
   * @param rb .
   * @return .
   */
  public static NamedList extractSpellcheckerSuggestions(ResponseBuilder rb) {
    if (rb.rsp.getValues().get("spellcheck") == null) {
      return null;
    }
    
    return (NamedList) ((SimpleOrderedMap) rb.rsp.getValues().get("spellcheck")).get("suggestions");
  }
  
  /**
   * Extract spellchecker's collation from original response or null if there was no collation.
   * @param rb .
   * @return .
   */
  public static String extractSpellcheckerCollation(ResponseBuilder rb) {
    if (rb.rsp.getValues().get("spellcheck") != null) {
      if (((SimpleOrderedMap) rb.rsp.getValues().get("spellcheck")).get("suggestions") != null) {
        return (String) ((NamedList) ((SimpleOrderedMap) rb.rsp.getValues().get("spellcheck")).get("suggestions")).get("collation");
      }
    }
   
    return null;
  }
  
  /**
   * Contains logic for iterating over spellchecker's suggestions. If any of input parameters is null, just exits. This method
   * iterates over suggestions for one incorrect word (which was provided with parameter)
   * 
   * @param suggestions list of all suggestions, can be extracted with method .extractSpellcheckerSuggestions().
   * @param word term for which suggestions are being processed
   * @param processor instance of processor which will handle all suggestions for word
   */
  public static void iterateOverSpellcheckerSuggestionsForWord(NamedList suggestions, String word, SpellcheckerSuggestionProcessor processor) {
    if (suggestions == null || word == null || processor == null) {
      return;
    }
    NamedList stuff = (NamedList) suggestions.get(word);
    
    iterateOverSpellcheckerSuggestionsForWord(stuff, processor);    
  }

  /**
   * Contains logic for iterating over spellchecker's suggestions. If any of input parameters is null, just exits. This method
   * iterates over suggestions for one incorrect word (for which parameter <code>suggestions</code> should contain spellchecker's suggestions).
   * 
   * 
   * @param suggestions list of suggestions for some word, so word parameter isn't needed
   * @param processor instance of processor which will handle all suggestions for word
   */
  @SuppressWarnings("unchecked")
  public static void iterateOverSpellcheckerSuggestionsForWord(NamedList suggestions, SpellcheckerSuggestionProcessor processor) {
    int i = 4;
    // double topFreq = 0;
    
    if (suggestions.getVal(4) instanceof List) {
      // support for new version of Solr (valid after 2009-09-09 in version 1.4)
      List<SimpleOrderedMap> l = (List<SimpleOrderedMap>) suggestions.getVal(4);
      
      i = 0;
      
      while (true) {
        if (l.size() <= i) {
          break;
        }

        processor.process(l.get(i), (String) l.get(i).get("word"));
        
        i++;
      }
    }
    else {
      // old way, before 2009-09-09
      while (true) {
        if (suggestions.size() <= i) {
          break;
        }

        processor.process((NamedList) (suggestions).getVal(i), suggestions.getName(i));
        
        i++;
      }
    }
    
    processor.afterProcessingFinished();
  }

  /**
   * Contains logic for iterating over spellchecker's suggestions. If any of input parameters is null, just exits. This method
   * iterates over suggestions for ALL incorrectly spelled words. It will not sub-iterate over suggestions of each incorrect
   * word, that is the logic which SpellcheckerSuggestionProcessor should take care off.
   * 
   * @param suggestions list of all suggestions, can be extracted with method .extractSpellcheckerSuggestions().
   * @param word term for which suggestions are being processed
   * @param processor instance of processor which will handle all suggestions for word
   */
  @SuppressWarnings("unchecked")
  public static void iterateOverSpellcheckerSuggestionsForAllIncorrectWords(NamedList suggestions, SpellcheckerSuggestionProcessor processor) {
    if (suggestions == null || processor == null) {
      return;
    }

    for (int i = 0; i < suggestions.size(); i++) {
      // find first acceptable suggestion
      if (suggestions.getVal(i) instanceof SimpleOrderedMap) {
        processor.process((SimpleOrderedMap) suggestions.getVal(i), suggestions.getName(i));
      }
    }
    
    processor.afterProcessingFinished();
  }

  /**
   * Adds some values (most likely from (List) to response which will be sent to the client. The values will be added into 
   * root level under tag name specified by tagName
   * 
   * @param rb .
   * @param tagName .
   * @param value .
   */
  public static void addValueToResult(ResponseBuilder rb, String tagName, Object value) {
    rb.rsp.add(tagName, value);
  }
  
  /**
   * Utility method for creating map which should be passed into ReSearcherUtils.customPrepare as parametersToReplace when
   * only q value is changed. Other similar methods will not be provided, since changed q is the most used situation.
   * 
   * @param newQueryValue .
   * @return .
   */
  public static Map<String, List<String>> createParametersToReplaceMapWhenOnlyQueryChanged(String newQueryValue) {
    Map<String, List<String>> parametersToReplace = new HashMap<String, List<String>>();
    List<String> newQuery = new ArrayList<String>();
    newQuery.add(newQueryValue);
    parametersToReplace.put("q", newQuery);
    
    return parametersToReplace;
  }
  
  /**
   * Utility method for creating map which should be passed into ReSearcherUtils.customPrepare .
   * 
   * @param str .
   * @return .
   */
  public static Map<String, List<String>> createParameters(String... str) {
    Map<String, List<String>> parameters = new HashMap<String, List<String>>();

    for(int i = 0; i < str.length / 2; i++) {
      List<String> newQuery = new ArrayList<String>();
      newQuery.add(str[i*2+1]);
      parameters.put(str[i*2], newQuery);
    }
    
    return parameters;
  }
  
  /**
   * Separates tokens from query. Treats each quote as a separate token, since that makes it easier to examine the query.
   * 
   * @param queryString .
   * @param tokens .
   * @return number of quotes in the query
   */
  public static int tokenizeQueryString(String queryString, List<String> tokens) {
    int countOfQuotes = 0;
    
    try {
      // first tokenize words and treat each quote as a separate token
      Map<String,String> args = new HashMap<String, String>();
      args.put(WhitespaceTokenizerFactory.LUCENE_MATCH_VERSION_PARAM, Version.LUCENE_6_0_1.toString());
      WhitespaceTokenizerFactory f = new WhitespaceTokenizerFactory(args);
      
      WhitespaceTokenizer s = (WhitespaceTokenizer)f.create(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
      s.setReader(new StringReader(queryString));
      s.reset();
      
      while (true) {
        CharTermAttribute t = s.getAttribute(CharTermAttribute.class);

        if (t == null) {
          break;
        }
        
        String tokentText = new String(t.toString());
        
        if (tokentText.equals("\"")) {
          tokens.add("\"");
          countOfQuotes++;
        } else if (tokentText.startsWith("\"")) {
          tokens.add("\"");
          countOfQuotes++;
          
          if (tokentText.endsWith("\"")) {
            tokens.add(tokentText.substring(1, tokentText.length() - 1));
            tokens.add("\"");
            countOfQuotes++;
          } else {
            tokens.add(tokentText.substring(1));
          }
        } else if (tokentText.endsWith("\"")) {
          tokens.add(tokentText.substring(0, tokentText.length() - 1));
          tokens.add("\"");
          countOfQuotes++;
        } else if (!tokentText.trim().equals("")) {
          // take into account only if different than empty string
          tokens.add(tokentText);
        }
        
        if (!s.incrementToken()) {
          break;
        }
      }
      s.end();
      s.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return countOfQuotes;
  }
  
  public static void addComponentResponsesToResult(SolrQueryResponse rsp, SolrQueryResponse newRsp, String prefix) {
    rsp.add(prefix + "_response", newRsp.getValues().get("response"));
    rsp.add(prefix + "_grouped", newRsp.getValues().get("grouped"));
    rsp.add(prefix + "_facet_counts", newRsp.getValues().get("facet_counts"));
    rsp.add(prefix + "_terms", newRsp.getValues().get("terms"));
    rsp.add(prefix + "_termVectors", newRsp.getValues().get("termVectors"));
    rsp.add(prefix + "_highlighting", newRsp.getValues().get("highlighting"));
    rsp.add(prefix + "_stats", newRsp.getValues().get("stats"));
  }
}