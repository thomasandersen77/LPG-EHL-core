#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 ./kotlin-scripts/run-baseline-snapshot.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run.sh" baseline-snapshot

