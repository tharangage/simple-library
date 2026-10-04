#!/bin/bash
set -e

# Perform all actions using the default administrative user/database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create User 1 and their Database
    CREATE USER simple_library WITH PASSWORD simple_library;
    CREATE DATABASE simple_library;
    GRANT ALL PRIVILEGES ON DATABASE simple_library TO simple_library;

    -- Create User 2 and their Database
    CREATE USER 'keycloak' WITH PASSWORD 'keycloak';
    CREATE DATABASE 'keycloak';
    GRANT ALL PRIVILEGES ON DATABASE 'keycloak' TO 'keycloak';
EOSQL
