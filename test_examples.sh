#!/bin/bash

mvn clean install
#find current yajco version
#$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
yajco_version=`mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version | grep -v '\['`
#clone yajco-examples and execute tests
git clone --depth=1 https://github.com/mfecenko/yajco-examples
cd yajco-examples
echo "Using YAJCo version $yajco_version to run tests in yajco-examples"
# TODO: use yajco_version variable
mvn verify -Dyajco.version=0.5.10-SNAPSHOT --fail-at-end