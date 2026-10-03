#!/bin/bash
set -e

# Perform all actions using the default administrative user/database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create User 1 and their Database
    CREATE USER 'simple-library' WITH PASSWORD 'simple-library';
    CREATE DATABASE 'simple-library';
    GRANT ALL PRIVILEGES ON DATABASE 'simple-library' TO 'simple-library';

    -- Create User 2 and their Database
    CREATE USER 'keycloak' WITH PASSWORD 'keycloak';
    CREATE DATABASE 'keycloak';
    GRANT ALL PRIVILEGES ON DATABASE 'keycloak' TO 'keycloak';
EOSQL
