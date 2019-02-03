#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# setup ImmoTool server for systemd / launchd
# Copyright (C) 2009-2019 OpenEstate.org
# ----------------------------------------------------------------------------

# detect server directory
SERVER_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
HOME_DIR="$( cd ~ && pwd -P )"

# details about the service
SERVICE_NAME="openestate"
SERVICE_DESCRIPTION="OpenEstate-ImmoServer"
SERVICE_USER=""
SERVICE_GROUP=""

# settings for systemd
SYSTEMD_SERVICE_DIR="/etc/systemd/system"
SYSTEMD_SERVICE_FILE="$SYSTEMD_SERVICE_DIR/$SERVICE_NAME.service"
SYSTEMD_SYSTEMCTL=""

# settings for launchd
#LAUNCHD_SERVICE_DIR="/Library/LaunchDaemons"
LAUNCHD_SERVICE_DIR="$HOME_DIR/Library/LaunchAgents"
LAUNCHD_SERVICE_FILE="$LAUNCHD_SERVICE_DIR/$SERVICE_NAME.plist"
LAUNCHD_LAUNCHCTL=""

# OS specific initialization
SYSTEM="$( uname -s )"
SYSTEM_DARWIN=0
SYSTEM_LINUX=0
case "$SYSTEM" in

  Darwin)
    #echo "Initializing macOS environment..."
    SYSTEM_DARWIN=1

    # check service directory
    if [ ! -d "$LAUNCHD_SERVICE_DIR" ]; then
      echo "ERROR!"
      echo "Can't find launchd service directory at \"$LAUNCHD_SERVICE_DIR\"."
      exit 1
    fi

    # check for launchctl
    if [ -z "$LAUNCHD_LAUNCHCTL" ] && [ -x "$( which launchctl )" ]; then
      #echo "Using default launchctl command."
      LAUNCHD_LAUNCHCTL="launchctl"
    elif [ -z "$LAUNCHD_LAUNCHCTL" ] || [ ! -x "$LAUNCHD_LAUNCHCTL" ]; then
      echo "ERROR!"
      echo "Can't find the launchctl command."
      echo "Maybe launchctl is not available through your PATH environment or properly configured."
      exit 1
    fi
    ;;

  Linux)
    #echo "Initializing Linux environment..."
    SYSTEM_LINUX=1

    # check service directory
    if [ ! -d "$SYSTEMD_SERVICE_DIR" ]; then
      echo "ERROR!"
      echo "Can't find systemd service directory at \"$SYSTEMD_SERVICE_DIR\"."
      echo "Maybe your Linux system does not support systemd."
      exit 1
    fi

    # check for systemctl
    if [ -z "$SYSTEMD_SYSTEMCTL" ] && [ -x "$( which systemctl )" ]; then
      #echo "Using default systemctl command."
      SYSTEMD_SYSTEMCTL="systemctl"
    elif [ -z "$SYSTEMD_SYSTEMCTL" ] || [ ! -x "$SYSTEMD_SYSTEMCTL" ]; then
      echo "ERROR!"
      echo "Can't find the systemctl command."
      echo "Maybe systemctl is not available through your PATH environment or properly configured."
      exit 1
    fi
    ;;

  *)
    echo "ERROR!"
    echo "Your operating system ($SYSTEM) is not supported."
    exit 1
    ;;

esac


# This is a general-purpose function to ask Yes/No questions in Bash, either
# with or without a default answer. It keeps repeating the question until it
# gets a valid answer.
#
# see https://gist.github.com/davejamesmiller/1965569
ask() {
  # http://djm.me/ask
  local prompt default REPLY

  while true; do

    if [ "${2:-}" = "Y" ]; then
      prompt="Y/n"
      default=Y
    elif [ "${2:-}" = "N" ]; then
      prompt="y/N"
      default=N
    else
      prompt="y/n"
      default=
    fi

    # Ask the question (not using "read -p" as it uses stderr not stdout)
    echo -n "$1 [$prompt] "

    # Read the answer (use /dev/tty in case stdin is redirected from somewhere else)
    read REPLY </dev/tty

    # Default?
    if [ -z "$REPLY" ]; then
      REPLY=$default
    fi

    # Check if the reply is valid
    case "$REPLY" in
      Y*|y*) return 0 ;;
      N*|n*) return 1 ;;
    esac

  done
}
