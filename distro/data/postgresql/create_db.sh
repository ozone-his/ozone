#!/usr/bin/env bash

# Apply the init scripts found in docker-entrypoint-initdb.d/db/ directory.
#
# Workaround for PostgreSQL Docker image not running files from sub-dirs.
# https://github.com/docker-library/postgres/issues/605#issuecomment-567236795
#
# Only execute shell scripts (*.sh) found in the subdirectories so that
# SQL files are not interpreted by the shell (they should be applied via
# psql from the shell scripts that know how to apply them).
#
# DB_INIT_LIST restricts which database directories are initialized: a
# space- or comma-separated list of directory names, as produced by
# the Ozone CLI from the compose model's `db-init` selection. When DB_INIT_LIST
# is unset or empty, every directory is initialized.
#
set -e
directory=/docker-entrypoint-initdb.d/db/
init_list="${DB_INIT_LIST:-}"

should_init() {
    local db_name="$1"
    if [ -z "$init_list" ]; then
        return 0
    fi
    local item
    for item in $(echo "$init_list" | tr ',' ' '); do
        if [ "$item" = "$db_name" ]; then
            return 0
        fi
    done
    return 1
}

if [ -d "${directory}" ]
then
    for db_dir in "${directory}"*/; do
        [ -d "$db_dir" ] || continue
        db_name=$(basename "$db_dir")
        if should_init "$db_name"; then
            # Find and execute only shell scripts. execdir runs the command
            # from the directory where the matched file is located, which
            # preserves relative paths used by the scripts.
            find "$db_dir" -type f -name '*.sh' -execdir ls {} \; -execdir bash {} \;
        else
            echo "Skipping: $db_name (not in DB_INIT_LIST)"
        fi
    done
else
    echo "Directory '${directory}' does not exist. No database to create. Exit 0."
fi
exit 0
