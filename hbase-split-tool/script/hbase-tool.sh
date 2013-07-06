#!/bin/sh

APP_HOME=.

APP_MAINCLASS=org.ctrip.hbase.tool.RawlogRowAnalysis

CLASSPATH=$APP_HOME
for i in "$APP_HOME"/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

JAVA_OPTS="-Xms1024m -Xmx1024m -Djava.library.path=$APP_HOME/lib/native"
$JAVA_HOME/bin/java $JAVA_OPTS -classpath $CLASSPATH $APP_MAINCLASS $@
