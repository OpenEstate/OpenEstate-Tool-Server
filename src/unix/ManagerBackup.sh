#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# ${project.baseName} ${project.version}
# backup databases of the currently running HSQLDB server
# Copyright (C) 2009-2019 OpenEstate.org
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

# load default settings
DEFAULTS="/etc/default/OpenEstate-ImmoServer"
if [[ -f "$DEFAULTS" ]] && [[ -r "$DEFAULTS" ]] ; then
    source "$DEFAULTS"
fi

# Use a specific command to launch the Java Runtime Environment
#JAVA_COMMAND=""

# Path to the Java Environment is used if $JAVA_COMMAND is undefined
#JAVA_HOME=""

# Memory settings of the Java Runtime Environment
JAVA_HEAP_MINIMUM="32m"
JAVA_HEAP_MAXIMUM="256m"

# Additional Java options for all operating systems
JAVA_OPTIONS="-Dfile.encoding=UTF-8"

# Additional Java options for Linux
JAVA_OPTIONS_LINUX=""

# Additional Java options for macOS
JAVA_OPTIONS_MAC=""

# Additional Java options for other systems
JAVA_OPTIONS_OTHER=""

# Path to the folder, where the server configuration files are stored.
#SERVER_ETC_DIR=""

# Path to the folder, where the server log files are stored.
#SERVER_LOG_DIR=""

# Path to the folder, where the server data files are stored.
#SERVER_VAR_DIR=""


#
# Start execution...
#

SCRIPT="$( basename "${BASH_SOURCE[0]}" )"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$( cd "$( dirname "$SCRIPT_DIR" )" && pwd )"

# Use integrated Java, if $JAVA_COMMAND is not explicitly configured
if [[ -d "$BASE_DIR/jre" ]] && [[ -z "$JAVA_COMMAND" ]] ; then
    JAVA_HOME="$BASE_DIR/jre"
fi

# OS specific initialization.
SYSTEM="$( uname -s )"
case "$SYSTEM" in
    Darwin)
        echo "Initializing macOS environment..."
        JAVA_OPTIONS="$JAVA_OPTIONS $JAVA_OPTIONS_MAC"

        # Look for a usable JDK installation.
        if [[ -z "$JAVA_HOME" ]] ; then
            JDK_PATH="$( /usr/libexec/java_home -v '11*' )"
            if [[ -d "$JDK_PATH" ]] ; then
                JAVA_HOME="$JDK_PATH"
            fi
        fi

        # Look for JRE at the default installation location.
        if [[ -z "$JAVA_HOME" ]] ; then
            JRE_PATH="/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home"
            if [[ -d "$JRE_PATH" ]] ; then
                JAVA_HOME="$JRE_PATH"
            fi
        fi
        ;;

    Linux)
        echo "Initializing Linux environment..."
        JAVA_OPTIONS="$JAVA_OPTIONS $JAVA_OPTIONS_LINUX"

        # Look for JRE on Gentoo systems.
        if [[ -z "$JAVA_HOME" ]] && [[ -r "/etc/gentoo-release" ]] ; then
            JAVA_HOME="$( java-config --jre-home )"
        fi

        # Search for default java command, if environment variables are not set.
        if [[ -z "$JAVA_HOME" ]] && [[ -z "$JAVA_COMMAND" ]] ; then
            JAVA_COMMAND="$( which java )"
        fi
    ;;

    *)
        echo "Initializing unknown environment ($SYSTEM)..."
        JAVA_OPTIONS="$JAVA_OPTIONS $JAVA_OPTIONS_OTHER"

        # Search for default java command, if environment variables are not set.
        if [[ -z "$JAVA_HOME" ]] && [[ -z "$JAVA_COMMAND" ]] ; then
            JAVA_COMMAND="$( which java )"
        fi
    ;;
esac

# Use java command from JAVA_HOME, if JAVA_COMMAND not explicitly specified.
if [[ -z "$JAVA_COMMAND" ]] && [[ -n "$JAVA_HOME" ]] ; then
    if [[ -x "$JAVA_HOME/jre/sh/java" ]] ; then
        # IBM's JDK on AIX uses strange locations for the executables.
        JAVA_COMMAND="$JAVA_HOME/jre/sh/java"
    else
        JAVA_COMMAND="$JAVA_HOME/bin/java"
    fi
fi

# Test for an executable java command.
if [[ ! -x "$JAVA_COMMAND" ]] ; then
    echo "ERROR!"
    echo "Can't find Java executable at: $JAVA_COMMAND"
    echo "Please make sure, that Java is properly installed and that JAVA_HOME or JAVA_COMMAND environment variable is properly set."
    exit 1
fi

# Set default path to the etc folder.
if [[ -z "$SERVER_ETC_DIR" ]] ; then
    SERVER_ETC_DIR="$BASE_DIR/etc"
fi

# Set default path to the log folder.
if [[ -z "$SERVER_LOG_DIR" ]] ; then
    SERVER_LOG_DIR="$HOME/${project.baseName}/log"
fi

# Set default path to the var folder.
if [[ -z "$SERVER_VAR_DIR" ]] ; then
    SERVER_VAR_DIR="$HOME/${project.baseName}"
fi

# Launch application.
cd "$BASE_DIR"
exec "$JAVA_COMMAND" \
    "-Xms$JAVA_HEAP_MINIMUM" \
    "-Xmx$JAVA_HEAP_MAXIMUM" \
    -classpath "./lib/*" \
    ${JAVA_OPTIONS} \
    -Dopenestate.server.app="manager-backup" \
    -Dopenestate.server.etcDir="$SERVER_ETC_DIR" \
    -Dopenestate.server.logDir="$SERVER_LOG_DIR" \
    -Dopenestate.server.varDir="$SERVER_VAR_DIR" \
    org.openestate.tool.server.manager.ManagerBackup "$@"
