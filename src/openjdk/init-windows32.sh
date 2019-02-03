#!/usr/bin/env bash
#
# Copyright (C) 2009-2019 OpenEstate.org
#

# -----------------------------------------------------------------------
#
# Build a runtime environment for Windows 32-bit x86
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

TARGET="windows32"
TARGET_JDK="$WINDOWS32_JDK"


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
unzip -q "$DOWNLOADS_DIR/$(basename "$TARGET_JDK")"
find . -type f -exec chmod ugo-x {} \;
mv "$(ls -1)/jmods" "$DIR/jmods/$TARGET"


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
JLINK="$SYSTEM_JDK_DIR/$(ls -1)/bin/jlink"


#
# build OpenJDK runtime
#

rm -Rf "$DIR/runtime/$TARGET"
mkdir -p "$DIR/runtime"

echo "Building runtime environment for $TARGET..."

# ZIP compression seems to produce errors in Windows x86
#
# see https://github.com/AdoptOpenJDK/openjdk-build/issues/763
# see https://bugs.openjdk.java.net/browse/JDK-8215123
#
# Therefore we're using --compress=1 instead of --compress=2

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
