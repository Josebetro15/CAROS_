#!/bin/sh
set -e
basedir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
wrapper="$basedir/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapper" ]; then
  echo "Falta .mvn/wrapper/maven-wrapper.jar"
  exit 1
fi
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
exec "$JAVACMD" \
  -classpath "$wrapper" \
  "-Dmaven.multiModuleProjectDirectory=$basedir" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
