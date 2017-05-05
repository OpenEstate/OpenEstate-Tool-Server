@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM startup script for the ImmoTool server
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: Use a specific command to launch the Java Runtime Environment
set "JAVACMD="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32m"
set "JAVA_HEAP_MAXIMUM=512m"

:: Additional options for the Java Runtime Environment
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dopenestate.server.trayIcon=true"


::
:: Start execution...
::

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\..\"
if "%JAVACMD%"=="" set "JAVACMD=java"

pushd %BASE_DIR%
set "BASE_DIR=%CD%"

:: echo SCRIPT: %SCRIPT%
:: echo SCRIPT_DIR: %SCRIPT_DIR%
:: echo BASE_DIR: %BASE_DIR%

%JAVACMD% -Xms%JAVA_HEAP_MINIMUM% -Xmx%JAVA_HEAP_MAXIMUM% -classpath "etc;lib\*" %JAVA_OPTS% org.openestate.tool.server.Server %*
popd
