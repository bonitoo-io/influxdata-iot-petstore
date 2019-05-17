#!/bin/bash

set -e
SCRIPT_PATH="$( cd "$(dirname "$0")" ; pwd -P )"
cd $SCRIPT_PATH/../device-java
mvn package
java -jar ./target/device-java-1.0-SNAPSHOT-spring-boot.jar