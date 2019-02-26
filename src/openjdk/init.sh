#!/usr/bin/env bash
#
# Detect operating system and select the appropriate OpenJDK bundle.
# Copyright 2009-2019 OpenEstate.org
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

# -----------------------------------------------------------------------
#
# OpenJDK binaries are provided by:
# https://adoptopenjdk.net/
# https://www.bell-sw.com/java.html
#
# -----------------------------------------------------------------------

LINUX_X86_JDK="https://github.com/OpenIndex/openjdk-linux-x86/releases/download/jdk-11.0.2%2B9/jdk-11.0.2+9-linux-x86.tar.gz"
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
        i386 | i586 | i686)
          echo "Initializing Linux x86 environment..."
          SYSTEM_JDK="$LINUX_X86_JDK"
          ;;
        x86_64)
          echo "Initializing Linux x86_64 environment..."
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
