#!/bin/sh
JAVA_HOME="${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(command -v java))))}" 
exec java -jar "$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar" "$@"
