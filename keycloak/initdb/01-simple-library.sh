#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE ROLE simple_library LOGIN PASSWORD '${SIMPLE_LIBRARY_DB_PASSWORD}';
CREATE DATABASE simple_library OWNER simple_library;
REVOKE CONNECT ON DATABASE simple_library FROM PUBLIC;
REVOKE CONNECT ON DATABASE keycloak FROM PUBLIC;
GRANT CONNECT ON DATABASE keycloak TO keycloak;
SQL
