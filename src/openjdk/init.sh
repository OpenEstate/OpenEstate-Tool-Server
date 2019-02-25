#!/usr/bin/env bash
#
# Copyright (C) 2009-2019 OpenEstate.org
#

# -----------------------------------------------------------------------
#
# Detect operating system and architecture and select the appropriate
# OpenJDK bundle.
#
# OpenJDK is taken from
# https://adoptopenjdk.net/
# https://www.bell-sw.com/java.html
#
# -----------------------------------------------------------------------

LINUX_X86_64_JDK="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz"
LINUX_ARM32_JDK="https://github.com/bell-sw/Liberica/releases/download/11.0.2/bellsoft-jdk11.0.2-linux-arm32-vfp-hflt.tar.gz"
MAC64_JDK="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x64_mac_hotspot_11.0.2_9.tar.gz"
WINDOWS32_JDK="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x86-32_windows_hotspot_11.0.2_9.zip"
WINDOWS64_JDK="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x64_windows_hotspot_11.0.2_9.zip"

MODULES="java.se"

SYSTEM="$( uname -s )"
SYSTEM_ARCH="$( arch )"
case "$SYSTEM" in

  Darwin)
    echo "Initializing macOS environment..."
    SYSTEM_JDK="$MAC64_JDK"
    ;;

  Linux)
    case "$SYSTEM_ARCH" in
        x86_64)
          echo "Initializing Linux 64bit environment..."
          SYSTEM_JDK="$LINUX_X86_64_JDK"
          ;;
        *)
          echo "Unsupported Linux environment ($SYSTEM_ARCH)..."
          exit 1
          ;;
    esac
    ;;

  *)
    echo "Unsupported environment ($SYSTEM)..."
    exit 1
    ;;

esac
