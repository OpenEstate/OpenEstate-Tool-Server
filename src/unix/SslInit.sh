#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer ${project.version}
# generate RSA key pair and certificate for SSL encryption
# Copyright (C) 2009-2019 OpenEstate.org
# ----------------------------------------------------------------------------

# Use a specific command to launch the Java Runtime Environment
#JAVA_COMMAND=""

# Path to the Java Environment is used if $JAVA_COMMAND is undefined
#JAVA_HOME=""

# Memory settings of the Java Runtime Environment
JAVA_HEAP_MINIMUM="32m"
JAVA_HEAP_MAXIMUM="128m"

# Additional Java options for all operating systems
JAVA_OPTIONS="-Dfile.encoding=UTF-8"

# Additional Java options for Linux
JAVA_OPTIONS_LINUX=""
#JAVA_OPTIONS_LINUX="-Dawt.useSystemAAFontSettings=gasp"

# Additional Java options for macOS
JAVA_OPTIONS_MAC=""
#JAVA_OPTIONS_MAC="-Dapple.laf.useScreenMenuBar=true -Xdock:name=$APP -Xdock:icon=./share/$APP.icns"

# Additional Java options for other systems
JAVA_OPTIONS_OTHER=""


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

# Launch application.
cd "$BASE_DIR"
exec "$JAVA_COMMAND" \
    "-Xms$JAVA_HEAP_MINIMUM" \
    "-Xmx$JAVA_HEAP_MAXIMUM" \
    -classpath "./etc:./lib/*" \
    ${JAVA_OPTIONS} \
    org.openestate.tool.server.utils.SslGenerator ${@}
