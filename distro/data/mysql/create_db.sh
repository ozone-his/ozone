#!/usr/bin/env bash

# Apply all scripts found in docker-entrypoint-initdb.d/db/ directory.
#
# Similar workaround as for the PostgreSQL Docker image not running files from sub-dirs.
# https://github.com/docker-library/postgres/issues/605#issuecomment-567236795
#
#
set -e
directory=/docker-entrypoint-initdb.d/db/
if [ -d "${directory}" ]
then
    find ${directory}* -type f | sort | while read -r f; do
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
    echo "Directory '${directory}' does not exist. No database to create. Exit 0."
fi
exit 0
