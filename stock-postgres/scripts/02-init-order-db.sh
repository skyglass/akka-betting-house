#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER $ORDER_DB_USER WITH PASSWORD '$ORDER_DB_PASSWORD';
    CREATE DATABASE $ORDER_DB_NAME;
    GRANT ALL PRIVILEGES ON DATABASE $ORDER_DB_NAME TO $ORDER_DB_USER;
    ALTER DATABASE $ORDER_DB_NAME SET TIMEZONE='Europe/Amsterdam';
    \c $ORDER_DB_NAME
    ALTER SCHEMA public OWNER TO $ORDER_DB_USER;
    GRANT ALL PRIVILEGES ON SCHEMA public TO $ORDER_DB_USER;
EOSQL
