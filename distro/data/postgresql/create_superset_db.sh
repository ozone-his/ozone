export db_name=$SUPERSET_DB
export db_username=$SUPERSET_DB_USER
export db_password=$SUPERSET_DB_PASSWORD

createuser ${db_username}
createdb ${db_name}

psql -d ${db_name} -c "alter user ${db_username} with password '${db_password}';"
psql -d ${db_name} -c "grant all privileges on database ${db_name} to ${db_username};"
