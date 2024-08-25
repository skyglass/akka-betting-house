#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "postgres" <<-EOSQL
    CREATE USER betting WITH PASSWORD 'dbpass';
    CREATE DATABASE betting;
    GRANT ALL PRIVILEGES ON DATABASE betting TO betting;
    ALTER DATABASE betting SET TIMEZONE='Europe/Amsterdam';
    \c betting
    ALTER SCHEMA public OWNER TO betting;
    GRANT ALL PRIVILEGES ON SCHEMA public TO betting;
EOSQL