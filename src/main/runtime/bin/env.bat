@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM setup ImmoTool server for the YAJSW wrapper
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: default java home
set wrapper_home=%~dp0/..

:: default java exe for running the wrapper
:: note this is not the java exe for running the application. the exe for running the application is defined in the wrapper configuration file
set java_exe="java"
set javaw_exe="javaw"

:: location of the wrapper jar file. necessary lib files will be loaded by this jar. they must be at <wrapper_home>/lib/...
set wrapper_jar="%wrapper_home%/var/wrapper/wrapper.jar"
set wrapper_app_jar="%wrapper_home%/var/wrapper/wrapperApp.jar"

:: setting java options for wrapper process. depending on the scripts used, the wrapper may require more memory.
set wrapper_java_options=-Xmx30m -Dwrapper_home="%wrapper_home%" -Djna_tmpdir="%wrapper_home%/var/tmp" -Djava.net.preferIPv4Stack=true

:: wrapper bat file for running the wrapper
set wrapper_bat="%wrapper_home%/bin/wrapper.bat"
::set wrapperw_bat="%wrapper_home%/bin/wrapperW.bat"

:: configuration file used by all bat files
set conf_file="%wrapper_home%/etc/wrapper.conf"

:: default configuration used in genConfig
::set conf_default_file="%wrapper_home%/etc/wrapper.conf.default"
