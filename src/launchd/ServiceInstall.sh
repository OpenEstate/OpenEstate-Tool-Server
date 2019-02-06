#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer ${project.version}
# install as a launchd daemon
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


#
# Start execution...
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ETC_DIR="$( cd "$( dirname "$DIR" )" && pwd )/etc"
TEMPLATE="$ETC_DIR/launchd/org.openestate.tool.server.service.plist"
UNIT="/Library/LaunchDaemons/$SERVICE_NAME.plist"

if [[ -f "$UNIT" ]] ; then
    echo "It seems, that the service was already installed."
    exit 1
fi

# Set startup script for the service.
UNIT_EXEC_START="$DIR/Start.sh"

# Set working directory for the service.
UNIT_WORKING_DIRECTORY="$DIR"

# Ask for the executing user.
read -p "Which user should execute the service [default is $USER]: " UNIT_USER
UNIT_USER="$( echo ${UNIT_USER} | sed -e 's/^[ \t]*//' )"
if [[ -z "$UNIT_USER" ]] ; then
    UNIT_USER="$USER"
fi
id -u ${UNIT_USER} >/dev/null 2>&1
if [[ $? -ne 0 ]] ; then
    echo "ERROR: The user \"$UNIT_USER\" does not exist on this machine."
    exit 1
fi

# Ask for the executing group.
read -p "Which group should execute the service [default is staff]: " UNIT_GROUP
UNIT_GROUP="$( echo ${UNIT_GROUP} | sed -e 's/^[ \t]*//' )"
if [[ -z "$UNIT_GROUP" ]] ; then
    UNIT_GROUP="staff"
fi
cat /etc/group | grep -q -E "^$UNIT_GROUP:"
if [[ $? -ne 0 ]] ; then
    echo "ERROR: The group \"$UNIT_GROUP\" does not exist on this machine."
    exit 1
fi

# Set etc directory for the service.
UNIT_ETC_DIRECTORY="/Users/$UNIT_USER/OpenEstate-ImmoServer/etc"

# Create temporary unit file.
UNIT_TEMP="$(mktemp)"
sed \
    -e "s|\${Program}|$UNIT_EXEC_START|g" \
    -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
    -e "s|\${EtcDirectory}|$UNIT_ETC_DIRECTORY|g" \
    -e "s|\${UserName}|$UNIT_USER|g" \
    -e "s|\${GroupName}|$UNIT_GROUP|g" \
    "$TEMPLATE" > "$UNIT_TEMP"

# Move temporary unit into the service folder.
sudo mv "$UNIT_TEMP" "$UNIT"
sudo chown root:wheel "$UNIT"
sudo chmod go-w "$UNIT"
sudo chmod ugo+r "$UNIT"

# Make sure, that the unit file is available.
if [[ ! -f "$UNIT" ]] ; then
    echo "ERROR: Can't copy the service file."
    exit 1
fi

# Update launchd.
sudo launchctl load "$UNIT"
if [[ $? -ne 0 ]] ; then
    echo "ERROR: Can't enable the service."
    exit 1
fi

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully registered as a launchd daemon at:"
echo ""
echo " $UNIT"
echo ""
echo " The service will start automatically with the next system boot."
echo "----------------------------------------------------------------------"
echo " You can start the service manually via:"
echo ""
echo " sudo launchctl start $SERVICE_NAME"
echo "----------------------------------------------------------------------"
echo " You can stop the service manually via:"
echo ""
echo " sudo launchctl stop $SERVICE_NAME"
echo "----------------------------------------------------------------------"
echo " You can check the service status via:"
echo ""
echo " sudo launchctl list $SERVICE_NAME"
echo "----------------------------------------------------------------------"
echo ""
