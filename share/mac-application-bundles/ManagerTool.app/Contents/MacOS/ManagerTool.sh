#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# Launch the HSQLDB-Manager from macOS application bundle.
# Copyright (C) 2009-2019 OpenEstate.org
# ----------------------------------------------------------------------------

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$( cd "$SCRIPT_DIR/../../.." && pwd )"
LAUNCHER="$BASE_DIR/bin/ManagerTool.sh"
echo "launching $LAUNCHER"
exec "$LAUNCHER"
