@REM ----------------------------------------------------------------------------
@REM OpenEstate-ImmoServer
@REM uninstall service of the ImmoTool server
@REM Copyright (C) 2009-2017 OpenEstate.org
@REM ----------------------------------------------------------------------------
@echo off

pushd %~dp0
call WrapperSettings.bat
%wrapper_bat% -r %conf_file%
popd
pause
