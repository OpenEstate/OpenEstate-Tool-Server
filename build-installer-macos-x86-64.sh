#!/usr/bin/env bash
#
# Copyright (C) 2009-2019 OpenEstate.org
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
# NOTICE: This script has to be executed on a macOS system with the
# required certificate available. In order to sign the application for
# yourself, you need to obtain a Developer ID from Apple and set the
# KEY variable accordingly.
# ----------------------------------------------------------------------------

KEY="Developer ID Application: Andreas Rudolph (H48THMS543)"
DIR=$( cd $( dirname ${BASH_SOURCE[0]} ) && pwd )
PACKAGE_DIR="$DIR/package"
TARGET_DIR="$DIR/target"
TEMP_DIR="$TARGET_DIR/macos-x86-64-build"
SRC_DIR="$DIR/src/dmgbuild"
FOUND="0"
set -e

export LANG="en_US.UTF-8"

for f in ${PACKAGE_DIR}/*.macos-x86-64.tar.gz; do

    if [[ "$FOUND" == "0" ]]; then
        echo ""
        echo "----------------------------------------------------------------"
        echo "Unlocking keychain..."
        echo "----------------------------------------------------------------"
        echo ""
        security unlock-keychain
    fi

    FOUND="1"
    pkgName=$(basename ${f%.macos-x86-64.tar.gz})
    appName=$(echo "$pkgName" | rev | cut -d'-' -f3- | rev)
    dmgName="${pkgName}.macos-x86-64.dmg"
    pkg="$appName.app"

    echo ""
    echo "----------------------------------------------------------------"
    echo "Signing $pkg..."
    echo "----------------------------------------------------------------"
    echo ""
    rm -Rf "$TEMP_DIR"
    mkdir -p "$TEMP_DIR"
    tar xfz "$f" -C "$TEMP_DIR"
    codesign --deep -s "$KEY" "$TEMP_DIR/$pkg"
    echo "Verifying signature:"
    codesign -d --verbose=4 "$TEMP_DIR/$pkg"
    echo ""
    echo "Verifying access for Gatekeeper:"
    spctl --assess --verbose=4 --type execute "$TEMP_DIR/$pkg"

    echo ""
    echo "----------------------------------------------------------------"
    echo "Creating $dmgName..."
    echo "----------------------------------------------------------------"
    echo ""
    rm -f "$PACKAGE_DIR/$dmgName"
    dmgbuild \
        -s "$SRC_DIR/settings.py" \
        -D app="$TEMP_DIR/$pkg" \
        "$appName Installer" \
        "$PACKAGE_DIR/$dmgName"

    echo ""
    echo "----------------------------------------------------------------"
    echo "Signing $dmgName..."
    echo "----------------------------------------------------------------"
    echo ""
    codesign --force --sign "$KEY" "$PACKAGE_DIR/$dmgName"
    echo "Verifying signature:"
    codesign -d --verbose=4 "$PACKAGE_DIR/$dmgName"
    echo ""
    echo "Verifying access for Gatekeeper:"
    spctl -a -t open --context context:primary-signature -v "$PACKAGE_DIR/$dmgName"

done

if [[ "$FOUND" == "0" ]]; then
    echo "ERROR: No macOS packages were found at $DIR"
fi
