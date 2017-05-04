@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM query service of the ImmoTool server
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

pushd %~dp0
call WrapperSettings.bat
%wrapper_bat% -q %conf_file%
popd
pause
