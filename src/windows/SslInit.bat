@REM ----------------------------------------------------------------------------
@REM ${project.baseName} ${project.version}
@REM generate RSA key pair and certificate for SSL encryption
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM
@echo off
setlocal

:: Use a specific command to launch the Java Runtime Environment
set "JAVA_COMMAND="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32m"
set "JAVA_HEAP_MAXIMUM=128m"

:: Additional options for the Java Runtime Environment
set "JAVA_OPTIONS=-Dfile.encoding=UTF-8"

:: Path to the folder, where the server configuration files are stored.
set "SERVER_ETC_DIR="

:: Path to the folder, where the server log files are stored.
set "SERVER_LOG_DIR="

:: Path to the folder, where the server data files are stored.
set "SERVER_VAR_DIR="


::
:: Start execution...
::

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\..\"

pushd "%BASE_DIR%"
set "BASE_DIR=%CD%"

if "%JAVA_COMMAND%"=="" (
    if exist "%BASE_DIR%\jre\" (
        set "JAVA_COMMAND=%BASE_DIR%\jre\bin\java.exe"
    ) else (
        set "JAVA_COMMAND=java"
    )
)

:: Set default path to the etc folder.
if "%SERVER_ETC_DIR%"=="" (
    set "SERVER_ETC_DIR=%BASE_DIR%\etc"
)

:: Set default path to the log folder.
if "%SERVER_LOG_DIR%"=="" (
    set "SERVER_LOG_DIR=%USERPROFILE%\OpenEstate-Files\logs"
)

:: Set default path to the var folder.
if "%SERVER_VAR_DIR%"=="" (
    set "SERVER_VAR_DIR=%USERPROFILE%\OpenEstate-Files"
)

"%JAVA_COMMAND%" ^
    -Xms%JAVA_HEAP_MINIMUM% ^
    -Xmx%JAVA_HEAP_MAXIMUM% ^
    -classpath "lib\*" ^
    %JAVA_OPTIONS% ^
    -Dopenestate.server.app=ssl-init ^
    -Dopenestate.server.etcDir="%SERVER_ETC_DIR%" ^
    -Dopenestate.server.logDir="%SERVER_LOG_DIR%" ^
    -Dopenestate.server.varDir="%SERVER_VAR_DIR%" ^
    org.openestate.tool.server.utils.SslGenerator %*
popd
