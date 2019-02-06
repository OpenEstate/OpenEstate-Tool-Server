#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer ${project.version}
# uninstall systemd service
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

UNIT="/etc/systemd/system/$SERVICE_NAME.service"
SYSTEMCTL="$(which systemctl)"
SUDO="$(which sudo)"

if [[ ! -f "$UNIT" ]] ; then
    echo "It seems, that the service was not installed yet."
    exit 1
fi

if [[ ! -f "$SYSTEMCTL" ]] || [[ ! -x "$SYSTEMCTL" ]] ; then
    echo "It seems, that systemd is not available on your operating system."
    exit 1
fi

if [[ $EUID -ne 0 ]] ; then
    if [[ ! -f "$SUDO" ]] || [[ ! -x "$SUDO" ]] ; then
        echo "It seems, that sudo is not available on your operating system."
        echo "Either start this script as root user or install sudo."
        exit 1
    fi
    SYSTEMCTL_COMMAND="$SUDO $SYSTEMCTL"
else
    SYSTEMCTL_COMMAND="$SYSTEMCTL"
fi

# Stop the service, if it is currently running.
${SYSTEMCTL_COMMAND} stop "$SERVICE_NAME"

# Disable the service.
${SYSTEMCTL_COMMAND} disable "$SERVICE_NAME"

# Remove service file.
if [[ $EUID -ne 0 ]] ; then
    "$SUDO" rm -f "$UNIT"
else
    rm -f "$UNIT"
fi

if [[ -f "$UNIT" ]] ; then
    echo "ERROR: The service file was not properly removed."
    exit 1
fi

# Update systemd.
${SYSTEMCTL_COMMAND} daemon-reload

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully uninstalled."
echo "----------------------------------------------------------------------"
echo ""
