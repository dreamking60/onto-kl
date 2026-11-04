#!/usr/bin/env bash
# Starts the Spring Boot REST server (builds the jar first when necessary).
#
#   ./scripts/run-server.sh                # default profile from config/application.properties
#   ./scripts/run-server.sh --server.port=9090
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$ROOT_DIR/target/ontology-reasoning-server.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Building $JAR ..."
  "$ROOT_DIR/scripts/build.sh" -q -DskipTests package
fi

exec java -jar "$JAR" "$@"
