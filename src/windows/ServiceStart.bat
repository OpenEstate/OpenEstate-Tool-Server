@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer ${project.version}
@REM start Windows service for OpenEstate-ImmoServer
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\..\"

pushd %BASE_DIR%
set "BASE_DIR=%CD%"
popd

reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set ARCH=32BIT || set ARCH=64BIT
if %ARCH%==32BIT (
    :: This is a 32bit operating system.
    set "SERVICE_COMMAND=%SCRIPT_DIR%\service\Service32.exe"
) else if %ARCH%==64BIT (
    :: This is a 64bit operating system.
    set "SERVICE_COMMAND=%SCRIPT_DIR%\service\Service64.exe"
) else (
    echo Your operating system is not supported!
    pause
    exit 1
)

%SERVICE_COMMAND% //ES//OpenEstate-ImmoServer

IF %ERRORLEVEL% NEQ 0 (
    echo The service was NOT started! ^(error %ERRORLEVEL%^)
    echo Please make sure, that the service is installed and not already running.
) else (
    echo The service was started successfully.
)

pause
