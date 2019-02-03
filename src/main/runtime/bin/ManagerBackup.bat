@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM launch a live backup on the database server
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: Use a specific command to launch the Java Runtime Environment
set "JAVACMD="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32m"
set "JAVA_HEAP_MAXIMUM=128m"

:: Additional options for the Java Runtime Environment
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Djava.awt.headless=true"

:: Path to management-configuration
set "MANAGER_CONF=etc\manager.conf"

:: Managed database, identified by 'urlid'
set "MANAGER_URLID=immotool"

:: Path to backup directory (must end with trailing '\')
set "BACKUP_DIR=.\var\backup\%MANAGER_URLID%\"


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

%JAVACMD% -Xms%JAVA_HEAP_MINIMUM% -Xmx%JAVA_HEAP_MAXIMUM% -classpath "etc;lib\*" %JAVA_OPTS% org.hsqldb.cmdline.SqlTool --rcFile=%MANAGER_CONF% --sql="CHECKPOINT; BACKUP DATABASE TO '%BACKUP_DIR%' BLOCKING;" %MANAGER_URLID%
popd
