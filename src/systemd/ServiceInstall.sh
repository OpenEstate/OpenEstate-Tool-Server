#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
# install as a systemd service
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

SERVICE_NAME="openestate-immoserver"
BACKUP_NAME="openestate-immoserver-backup"


#
# Start execution...
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ETC_DIR="$( cd "$( dirname "$DIR" )" && pwd )/etc"
SERVER_UNIT="/etc/systemd/system/$SERVICE_NAME.service"
SERVER_UNIT_TEMPLATE="$ETC_DIR/systemd/openestate-immoserver.service"
BACKUP_TIMER="/etc/systemd/system/$BACKUP_NAME.timer"
BACKUP_TIMER_TEMPLATE="$ETC_DIR/systemd/openestate-immoserver-backup.timer"
BACKUP_UNIT="/etc/systemd/system/$BACKUP_NAME.service"
BACKUP_UNIT_TEMPLATE="$ETC_DIR/systemd/openestate-immoserver-backup.service"

SYSTEMCTL="$(which systemctl)"
SUDO="$(which sudo)"
SED="$(which sed)"
TEE="$(which tee)"
SHELL="$(which bash)"

if [[ -f "$SERVER_UNIT" ]] ; then
    echo "It seems, that the service was already installed."
    exit 1
fi

if [[ ! -f "$SYSTEMCTL" ]] || [[ ! -x "$SYSTEMCTL" ]] ; then
    echo "It seems, that systemd is not available on your operating system."
    exit 1
fi

if [[ ! -f "$SED" ]] || [[ ! -x "$SED" ]] ; then
    echo "It seems, that the \"sed\" command is not available on your operating system."
    exit 1
fi

if [[ ! -f "$SHELL" ]] || [[ ! -x "$SHELL" ]] ; then
    echo "It seems, that the \"bash\" command is not available on your operating system."
    exit 1
fi

if [[ $EUID -ne 0 ]] ; then
    if [[ ! -f "$TEE" ]] || [[ ! -x "$TEE" ]] ; then
        echo "It seems, that the \"tee\" command is not available on your operating system."
        echo "Either start this script as root user or install \"tee\"."
        exit 1
    fi
    if [[ ! -f "$SUDO" ]] || [[ ! -x "$SUDO" ]] ; then
        echo "It seems, that the \"sudo\" command is not available on your operating system."
        echo "Either start this script as root user or install \"sudo\"."
        exit 1
    fi
    SYSTEMCTL_COMMAND="$SUDO $SYSTEMCTL"
else
    SYSTEMCTL_COMMAND="$SYSTEMCTL"
fi

# Set startup script for the service.
UNIT_EXEC_START="$SHELL $DIR/Start.sh"

# Set working directory for the service.
UNIT_WORKING_DIRECTORY="$DIR"

# Ask for the executing user.
read -p "Which user should execute the service [default is $USER]: " UNIT_USER
UNIT_USER="$( echo ${UNIT_USER} | sed -e 's/^[ \t]*//' )"
if [[ -z "$UNIT_USER" ]] ; then
    UNIT_USER="$USER"
fi
getent passwd ${UNIT_USER} >/dev/null 2>&1
if [[ $? -ne 0 ]] ; then
    echo "ERROR: The user \"$UNIT_USER\" does not exist on this machine."
    exit 1
fi

# Ask for the executing group.
read -p "Which group should execute the service [default is $UNIT_USER]: " UNIT_GROUP
UNIT_GROUP="$( echo ${UNIT_GROUP} | sed -e 's/^[ \t]*//' )"
if [[ -z "$UNIT_GROUP" ]] ; then
    UNIT_GROUP="$UNIT_USER"
fi
getent group ${UNIT_GROUP} >/dev/null 2>&1
if [[ $? -ne 0 ]] ; then
    echo "ERROR: The group \"$UNIT_GROUP\" does not exist on this machine."
    exit 1
fi

# Ask for installation of a backup timer.
read -p "Do you want to install a timer for automatic daily backups (yes/no)? [default is yes]: " BACKUP
BACKUP="$( echo ${BACKUP} | sed -e 's/^[ \t]*//' )"
BACKUP="$( echo ${BACKUP} | tr '[:upper:]' '[:lower:]' )"

# Create temporary unit file.
UNIT_TEMP="$(mktemp)"
"$SED" \
    -e "s|\${ExecStart}|$UNIT_EXEC_START|g" \
    -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
    -e "s|\${User}|$UNIT_USER|g" \
    -e "s|\${Group}|$UNIT_GROUP|g" \
    "$SERVER_UNIT_TEMPLATE" > "$UNIT_TEMP"

# Move temporary unit into the service folder.
if [[ $EUID -eq 0 ]] ; then
    mv "$UNIT_TEMP" "$SERVER_UNIT"
    chown root:root "$SERVER_UNIT"
    chmod 644 "$SERVER_UNIT"
else
    "$SUDO" mv "$UNIT_TEMP" "$SERVER_UNIT"
    "$SUDO" chown root:root "$SERVER_UNIT"
    "$SUDO" chmod 644 "$SERVER_UNIT"
fi

# Make sure, that the unit file is available.
if [[ ! -f "$SERVER_UNIT" ]] ; then
    echo "ERROR: Can't copy the service file."
    exit 1
fi

# Update systemd.
${SYSTEMCTL_COMMAND} daemon-reload
${SYSTEMCTL_COMMAND} enable "$SERVICE_NAME"
if [[ $? -ne 0 ]] ; then
    echo "ERROR: Can't enable the service."
    exit 1
fi

# Install backup timer.
if [[ -z "$BACKUP" ]] || [[ "$BACKUP" == "y" ]] || [[ "$BACKUP" == "yes" ]] ; then
    BACKUP="1"

    # Set startup script for the backup timer.
    UNIT_EXEC_BACKUP="$SHELL $DIR/ManagerBackup.sh"

    # Create temporary unit file.
    UNIT_TEMP="$(mktemp)"
    "$SED" \
        -e "s|\${ExecStart}|$UNIT_EXEC_BACKUP|g" \
        -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
        -e "s|\${User}|$UNIT_USER|g" \
        -e "s|\${Group}|$UNIT_GROUP|g" \
        "$BACKUP_UNIT_TEMPLATE" > "$UNIT_TEMP"

    # Move temporary unit into the service folder.
    if [[ $EUID -eq 0 ]] ; then
        mv "$UNIT_TEMP" "$BACKUP_UNIT"
        chown root:root "$BACKUP_UNIT"
        chmod 644 "$BACKUP_UNIT"
    else
        "$SUDO" mv "$UNIT_TEMP" "$BACKUP_UNIT"
        "$SUDO" chown root:root "$BACKUP_UNIT"
        "$SUDO" chmod 644 "$BACKUP_UNIT"
    fi

    # Copy timer file into the service folder.
    if [[ $EUID -eq 0 ]] ; then
        cp "$BACKUP_TIMER_TEMPLATE" "$BACKUP_TIMER"
        chown root:root "$BACKUP_TIMER"
        chmod 644 "$BACKUP_TIMER"
    else
        "$SUDO" cp "$BACKUP_TIMER_TEMPLATE" "$BACKUP_TIMER"
        "$SUDO" chown root:root "$BACKUP_TIMER"
        "$SUDO" chmod 644 "$BACKUP_TIMER"
    fi

    # Make sure, that the unit file is available.
    if [[ ! -f "$BACKUP_UNIT" ]] || [[ ! -f "$BACKUP_TIMER" ]] ; then
        echo "WARNING: Can't copy the backup service files."
        rm -f "$BACKUP_UNIT"
        rm -f "$BACKUP_TIMER"
    else
        ${SYSTEMCTL_COMMAND} daemon-reload
        ${SYSTEMCTL_COMMAND} enable "$BACKUP_NAME.timer"
        if [[ $? -ne 0 ]] ; then
            echo "WARNING: Can't enable the backup timer."
        fi
    fi
fi

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully registered as a systemd unit at:"
echo ""
echo " $SERVER_UNIT"
echo ""
echo " The service will start automatically with the next system boot."
echo "----------------------------------------------------------------------"
echo " You can start the service manually via:"
echo ""
echo " sudo systemctl start $SERVICE_NAME"
echo "----------------------------------------------------------------------"
echo " You can stop the service manually via:"
echo ""
echo " sudo systemctl stop $SERVICE_NAME"
echo "----------------------------------------------------------------------"
echo " You can check the service status via:"
echo ""
echo " sudo systemctl status $SERVICE_NAME"
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
