#!/usr/bin/env bash
set -euo pipefail

# Wrapper kept for backwards compatibility.
# Real script lives in projects/lpg-ehl/.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/projects/lpg-ehl/build_monolith.sh" "$@"

