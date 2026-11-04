#!/usr/bin/env bash
# Build wrapper.
#
#  * selects a JDK 11 if JAVA_HOME is not already set (macOS: /usr/libexec/java_home)
#  * uses config/maven-settings.xml (Aliyun mirror) when the user has no
#    ~/.m2/settings.xml, which makes downloads fast in mainland China
#
# Usage:
#   ./scripts/build.sh            # same as: mvn package
#   ./scripts/build.sh clean package
#   ./scripts/build.sh test
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  DETECTED="$(/usr/libexec/java_home -v 11 2>/dev/null || true)"
  if [[ -n "${DETECTED}" ]]; then
    export JAVA_HOME="${DETECTED}"
  fi
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME=${JAVA_HOME}"
else
  echo "JAVA_HOME is not set; make sure 'mvn -v' reports a Java 11 runtime." >&2
fi

MAVEN_ARGS=()
if [[ ! -f "${HOME}/.m2/settings.xml" && -f "${ROOT_DIR}/config/maven-settings.xml" ]]; then
  MAVEN_ARGS+=("-s" "${ROOT_DIR}/config/maven-settings.xml")
fi

cd "${ROOT_DIR}"
exec mvn "${MAVEN_ARGS[@]}" "$@"
