@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM launch a management console on the database server
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: Use a specific command to launch the Java Runtime Environment
set "JAVACMD="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32m"
set "JAVA_HEAP_MAXIMUM=256m"

:: Additional options for the Java Runtime Environment
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Djava.awt.headless=true"

:: Path to management-configuration
set "MANAGER_CONF=etc\manager.conf"

:: Managed database, identified by 'urlid'
set "MANAGER_URLID=immotool"


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

%JAVACMD% -Xms%JAVA_HEAP_MINIMUM% -Xmx%JAVA_HEAP_MAXIMUM% -classpath "etc;lib\*" %JAVA_OPTS% org.hsqldb.cmdline.SqlTool --rcFile=%MANAGER_CONF% %MANAGER_URLID%
popd
