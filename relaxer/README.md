# Guide: Query Relaxer

### What is Query Relaxer?
The Query Relaxer is a Solr component that can be used to improve users’ search experience. It focuses on queries constructed of more than one term and producing very few hits. For such queries, it creates multiple query suggestions (relaxed queries), tries each one of them, and returns the best suggestion, along with its results. As its name suggests, it “relaxes” original queries, meaning it tries to find less restricted version of user's query (for instance, by removing quotes if there are any or by removing some of the original query terms).

Query Relaxer implements functionality that can be seen, for instance, on Amazon.com. It has a set of its own query relaxing heuristics and therefore offers a bit different suggestions than what can be seen on Amazon.com, but the idea is the same. On Amazon.com's main page, you can try entering following queries into the search box:

1. **"blues brothers greatest hits"** (include quotes) – At the time of this writing, this produced just 1 result, and offers the alternative query: "brothers greatest hits" (notice how word blues is crossed over in the screenshot below). In this case, Relaxer would offer initial query without quotes and possibly a few queries with quotes but without some terms. For the top suggestion, Relaxer will also include its results in the response.
2. **"bee gees greatest hits video"** (include quotes) – Amazon will show that there were no results and will offer 4 alternative searches, each without just 2 words. Relaxer would again offer the query without quotes (which probably makes more sense in this case than Amazon's suggestions which all exclude word 'video', since non-quoted query really returns a product which user was searching for) 
3. **bee gees greatest hits video** (without quotes) – Amazon shows 1 result and 4 suggestions, each excluding 2 words from initial query. Relaxer's current heuristics would produce queries which omit just one word. So, Relaxer would return the only result which matches original query and also suggest query 'bee gees greatest hits' (without quotes) as the best alternative, since it returns the most results.

Relaxer comes with three heuristic algorithms out of the box (`RemoveAllQuotes` and `RemoveOneTokenFromPhrase` for phrase queries and `RemoveOneTerm` for queries without quotes). It can also be easily customized by writing implementations of one specific interface: `QueryRelaxerHeuristic`.

### How does it work?   
Relaxer processes each query in 2 steps:

1. Fixing of common language-specific misspellings – in case a client's query returned too few results, and Relaxer's property commonMisspellingsFile is defined in the configuration file `solrconfig.xml`, Relaxer will first check if there are any common language misspellings. If there are, Relaxer will fix them according to the definition in the file and will use the corrected query from here on. If such query returns the satisfying number of results, that query and its results will be returned to the client application. Otherwise, Relaxer goes to step 2.
2. Correcting query based on different heuristic methods. It differentiates between phrase and regular queries (without quotes) and uses different sets of heuristics for each of these types. Everything needed for its work is defined in `solrconfig.xml`. See later sections for details about heuristic methods and configuration.

### Deployment
Relaxer is packaged in two jar files (you can find them in zip package in directories `/relaxer/target` and `/relaxer/target/dependencies`). These files must be copied to the `lib/` directory under SOLR_HOME in the Solr installation. Also, Relaxer component should be defined in `solrconfig.xml`, as show in the example below:

```xml
<searchComponent name="relaxerComponent"
  class="com.sematext.solr.handler.component.relaxer.QueryRelaxerComponent">
  <str name="commonMisspellingsFile">./example/solr/conf/common_misspellings_en.txt</str>
  <int name="maxOriginalResults">0</int>
  <arr name="phraseQueryHeuristics">
<str>com.sematext.solr.handler.component.relaxer.heuristics.phrase.RemoveAllQuotes</str>
  </arr>
  <arr name="regularQueryHeuristics">
<str>com.sematext.solr.handler.component.relaxer.heuristics.regular.RemoveOneTerm</str>
  </arr>
</searchComponent>
```
Configuration of Relaxer component consists of:
* Property `maxOriginalResults` (Integer, default value is 0) - Relaxer will produce suggestions only in situations where original query returned <= `maxOriginalResults` results, otherwise it will assume suggestions are not needed.
* Property `minResultsForGoodFgsSuggestion` (integer, default value is 0) - The minimal number of results one suggestion has to return to label it as a 'good' suggestion.
* Property `fieldAnalyzerMaps`. You can specify a custom analyzer (config in schema.xml) to parse query intead of original field analyzer. 
* Property `phraseQueryHeuristics` – a list of classes containing heuristics to be used on phrase queries (queries which contain one or more pairs of double-quotes). Currently, there are two such classes `com.sematext.solr.handler.component.relaxer.heuristics.phrase.RemoveAllQuote` (this one is considered default in case no phrase heuristics are specified in configuration), and `com.sematext.solr.handler.component.relaxer.heuristics.phrase.RemoveOneToken` FromPhrase. It is easy to write custom implementations by implementing interface `QueryRelaxerHeuristic` which contains just one method - `createSuggestions`
* Property `regularQueryHeuristics` - a list of classes containing heuristics to be used on non-phrase queries. Currently, there is only one such class (and it is a default value), `com.sematext.solr.handler.component.relaxer.heuristics.regular.RemoveOneTerm`. For custom implementations, the same interface should be implemented as for `phraseQueryHeuristics`.

The following parameters can be used to control Relaxer and can be passed in the URL:
* `queryRelaxer` (true|false): defines whether Relaxer should run for particular query or not
* `queryRelaxer.q`: query to be used for Relaxer.  This is useful when the actual full query sent to Solr is complex and when the user-entered portion is hard or impossible to extract.  In such cases the user-entered query string can be specified in `queryRelaxer.q` parameter.
* `queryRelaxer.field`: specified the field to act on; available in `RemoveOnClause` heuristic
* `queryRelaxer.preferFewerMatches` (true|false): best suggestion usually has more search results, but in some case we prefer fewer result suggestions
* `queryRelaxer.rows` (integer, default value is from `rows` core search param). The number of docs to be returned for each suggested relaxed query
queryRelaxer.rowsPerQuery (integer, default value 5): number of suggestions to return
* `queryRelaxer.longQueryTerms` (integer, default value 5): the length expressed in the number of tokens beyond which a query will be considered long and thus relaxed via the `mm` param; available in `RemoveOnClause` heuristic
* `queryRelaxer.longQueryMM` (a valid `mm` value): when a query is considered long, the original mm is relaxed by this parameter; available in `RemoveOnClause` heuristic

Complex queries that use sub-queries let one specify the field to “act on”.  Here are a few ways to do that in RemoveOneClause heuristic:
* use `queryRelaxer.field` - Relaxer will work on all sub-queries that use the specified field
* add signal `relax=on` to sub-query. For example: `{!edismax qf=title v=”Analytics Software” relax=on} {!edismax qf=content v=”Analytics Software”}`, relaxer will only relax the first sub-query
* use `queryRelaxer.q` (similar to `hl.q`) - must be used with parameter dereferencing because Relaxer only relaxes `queryRelaxer.q`. For example: `q={!edismax qf=title v=$relaxerQuery.q} {!edismax qf=content v=”Analytics Software”&queryRelaxer.q=Analytics Software`

One can define as many different search component configurations as needed, each under a different name. Once defined, a component needs to be added to the appropriate request handler. For instance:

```xml
<requestHandler name="dismax_relaxer" class="solr.SearchHandler">
  <lst name="defaults">
    <str name="defType">dismax</str>
    <str name="echoParams">explicit</str>
    <float name="tie">0.01</float>
    <str name="fl">*</str>
    <str name="q.alt">*:*</str>
  </lst>
  <arr name="last-components">        
    <str>relaxerComponent</str>
  </arr>
</requestHandler>
```

### How Relaxer creates suggestions?
For each user's query sent to a request handler to which the Relaxer component is attached, Relaxer first checks if original query produced less than or equal to `maxOriginalResults` hits. If it did, Relaxer first checks if there were any “common language misspellings” in the query. If there are, it creates a new query which fixes them and executes such corrected query. If such query produces more than `maxOriginalResults` hits it returns it as a query suggestion along with the results. If there were no such misspellings, or if there were, but correction still wasn't good enough, it proceeds to step two. In this second step Relaxer checks if the query contained any phrases. If it did, Relaxer will invoke the chain of `phraseQueryHeuristics` algorithms and collect all their suggestions. If it didn't, Relaxer will invoke the chain of `regularQueryHeuristics` algorithms.

Once all query alternatives are collected, Relaxer tries each one of them and returns them ordered by number of hits under tag `relaxer_suggestions`. The query with most hits is at the top of the list and results for that query are provided under another tag, `relaxer_response`.

### Fixing of common misspellings
For information about fixing common misspellings, please refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### How to use Relaxer
After Relaxer is correctly deployed and configured on your Solr server, you can start using it. The only mandatory parameter you'll have to provide in your query URLs to get correct results is `queryRelaxer=true`. In case it isn't provided, Relaxer component will not run.

Relaxer works best with dismax request handler. The reason for this is the format of the queries which are sent to dismax handler (for instance, no field names, no logical operators), but it also works with standard handler.

Here are a few examples of queries:
```html
http://localhost:8080/solr/select/?q=”blues%20brothers%20greatest%20hits %20album”&facet=true&facet.mincount=1&facet.field=foo&facet.field=bar&qt=dismax_relaxer&hl=true&queryRel axer=true&hl.res.replaced=b&hl.res.removed=strong
```
```html
http://localhost:8080/solr/select/?q=Shinw on you crazy diamond&qt=dismax_relaxer&hl=true&queryRelaxer=true
```
If Relaxer finds any good query suggestions, it will return them in separate XML element, for instance like this:
```html
<arr name="relaxer_suggestions"> <str>shine on you crazy diamond</str> </arr>
```
Multiple suggestions are possible, so in such cases you'll get multiple <str> elements. Suggestions will be ordered with best suggestions at the top.

Results for best suggestion are returned like in field `relaxer_response`:
```xml
<arr name="relaxer_suggestions">
  <lst>
    <result name=”relaxer_response” numFound="2334" start="0"> 
      <doc>
        <str name="foo">Shine on you crazy diamond</str>
  ......
```
Results of the original query can be found, as usual, under <respose> tag, they are not removed from response.

If you use faceting, group or highlighting in your queries, Relaxer will perform the same operations on the results of best suggestion. 

The structure of both fields is the same as the structure of original fields (`facet_counts` and `highlighting`).

### Out-of-the-box heuristic algorithms
Class name | Phrase or regular query? | Description
---------- | ------------------------ | -----------
`RemoveAllQuotes` | phrase | Removes all quotes from the phrase query. Such queries are relaxed compared to the original query and are, therefore, expected to yield more results.
`RemoveOneTokenFromPhrase` | phrase | This heuristic creates N possible queries, where N is the number of words in the query. Each variations will omit just one word from the original query. All queries will be tried and the one with the most results will be returned. For instance, in case of a phrase query like (double quotes represent quotes from original query) : ' “harry potter” booj ', three suggestions will be created (only the third one will still contain the phrase): ' potter booj ', ' harry booj ' and ' “harry potter” '. The last one would most likely return the most results and would be returned as the suggested query.
`RemoveOneTerm` | regular | This heuristic works exactly the same as `RemoveOneTokenFromPhrase`, but should be used only on regular queries, since it doesn't handle phrases.
`RemoveOneClause` | regular & phrase | This heuristic works similar to `RemoveOneTerm` but with some additional improvements: it handles phrases and treats them as terms in `RemoveOneTerm` heuristic when relaxing. It allows tokenizer specification and supports boolean operators AND, OR, NOT.  It can also handle sub-queries. Long queries with many suggestion candidates can affect search performance, so this heuristic allows one to relax `mm` parameters for long queries.

All phrase heuristics are located in package `com.sematext.solr.handler.component.relaxer.heuristics.phrase`, while all regular query heuristics are in `com.sematext.solr.handler.component.relaxer.heuristics.regular`. Be sure to write full class name (including package name) in `solrconfig.xml`.

### Correction highlighting feature
For information about Correction Highlighting Feature, please refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### Using Relaxer with SolrJ
For information about using Relaxer with SolrJ, please refer to ReSearcher-common.pdf which describes functionality common to all Sematext's ReSearcher components.

### Query Relaxer in distributed environment
Query Relaxer works both in non-distributed (single node or simple master-slave) and distributed setup. If you are using SolrCloud, everything will work out-of-the-box automatically. There are no special parameters which should tell whether the setup is distributed or not. Also, there is only one version of Query Relaxer jar which knows how to work in all kinds of setups.

If you are using “manually” configured distributed search (where you manually define `shards` parameter in your requests or in `solrconfig.xml`), few things may have to be adjusted:
* make sure you properly use `shards.qt` parameter - if non-default search handler is used, you should mention its name with this parameter. If you already had a functional distributed setup before using Query Relaxer, chances are that you are already using `shards.qt` parameter in correct way and there is nothing to change related to this parameter
* standard request handler (into whose chain Query Relaxer is added) shouldn’t have `shards` parameter defined in solrconfig (this is true regardless of Query Relaxer, because it can cause infinite recursion). If you are not using standard handler, your are most likely fine. Also, if you were already using this same handler in distributed setup, you are most likely fine too. If not, that means you can do one of the following:
    * specify `shards` parameter in request sent to Solr (which is not practical in all cases)
    * define a separate request handler in solrconfig for Query Relaxer. That separate request handler can be a copy of your original query handler in everything but `shards` parameter. Your client application would still send requests to your original request handler, but you would have to add `shards.qt` parameter with value which matches the name of request handler “copy”

  There is an exception to this case: if you have one aggregator shard sitting in front of N other shards, where aggregator is the only one receiving requests and the only one having `shards` parameter in its config, you can ignore this step. In that case, you need Relaxer configuration only on your aggregator shard, other sub-shards don’t need it.

### HTTPS
If solr is exposed through SSL, `shardHandlerFactory` configuration should be added to Relaxer search component:
```xml
<searchComponent name="relaxerComponent"    class="com.sematext.solr.handler.component.relaxer.QueryRelaxerComponent">
    <int name="maxOriginalResults">0</int>
    <arr name="phraseQueryHeuristics">      <str>com.sematext.solr.handler.component.relaxer.heuristics.phrase.RemoveAllQuotes</str>
    </arr>
    <arr name="regularQueryHeuristics">
 <str>com.sematext.solr.handler.component.relaxer.heuristics.regular.RemoveOneTerm</str>
    </arr>
    <shardHandlerFactory class="HttpShardHandlerFactory">
        <str name="urlScheme">https://</str>
        <int name="socketTimeOut">1000</int>
        <int name="connTimeOut">5000</int>
    </shardHandlerFactory>
</searchComponent>
```

### Limitations
Currently, there are only a few limitations of Relaxer. One is the number of heuristic algorithms implemented by Sematext. This number will grow in the future, but the existing algorithms should satisfy most needs. If you have any specific requirements, implementing new heuristics is very easy and consists of implementing one interface and defining it in the configuration file.

The other limitation is related to the format of queries sent to Solr. Any query whose terms contain spaces will be problematic, like range terms (for instance `price:[10 TO 20]`). This isn't true for regular phrase queries (`“united states”`), Relaxer works well with them.

