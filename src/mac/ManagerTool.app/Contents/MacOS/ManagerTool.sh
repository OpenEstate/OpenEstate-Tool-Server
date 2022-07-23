#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
# start a graphical tool for database management
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

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESOURCES_DIR="$( cd "$( dirname "$DIR" )/Resources" && pwd )"
LAUNCHER="$RESOURCES_DIR/bin/ManagerTool.sh"

# load configuration files outside of the application bundle
export SERVER_ETC_DIR="$HOME/OpenEstate-Files/etc"

exec "$LAUNCHER" "$@"
