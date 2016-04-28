# solr-researcher

The Solr ReSearcher has Solr components that can be used to improve user's search experience. 

### Project Layout
Currently, the Solr ReSearcher has two modules:
* core – Common classes for other researcher component
* relaxer – Query Relaxer is a Solr component that improves search experience. It executes alternative queries when it detects that original query produced poor results or no results at all due to being too restrictive. It transparently returns better search results to the client without the client having to restructure the query and send additional requests to Solr over the wire and re-examine the results.
* dym – Solr DYM ReSearcher (aka Did You Mean ReSearcher) is a Solr component that improves search experience. It executes alternative queries when it detects that original queries produced poor results or no results at all due to spelling mistakes or typos. It transparently returns better search results to the client.

### Usage
Check usage for each component in thier own README.md
* [Solr Query Relaxer](https://github.com/sematext/solr-researcher/tree/master/relaxer)
* [Solr DYM ReSearcher](https://github.com/sematext/solr-researcher/tree/master/dym)


### Solr Version
5.2.X

### Maven Artifacts
TODO

### Continuous Integration
Continuous Integration environment for Solr ReSearcher project can be found at: https://travis-ci.org/sematext/solr-researcher

### Build

You need maven and JDK 7:

```sh
$ mvn clean package
```

## License
Solr ReSearcher is released under Apache License, Version 2.0

## Contact
For any questions ping @sematext, @nmtien.
