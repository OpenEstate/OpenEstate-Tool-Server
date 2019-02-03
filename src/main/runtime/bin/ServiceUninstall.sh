#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# uninstall service for the ImmoTool server
# Copyright (C) 2009-2019 OpenEstate.org
# ----------------------------------------------------------------------------

# init environment
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$DIR/env.sh"

# start execution
if [ "$SYSTEM_LINUX" == "1" ]; then

  echo "Disable service \"$SERVICE_NAME\"."
  "$SYSTEMD_SYSTEMCTL" stop "$SERVICE_NAME.service"
  "$SYSTEMD_SYSTEMCTL" disable "$SERVICE_NAME.service"

  if [ -f "$SYSTEMD_SERVICE_FILE" ]; then
    if ask "Do you want to remove the service file?" Y; then
      echo "Removing service file at \"$SYSTEMD_SERVICE_FILE\"."
      rm "$SYSTEMD_SERVICE_FILE"
      "$SYSTEMD_SYSTEMCTL" daemon-reload
    fi
  fi

  echo "----------------------------------------------------------------------"
  echo "The service was uninstalled successfully!"
  echo "----------------------------------------------------------------------"

elif [ "$SYSTEM_DARWIN" == "1" ]; then

  echo "Disable service \"$SERVICE_NAME\"."
  "$LAUNCHD_LAUNCHCTL" stop "$SERVICE_NAME"
  "$LAUNCHD_LAUNCHCTL" unload "$LAUNCHD_SERVICE_FILE"

  if [ -f "$LAUNCHD_SERVICE_FILE" ]; then
    if ask "Do you want to remove the service file?" Y; then
      echo "Removing service file at \"$LAUNCHD_SERVICE_FILE\"."
      rm "$LAUNCHD_SERVICE_FILE"
    fi
  fi

  echo "----------------------------------------------------------------------"
  echo "The service was uninstalled successfully!"
  echo "----------------------------------------------------------------------"

else

  echo "ERROR!"
  echo "Your operating system ($SYSTEM) is not supported."
  exit 1

fi
