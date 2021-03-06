#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
# uninstall launchd daemon
# Copyright (C) 2009-2019 OpenEstate.org
# ----------------------------------------------------------------------------
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SERVICE_NAME="org.openestate.tool.server.service"
BACKUP_NAME="org.openestate.tool.server.backup"


#
# Start execution...
#

echo ""
echo "----------------------------------------------------------------------"
echo " Uninstalling service for ${project.baseName}..."
echo "----------------------------------------------------------------------"
echo ""

SERVER_UNIT="/Library/LaunchDaemons/$SERVICE_NAME.plist"
BACKUP_UNIT="/Library/LaunchDaemons/$BACKUP_NAME.plist"

if [[ ! -f "$SERVER_UNIT" ]] ; then
    echo "It seems, that the service was not installed yet."
    exit 1
fi

sudo launchctl stop "$SERVICE_NAME"
sudo launchctl unload "$SERVER_UNIT"
sudo rm -f "$SERVER_UNIT"

if [[ -f "$SERVER_UNIT" ]] ; then
    echo "ERROR: The service file was not properly removed."
    exit 1
fi

if [[ -f "$BACKUP_UNIT" ]] ; then
    sudo launchctl stop "$BACKUP_NAME"
    sudo launchctl unload "$BACKUP_UNIT"
    sudo rm -f "$BACKUP_UNIT"
fi

if [[ -f "$BACKUP_UNIT" ]] ; then
    echo "WARNING: The backup service file was not properly removed."
fi

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully uninstalled."
echo "----------------------------------------------------------------------"
echo ""
