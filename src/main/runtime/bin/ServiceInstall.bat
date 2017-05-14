@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM install service of the ImmoTool server
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

pushd %~dp0
call env.bat
%wrapper_bat% -i %conf_file%
popd
pause
