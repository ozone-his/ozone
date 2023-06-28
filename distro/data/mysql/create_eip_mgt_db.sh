#!/bin/bash

set -eu

function create_user_and_database() {
mysql --password=$MYSQL_ROOT_PASSWORD --user=root <<MYSQL_SCRIPT
    CREATE DATABASE $1;
    CREATE USER '$2'@'localhost' IDENTIFIED BY '$3';
    CREATE USER '$2'@'%' IDENTIFIED BY '$3';
    GRANT ALL PRIVILEGES ON $1.* TO '$2'@'localhost';
    GRANT ALL PRIVILEGES ON $1.* TO '$2'@'%';
    FLUSH PRIVILEGES;
MYSQL_SCRIPT
}

create_user_and_database ${EIP_DB_NAME} ${EIP_DB_USER} ${EIP_DB_PASSWORD}