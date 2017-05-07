#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# unlock application bundles in macOS Gatekeeper
# Copyright (C) 2009-2017 OpenEstate.org
# ----------------------------------------------------------------------------

LABEL="OpenEstate.org"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"

SYSTEM="$( uname -s )"
if [ "$SYSTEM" != "Darwin" ] ; then
  echo "ERROR!"
  echo "You are not running a macOS operating system."
  exit 1
fi

echo ""
echo "--------------------------------------------------------------"
echo " Register application bundles..."
echo "--------------------------------------------------------------"
echo ""
sudo spctl --remove -v --path "$BASE_DIR/Start.app"
set -e
sudo spctl --add -v --label "$LABEL" "$BASE_DIR/Start.app"
sudo spctl --enable -v --label "$LABEL"

echo ""
echo "--------------------------------------------------------------"
echo " Remove quarantine status..."
echo "--------------------------------------------------------------"
echo ""
xattr -d -r com.apple.quarantine "$BASE_DIR/Start.app"

echo ""
echo "--------------------------------------------------------------"
echo " Testing Gatekeeper permissions..."
echo "--------------------------------------------------------------"
echo ""
sudo spctl -v -a "$BASE_DIR/Start.app"
echo ""
