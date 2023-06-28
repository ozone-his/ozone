#!/bin/bash

set -eu

function create_user() {
	local user=$1
	local password=$2
	echo "  Creating '$user' user..."
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" $POSTGRES_DB <<-EOSQL
	    CREATE USER $user WITH  PASSWORD '$password';
	    ALTER USER $user CREATEDB;
EOSQL
}

create_user ${ODOO_DB_USER} ${ODOO_DB_PASSWORD}
