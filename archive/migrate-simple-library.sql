-- One-time migration for the already-running shared Postgres.
-- Run:  docker exec -i keycloak-postgres-1 psql -U keycloak -d postgres < keycloak/migrate-simple-library.sql
-- Set the password below first (must match SIMPLE_LIBRARY_DB_PASSWORD / SPRING_DATASOURCE_PASSWORD).

DROP DATABASE IF EXISTS "simple-library";
DROP DATABASE IF EXISTS simple_library;

SELECT 'CREATE ROLE simple_library LOGIN PASSWORD ''simple_library'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'simple_library')\gexec

CREATE DATABASE simple_library OWNER simple_library;

REVOKE CONNECT ON DATABASE simple_library FROM PUBLIC;
REVOKE CONNECT ON DATABASE keycloak FROM PUBLIC;
GRANT CONNECT ON DATABASE keycloak TO keycloak;
