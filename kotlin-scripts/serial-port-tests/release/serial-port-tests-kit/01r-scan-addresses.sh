#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./kotlin-scripts/run-scan-addresses.sh
#   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR_RANGE=1-64 ./kotlin-scripts/run-scan-addresses.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run.sh" scan-addresses

