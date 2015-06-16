cd %~dp0

set "JAVA_OPTS=-Xms128m -Xmx512m -Dcmis.server.port=8081 -Dcmis.server.workdir=. -Dcmis.server.loglevel=WARN -Dcmis.server.record=false"

java %JAVA_OPTS% -classpath ".;*" com.manning.cmis.Server