#!/bin/env bash

licenseFile=`ls -l licenseDymReSearcher.lic.jar`
echo
echo "IMPORTANT: YOU NEED TO CREATE THE LICENSE FIRST"
echo "           AND YOU MUST PLACE IT IN THIS (core) DIRECTORY"
echo
echo "USING LICENSE FILE: $licenseFile"
echo
licenseFile=`echo $licenseFile | sed -e 's/.* //'`
unzip -l $licenseFile
echo

licenseFile=`ls -l licenseQueryRelaxerComponent.lic.jar`
licenseFile=`echo $licenseFile | sed -e 's/.* //'`
echo "IMPORTANT: YOU NEED TO CREATE THE LICENSE FIRST"
echo "           AND YOU MUST PLACE IT IN THIS (core) DIRECTORY"
echo
echo "USING LICENSE FILE: $licenseFile"
echo
unzip -l $licenseFile
echo

sleep 5

version=`grep '<version>' pom.xml | head -1 | grep version | cut -d\> -f2 | cut -d\< -f1`

rm -v st-ReSearcher-dym-$version.zip
rm -v st-ReSearcher-relaxer-$version.zip
mkdir -p st-ReSearcher-dym-$version/lib
mkdir -p st-ReSearcher-relaxer-$version/lib
mkdir -p st-ReSearcher-dym-$version/doc/javadoc/ReSearcher-dym
mkdir -p st-ReSearcher-relaxer-$version/doc/javadoc/ReSearcher-relaxer
mkdir -p st-ReSearcher-dym-$version/example/solr/conf/
mkdir -p st-ReSearcher-relaxer-$version/example/solr/conf/


# build everything from the top
echo
echo Building ReSearcher from the top
echo
pushd .. && mvn clean package; popd

# build javadoc from each module (doesn't work from top)
echo
echo Building Javadocs for Core
echo
mvn javadoc:javadoc

echo
echo Building Javadocs for Relaxer
echo
pushd ../dym && mvn javadoc:javadoc; popd

echo
echo Building Javadocs for Relaxer
echo
pushd ../relaxer && mvn javadoc:javadoc; popd

echo
echo Copying jars from target dirs to lib dirs for DYM and Relaxer
echo
cp -v ./target/*jar st-ReSearcher-dym-$version/lib
cp -v ./target/*jar st-ReSearcher-relaxer-$version/lib
cp -v ../dym/target/*jar st-ReSearcher-dym-$version/lib
cp -v ../relaxer/target/*jar st-ReSearcher-relaxer-$version/lib

# only some dependencies should be fetched
echo
echo Gathering ReSearcher dependencies
echo
### NB: core has the same deps, so only one mvn needed from core, really
sleep 5
pushd ..
mvn dependency:copy-dependencies
popd
#pushd ../dym
#mvn dependency:copy-dependencies -DincludeArtifactIds=st-Licensor-validator,st-Licensor-core,truelicense,truexml,licensorPublicKeyStore
#popd
#pushd ../relaxer
#mvn dependency:copy-dependencies -DincludeArtifactIds=st-Licensor-validator,st-Licensor-core,truelicense,truexml,licensorPublicKeyStore
#popd

# FIXME these 2 don't work why?
#echo XXX
#pwd
#ls -al target
#ls -al target/dependency/
#cp -v target/dependency/*jar st-ReSearcher-dym-$version/lib
#echo YYY
#cp -v target/dependency/*jar st-ReSearcher-relaxer-$version/lib
cp -v ../dym/target/dependency/*jar st-ReSearcher-dym-$version/lib
cp -v ../relaxer/target/dependency/*jar st-ReSearcher-relaxer-$version/lib


##
## Licensor
#
# get Licensor-validator version
licVersion=`grep -C 1 st-Licensor-validator pom.xml | grep '<version' | cut -d\> -f2 | cut -d\< -f1`
#licDir="../../../../st-Licensor/branch/$licVersion/validator"
#licDir="../../../st-Licensor/branch/1.0.0/validator"
licDir="../../../st-Licensor/trunk/validator"

echo
echo Building Licensor-validator version $licVersion from $licDir
echo
sleep 5
pushd $licDir
mvn clean package

echo
echo Gathering Licensor-validator dependencies from $licDir
echo
sleep 5
mvn dependency:copy-dependencies
popd

echo
echo Copying Licensor-validator dependencies from $licDir/target/dependency/
echo
sleep 5
cp -v $licDir/target/dependency/*jar st-ReSearcher-dym-$version/lib
cp -v $licDir/target/dependency/*jar st-ReSearcher-relaxer-$version/lib

# remove junit
#rm -v st-ReSearcher-dym-$version/lib/junit*jar
#rm -v st-ReSearcher-relaxer-$version/lib/junit*jar

# copy docs
cp -v ../doc/DYM-ReSearcher-Guide.pdf st-ReSearcher-dym-$version/doc
cp -v ../doc/Relaxer-Guide.pdf st-ReSearcher-relaxer-$version/doc
cp -v ../doc/ReSearcher-common.pdf st-ReSearcher-dym-$version/doc
cp -v ../doc/ReSearcher-common.pdf st-ReSearcher-relaxer-$version/doc
cp -v ../LICENSE.pdf st-ReSearcher-dym-$version/
cp -v ../LICENSE.pdf st-ReSearcher-relaxer-$version/

# TODO do this for core, too?
# copy javadoc
cp -va ../dym/target/site/apidocs/* st-ReSearcher-dym-$version/doc/javadoc/ReSearcher-dym/
cp -va ../relaxer/target/site/apidocs/* st-ReSearcher-relaxer-$version/doc/javadoc/ReSearcher-relaxer/

# copy sample config
cp -v ../example/solr/conf/schema.xml ../example/solr/conf/solrconfig_dym.xml ../example/solr/conf/common_misspellings_en.txt st-ReSearcher-dym-$version/example/solr/conf/
cp -v ../example/solr/conf/schema.xml ../example/solr/conf/solrconfig_dym.xml ../example/solr/conf/common_misspellings_en.txt st-ReSearcher-relaxer-$version/example/solr/conf/

# copy the license
cp -v licenseDymReSearcher.lic.jar st-ReSearcher-dym-$version/lib/
cp -v licenseQueryRelaxerComponent.lic.jar st-ReSearcher-relaxer-$version/lib/

# remove unlimited licenses
#rm -v ./target/dependency/*license*unlimited*
#rm -v ../dym/target/dependency/*license*unlimited*
#rm -v ../relaxer/target/dependency/*license*unlimited*  ## -v ???
##rm -v st-ReSearcher-dym-$version/lib/*license*unlimited*
##rm -v st-ReSearcher-relaxer-$version/lib/*license*unlimited*

for p in st-ReSearcher-dym-$version st-ReSearcher-relaxer-$version; do
    rm -v $p/lib/*license*unlimited*
    rm -v $p/lib/*test*jar
    rm -v $p/lib/*junit*jar
    rm -v $p/lib/*jetty*jar
    rm -v $p/lib/*morfologik*jar
    rm -v $p/lib/*servlet*jar
    rm -v $p/lib/*spatial*jar
    rm -v $p/lib/*ant*jar
    rm -v $p/lib/lucene*jar
done

# package
zip -r st-ReSearcher-dym-$version.zip st-ReSearcher-dym-$version  -x "*.svn*"
zip -r st-ReSearcher-relaxer-$version.zip st-ReSearcher-relaxer-$version  -x "*.svn*"

echo
unzip -l st-ReSearcher-dym-$version.zip | egrep -v '.html$|/$'
unzip -l st-ReSearcher-relaxer-$version.zip | egrep -v '.html$|/$'

echo
#egrep -i "error|fail|cannot|can't|could not|no such" pack.log
echo
ls -al *zip
