# Guide: DYM Searcher

### What is DYM ReSearcher?
Solr ships with a Spellchecker component that can be used to obtain simple suggestions for misspelled query terms. The query term suggestions offered by this Spellchecker are the ones that are the most similar to those from the original query, and optionally more popular or frequent than the misspelled term. As such, these suggestions can be of limited utility. For example, for query like “amerik”, the Spellchecker might suggest “amerika” because that term is “close” to the original misspelled term “amerik”. Even if we instruct the Spellchecker to suggest only more popular terms, if there are enough instances of “amerik”, that suggestion will still be offered. Yet, “america” is probably the suggestion you'd want Spellchecker to make.

The above example is the first case where DYM ReSearcher helps. DYM ReSearcher is a component that builds on top of the Solr Spellchecker (often referred to as providing “Did You Mean” or DYM functionality) and improves it by considering not only term similarity, but also the “meaningfulness” of the whole query. This is done by trying different possible suggestions and choosing either the one which yields the most hits or the first one that produces at least N hits (where N is defined in configuration). In the latter case, the order in which suggestions are tried is obviously important, so DYM ReSearcher first orders possible suggestions by combination of weighted similarity and popularity combination of suggested words. This way, it would push the word america above amerika, if america were a more popular term in the index.

Another issue with the original Solr's Spellchecker occurs when a query is a phrase or a multi-term query. For instance, for a misspelled query like “harry kotter”, the Spellchecker might suggest “harry kotler” if the index contains the word “kotler”, even though there are actually no documents in the index that match that query. Using this suggested phrase as the query would result in no matches (or a small number of unwanted matches if the suggested misspelled terms happen to be present in the index) and lead to a suboptimal search experience. Nobody needs “dumb” query suggestions, so we created DYM ReSearcher.

### DYM ReSearcher to the rescue
DYM ReSearcher processes each incorrectly spelled query in 2 steps:

1. Fixing of common language-specific misspellings – in case a client's query returned too few results, and DYM ReSearcher's property commonMisspellingsFile is defined in the configuration file (`solrconfig.xml`), DYM ReSearcher will first check if there are any common language misspellings. If there are, DYM ReSearcher will fix them according to the definition in the file and will use the corrected query from here on. If such query returns the satisfying number of results, that query and its results will be returned to the client application. Otherwise, DYM ReSearcher goes to step 2.
2. Correcting query based on original Spellchecker's suggestions.

In step 2, DYM ReSearcher uses Spellchecker’s own suggestions, but scores and orders suggestions based on metrics different from the Spellchecker and controlled via configuration settings. Not only does DYM ReSearcher pick better suggestions than Solr Spellchecker, it also immediately queries Solr and delivers the results for the corrected query composed using the best suggested term(s) without the client having to do any extra work. These results are delivered along the results for the original query, so the client can still choose which results to use.

In addition to results themselves, DYM ReSearcher will also return the facets if they were requested, as well as highlighting data for the best suggestion. In previous examples, DYM ReSearcher would return and immediately run queries “america” and “harry potter”, returning their results in the response. This avoids having to build that logic into one or more client applications, and avoids multiple trips to the backend.

### Deployment
DYM ReSearcher is packaged in two jar files and has several dependencies. All required jars are included in the package and can be found in the `lib/` subdirectory. The following jars need to be placed in the lib directory under Solr home:

* st-ReSearcher-core-VERSION .jar
* st-ReSearcher-dym-VERSION .jar
* slf4j-api-VERSION .jar
* slf4j-jcl-VERSION jar

Finally, the DYM ReSearcher component must be defined in the `solrconfig.xml`, as shown in the example below:

```xml
<searchComponent name="dymReSearcher"
  class="com.sematext.solr.handler.component.dym.DymReSearcher">
    <int name="maxOriginalResults">0</int>
    <int name="maxOriginalResultsToReturnResults">5</int>
    <int name="suggestionsToReturn">5</int>
    <int name="minResultsForAcceptableSuggestion">3</int>
    <float name="minRequiredSuggestionRatio">0.0</float>
</searchComponent>

<searchComponent name="dymReSearcherFirstGoodSuggestion"
  class="com.sematext.solr.handler.component.dym.DymReSearcher">
    <int name="maxOriginalResults">0</int>
    <int name="maxOriginalResultsToReturnResults">5</int>
    <int name="suggestionsToReturn">5</int>
    <int name="minResultsForAcceptableSuggestion">3</int>
    <int name="minResultsForGoodFgsSuggestion">6</int>
    <int name="maxSuggestionsToTryForFgs">2</int>
    <float name="minRequiredSuggestionRatio">0.0</float>
</searchComponent>
```
These are two different configurations, just for illustration purposes. Each configuration shows one mode of operation supported by DYM ReSearcher (you can find more about modes of operation further in the document). One can define as many different search component configurations as needed. Once defined, a component needs to be added to the appropriate request handler. For instance:

```xml
<requestHandler name="dismax" class="solr.SearchHandler" >
   <lst name="defaults">
     <str name="defType">dismax</str>
     <str name="echoParams">explicit</str>
     <float name="tie">0.01</float>
     <str name="fl">*</str>
     <str name="q.alt">*:*</str>
   </lst>
   <arr name="last-components">
     <str>spellcheck</str>
     <str>dymReSearcher</str>
   </arr>
   <lst name="invariants">
      <bool name="spellcheck.collate">true</bool>
   </lst>
</requestHandler>
```

Note that the Spellcheck component is set to run before our `dymReSearcher` component. This Spellcheck component is absolutely necessary and needs to run before the DYM ReSearcher component because DYM ReSearcher depends on its output. Furthermore,either spellcheck.collate or `spellcheck.ignoreCollation` should be specified and set to true. The Spellcheck component might be defined as shown below. The exact configuration depends on the specifics of the Solr setup.

```xml
<searchComponent name="spellcheck" class="solr.SpellCheckComponent">
    <str name="queryAnalyzerFieldType">textSpell</str>
    <lst name="spellchecker">
      <str name="name">default</str>
      <str name="field">spellchecker_field</str>
      <str name="spellcheckIndexDir">./spellchecker</str>
   </lst>
</searchComponent>
```

### Configuration settings 
There are multiple configuration settings for DYM ReSearcher. Not all need to be populated, because they all have their default values. Here is the list: 


Property | Meaning | Default value
-------- | ------- | -------------
`maxOriginalResults` | This component will try to find suggestions only if original query returned <= `maxOriginalResults` results. | Integer, 0
`maxOriginalResultsToReturnResults` | If original query returned more than `maxOriginalResultsToReturnResults` results, this component will not return results for the best suggestions. | Integer, 5
`suggestionsToReturn` | The max number of suggestions returned by this component. The higher the number, the better suggestion will be returned (since more suggestions will be tried). | Integer, 5
`minResultsForAcceptableSuggestion` | If some suggestion returns less than `minResultsForAcceptableSuggestion`, it will be discarded. | Integer, 5
`minResultsForGoodFgsSuggestion` | The minimal number of results some suggestion has to return to label it as a 'good' suggestion. | Integer, 50
`maxSuggestionsToTryForFgs` | The maximal number of suggestions to be tried in searching for first 'good' suggestion. | Integer, 100
`fgsModeFixedMinResults` | Defines if FGS algorithm works in a mode a) or b). In case value is false, it works in mode a), otherwise in mode b). Cannot be set from configuration, it is implicitly set to true in case property minResultsForGoodFgsSuggestion is defined, otherwise it defaults to false. <br> Mode a) works by comparing the number of hits a suggestion produces to the number of hits of the original query. In case a suggestion produces at least **((number of hits of original query + 1) * 2)** results, it is classified as "good suggestion". <br> Mode b) works by comparing the number of hits a suggestion produces to the value defined by `minResultsForGoodFgsSuggestion`. In case it exceeds (or equals) that value, a suggestion is "good".  | Boolean, false
`minRequiredSuggestionRatio` (for advanced usage) | The minimal acceptable ratio of the number of found suggestions for some term and the number of requested suggestions (defined by `spellcheck.count` attribute) to consider suggestions for that term. If the ratio doesn't satisfy this condition, that term will be considered correctly spelled. The lower the value, the more correctly spelled words will be inspected. <br> For instance, words with low ratio are generally already correctly spelled, so there is no need to try spellchecker's suggestions. Words with high ratio (close to 1.0) are more often incorrectly spelled. <br> Default value is 0.0. For very small indices, use default value 0.0. For very large indices, use spellcheck.count at around 100 and value for this field 0.75. | Float, 0.0
`minRequiredSuggestionRatioForZeroHits` (for advanced usage) | Same meaning as `minRequiredSuggestionRatio`, but this ratio is used when original query had no hits at all. | Float, 0.0
`ignoreCollation` | Defines if ReSearcher should use Spellchecker's collation as one of suggestions to try. Also, in case ReSearcher doesn't come up with any suggestions, it defines if it should at least return results for Spellchecker's collation. Default is false, so, ReSearcher would use collation as one of possible suggestions. | Boolean, false
`useScGenericOrdering` | Defines if ReSearcher is to use generic ordering of original Spellchecker, or if it should use its custom algorithm to sort suggestions before trying any of them. This custom algorithm uses both similarity and frequency to determine ordering, while generic ordering sorts only by similarity. Default is false, hence this custom algorithm would be used. | Boolean, false
`commonMisspellingsFile` | Defines the location of the file with definitions of common language misspellings. In case it is not defined, “fixing common misspellings” step will not be performed. | String, null

### Modes of Operation
There are two modes of work for DYM ReSearcher:

1. **Standard algorithm** – this is the algorithm where DYM ReSearcher finds all possible suggestions and tries each one of them. The resulting suggestion is the one which returns the most documents. The number of suggestions that will be tried depends on the number of suggestion for incorrect word generated by Solr's default `SpellcheckerComponent`. That number is affected by the `spellcheck.count` parameter. You'll need to consider the value you provide for `spellcheck.count`. If the number is low (for instance 5), you will minimize performance impact of DYM ReSearcher on your Solr server. However, the quality of suggestions will be affected. On the other hand, if you set it to 100, your Solr will be under heavier load, but you'll get higher quality of suggestions. Also, for small indices, large values don't have much sense, while for larger indices, you'll want it to be a bit higher. Our recommendation is to use a value between 10 and 20 and then test the quality of suggestions and the load on your server.

2. **“First Good Suggestion”** algorithm (aka FGS) – By using this algorithm, DYMReSearcher tries one suggestion it created after another, until it finds the first which returns `minResultsForGoodFgsSuggestion` or more results. This algorithm will in most cases put much lower load on your Solr server, since it will not try all possible suggestion. At most, it will try `maxSuggestionsToTryForFgs` suggestions it created, but in most cases it will find first good suggestion much earlier. This algorithm will be used if any one of the following parameters is defined: `minResultsForGoodFgsSuggestion` or `maxSuggestionsToTryForFgs`. Another important parameter to consider when using the FGS algorithm is `useScGenericOrdering`. This way, you can choose in which order suggestions are tried. This is important because it can affect which suggestion will be returned by DYM ReSearcher. In case you set this property to true, suggestions will be ordered by similarity and then each one of them will be tried. In most cases, a better solution is to set it to false. This way, DYM ReSearcher will order suggestions by combining similarity to original term with frequency of suggestion in the index (this frequency relates only to the term which was incorrect, not to the whole phrase). So, in case of query like “amerik”, suggestion “amerika” will be pushed lower in the list of suggestions, and suggestion “america” will be pushed higher, although “amerika” is more similar to original query (but obviously america makes more sense).

Another important setting when using the FGS algorithm is `fgsModeFixedMinResults` . It defines what is needed for some suggestion to be considered as “good” and therefore suggested by this algorithm. In case it is set to true, suggestion must return at least `minResultsForGoodFgsSuggestion` hits to be returned by DYM ReSearcher.If it is set to false,then it has to produce at least((number of hits of original query+1)*2) results.

### Fixing of Common Misspellings
For information about fixing common misspellings, please, refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### How DYM ReSearcher Creates Suggestions
Original Solr Spellchecker provides possible suggestions for each term that might be found in the phrase. DYM ReSearcher finds the term in query with the highest number of possible corrections (meaning: the word which has the most likely corrections that occur more frequently than the word itself in the index). Such term is considered as most likely to be incorrectly spelled, so DYM ReSearcher will try to replace it. That term must satisfy `minRequiredSuggestionRatio` and `minRequiredSuggestionRatioForZeroHits` parameters from configuration (if using default values, these parameters are always satisfied). In case no terms satisfy these parameters, DYM ReSearcher will consider original query as good enough and it will not provide any suggestions. However, if there are such terms, the one with the highest number of possible corrections is chosen to create new query suggestions. By tweaking these values you can affect when a query term will be considered as incorrect.

DYM ReSearcher doesn't make combinations of all possibly incorrectly spelled words in the query. That is currently one limitation of DYM ReSearcher and it exists primarily because of performance considerations. Future versions of DYM ReSearcher might support spellchecking of the whole phrases, though.

### How to Use DYM ReSearcher
After it is correctly deployed and configured on your Solr server, you can start using it. There are some mandatory parameters you'll have to provide in your query URLs to get correct results:

1. `spellcheck=true`
2. `spellcheck.onlyMorePopular=true`
3. `spellcheck.extendedResults=true`
4. `DymReSearcher=true`

By changing the value of DymReSearcher parameter to false, you can disable DYM ReSearcher for some queries (if you don’t need its suggestions).

The `spellcheck.count` parameter is optional because it has a default value, but you'll likely want to adjust it to suit your needs. Value of 20 is often a good starting point.

Also, since DYM ReSearcher depends on results of SpellcheckComponent, you'll have to build its index before using DYM ReSearcher.

DYM ReSearcher works best with dismax request handler. The reason for this is the format of the queries which are sent to dismax handler (no field names, no logical operators), but it also works with standard handler.

Here are a few examples of queries:

```
http://localhost:8080/solr/select/?q=harry
%20kotter&facet=true&facet.mincount=1&facet.field=foo&facet.field=bar&spellcheck=true&spellcheck.onlyMorePopular=true&spellcheck.count=20&spellcheck.extendedResults=true&qt=dismax_fgs&hl=true&DymReSearcher=true
```

```
http://localhost:8080/solr/select/?
q=amerika&facet=true&facet.mincount=1&facet.field=foo&facet.field=bar&spellcheck=true&spellcheck.onlyMorePopular=true&spellcheck.count=20&spellcheck.extendedResults=true&qt=dismax_fgs&hl=true&hl.res.replaced=b&hl.res.removed=strike&DymReSearcher=true
```

In case DYM ReSearcher finds some suggestions, it will return it in a separate XML element in the response:
```xml
<arr name="extended_spellchecker_suggestions">
    <str>america</str>
</arr>
```

Multiple query suggestions are possible. In such cases multiple <str> elements will be included, best suggestion first.

The results for the best suggestions are included in the response, as seen below:

```xml
<result name="spellchecked_response" numFound="2334" start="0"> <doc>
       <str name="foo">america</str>
        ......
```

Results of the original query are not removed from the response and can be found in their usual place under the <response> tag.
    
If you use faceting or highlighting in your queries, DYM ReSearcher will perform the same operations on the results of the best suggestion. Faceting will be displayed under:

```xml
<lst name="spellchecked_facet_counts">
```

while you can find `highlighting`, `stats`, `termVector`, `terms` data under:

```xml
<lst name="spellchecked_highlighting">
<lst name="spellchecked_terms">
<lst name="spellchecked_termVectors">
<lst name="spellchecked_stats">
<lst name="spellchecked_grouped">
```

The structure of both fields is the same as the structure of original fields (facet_counts and highlighting).

#### Using spellcheck.q parameter
Dym ReSearcher requires that `q` parameter is present. Parameter `spellcheck.q` should not be used since it can trigger errors in some cases. If it is used, it must contain the same value as `q` parameter (or its lower/upper cased version).

### Correction Highlighting Feature
For information about Correction Highlighting Feature, please refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### Using DYM ReSearcher with SolrJ
For information about using DYM ReSearcher with SolrJ, please refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### DYM ReSearcher in distributed environment
DYM ReSearcher works both in non-distributed (single node or simple master-slave) and distributed setup. If you are using SolrCloud, everything will work out-of-the-box automatically. There are no special parameters which should tell whether the setup is distributed or not. Also, there is only one version of DYM ReSearcher jar which knows how to work in all kinds of setups.

If you are using “manually” configured distributed search (where you manually define `shards` parameter in your requests or in `solrconfig.xml`), few things may have to be adjusted:

* make sure you properly use `shards.qt` parameter - if non-default search handler is used, you should mention its name with this parameter. If you already had a functional distributed setup before using DYM ReSearcher, chances are that you are already using `shards.qt` parameter in correct way and there is nothing to change related to this parameter
* standard request handler (into whose chain DYM ReSearcher is added) shouldn’t have `shards` parameter defined in `solrconfig.xml` (this is true regardless of DYM ReSearcher, because it can cause infinite recursion). If you are not using standard handler, your are most likely fine. Also, if you were already using this same handler in distributed setup, you are most likely fine too. If not, that means you can do one of the following:
    * specify `shards` parameter in request sent to Solr (which is not practical in all cases)
    * define a separate request handler in `solrconfig.xml` for DYM ReSearcher. That separate request handler can be a copy of your original query handler in everything but `shards` parameter. Your client application would still send requests to your original request handler, but you would have to add `shards.qt` parameter with value which matches the name of request handler “copy” 
  There is an exception to this case: if you have one aggregator shard sitting in front of N other shards, where aggregator is the only one receiving requests and the only one having `shards` parameter in its config, you can ignore this step. In that case, you need DYM ReSearcher configuration only on your aggregator shard, other sub-shards don’t need it.

### Https
If solr is exposed through SSL, shardHandlerFactory configuration should be added to DYM ReSearcher component:

```xml
<searchComponent name="dymReSearcher"
  class="com.sematext.solr.handler.component.dym.DymReSearcher">
    <int name="maxOriginalResults">0</int>
    <int name="maxOriginalResultsToReturnResults">5</int>
    <int name="suggestionsToReturn">5</int>
    <int name="minResultsForAcceptableSuggestion">3</int>
    <float name="minRequiredSuggestionRatio">0.0</float>

    <shardHandlerFactory class="HttpShardHandlerFactory">
       <str name="urlScheme">https://</str>
       <int name="socketTimeOut">1000</int>
       <int name="connTimeOut">5000</int>
    </shardHandlerFactory>
</searchComponent>
```

### Limitations
Currently, the only serious limitation is when phrase queries contain multiple incorrectly spelled words. Although such cases aren't very common (phrase queries with one incorrectly spelled term are much more common), some of them can and will be corrected by the “fixing common language misspellings” step. Furthermore, DYM ReSearcher produces partially meaningful suggestions even if “fixing common language misspellings” doesn't help, though this still represents a limitation. In such cases, DYM ReSearcher finds only “the most incorrectly spelled” term and makes new query suggestions based on its variations. Other incorrectly spelled terms are considered correct in suggestions created. In many cases, this behavior will produce very good results, but there certainly are corner cases where this may not be good enough. Because this happens very rarely, but could cause a high number of rapid queries, we limit query spelling correction to a single query term. For illustration, if a query has 3 incorrectly spelled terms and if DYM ReSearcher has to try top 20 suggestions for each of them (which isn't very high number at all), DYM ReSearcher would have to try 20 * 20 * 20 = 8000 queries.
