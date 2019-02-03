@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer ${project.version}
@REM start OpenEstate-ImmoServer in foreground
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: Use a specific command to launch the Java Runtime Environment
set "JAVA_COMMAND="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32m"
set "JAVA_HEAP_MAXIMUM=512m"

:: Additional options for the Java Runtime Environment
set "JAVA_OPTIONS=-Dfile.encoding=UTF-8"


::
:: Start execution...
::

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\..\"
if "%JAVA_COMMAND%"=="" (
    if exist "%BASE_DIR%\jre\" (
        set "JAVA_COMMAND=%BASE_DIR%\jre\bin\java.exe"
    ) else (
        set "JAVA_COMMAND=java"
    )
)

pushd %BASE_DIR%
set "BASE_DIR=%CD%"
%JAVA_COMMAND% ^
    -Xms%JAVA_HEAP_MINIMUM% ^
    -Xmx%JAVA_HEAP_MAXIMUM% ^
    -classpath "etc;lib\*" ^
    %JAVA_OPTIONS% ^
    org.openestate.tool.server.Server %*
popd
