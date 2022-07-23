#!/usr/bin/env bash
#
# Copyright (C) 2009-2022 OpenEstate.org
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

VERSION="1.1.0"
OPTIONS=""
TARGET="windows-x86-64"
WINE="wine"
WINEPATH="winepath"
ISCC="$HOME/.wine/drive_c/Program Files/Inno Setup 5/ISCC.exe"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

set -e
cd "$DIR/package"

if [[ -n "$BUILD_VERSION" ]] ; then
    VERSION="$BUILD_VERSION"
fi

if [[ "$BUILD_QUIETLY" -eq "1" ]]; then
    OPTIONS="$OPTIONS /Q"
fi

if [[ "$VERSION" == *"-"* ]]; then
    VERSION_NUMBER="$(echo "$VERSION" | cut -d'-' -f 1)"
else
    VERSION_NUMBER="$VERSION"
fi

PACKAGE_DIR="$( "$WINEPATH" -w "." )"
PACKAGE_SRC_DIR="$( "$WINEPATH" -w "$TARGET/OpenEstate-ImmoServer" )"

exec "$WINE" "$ISCC" \
    "/DPackage=$PACKAGE_SRC_DIR" \
    "/DVersion=$VERSION" \
    "/DVersionNumber=$VERSION_NUMBER" \
    "/O$PACKAGE_DIR" \
    ${OPTIONS} \
    "../src/innosetup/$TARGET.iss"
