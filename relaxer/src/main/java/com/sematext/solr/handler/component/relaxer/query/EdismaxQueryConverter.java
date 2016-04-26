/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.relaxer.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DisMaxQParser;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 
 * {@link QueryConverter} that converts query to a list of {@link Clause}s.
 * 
 * @author sematext, http://www.sematext.com/
 */
public class EdismaxQueryConverter extends QueryConverter {
  private static final Logger LOG = LoggerFactory.getLogger(EdismaxQueryConverter.class);

  @Override
  public List<Clause> convert(String query, SolrQueryRequest req) {
    try {
      Map<String, Float> queryFields = DisMaxQParser.parseQueryFields(req.getSchema(), req.getParams());
      return splitIntoClauses(query, queryFields.keySet(), false);
    } catch (SyntaxError e) {
      throw new RuntimeException();
    }
  }

  private List<Clause> splitIntoClauses(String s, Set<String> defaultFields, boolean ignoreQuote) {
    ArrayList<Clause> lst = new ArrayList<Clause>(4);
    Clause clause;

    int pos = 0;
    int end = s.length();
    char ch = 0;
    int start;
    boolean disallowUserField;
    while (pos < end) {
      clause = new Clause();
      disallowUserField = true;

      ch = s.charAt(pos);

      while (Character.isWhitespace(ch)) {
        if (++pos >= end)
          break;
        ch = s.charAt(pos);
      }

      start = pos;

      if (ch == '+' || ch == '-') {
        clause.must = ch;
        pos++;
      }

      clause.field = getFieldName(s, pos, end);
      if (clause.field != null) {
        disallowUserField = false;
        int colon = s.indexOf(':', pos);
        clause.rawField = s.substring(pos, colon);
        pos += colon - pos; // skip the field name
        pos++; // skip the ':'
      }

      if (pos >= end)
        break;

      char inString = 0;

      ch = s.charAt(pos);
      if (!ignoreQuote && ch == '"') {
        clause.isPhrase = true;
        inString = '"';
        pos++;
      }

      StringBuilder sb = new StringBuilder();
      while (pos < end) {
        ch = s.charAt(pos++);
        if (ch == '\\') { // skip escaped chars, but leave escaped
          sb.append(ch);
          if (pos >= end) {
            sb.append(ch); // double backslash if we are at the end of the string
            break;
          }
          ch = s.charAt(pos++);
          sb.append(ch);
          continue;
        } else if (inString != 0 && ch == inString) {
          inString = 0;
          break;
        } else if (Character.isWhitespace(ch)) {
          clause.hasWhitespace = true;
          if (inString == 0) {
            // end of the token if we aren't in a string, backing
            // up the position.
            pos--;
            break;
          }
        }

        if (inString == 0) {
          switch (ch) {
            case '!':
            case '(':
            case ')':
            case ':':
            case '^':
            case '[':
            case ']':
            case '{':
            case '}':
            case '~':
            case '*':
            case '?':
            case '"':
            case '+':
            case '-':
            case '\\':
            case '|':
            case '&':
            case '/':
              clause.hasSpecialSyntax = true;
              sb.append('\\');
          }
        } else if (ch == '"') {
          // only char we need to escape in a string is double quote
          sb.append('\\');
        }
        sb.append(ch);
      }

      clause.val = sb.toString();
      Analyzer analyzer = findField(clause.field, defaultFields, getFieldAnalyzerMaps());
      if (analyzer != null) {
        try {
          clause.tokens = analyze(sb.toString(), analyzer);
        } catch (IOException e) {
          LOG.warn("Analysis text:" + sb.toString() + ": failed by " + analyzer.toString(), e);
        }
      }

      if (clause.isPhrase) {
        if (inString != 0) {
          // detected bad quote balancing... retry
          // parsing with quotes like any other char
          return splitIntoClauses(s, defaultFields, true);
        }

        // special syntax in a string isn't special
        clause.hasSpecialSyntax = false;
      } else {
        // an empty clause... must be just a + or - on it's own
        if (clause.val.length() == 0) {
          clause.syntaxError = true;
          if (clause.must != 0) {
            clause.val = "\\" + clause.must;
            clause.must = 0;
            clause.hasSpecialSyntax = true;
          } else {
            // uh.. this shouldn't happen.
            clause = null;
          }
        }
      }

      if (clause != null) {
        if (disallowUserField) {
          clause.raw = s.substring(start, pos);
          // escape colons, except for "match all" query
          if (!"*:*".equals(clause.raw)) {
            clause.raw = clause.raw.replaceAll(":", "\\\\:");
          }
        } else {
          clause.raw = s.substring(start, pos);
        }
        lst.add(clause);
      }
    }

    return lst;
  }

  private Analyzer findField(String field, Set<String> defaultFields, Map<Pattern, Analyzer> fieldAnalyzerMaps) {
    if (fieldAnalyzerMaps != null && !fieldAnalyzerMaps.isEmpty()) {
      if (field == null) {
        if (defaultFields != null) {
          for (String defaultField : defaultFields) {
            Analyzer analyzer = findAnalyzer(defaultField, fieldAnalyzerMaps);
            if (analyzer != null) {
              return analyzer;
            }
          }
        }
      } else {
        return findAnalyzer(field, fieldAnalyzerMaps);
      }

    }

    return null;
  }

  private Analyzer findAnalyzer(String field, Map<Pattern, Analyzer> fieldAnalyzerMaps) {
    for (Pattern pattern : fieldAnalyzerMaps.keySet()) {
      if (pattern.matcher(field).find()) {
        return fieldAnalyzerMaps.get(pattern);
      }
    }
    return null;
  }

  /**
   * returns a field name from the current position of the string
   */
  private String getFieldName(String s, int pos, int end) {
    if (pos >= end)
      return null;
    int p = pos;
    int colon = s.indexOf(':', pos);
    // make sure there is space after the colon, but not whitespace
    if (colon <= pos || colon + 1 >= end || Character.isWhitespace(s.charAt(colon + 1)))
      return null;
    char ch = s.charAt(p++);
    while ((ch == '(' || ch == '+' || ch == '-') && (pos < end)) {
      ch = s.charAt(p++);
      pos++;
    }
    if (!Character.isJavaIdentifierPart(ch))
      return null;
    while (p < colon) {
      ch = s.charAt(p++);
      if (!(Character.isJavaIdentifierPart(ch) || ch == '-' || ch == '.'))
        return null;
    }
    String fname = s.substring(pos, p);

    return fname;
  }

  protected String[] analyze(String text, Analyzer analyzer) throws IOException {
    List<String> result = new ArrayList<String>();
    TokenStream stream = analyzer.tokenStream("", new StringReader(text));
    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
    stream.reset();
    while (stream.incrementToken()) {
      result.add(new String(termAtt.buffer(), 0, termAtt.length()));
    }
    stream.end();
    stream.close();

    return result.toArray(new String[result.size()]);
  }
}
