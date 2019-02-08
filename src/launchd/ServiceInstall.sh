#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
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
BACKUP_NAME="org.openestate.tool.server.backup"


#
# Start execution...
#

echo ""
echo "----------------------------------------------------------------------"
echo " Installing service for ${project.baseName}..."
echo "----------------------------------------------------------------------"
echo ""

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ETC_DIR="$( cd "$( dirname "$DIR" )" && pwd )/etc"
SERVER_UNIT="/Library/LaunchDaemons/$SERVICE_NAME.plist"
SERVER_UNIT_TEMPLATE="$ETC_DIR/launchd/org.openestate.tool.server.service.plist"
BACKUP_UNIT="/Library/LaunchDaemons/$BACKUP_NAME.plist"
BACKUP_UNIT_TEMPLATE="$ETC_DIR/launchd/org.openestate.tool.server.backup.plist"

if [[ -f "$SERVER_UNIT" ]] ; then
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

# Ask for installation of a backup timer.
read -p "Do you want to install a timer for automatic daily backups (yes/no)? [default is yes]: " BACKUP
BACKUP="$( echo ${BACKUP} | sed -e 's/^[ \t]*//' )"
BACKUP="$( echo ${BACKUP} | tr '[:upper:]' '[:lower:]' )"

# Set etc directory for the service.
UNIT_ETC_DIRECTORY="/Users/$UNIT_USER/${project.baseName}/etc"

# Create temporary unit file.
UNIT_TEMP="$(mktemp)"
sed \
    -e "s|\${Program}|$UNIT_EXEC_START|g" \
    -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
    -e "s|\${EtcDirectory}|$UNIT_ETC_DIRECTORY|g" \
    -e "s|\${UserName}|$UNIT_USER|g" \
    -e "s|\${GroupName}|$UNIT_GROUP|g" \
    "$SERVER_UNIT_TEMPLATE" > "$UNIT_TEMP"

# Move temporary unit into the service folder.
sudo mv "$UNIT_TEMP" "$SERVER_UNIT"
sudo chown root:wheel "$SERVER_UNIT"
sudo chmod go-w "$SERVER_UNIT"
sudo chmod ugo+r "$SERVER_UNIT"

# Make sure, that the unit file is available.
if [[ ! -f "$SERVER_UNIT" ]] ; then
    echo "ERROR: Can't copy the service file."
    exit 1
fi

# Update launchd.
sudo launchctl load "$SERVER_UNIT"
if [[ $? -ne 0 ]] ; then
    echo "ERROR: Can't enable the service."
    exit 1
fi

# Install backup timer.
if [[ -z "$BACKUP" ]] || [[ "$BACKUP" == "y" ]] || [[ "$BACKUP" == "yes" ]] ; then
    BACKUP="1"

    # Set startup script for the backup timer.
    UNIT_EXEC_BACKUP="$DIR/ManagerBackup.sh"

    # Create temporary unit file.
    UNIT_TEMP="$(mktemp)"
    sed \
        -e "s|\${Program}|$UNIT_EXEC_BACKUP|g" \
        -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
        -e "s|\${EtcDirectory}|$UNIT_ETC_DIRECTORY|g" \
        -e "s|\${UserName}|$UNIT_USER|g" \
        -e "s|\${GroupName}|$UNIT_GROUP|g" \
        "$BACKUP_UNIT_TEMPLATE" > "$UNIT_TEMP"

    # Move temporary unit into the service folder.
    sudo mv "$UNIT_TEMP" "$BACKUP_UNIT"
    sudo chown root:wheel "$BACKUP_UNIT"
    sudo chmod go-w "$BACKUP_UNIT"
    sudo chmod ugo+r "$BACKUP_UNIT"

    # Make sure, that the unit file is available.
    if [[ ! -f "$BACKUP_UNIT" ]] ; then
        echo "WARNING: Can't copy the backup service file."
    else
        sudo launchctl load "$BACKUP_UNIT"
        if [[ $? -ne 0 ]] ; then
            echo "WARNING: Can't enable the backup service."
        fi
    fi
fi

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully registered as a launchd daemon at:"
echo ""
echo " $SERVER_UNIT"
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

if [[ "$BACKUP" == "1" ]] ; then
    echo " IMPORTANT NOTICE:"
    echo ""
    echo " Daily automatic backups were enabled. Please make sure, that the"
    echo " connection settings for the databases to backup are properly set in:"
    echo ""
    echo " $ETC_DIR/manager.conf"
    echo ""
    echo " Otherwise automatic backups will fail!"
    echo " See https://manual.openestate.org for more information."
    echo "----------------------------------------------------------------------"
fi

echo ""
