@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM launch ImmoTool server within the YAJSW wrapper in foreground
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

pushd %~dp0
call env.bat
%wrapper_bat% -c %conf_file%
popd
pause
