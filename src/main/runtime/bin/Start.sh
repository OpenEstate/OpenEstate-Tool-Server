#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# startup script for the ImmoTool server
# Copyright (C) 2009-2017 OpenEstate.org
# ----------------------------------------------------------------------------

# Use a specific command to launch the Java Runtime Environment
#JAVACMD=

# Path to the Java Runtime Environment, if $JAVACMD is undefined
#JAVA_HOME=

# Memory settings of the Java Runtime Environment
JAVA_HEAP_MINIMUM=32m
JAVA_HEAP_MAXIMUM=512m

# Additional options for the Java Runtime Environment
JAVA_OPTS="-Dfile.encoding=UTF-8"


#
# Start execution...
#

SCRIPT="$( basename "${BASH_SOURCE[0]}" )"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"

# OS specific initialization.
SYSTEM="$( uname -s )"
case "$SYSTEM" in

  Darwin)
    echo "Initializing macOS environment..."
    JAVA_OPTS="$JAVA_OPTS -Dopenestate.server.systemTray=true"
    JAVA_OPTS="$JAVA_OPTS -Dapple.awt.UIElement=true"

    # Look for JRE at the default installation location.
    if [ -z "$JAVA_HOME" ] ; then
      JRE_PATH="/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home"
      if [ -d "$JRE_PATH" ]; then
        JAVA_HOME="$JRE_PATH"
      fi
    fi

    # Look for a usable JDK installation.
    if [ -z "$JAVA_HOME" ] ; then
      JDK_PATH="$( /usr/libexec/java_home -v '1.7*' )"
      if [ -d "$JDK_PATH" ]; then
        JAVA_HOME="$JDK_PATH"
      fi
    fi
    ;;

  Linux)
    echo "Initializing Linux environment..."

    # Look for JRE on Gentoo systems.
    if [ -z "$JAVA_HOME" ] ; then
      if [ -r /etc/gentoo-release ] ; then
        JAVA_HOME="$( java-config --jre-home )"
      fi
    fi

    # Search for default java command, if environment variables are not set.
    if [ -z "$JAVA_HOME" ] && [ -z "$JAVACMD" ] ; then
      JAVACMD="$( which java )"
    fi
    ;;

  *)
    echo "Initializing unknown environment ($SYSTEM)..."

    # Search for default java command, if environment variables are not set.
    if [ -z "$JAVA_HOME" ] && [ -z "$JAVACMD" ] ; then
      JAVACMD="$( which java )"
    fi
    ;;
esac

# Use java command from JAVA_HOME, if it is not explicitly specified.
if [ -z "$JAVACMD" ] && [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
fi

# Test for an executable java command.
if [ ! -x "$JAVACMD" ] ; then
  echo "ERROR!"
  echo "Can't find Java executable at: $JAVACMD"
  echo "Please make sure, that Java is properly installed and that JAVA_HOME or JAVACMD environment variable is properly set."
  exit 1
fi

# Launch application.
cd "$BASE_DIR"
exec "$JAVACMD" \
  -Xms$JAVA_HEAP_MINIMUM \
  -Xmx$JAVA_HEAP_MAXIMUM \
  -classpath "./etc:./lib/*" \
  $JAVA_OPTS \
  org.openestate.tool.server.Server "$@"
