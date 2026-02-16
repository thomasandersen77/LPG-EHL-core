#!/usr/bin/env bash
set -euo pipefail

# Unified runner for both:
# - in-repo development (uses ../../mvnw exec:java)
# - release kit on destination device (uses java -cp app.jar:lib/* ...)

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CMD="${1:-help}"
shift || true

APP_JAR="${SCRIPT_DIR}/app.jar"
LIB_DIR="${SCRIPT_DIR}/lib"

if [[ -f "${APP_JAR}" ]]; then
  exec java -cp "${APP_JAR}:${LIB_DIR}/*" no.cloudberries.lpg.scripts.SerialPortTestsMainKt "${CMD}" "$@"
fi

# Dev fallback (repo checkout): call mvnw from repo root.
ROOT_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
ARGS=( "${CMD}" )
if [[ $# -gt 0 ]]; then
  ARGS+=( "$@" )
fi
exec "${ROOT_DIR}/mvnw" -pl kotlin-scripts/serial-port-tests -Dexec.mainClass=no.cloudberries.lpg.scripts.SerialPortTestsMainKt exec:java -Dexec.args="${ARGS[*]}"

