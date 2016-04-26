* build solr.war and put it in example/solr/webapps

* build ReSearcher jar
mvn clean compile jar:jar

* put ReSearcher jar in example/solr/lib

* Start Jetty with Solr
cd example
java -Dsolr.solr.home=/home/otis/dev/repos/sematext_ext/st-ReSearcher/trunk/example/solr -jar start.jar &> jetty.log &

* Generate docs, for example (foo is the field name)
grep ^A enwiki-latest-all-titles-in-ns0 | head -100000 | sh bin/generate-xml.sh foo > example/exampledocs/docs.xml

* Index example documents:
sh bin/post.sh example/exampledocs/docs.xml

* Build spellchecker index(es):
curl --silent \
'http://localhost:8983/solr/select/?q=foo:nothing&spellcheck=true&spellcheck.build=true&spellcheck.dictionary=levenstein'
curl --silent \
'http://localhost:8983/solr/select/?q=foo:nothing&spellcheck=true&spellcheck.build=true&spellcheck.dictionary=jw'
curl --silent \
'http://localhost:8983/solr/select/?q=foo:nothing&spellcheck=true&spellcheck.build=true&spellcheck.dictionary=ngram'

* Test
curl --silent \
'http://localhost:8983/solr/select/?indent=on&spellcheck=true&spellcheck.count=10&spellcheck.onlyMorePopular=true&spellcheck.extendedResults=true&spellcheck.collate=true&qt=standard&spellcheck.dictionary=levenstein&q=foo:(ameriken+AND+dreamerz)'
