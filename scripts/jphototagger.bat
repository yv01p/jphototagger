@echo off
setlocal

set SCRIPT_DIR=%~dp0
set APP_HOME=%SCRIPT_DIR%..

set JVM_OPTS=-XX:+UseZGC -XX:+UseStringDeduplication -Xmx1g -Xms256m

if exist "%APP_HOME%\lib\jphototagger.jsa" (
    set JVM_OPTS=%JVM_OPTS% -XX:SharedArchiveFile="%APP_HOME%\lib\jphototagger.jsa"
)

java %JVM_OPTS% -jar "%APP_HOME%\lib\jphototagger.jar" %*
