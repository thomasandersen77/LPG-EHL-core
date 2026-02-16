#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Clear entrypoint for starting ONLY the payment terminal simulator.
# Help MUST NOT start anything.
if [[ "${1:-}" == "help" ]]; then
  exec "$SCRIPT_DIR/sim-terminal.sh" --help
fi

exec "$SCRIPT_DIR/sim-terminal.sh" "$@"
