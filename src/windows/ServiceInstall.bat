@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer ${project.version}
@REM install Windows service for OpenEstate-ImmoServer
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

set "SCRIPT=%~nx0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "BASE_DIR=%SCRIPT_DIR%\.."

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

if exist "%BASE_DIR%\jre\" (
    set "JVM_DLL=%BASE_DIR%\jre\bin\server\jvm.dll"
) else (
    set "JVM_DLL=auto"
)

%SERVICE_COMMAND% //IS//OpenEstate-ImmoServer ^
    --DisplayName="OpenEstate-ImmoServer" ^
    --Description="Server for OpenEstate-ImmoTool" ^
    --Install="%SERVICE_COMMAND%" ^
    --Startup=auto ^
    --Jvm="%JVM_DLL%" ^
    --JvmMs=32 ^
    --JvmMx=512 ^
    --Classpath="%BASE_DIR%\etc;%BASE_DIR%\lib\*" ^
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
    --LogPath="%BASE_DIR%\var\log" ^
    --LogPrefix="service"

IF %ERRORLEVEL% NEQ 0 (
    echo The service was NOT installed! ^(error %ERRORLEVEL%^)
    echo Please make sure, that the service is not already installed.
) else (
    echo The service was installed successfully.
)

pause
