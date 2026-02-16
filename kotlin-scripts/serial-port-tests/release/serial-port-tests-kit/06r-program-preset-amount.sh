#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
#   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_AMOUNT_5DIGITS=00100 \
#   ./kotlin-scripts/run-program-preset-amount.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run.sh" program-preset-amount

