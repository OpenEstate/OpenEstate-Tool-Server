#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# install service for the ImmoTool server
# Copyright (C) 2009-2017 OpenEstate.org
# ----------------------------------------------------------------------------

# init environment
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "$DIR/env.sh"

# create service file for systemd
function systemd_write_service_file {

  if [ -z "$SERVICE_USER" ]; then
    DEFAULT_SERVICE_USER="$( id -un )"
    read -r -p "Enter the user, that executes the service [$DEFAULT_SERVICE_USER]: " SERVICE_USER
    if [ -z "$SERVICE_USER" ]; then
      SERVICE_USER="$DEFAULT_SERVICE_USER"
    fi
  fi

  if [ -z "$SERVICE_GROUP" ]; then
    DEFAULT_SERVICE_GROUP="$( id -gn )"
    read -r -p "Enter the group, that executes the service [$DEFAULT_SERVICE_GROUP]: " SERVICE_GROUP
    if [ -z "$SERVICE_GROUP" ]; then
      SERVICE_GROUP="$DEFAULT_SERVICE_GROUP"
    fi
  fi

  echo "Creating service file at \"$SYSTEMD_SERVICE_FILE\"."

  echo "[Unit]" > "$SYSTEMD_SERVICE_FILE"
  echo "Description=$SERVICE_DESCRIPTION" >> "$SYSTEMD_SERVICE_FILE"
  echo "After=network-online.target" >> "$SYSTEMD_SERVICE_FILE"
  echo "" >> "$SYSTEMD_SERVICE_FILE"

  echo "[Service]" >> "$SYSTEMD_SERVICE_FILE"
  echo "ExecStart=$SERVER_DIR/bin/Start.sh" >> "$SYSTEMD_SERVICE_FILE"
  echo "WorkingDirectory=$SERVER_DIR" >> "$SYSTEMD_SERVICE_FILE"
  echo "User=$SERVICE_USER" >> "$SYSTEMD_SERVICE_FILE"
  echo "Group=$SERVICE_GROUP" >> "$SYSTEMD_SERVICE_FILE"
  echo "Restart=always" >> "$SYSTEMD_SERVICE_FILE"
  echo "" >> "$SYSTEMD_SERVICE_FILE"

  echo "[Install]" >> "$SYSTEMD_SERVICE_FILE"
  echo "WantedBy=multi-user.target" >> "$SYSTEMD_SERVICE_FILE"
}

# create service file for launchd
function launchd_write_service_file {

  #if [ -z "$SERVICE_USER" ]; then
  #  DEFAULT_SERVICE_USER="$( id -un )"
  #  read -r -p "Enter the user, that executes the service [$DEFAULT_SERVICE_USER]: " SERVICE_USER
  #  if [ -z "$SERVICE_USER" ]; then
  #    SERVICE_USER="$DEFAULT_SERVICE_USER"
  #  fi
  #fi

  #if [ -z "$SERVICE_GROUP" ]; then
  #  #DEFAULT_SERVICE_GROUP="$( id -gn )"
  #  DEFAULT_SERVICE_GROUP="staff"
  #  read -r -p "Enter the group, that executes the service [$DEFAULT_SERVICE_GROUP]: " SERVICE_GROUP
  #  if [ -z "$SERVICE_GROUP" ]; then
  #    SERVICE_GROUP="$DEFAULT_SERVICE_GROUP"
  #  fi
  #fi

  echo "Creating service file at \"$LAUNCHD_SERVICE_FILE\"."

  echo "<?xml version="1.0" encoding="UTF-8"?>" > "$LAUNCHD_SERVICE_FILE"
  echo "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">" >> "$LAUNCHD_SERVICE_FILE"
  echo "<plist version=\"1.0\">" >> "$LAUNCHD_SERVICE_FILE"
  echo "  <dict>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>Disabled</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <false/>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>Label</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <string>$SERVICE_NAME</string>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>Program</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <string>$SERVER_DIR/bin/Start.sh</string>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>StandardOutPath</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <string>$SERVER_DIR/var/log/launchd.out.log</string>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>StandardErrorPath</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <string>$SERVER_DIR/var/log/launchd.err.log</string>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>WorkingDirectory</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <string>$SERVER_DIR</string>" >> "$LAUNCHD_SERVICE_FILE"

  #echo "    <key>UserName</key>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "    <string>$SERVICE_USER</string>" >> "$LAUNCHD_SERVICE_FILE"

  #echo "    <key>GroupName</key>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "    <string>$SERVICE_GROUP</string>" >> "$LAUNCHD_SERVICE_FILE"

  #echo "    <key>InitGroups</key>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "    <true/>" >> "$LAUNCHD_SERVICE_FILE"

  echo "    <key>RunAtLoad</key>" >> "$LAUNCHD_SERVICE_FILE"
  echo "    <true/>" >> "$LAUNCHD_SERVICE_FILE"

  #echo "    <key>KeepAlive</key>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "    <dict>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "      <key>NetworkState</key>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "      <true/>" >> "$LAUNCHD_SERVICE_FILE"
  #echo "    </dict>" >> "$LAUNCHD_SERVICE_FILE"

  echo "  </dict>" >> "$LAUNCHD_SERVICE_FILE"
  echo "</plist>" >> "$LAUNCHD_SERVICE_FILE"
}

# start execution
if [ "$SYSTEM_LINUX" == "1" ]; then

  if [ ! -f "$SYSTEMD_SERVICE_FILE" ]; then
    systemd_write_service_file
  else
    echo "The service file already exists."
    if ask "Do you want to recreate the service file?" Y; then
      "$SYSTEMD_SYSTEMCTL" stop "$SERVICE_NAME.service"
      "$SYSTEMD_SYSTEMCTL" disable "$SERVICE_NAME.service"
      systemd_write_service_file
      "$SYSTEMD_SYSTEMCTL" daemon-reload
    fi
  fi

  echo "Enable service $SERVICE_NAME"
  "$SYSTEMD_SYSTEMCTL" enable "$SERVICE_NAME.service"

  echo "----------------------------------------------------------------------"
  echo "The service was installed successfully within systemd!"
  echo "Use one of the following commands in order to start the service:"
  echo "  sudo $SYSTEMD_SYSTEMCTL start $SERVICE_NAME.service"
  echo "  sudo $DIR/ServiceStart.sh"
  echo "----------------------------------------------------------------------"

elif [ "$SYSTEM_DARWIN" == "1" ]; then

  if [ ! -f "$LAUNCHD_SERVICE_FILE" ]; then
    launchd_write_service_file
  else
    echo "The service file already exists."
    if ask "Do you want to recreate the service file?" Y; then
      "$LAUNCHD_LAUNCHCTL" stop "$SERVICE_NAME"
      "$LAUNCHD_LAUNCHCTL" unload "$LAUNCHD_SERVICE_FILE"
      launchd_write_service_file
    fi
  fi

  echo "Enable service $SERVICE_NAME"
  "$LAUNCHD_LAUNCHCTL" load "$LAUNCHD_SERVICE_FILE"
  "$LAUNCHD_LAUNCHCTL" stop "$SERVICE_NAME"

  echo "----------------------------------------------------------------------"
  echo "The service was installed successfully within launchd!"
  echo "Use one of the following commands in order to start the service:"
  echo "  $LAUNCHD_LAUNCHCTL start $SERVICE_NAME"
  echo "  $DIR/ServiceStart.sh"
  echo "----------------------------------------------------------------------"

else

  echo "ERROR!"
  echo "Your operating system ($SYSTEM) is not supported."
  exit 1

fi
