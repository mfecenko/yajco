#!/bin/bash

mvn install package
#find current yajco version
yajco_version=$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate \
                -Dexpression=project.version -q -DforceStdout)
#clone yajco-examples and execute tests
git clone --depth=1 https://github.com/kpi-tuke/yajco-examples
cd yajco-examples
mvn verify -Dyajco.version=$yajco_version
