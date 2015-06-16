#!/bin/sh

if [ -z "$JAVA_HOME" ]; then
  j=$(which java 2>/dev/null)
  if [ -z "$j" ]; then
 	echo "Unable to locate Java!"
    exit 1
  else
    JAVA="$j"
  fi
else
  JAVA="$JAVA_HOME/bin/java"
fi


SCRIPT_DIR=$(dirname "$0")
cd "$SCRIPT_DIR"

WCP="."
for i in *.jar; do
  WCP="$i:${WCP}"
done

JAVA_OPTS="-Xms128m -Xmx512m -Dcmis.server.port=8081 -Dcmis.server.workdir=. -Dcmis.server.loglevel=WARN -Dcmis.server.record=false"

exec $JAVA $JAVA_OPTS -classpath $WCP com.manning.cmis.Server