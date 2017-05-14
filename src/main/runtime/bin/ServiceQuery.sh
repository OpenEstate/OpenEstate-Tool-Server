#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# query service for the ImmoTool server
# Copyright (C) 2009-2017 OpenEstate.org
# ----------------------------------------------------------------------------

# init environment
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$DIR/env.sh"

# start execution
if [ "$SYSTEM_LINUX" == "1" ]; then

  "$SYSTEMD_SYSTEMCTL" status "$SERVICE_NAME.service"

elif [ "$SYSTEM_DARWIN" == "1" ]; then

  echo "ERROR!"
  echo "Service for macOS is not implemented yet."
  exit 1

else

  echo "ERROR!"
  echo "Your operating system ($SYSTEM) is not supported."
  exit 1

fi
