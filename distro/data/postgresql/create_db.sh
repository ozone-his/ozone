#!/usr/bin/env bash

# Apply all scripts found in docker-entrypoint-initdb.d/db/ directory.
#
# Workaround for PostgreSQL Docker image not running files from sub-dirs.
# https://github.com/docker-library/postgres/issues/605#issuecomment-567236795
#
# Only execute shell scripts (*.sh) found in the subdirectories so that
# SQL files are not interpreted by the shell (they should be applied via
# psql from the shell scripts that know how to apply them).
#
set -e
directory=/docker-entrypoint-initdb.d/db/
if [ -d "${directory}" ]
then
    # Find and execute only shell scripts. execdir runs the command from the
    # directory where the matched file is located, which preserves relative
    # paths used by the scripts.
    find "${directory}" -type f -name '*.sh' -execdir ls {} \; -execdir bash {} \;
else
    echo "Directory '${directory}' does not exist. No database to create. Exit 0."
fi
exit 0
