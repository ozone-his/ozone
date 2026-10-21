#!/usr/bin/env bash

# Apply the init scripts found in docker-entrypoint-initdb.d/db/ directory.
#
# Similar workaround as for the PostgreSQL Docker image not running files from sub-dirs.
# https://github.com/docker-library/postgres/issues/605#issuecomment-567236795
#
# DB_INIT_LIST restricts which database directories are initialized: a
# space- or comma-separated list of directory names, as produced by
# ozonectl from the compose model's `db-init` selection. When DB_INIT_LIST
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
            find "$db_dir" -type f | sort | while read -r f; do
                echo "Processing: $f"
                case "$f" in
                    *.sh)
                        bash "$f"
                        ;;
                    *.sql)
                        db_name=$(basename "$(dirname "$f")")
                        mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "$db_name" < "$f"
                        ;;
                    *.sql.gz)
                        db_name=$(basename "$(dirname "$f")")
                        gunzip -c "$f" | mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "$db_name"
                        ;;
                    *)
                        echo "Ignoring unsupported file type: $f"
                        ;;
                esac
            done
        else
            echo "Skipping: $db_name (not in DB_INIT_LIST)"
        fi
    done
else
    echo "Directory '${directory}' does not exist. No database to create. Exit 0."
fi
exit 0
