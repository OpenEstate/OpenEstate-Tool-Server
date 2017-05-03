:: ----------------------------------------------------------------------------
:: OpenEstate-ImmoServer
:: $Id: init_ssl.bat 1438 2012-03-16 17:13:37Z andy $
:: Create SSL key pair and certificate for the server
:: ----------------------------------------------------------------------------
@echo off

:: path to the keytool binary, that is provided by java
set KEYTOOL=keytool.exe

:: number of days, for how long the created certificate will be valid
set VALIDITY=999

:: path to the created server keystore
set SERVER_KEYSTORE=OpenEstate-ImmoServer.jks

:: path to the created server certificate
set SERVER_CERTIFICATE=OpenEstate-ImmoServer.crt

::
:: Start execution...
::

set BASEDIR=%~dp0
set SERVER_KEYSTORE=%BASEDIR%%SERVER_KEYSTORE%
set SERVER_CERTIFICATE=%BASEDIR%%SERVER_CERTIFICATE%
::echo %SERVER_KEYSTORE%
::echo %SERVER_CERTIFICATE%

:init
set found=0
if exist "%SERVER_KEYSTORE%" set found=1
if exist "%SERVER_CERTIFICATE%" set found=1
if "%found%"=="1" goto do_ask_for_delete

:step1
cls
echo.
echo ############################################
echo # Step 1: Create private / public key pair #
echo ############################################
echo.
%KEYTOOL% -genkey -alias openestate-server -keyalg RSA -validity %VALIDITY% -keystore "%SERVER_KEYSTORE%" -storetype JKS
echo.

:step2
cls
echo.
echo ############################################
echo # Step 2: Export certificate from key pair #
echo ############################################
echo.
%KEYTOOL% -export -alias openestate-server -keystore "%SERVER_KEYSTORE%" -rfc -file "%SERVER_CERTIFICATE%"
echo.

:finish
cls
echo.
echo ############################################
echo # SSL files were successfully created      #
echo ############################################
echo.
echo Stored server keystore at:
echo %SERVER_KEYSTORE%
echo.
echo Stored server certificate at:
echo %SERVER_CERTIFICATE%
goto do_quit

:do_ask_for_delete
cls
echo.
echo ############################################
echo # Warning: The SSL files already exist!    #
echo ############################################
echo.
set /p selection="Do you want to delete the SSL files and continue (y/n)? "
set selection=%selection:~0,1%
if /I "%selection%"=="Y" goto do_delete
if /I "%selection%"=="J" goto do_delete
echo Script was cancelled...
goto do_quit

:do_delete
del /F "%SERVER_KEYSTORE%"
del /F "%SERVER_CERTIFICATE%"
goto step1

:do_quit
echo.
pause
