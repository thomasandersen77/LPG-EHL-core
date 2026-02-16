#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Clear entrypoint for starting BOTH simulators at once.
# Help MUST NOT start anything.
if [[ "${1:-}" == "help" ]]; then
  exec "$SCRIPT_DIR/sim-all.sh" --help
fi

exec "$SCRIPT_DIR/sim-all.sh" "$@"
