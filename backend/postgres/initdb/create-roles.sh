#!/usr/bin/env bash
# Runs once, when PostgreSQL first starts on an empty data directory, as the
# container's superuser. It creates AmpolaTech's two roles (ADR 0003):
#
# - ampolatech_owner owns the database, and so its public schema. It is the
#   role migrations run as, so the tables they create belong to it.
# - ampolatech_app is the role the app connects as. It owns nothing, not even a
#   temporary table, can't bypass row-level security, and gets data access
#   table by table, granted by the migration that creates each table.
#
# The official postgres image runs it from /docker-entrypoint-initdb.d. Its two
# passwords come from the container's environment, AMPOLATECH_OWNER_PASSWORD
# and AMPOLATECH_APP_PASSWORD.
set -euo pipefail

psql --no-psqlrc --set ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set owner_password="$AMPOLATECH_OWNER_PASSWORD" \
  --set app_password="$AMPOLATECH_APP_PASSWORD" \
  --set database="$POSTGRES_DB" <<'SQL'
CREATE ROLE ampolatech_owner LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
  NOREPLICATION NOBYPASSRLS PASSWORD :'owner_password';
CREATE ROLE ampolatech_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
  NOREPLICATION NOBYPASSRLS PASSWORD :'app_password';
ALTER DATABASE :"database" OWNER TO ampolatech_owner;
REVOKE TEMPORARY ON DATABASE :"database" FROM PUBLIC;
SQL
