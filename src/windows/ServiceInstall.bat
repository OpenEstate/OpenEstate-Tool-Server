@REM ----------------------------------------------------------------------------
@REM ${project.baseName} ${project.version}
@REM install Windows service for ${project.baseName}
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

::
:: Start execution...
::

:: Path to the folder, where the server configuration files are stored.
set "SERVER_ETC_DIR="

:: Path to the folder, where the server log files are stored.
set "SERVER_LOG_DIR="

:: Path to the folder, where the server data files are stored.
set "SERVER_VAR_DIR="

:: Memory settings of the Java Runtime Environment
set "JAVA_HEAP_MINIMUM=32"
set "JAVA_HEAP_MAXIMUM=512"


::
:: Start execution...
::

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\.."

pushd "%BASE_DIR%"
set "BASE_DIR=%CD%"
popd

IF "%1"=="/q" (
    set "QUIET=1"
) else (
    set "QUIET=0"
)

reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\services\${project.baseName}" >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
    if "%QUIET%"=="0" (
        echo The service is already installed!
        pause
    )
    exit /b 1
)

if exist "%BASE_DIR%\jre\" (
    set "JVM_DLL=%BASE_DIR%\jre\bin\server\jvm.dll"
) else (
    set "JVM_DLL=auto"
)

:: Set default path to the etc folder.
if "%SERVER_ETC_DIR%"=="" (
    set "SERVER_ETC_DIR=%BASE_DIR%\etc"
)

:: Set default path to the log folder.
if "%SERVER_LOG_DIR%"=="" (
    set "SERVER_LOG_DIR=%USERPROFILE%\${project.baseName}\log"
)

:: Set default path to the var folder.
if "%SERVER_VAR_DIR%"=="" (
    set "SERVER_VAR_DIR=%USERPROFILE%\${project.baseName}"
)

:: Create log directory.
mkdir "%SERVER_LOG_DIR%"

:: Install the service.
set "SERVICE_COMMAND=%SCRIPT_DIR%\service\Service.exe"
"%SERVICE_COMMAND%" //IS//${project.baseName} ^
    --DisplayName="${project.baseName}" ^
    --Description="Server for OpenEstate-ImmoTool" ^
    --Install="%SERVICE_COMMAND%" ^
    --Startup=auto ^
    --Jvm="%JVM_DLL%" ^
    --JvmMs="%JAVA_HEAP_MINIMUM%" ^
    --JvmMx="%JAVA_HEAP_MAXIMUM%" ^
    ++JvmOptions=-Dfile.encoding=UTF-8 ^
    ++JvmOptions=-Dopenestate.server.app=server ^
    ++JvmOptions=-Dopenestate.server.etcDir="%SERVER_ETC_DIR%" ^
    ++JvmOptions=-Dopenestate.server.logDir="%SERVER_LOG_DIR%" ^
    ++JvmOptions=-Dopenestate.server.varDir="%SERVER_VAR_DIR%" ^
    --Classpath="%BASE_DIR%\lib\*" ^
    --StartMode=jvm ^
    --StartPath="%BASE_DIR%" ^
    --StartClass=org.openestate.tool.server.WindowsService ^
    --StartParams="" ^
    --StartMethod=start ^
    --StopMode=jvm ^
    --StopPath="%BASE_DIR%" ^
    --StopClass=org.openestate.tool.server.WindowsService ^
    --StopMethod=stop ^
    --StopTimeout=30 ^
    --LogPath="%SERVER_LOG_DIR%" ^
    --LogPrefix="service"

IF %ERRORLEVEL% NEQ 0 (
    if "%QUIET%"=="0" (
        echo The service was NOT installed! ^(error %ERRORLEVEL%^)
        echo Please make sure, that the service is not already installed.
        pause
    )
    exit /b 1
)

if "%QUIET%"=="0" (
    echo The service was installed successfully.
    echo Opening the service dialog for further configuration...
    "%SCRIPT_DIR%\service\${project.baseName}.exe"
)

IF %ERRORLEVEL% NEQ 0 (
    if "%QUIET%"=="0" (
        echo Can't open the service dialog. ^(error %ERRORLEVEL%^)
        pause
    )
    exit /b 1
)
