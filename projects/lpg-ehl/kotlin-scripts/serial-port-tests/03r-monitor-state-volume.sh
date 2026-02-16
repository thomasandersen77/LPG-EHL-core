#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_MONITOR_SECONDS=30 ./kotlin-scripts/run-monitor-state-volume.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run.sh" monitor-state-volume

