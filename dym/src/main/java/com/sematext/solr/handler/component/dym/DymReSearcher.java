/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.solr.handler.component.dym;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sematext.solr.handler.component.AbstractReSearcherComponent;
import com.sematext.solr.handler.component.ReSearcherRequestContext;
import com.sematext.solr.handler.component.ReSearcherUtils;
import com.sematext.solr.handler.component.dym.processor.CreateNewSuggestionsProcessor;
import com.sematext.solr.handler.component.dym.processor.FindTopFrequencyProcessor;
import com.sematext.solr.handler.component.dym.processor.OrderByFrequencyAndDistanceProcessor;
import com.sematext.solr.handler.component.dym.processor.SuggestionRatioProcessor;

public class DymReSearcher extends AbstractReSearcherComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DymReSearcher.class);

  public static final String COMPONENT_NAME = "DymReSearcher";

  /**
   * If original query returned more than maxOriginalResultsToReturnResults results, this component will not return
   * results for the best suggestion.
   */
  private int maxOriginalResultsToReturnResults = 5;

  /**
   * The max number of suggestions returned by this component. The higher the number, the better suggestion will be
   * returned (since more suggestions will be tried).
   */
  private int suggestionsToReturn = 5;

  /**
   * If some suggestion returns less than minResultsForAcceptableSuggestion, it will be discarded.
   */
  private int minResultsForAcceptableSuggestion = 5;

  /**
   * cannot be set from configuration; it is set to true in case minResultsForGoodSuggestion or
   * maxSuggestionsToTryForFgs is defined;
   * 
   * A "good" suggestion is the one which returns enough results to "safely" assume it as something that should be
   * presented to the client (there is a difference between acceptable and good suggestion - good suggestion algorithm
   * searches for first suggestion with at least minResultsForGoodFgsSuggestion results; acceptable suggestion algorithm
   * searches for multiple suggestions that have at least minResultsForAcceptableSuggestion results).
   */
  private boolean useFgsAlgorithm = false;

  /**
   * The minimal number of results some suggestion has to return to label it as a 'good' suggestion
   */
  private int minResultsForGoodFgsSuggestion = 50;

  /**
   * The maximal number of suggestions to be tried in searching for first 'good' suggestion.
   */
  private int maxSuggestionsToTryForFgs = 100;

  /**
   * Defines if FGS algorithm works in a mode a) or b). In case value is false, it works in mode a), otherwise in mode
   * b). Cannot be set from configuration, it is implicitly set to true in case property minResultsForGoodFgsSuggestion
   * is defined, otherwise it defaults to false.
   * 
   * Mode a) works by comparing the number of hits a suggestion produces to the number of hits of the original query. In
   * case a suggestion produces at least ((number of hits of original query + 1) * 2) results, it is classified as
   * "good suggestion".
   * 
   * Mode b) works by comparing the number of hits a suggestion produces to the value defined by
   * minResultsForGoodFgsSuggestion. In case it exceeds (or equals) that value, a suggestion is "good".
   */
  private boolean fgsModeFixedMinResults = false;

  /**
   * The minimal acceptable ratio of the number of found suggestions for some term and the number of requested
   * suggestions (defined by spellcheck.count attribute) to consider suggestions for that term. If the ratio doesn't
   * satisfy this condition, that term will be considered correctly spelled. The lower the value, the more correctly
   * spelled words will be inspected.
   * 
   * For instance, words with low ratio are generally already correctly spelled, so there is no need to try
   * spellchecker's suggestions. Words with high ratio (close to 1.0) are more often incorrectly spelled.
   * 
   * Default value is 0.0. For very small indices, use default value 0.0. For very large indices, use spellcheck.count
   * at around 100 and value for this field 0.75.
   */
  private float minRequiredSuggestionRatio = 0.00f;

  /**
   * Same meaning as minRequiredSuggestionRatio, but this ratio is used when original query had no hits at all.
   */
  private float minRequiredSuggestionRatioForZeroHits = 0.00f;

  // TODO : these two values can probably aren't flexible enough. It would be better to have two properties, one
  // which
  // defines range, and other which defines ratio for each range (of initial query hits). For instance, first would have
  // value like:
  // "4:10:100", and the other "0.25:0.35:0.55:0.75". The meaning would be :
  // - for range 0 to 4, use ratio 0.25
  // - for range 5 to 10, use ratio 0.35
  // - for range 11:100 use 0.55
  // - for range >= 101, use 0.75

  /**
   * Defines if ResSearcher should use Spellchecker's collation as one of suggestions to try. Also, in case ReSearcher
   * doesn't come up with any suggestions, it defines if it should at least return results for Spellchecker's collation.
   * Default is false, so, ReSearcher would use collation as one of possible suggestions.
   */
  private boolean ignoreCollation = false;

  /**
   * Defines if ReSearcher is to use generic ordering of original Spellchecker or if it should use its custom algorithm
   * to sort suggestions before trying any of them. This custom algorithm uses both similarity and frequency to
   * determine ordering, while generic ordering sorts only by similarity. Default is false, hence this custom algorithm
   * would be used.
   */
  private boolean useScGenericOrdering = false;

  @Override
  @SuppressWarnings("rawtypes")
  public void init(NamedList args) {
    super.init(args);
    maxOriginalResultsToReturnResults = getInt(args, "maxOriginalResultsToReturnResults");
    suggestionsToReturn = getInt(args, "suggestionsToReturn");
    minResultsForAcceptableSuggestion = getInt(args, "minResultsForAcceptableSuggestion");
    minResultsForGoodFgsSuggestion = getInt(args, "minResultsForGoodFgsSuggestion");
    maxSuggestionsToTryForFgs = getInt(args, "maxSuggestionsToTryForFgs");
    minRequiredSuggestionRatio = getFloat(args, "minRequiredSuggestionRatio");
    minRequiredSuggestionRatioForZeroHits = getFloat(args, "minRequiredSuggestionRatioForZeroHits");
    ignoreCollation = getBoolean(args, "ignoreCollation");
    useScGenericOrdering = getBoolean(args, "useScGenericOrdering");
  }

  @Override
  public void doProcess(ReSearcherRequestContext ctx, ResponseBuilder rb) throws Exception {
    SolrParams params = rb.req.getParams();
    int spellcheckCount = params.getInt(SpellCheckComponent.SPELLCHECK_COUNT, 0);
    long originalQueryHits = ctx.getOriginalQueryHits();
    String loggingHeader = "Searching for suggestions for query : '" + ctx.getOriginalQueryString() + "'";

    @SuppressWarnings("rawtypes")
    NamedList suggestions = ReSearcherUtils.extractSpellcheckerSuggestions(rb);
    Set<String> newSuggestions = null;

    SuggestionsFoundRatioCalculator ratioCalc = new SuggestionsFoundRatioCalculator(
        minRequiredSuggestionRatioForZeroHits, minRequiredSuggestionRatio);
    float highestRatio = 0.0f;
    FindMisspelingProcessor findMisspellingProcessor = new FindMisspelingProcessor(originalQueryHits, spellcheckCount,
        ratioCalc, highestRatio);
    ReSearcherUtils.iterateOverSpellcheckerSuggestionsForAllIncorrectWords(suggestions, findMisspellingProcessor);

    String misspellingWithHighestRatio = findMisspellingProcessor.getMisspellingWithHighestRatio();
    String misspelling = findMisspellingProcessor.getMisspelling();
    int countOfMisspellings = findMisspellingProcessor.getCountOfMisspellings();
    Float highestRatioFloat = findMisspellingProcessor.getHighestRatio();

    if (countOfMisspellings > 1) {
      // in this case, use the misspelling with highest ratio
      misspelling = misspellingWithHighestRatio;
    }

    if (misspelling == null) {
      // System.out.println("Since no terms satisfy minRatio condition, ReS is exiting.");
      return;
    }

    boolean isCustomSpellcheckingSuggestionPossible = checkCustomSpellcheckingSuggestionPossible(rb, suggestions,
        spellcheckCount, originalQueryHits);
    boolean isSpellcheckedQueryNeeded = checkSpellcheckedQueryNeeded(rb.rsp, suggestions, originalQueryHits);

    SortedSet<SortableSuggestion> sortedSet = new TreeSet<SortableSuggestion>();
    List<String> sortedSuggestionsList = null;

    if (isCustomSpellcheckingSuggestionPossible) {
      LOG.info(loggingHeader);
      newSuggestions = createSuggestions(rb, suggestions, ratioCalc, spellcheckCount, originalQueryHits,
          highestRatioFloat);

      // first find top frequency
      FindTopFrequencyProcessor processor = new FindTopFrequencyProcessor();
      ReSearcherUtils.iterateOverSpellcheckerSuggestionsForWord(suggestions, misspelling, processor);

      OrderByFrequencyAndDistanceProcessor orderingProcessor = new OrderByFrequencyAndDistanceProcessor(
          newSuggestions.iterator(), ignoreCollation, misspelling, processor.getTopFrequency());
      ReSearcherUtils.iterateOverSpellcheckerSuggestionsForWord(suggestions, misspelling, orderingProcessor);

      List<ScoredSuggestion> scoredSugs = orderingProcessor.getScoredSugs();

      // sort and pick top N. N comes from solrconfig
      if (useScGenericOrdering == false) {
        // only if generic ordering shouldn't be used
        Collections.sort(scoredSugs);
      }

      // LOG.info("Suggestions to try : " + scoredSugs);

      if (scoredSugs.size() != 0) {
        scoredSugs = scoredSugs.subList(0, 10 > scoredSugs.size() ? scoredSugs.size() : 10); // FIXME: N should come
                                                                                             // from solrconfig
        // go only through suggestions with best scores
        Iterator<ScoredSuggestion> iter = scoredSugs.iterator();
        int counter = 0;

        while (iter.hasNext()) {

          ScoredSuggestion scoredSuggestion = (ScoredSuggestion) iter.next();
          String sugQuery = scoredSuggestion.sug;

          // LOG.info(loggingHeader + " testING suggestion '" + sugString);

          // set new query string into rb
          long hits = ctx.getHandler().handleSuggestionHitsRequest(ctx, sugQuery, getComponentNames());

          if (hits >= minResultsForAcceptableSuggestion) {
            SortableSuggestion sug = new SortableSuggestion(sugQuery, hits, scoredSuggestion.distance);

            sortedSet.add(sug);
            LOG.info(loggingHeader + " tested suggestion '" + sugQuery + "' which resulted with " + hits + " hits");
          } else {
            // LOG.info(loggingHeader + " tested suggestion '" + sugString + "' which resulted with " + countResults +
            // " hits");
          }

          if (useFgsAlgorithm) {
            counter++;

            if (fgsModeFixedMinResults == true) {
              // in this case, check if this is good suggestion
              if (hits >= minResultsForGoodFgsSuggestion) {
                // if this is the case, we should stop processing suggestions and return what we got so far, since
                // this suggestion satisfies 'good' suggestion condition
                LOG.info("Searching for suggestions for query : '" + ctx.getOriginalQueryString()
                    + "' using FGS finds suggestion '" + sugQuery + "' with " + hits + " hits");
                break;
              } else {
                // System.out.println("FGS suggestion rejected : " + sugString + ", count results : " + countResults +
                // ", minForFGS :" + minResultsForGoodFgsSuggestion);
              }
            } else {
              long threshold = (originalQueryHits + 1) * 2;

              if (hits >= threshold) {
                LOG.info("Searching for suggestions for query : '" + ctx.getOriginalQueryString()
                    + "' using FGS in mode ((original_hits + 1) * 2) finds suggestion '" + sugQuery + "' with " + hits
                    + " hits");
                break;
              }
            }

            // also, in case max number of suggestions to try is exceeded, stop
            if (counter >= maxSuggestionsToTryForFgs) {
              break;
            }
          }
        }
      }

      sortedSuggestionsList = extractList(sortedSet);

      ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName(), sortedSuggestionsList);
      ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName() + "_hit_counts", extractHits(sortedSet));
      LOG.info(loggingHeader + " writes to extended_spellchecker_suggestions values : " + sortedSuggestionsList);
    }

    if (isSpellcheckedQueryNeeded == false) {
      return;
    }

    String collation = ReSearcherUtils.extractSpellcheckerCollation(rb);
    String bestSpellcheckedQuery = null;

    if (sortedSuggestionsList != null && !sortedSuggestionsList.isEmpty()) {
      bestSpellcheckedQuery = sortedSuggestionsList.get(0);
    }

    if (isCustomSpellcheckingSuggestionPossible == false && isSpellcheckedQueryNeeded == true
        && ignoreCollation == false) {
      // in this case, we'll run spellchecked query with collation value (the only possible solution, if there is
      // collation),
      // so we have to add that value as suggestion list

      if (collation != null) {
        List<String> sug = new ArrayList<String>();
        sug.add(collation);

        ReSearcherUtils.addValueToResult(rb, getSuggestionsTagName(), sug);

        // FIXME: is this correct? maybe I should check if collation also has some results,
        // before deciding I should query with it?
        bestSpellcheckedQuery = collation;
      } else {
        return;
      }
    }

    if (bestSpellcheckedQuery != null) {
      // set new query string into rb
      rb.req.setParams(ctx.getParams());
      SolrQueryResponse rsp = ctx.getHandler().handleSuggestionResponseRequest(ctx, bestSpellcheckedQuery, getComponentName(), rb);
      ReSearcherUtils.addComponentResponsesToResult(rb.rsp, rsp, "spellchecked");
    }

    // perform highlighting
    performHighlighting(ctx, rb);
  }

  @Override
  public boolean checkComponentShouldProcess(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, false)) {
      return false;
    }
    if (!params.getBool(SpellCheckComponent.COMPONENT_NAME, false)) {
      return false;
    }

    return true;
  }

  @Override
  protected String getBestSuggestionResultsTagName() {
    return "spellchecked_response";
  }

  @Override
  protected String getSuggestionsTagName() {
    return "extended_spellchecker_suggestions";
  }

  @Override
  public String getComponentName() {
    return COMPONENT_NAME;
  }

  @Override
  public String getDescription() {
    return "DymReSeacher";
  }

  @SuppressWarnings("rawtypes")
  private Set<String> createSuggestions(ResponseBuilder rb, NamedList suggestions,
      SuggestionsFoundRatioCalculator ratioCalc, int spellcheckCount, long originalQueryHits, Float highestRatio) {
    SuggestionRatioProcessor ratioCalcProcessor = new SuggestionRatioProcessor(originalQueryHits, spellcheckCount,
        ratioCalc, highestRatio);
    ReSearcherUtils.iterateOverSpellcheckerSuggestionsForAllIncorrectWords(suggestions, ratioCalcProcessor);

    Set<String> newSuggestions = null;

    if (ratioCalcProcessor.getSuggestionWithHighestRatio() != null) {
      CreateNewSuggestionsProcessor newSuggestionsProcessor = new CreateNewSuggestionsProcessor(rb.getQueryString(),
          ratioCalcProcessor.getSuggestionWithHighestRatio());
      ReSearcherUtils.iterateOverSpellcheckerSuggestionsForWord(ratioCalcProcessor.getSuggestionWithHighestRatio(),
          newSuggestionsProcessor);
      newSuggestions = newSuggestionsProcessor.getNewSuggestions();
    }

    if (!ignoreCollation) {
      newSuggestions.add((String) suggestions.get("collation"));
    }

    return newSuggestions;
  }

  @SuppressWarnings("rawtypes")
  private boolean checkCustomSpellcheckingSuggestionPossible(ResponseBuilder rb, NamedList suggestions,
      int spellcheckCount, long originalQueryHits) {
    if (suggestions == null || suggestions.size() == 0) {
      // if there are no spellchecking suggestions, we can't create spellchecking suggestions
      return false;
    }

    if (originalQueryHits > getMaxOriginalResults()) {
      return false;
    }

    return true;
  }

  @SuppressWarnings("rawtypes")
  private boolean checkSpellcheckedQueryNeeded(SolrQueryResponse rsp, NamedList suggestions, long originalQueryHits) {
    if (suggestions == null || suggestions.size() == 0) {
      // if there are no spellchecking suggestions, we can't run the spellchecked query
      return false;
    }
    if (originalQueryHits > maxOriginalResultsToReturnResults) {
      // if there are enough results already, no need to run the spellchecked query
      return false;
    }

    return true;
  }

  private List<String> extractList(SortedSet<SortableSuggestion> sortedSet) {
    List<String> list = new ArrayList<String>();

    for (SortableSuggestion s : sortedSet) {
      list.add(s.getSuggestion());

      // return just top suggestionsToReturn suggestions
      if (list.size() >= suggestionsToReturn) {
        break;
      }
    }

    return list;
  }

  private Map<String, Long> extractHits(SortedSet<SortableSuggestion> sortedSet) {
    Map<String, Long> map = new HashMap<String, Long>();

    for (SortableSuggestion s : sortedSet) {
      map.put(s.getSuggestion(), s.getNumHits());

      // return just top suggestionsToReturn suggestions
      if (map.size() >= suggestionsToReturn) {
        break;
      }
    }

    return map;
  }
}
