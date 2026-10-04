export db_name=$HCW_DB_NAME
export db_username=$HCW_DB_USER
export db_password=$HCW_DB_PASSWORD

echo "Creating '$db_username' user and '$db_name' database..."

createuser ${db_username}
createdb ${db_name}

psql -d ${db_name} -c "alter user ${db_username} with password '${db_password}';"
psql -d ${db_name} -c "grant all privileges on database ${db_name} to ${db_username};"
