#!/usr/bin/env bash
#
# Copyright (C) 2009-2019 OpenEstate.org
#

# -----------------------------------------------------------------------
#
# Build a runtime environment for macOS 64-bit.
#
# OpenJDK for this target platform is taken from
# https://adoptopenjdk.net/
#
# -----------------------------------------------------------------------

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DOWNLOADS_DIR="$DIR/downloads"
LOCAL_DIR="$DIR/local"
TEMP_DIR="$DIR/temp"


#
# initialization
#

set -e
source "$DIR/init.sh"
rm -Rf "$DIR/jmods/$TARGET"
mkdir -p "$DIR/jmods"
mkdir -p "$LOCAL_DIR"

TARGET="mac64"
TARGET_JDK="$MAC64_JDK"


#
# download OpenJDK binaries
#

mkdir -p "$DOWNLOADS_DIR"
cd "$DOWNLOADS_DIR"

if [[ ! -f "$DOWNLOADS_DIR/$(basename ${TARGET_JDK})" ]]; then
    echo "Downloading OpenJDK for $TARGET..."
    #wget -nc "$TARGET_JDK"
    curl -L \
      -o "$(basename ${TARGET_JDK})" \
      "$TARGET_JDK"
fi

if [[ ! -f "$DOWNLOADS_DIR/$(basename ${SYSTEM_JDK})" ]]; then
    echo "Downloading OpenJDK for jlink..."
    #wget -nc "$SYSTEM_JDK"
    curl -L \
      -o "$(basename ${SYSTEM_JDK})" \
      "$SYSTEM_JDK"
fi


#
# extract OpenJDK modules
#

echo "Extracting OpenJDK modules for $TARGET..."
rm -Rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"
tar xfz "$DOWNLOADS_DIR/$(basename "$TARGET_JDK")"
find "$(ls -1)" -type f -name "._*" -exec rm {} \;
if [[ -d "$(ls -1)/Contents/Home" ]]; then
    mv "$(ls -1)/Contents/Home/jmods" "$DIR/jmods/mac64"
else
    mv "$(ls -1)/jmods" "$DIR/jmods/mac64"
fi


#
# extract OpenJDK for jlink
#

echo "Extracting OpenJDK for jlink..."
SYSTEM_JDK_DIR="$LOCAL_DIR/$(basename "$SYSTEM_JDK")"
if [[ ! -d "$SYSTEM_JDK_DIR" ]]; then
    mkdir -p "$SYSTEM_JDK_DIR"
    cd "$SYSTEM_JDK_DIR"
    tar xfz "$DOWNLOADS_DIR/$(basename "$SYSTEM_JDK")"
    find "$(ls -1)" -type f -name "._*" -exec rm {} \;
fi
cd "$SYSTEM_JDK_DIR"
if [[ -d "$SYSTEM_JDK_DIR/$(ls -1)/Contents/Home" ]]; then
    JLINK="$SYSTEM_JDK_DIR/$(ls -1)/Contents/Home/bin/jlink"
else
    JLINK="$SYSTEM_JDK_DIR/$(ls -1)/bin/jlink"
fi


#
# build OpenJDK runtime
#

rm -Rf "$DIR/runtime/$TARGET"
mkdir -p "$DIR/runtime"

echo "Building runtime environment for $TARGET..."
"$JLINK" \
    -p "$DIR/jmods/$TARGET" \
    --add-modules "$MODULES" \
    --output "$DIR/runtime/$TARGET" \
    --compress=1 \
    --strip-debug \
    --no-header-files \
    --no-man-pages


#
# cleanup
#

cd "$DIR"
rm -Rf "$TEMP_DIR"
