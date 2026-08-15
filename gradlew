#!/bin/sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################
APP_HOME=$( cd "$( dirname "$0" )" && pwd )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
    echo "Gradle wrapper JAR manquant — exécute: gradle wrapper"
    exit 1
fi
exec java -Xmx64m -Xms64m -classpath "$CLASSPATH" -Dorg.gradle.appname="$0" org.gradle.wrapper.GradleWrapperMain "$@"
