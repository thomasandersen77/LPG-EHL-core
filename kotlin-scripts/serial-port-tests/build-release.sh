#!/usr/bin/env bash
set -euo pipefail

# Build a self-contained “testing kit” folder for SFTP transfer.
#
# Output:
#   kotlin-scripts/serial-port-tests/release/serial-port-tests-kit/
#     README.md
#     run.sh
#     00r-*.sh ... 07r-*.sh
#     app.jar
#     lib/*.jar
#
# Run:
#   ./kotlin-scripts/serial-port-tests/build-release.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

KIT_DIR="${SCRIPT_DIR}/release/serial-port-tests-kit"

rm -rf "${KIT_DIR}"
mkdir -p "${KIT_DIR}/lib"

echo "Building module (and dependencies) ..."
"${ROOT_DIR}/mvnw" -pl kotlin-scripts/serial-port-tests -am -DskipTests package

echo "Copying app jar ..."
APP_JAR="$(ls -1 "${SCRIPT_DIR}/target/"serial-port-tests-*.jar | head -n 1)"
cp -f "${APP_JAR}" "${KIT_DIR}/app.jar"

echo "Copying runtime dependencies ..."
"${ROOT_DIR}/mvnw" -pl kotlin-scripts/serial-port-tests -DskipTests \
  dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="${KIT_DIR}/lib"

echo "Copying scripts + README ..."
cp -f "${SCRIPT_DIR}/README.md" "${KIT_DIR}/README.md"
cp -f "${SCRIPT_DIR}/run.sh" "${KIT_DIR}/run.sh"
cp -f "${SCRIPT_DIR}/"[0-9][0-9]r-*.sh "${KIT_DIR}/"

chmod +x "${KIT_DIR}/run.sh" "${KIT_DIR}/"[0-9][0-9]r-*.sh

echo
echo "✅ Release kit ready:"
echo "  ${KIT_DIR}"
echo
echo "Suggested SFTP payload:"
echo "  Upload the whole 'serial-port-tests-kit' directory."

