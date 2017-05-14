#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# Launch the ImmoTool server from macOS application bundle.
# Copyright (C) 2009-2017 OpenEstate.org
# ----------------------------------------------------------------------------

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$( cd "$SCRIPT_DIR/../../.." && pwd )"
LAUNCHER="$BASE_DIR/bin/Start.sh"
echo "launching $LAUNCHER"
exec "$LAUNCHER"
