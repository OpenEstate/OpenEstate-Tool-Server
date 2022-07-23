#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
# start systemd service
# Copyright (C) 2009-2022 OpenEstate.org
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

SERVER_UNIT="/etc/systemd/system/$SERVICE_NAME.service"
SYSTEMCTL="$(which systemctl)"
SUDO="$(which sudo)"

if [[ ! -f "$SERVER_UNIT" ]] ; then
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

${SYSTEMCTL_COMMAND} start "$SERVICE_NAME"
if [[ $? -ne 0 ]] ; then
    echo "ERROR: Can't start the service."
    exit 1
fi

echo ""
echo "----------------------------------------------------------------------"
echo " The service was successfully started."
echo "----------------------------------------------------------------------"
echo ""
