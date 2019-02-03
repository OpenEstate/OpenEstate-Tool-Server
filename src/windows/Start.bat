@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer ${project.version}
@REM start OpenEstate-ImmoServer in foreground
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
