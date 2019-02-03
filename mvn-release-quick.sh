#!/usr/bin/env bash
#
# Copyright (C) 2009-2019 OpenEstate.org
#

MVN="mvn"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# force an update of submodules
#"$DIR/git-submodule-update.sh"

set -e
export LANG=en
cd "$DIR"
"$MVN" -Popenestate-release -Dgpg.skip=true -Dmaven.javadoc.skip=true clean install
