/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class with useful utility methods for handling "common misspellings" functionality in different Researcher components.
 *
 * @author sematext, http://www.sematext.com/
 */
public class CommonMisspellings {
  // private static final Logger LOG = LoggerFactory.getLogger(CommonMisspellings.class);

  public static Map<String, String> loadCommonMisspellingsFile(String commonMisspellingsFileLocation) {
    Map<String, String> commonMisspellingsMap = null;
    
    if (commonMisspellingsFileLocation != null) {
      commonMisspellingsMap = new HashMap<String, String>();
      BufferedReader br = null;
      FileReader fr = null;
      
      try {
        fr = new FileReader(commonMisspellingsFileLocation);
        br = new BufferedReader(fr);
        
        while (true) {
          String line = br.readLine();
          
          if (line == null || line.trim().length() == 0) {
            break;
          }
          
          int indexOfSeparator = line.indexOf("->");
          String incorrect = line.substring(0, indexOfSeparator);
          String correct = line.substring(indexOfSeparator + 2);
          
          indexOfSeparator = correct.indexOf(",");
          
          if (indexOfSeparator != -1) {
            correct = correct.substring(0, indexOfSeparator);
          }
          
          // add to the map; no need to lowercase correct spelling (we want America to remain America)
          commonMisspellingsMap.put(incorrect.toLowerCase(), correct);
        }
      } catch (Throwable thr) {
        String msg = "Error while parsing input file '" + commonMisspellingsFileLocation + "'!";
        throw new IllegalArgumentException(msg, thr);
      }
      finally {
        if (fr != null) {
          try {
            fr.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        if (br != null) {
          try {
            br.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return commonMisspellingsMap;
  }
  
  public static String correctedQuery(String originalQuery, Map<String, String> commonMisspellingsMap) {
    boolean misspellingFound = false;
    // else, check if there are any misspellings
    
    StringTokenizer str = new StringTokenizer(originalQuery, " ");
    StringBuilder sb = new StringBuilder();
    
    while (str.hasMoreTokens()) {
      String token = str.nextToken();
      String fieldName = null;
      String fieldValue = null;
      
      // support for field names
      int indexOfFieldSeparator = token.indexOf(':');
      if (indexOfFieldSeparator != -1) {
        if (token.charAt(indexOfFieldSeparator - 1) == '\\') {
          // if it is escaped,it has to be a part of the value; since that was the first occurrence, there is no
          // field name part
          ;
        }
        else {
          // separate name and value; take care of phrases
          fieldName = token.substring(0, indexOfFieldSeparator);
          fieldValue = token.substring(indexOfFieldSeparator + 1);
        }
      } else {
        fieldValue = token;
      }
      
      // now check the value and create new value, if needed
      // each field can start or end with a quote, so we have to remove it before testing
      if (fieldValue.trim().equals("\"")) {
        // just add it to the string
        sb.append("\" ");
      } else {
        boolean quoteAtStart = fieldValue.startsWith("\"");
        boolean quoteAtEnd = fieldValue.endsWith("\"");
        
        if (quoteAtStart) {
          fieldValue = fieldValue.substring(1);
        }
        if (quoteAtEnd) {
          fieldValue = fieldValue.substring(0, fieldValue.length() - 1);
        }
        
        String correctSpelling = commonMisspellingsMap.get(fieldValue);
        
        if (correctSpelling != null) {
          fieldValue = correctSpelling;
          misspellingFound = true;
        }
        
        if (fieldName != null) {
          sb.append(fieldName);
          sb.append(":");
        }
        
        if (quoteAtStart) {
          sb.append("\"");
        }
        
        sb.append(fieldValue);
        
        if (quoteAtEnd) {
          sb.append("\"");
        }

        sb.append(" ");
      }
    }
    
    if (misspellingFound) {
      return sb.toString().trim();
    } else {
      return null;
    }
  }
}