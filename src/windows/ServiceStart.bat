@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer ${project.version}
@REM start Windows service for OpenEstate-ImmoServer
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

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

IF "%1"=="/q" (
    set "QUIET=1"
) else (
    set "QUIET=0"
)

reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\services\OpenEstate-ImmoServer" >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    if "%QUIET%"=="0" (
        echo The service is not installed!
        pause
    )
    exit /b 1
)

set "SERVICE_COMMAND=%SCRIPT_DIR%\service\Service.exe"
"%SERVICE_COMMAND%" //ES//OpenEstate-ImmoServer

IF %ERRORLEVEL% NEQ 0 (
    if "%QUIET%"=="0" (
        echo The service was NOT started! ^(error %ERRORLEVEL%^)
        echo Please make sure, that the service is installed and not already running.
        pause
    )
    exit /b 1
) else (
    if "%QUIET%"=="0" (
        echo The service was started successfully.
        pause
    )
)
