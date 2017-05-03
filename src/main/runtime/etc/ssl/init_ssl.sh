#!/bin/sh
# ----------------------------------------------------------------------------
# OpenEstate-ImmoServer
# $Id: init_ssl.sh 1438 2012-03-16 17:13:37Z andy $
# Create SSL key pair and certificate for the server
# ----------------------------------------------------------------------------
set -e

# path to the keytool binary, that is provided by java
KEYTOOL=keytool

# number of days, for how long the created certificate will be valid
VALIDITY=999

# path to the created server keystore
SERVER_KEYSTORE=OpenEstate-ImmoServer.jks

# path to the created server certificate
SERVER_CERTIFICATE=OpenEstate-ImmoServer.crt

#
# start execution
#

#alias cls='printf "\033c"'
alias cls='clear'

if [ -z "$BASEDIR" ] ; then
  BASEDIR=`dirname $0`
  BASEDIR=`(cd "$BASEDIR"; pwd)`
fi
SERVER_KEYSTORE="$BASEDIR/$SERVER_KEYSTORE"
SERVER_CERTIFICATE="$BASEDIR/$SERVER_CERTIFICATE"

if [ -f "$SERVER_KEYSTORE" -o -f "$SERVER_CERTIFICATE" ] ; then
  cls
  echo ''
  echo '############################################'
  echo '# Warning: The SSL files already exist!    #'
  echo '############################################'
  echo ''
  read -p "Do you want to delete the SSL files and continue (y/n)?" choice
  case "$choice" in
    y|Y|j|J ) rm -f "$SERVER_KEYSTORE" "$SERVER_CERTIFICATE" ;;
    * ) exit ;;
  esac
fi

cls
echo ''
echo '############################################'
echo '# Step 1: Create private / public key pair #'
echo '############################################'
echo ''
"$KEYTOOL" -genkey \
 -alias OpenEstate-ImmoServer \
 -keyalg RSA \
 -validity $VALIDITY \
 -keystore "$SERVER_KEYSTORE" \
 -storetype JKS
echo ''

cls
echo ''
echo '############################################'
echo '# Step 2: Export certificate from key pair #'
echo '############################################'
echo ''
"$KEYTOOL" -export \
 -alias OpenEstate-ImmoServer \
 -keystore "$SERVER_KEYSTORE" \
 -rfc \
 -file "$SERVER_CERTIFICATE"
echo ''

cls
echo ''
echo '############################################'
echo '# SSL files were successfully created      #'
echo '############################################'
echo ''
echo 'Stored server keystore at:'
echo $SERVER_KEYSTORE
echo ''
echo 'Stored server certificate at:'
echo $SERVER_CERTIFICATE
echo ''
