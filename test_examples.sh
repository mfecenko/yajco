#!/bin/bash

mvn install package
#find current yajco version
yajco_version=`mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version | grep -v '\['`
#clone yajco-examples and execute tests
git clone --depth=1 https://github.com/mfecenko/yajco-examples
cd yajco-examples
mvn verify -Dyajco.version=$yajco_version