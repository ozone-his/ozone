export db_name=$KEYCLOAK_DB
export db_schema=$KEYCLOAK_DB_SCHEMA
export db_username=$KEYCLOAK_DB_USER
export db_password=$KEYCLOAK_DB_PASSWORD

createuser ${db_username}
createdb ${db_name}

psql -d ${db_name} -c "alter user ${db_username} with password '${db_password}';"
psql -d ${db_name} -c "grant all privileges on database ${db_name} to ${db_username};"
psql -d ${db_name} -c "create schema ${db_schema};"
psql -d ${db_name} -c "alter schema ${db_schema} owner to ${db_username};"
