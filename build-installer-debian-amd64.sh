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

# ----------------------------------------------------------------------------
# NOTICE: This script has to be executed on a Linux system with the
# dpkg-deb build tool available.
# ----------------------------------------------------------------------------

VERSION="1.1.0"
ARCHITECTURE="amd64"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SRC_DIR="$DIR/package/debian-${ARCHITECTURE}/OpenEstate-ImmoServer"
set -e

if [[ -n "$BUILD_VERSION" ]] ; then
    VERSION="$BUILD_VERSION"
fi

if [[ -z "$BUILD_TIME" ]] ; then
    BUILD_TIME="$(date +%Y%m%d%H%M%S)"
fi

if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
    VERSION="$(echo "$VERSION" | cut -d'-' -f 1)+dev$BUILD_TIME"
fi

# Write version into control file.
sed -i -e "s|\${PackageVersion}|$VERSION|g" "$SRC_DIR/DEBIAN/control"

# Write architecture into control file.
sed -i -e "s|\${Architecture}|$ARCHITECTURE|g" "$SRC_DIR/DEBIAN/control"

# Write package size into control file.
ETC_SIZE=$(du -s -k "$SRC_DIR/etc" | cut -f1)
OPT_SIZE=$(du -s -k "$SRC_DIR/opt" | cut -f1)
TOTAL_SIZE=$(($ETC_SIZE + $OPT_SIZE))
#echo "$ETC_SIZE + $OPT_SIZE = $TOTAL_SIZE"
sed -i -e "s|\${InstalledSize}|$TOTAL_SIZE|g" "$SRC_DIR/DEBIAN/control"

# Calculate md5 checksums.
cd "$SRC_DIR"
rm -f "DEBIAN/md5sums"
touch "DEBIAN/md5sums"
find etc opt -type f -exec md5sum {} >> "DEBIAN/md5sums" \;

# Create Debian package.
cd "$DIR/package"
dpkg-deb \
    --root-owner-group \
    --build "debian-${ARCHITECTURE}/OpenEstate-ImmoServer" \
    "openestate-immoserver_${VERSION}_${ARCHITECTURE}.deb"
