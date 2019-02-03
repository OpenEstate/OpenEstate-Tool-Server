@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM launch ImmoTool server within the YAJSW wrapper
@REM Copyright (C) 2009-2019 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

:: echo %java_exe% %wrapper_java_options% -jar %wrapper_jar% %1 %2 %3 %4 %5 %6 %7 %8 %9
%java_exe% %wrapper_java_options% -jar %wrapper_jar% %1 %2 %3 %4 %5 %6 %7 %8 %9
