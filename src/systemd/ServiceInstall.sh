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


#
# Start execution...
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ETC_DIR="$( cd "$( dirname "$DIR" )" && pwd )/etc"
TEMPLATE="$ETC_DIR/systemd/openestate-immoserver.service"
UNIT="/etc/systemd/system/$SERVICE_NAME.service"

SYSTEMCTL="$(which systemctl)"
SUDO="$(which sudo)"
SED="$(which sed)"
TEE="$(which tee)"

if [[ -f "$UNIT" ]] ; then
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
UNIT_EXEC_START="$DIR/Start.sh"

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

# Create temporary unit file.
UNIT_TEMP="$(mktemp)"
"$SED" \
    -e "s|\${ExecStart}|$UNIT_EXEC_START|g" \
    -e "s|\${WorkingDirectory}|$UNIT_WORKING_DIRECTORY|g" \
    -e "s|\${User}|$UNIT_USER|g" \
    -e "s|\${Group}|$UNIT_GROUP|g" \
    "$TEMPLATE" > "$UNIT_TEMP"

# Move temporary unit into the service folder.
if [[ $EUID -eq 0 ]] ; then
    mv "$UNIT_TEMP" "$UNIT"
    chown root:root "$UNIT"
    chmod o-w "$UNIT"
else
    "$SUDO" mv "$UNIT_TEMP" "$UNIT"
    "$SUDO" chown root:root "$UNIT"
    "$SUDO" chmod o-w "$UNIT"
fi

# Make sure, that the unit file is available.
if [[ ! -f "$UNIT" ]] ; then
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

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully registered as a systemd unit at:"
echo ""
echo " $UNIT"
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
echo ""
