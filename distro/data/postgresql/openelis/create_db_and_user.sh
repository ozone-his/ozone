#!/usr/bin/env bash
set -euo pipefail

echo "Setting up OpenELIS database, users and loading schema..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Using script dir: ${SCRIPT_DIR}"

export db_name=$OPENELIS_DB_NAME
export db_schema=$OPENELIS_DB_SCHEMA
export db_username=$OPENELIS_DB_USER
export db_password=$OPENELIS_DB_PASSWORD
export db_admin_password=$OPENELIS_DB_ADMIN_PASSWORD

createuser "${db_username}"
createdb "${db_name}"

psql -d "${db_name}" -c "alter user ${db_username} with password '${db_password}';"
psql -d "${db_name}" -c "grant all privileges on database ${db_name} to ${db_username};"
psql -d "${db_name}" -c "create schema ${db_schema};"
psql -d "${db_name}" -c "alter schema ${db_schema} owner to ${db_username};"

# Assign ownership of the database to the created user
psql -d "${db_name}" -c "ALTER DATABASE ${db_name} OWNER TO ${db_username};"
echo "Database '${db_name}' ownership assigned to user '${db_username}'."

echo "Database '${db_name}' and user '${db_username}' created."

# create a superuser for the database
psql -d "${db_name}" -c "create user admin with superuser;"
psql -d "${db_name}" -c "alter user admin with password '${db_admin_password}';"
echo "Superuser 'admin' created."

# Path to SQL files
OPENELIS_SQL="${SCRIPT_DIR}/OpenELIS-Global.sql"
SITEINFO_SQL="${SCRIPT_DIR}/siteInfo.sql"

# Ensure files exist
if [[ ! -f "${OPENELIS_SQL}" ]]; then
  echo "ERROR: ${OPENELIS_SQL} not found" >&2
  exit 1
fi
if [[ ! -f "${SITEINFO_SQL}" ]]; then
  echo "ERROR: ${SITEINFO_SQL} not found" >&2
  exit 1
fi

if [[ -f "${OPENELIS_SQL}" ]]; then
  awk '{
    orig=$0
    lo=tolower($0)
    if (lo ~ /^[[:space:]]*create[[:space:]]+schema[[:space:]]+clinlims[[:space:]]*;[[:space:]]*$/ && lo !~ /if[[:space:]]+not[[:space:]]+exists/) {
      if (orig !~ /^[[:space:]]*--/) {
        print "-- " orig
        next
      }
    }
    print
  }' "${OPENELIS_SQL}" > "${OPENELIS_SQL}.tmp"

  if ! cmp -s "${OPENELIS_SQL}" "${OPENELIS_SQL}.tmp"; then
    echo "Backing up ${OPENELIS_SQL} to ${OPENELIS_SQL}.bak"
    cp "${OPENELIS_SQL}" "${OPENELIS_SQL}.bak"
    mv "${OPENELIS_SQL}.tmp" "${OPENELIS_SQL}"
    echo "Commented out plain CREATE SCHEMA clinlims; in ${OPENELIS_SQL}"
  else
    rm "${OPENELIS_SQL}.tmp"
    echo "No plain 'CREATE SCHEMA clinlims;' found or already commented/has IF NOT EXISTS; no change."
  fi
fi

# Load schema/data into the newly created database
echo "Loading ${OPENELIS_SQL} into ${db_name}..."
psql -v ON_ERROR_STOP=1 --username admin --dbname "$db_name" -f "${OPENELIS_SQL}"

echo "Loading ${SITEINFO_SQL} into ${db_name}..."
psql -v ON_ERROR_STOP=1 --username admin --dbname "$db_name" -f "${SITEINFO_SQL}"

echo "SQL files imported successfully."
