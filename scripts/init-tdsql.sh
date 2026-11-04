#!/usr/bin/env bash
# Creates the relational A-Box schema and demo data in TDSQL / MySQL.
#
# Required environment variables (defaults match config/application.properties):
#   TDSQL_HOST      (127.0.0.1)
#   TDSQL_PORT      (3306)
#   TDSQL_USER      (ontology)
#   TDSQL_PASSWORD  (ontology123)
#   TDSQL_DATABASE  (ontology_db)
#
# The database itself must already exist (see sql/create-database.sql for a
# plain MySQL server, or create it from the TDSQL console).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

HOST="${TDSQL_HOST:-127.0.0.1}"
PORT="${TDSQL_PORT:-3306}"
USER="${TDSQL_USER:-ontology}"
PASSWORD="${TDSQL_PASSWORD:-ontology123}"
DATABASE="${TDSQL_DATABASE:-ontology_db}"

run_sql() {
  MYSQL_PWD="$PASSWORD" mysql --protocol=TCP -h "$HOST" -P "$PORT" -u "$USER" "$DATABASE" < "$1"
}

echo "Loading schema into ${USER}@${HOST}:${PORT}/${DATABASE}"
run_sql "$ROOT_DIR/sql/schema.sql"
echo "Loading demo data"
run_sql "$ROOT_DIR/sql/seed.sql"
echo "Done."
