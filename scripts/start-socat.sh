#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START SOCAT - Virtual Serial Port Pair
#═══════════════════════════════════════════════════════════════════════════════
#
# Creates a virtual serial port pair using socat:
#   /tmp/ttyV0 - For PLS Simulator
#   /tmp/ttyV1 - For Webapp
#
# Usage:
#   ./scripts/start-socat.sh         # Start in foreground
#   ./scripts/start-socat.sh &       # Start in background
#
# Prerequisites:
#   brew install socat   (macOS)
#   apt install socat    (Linux)
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[SOCAT]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check socat is installed
if ! command -v socat &> /dev/null; then
    log_error "socat is not installed"
    echo ""
    echo "Install with:"
    echo "  macOS:  brew install socat"
    echo "  Linux:  apt install socat"
    exit 1
fi

# Clean up any existing PTYs
rm -f /tmp/ttyV0 /tmp/ttyV1

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  SOCAT - Virtual Serial Port Pair${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
log_info "Creating virtual serial port pair..."
log_info "  /tmp/ttyV0 → PLS Simulator"
log_info "  /tmp/ttyV1 → Webapp"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Create PTY pair with raw mode and proper permissions
# link= creates symbolic links for easier access
exec socat -d -d \
    pty,raw,echo=0,link=/tmp/ttyV0 \
    pty,raw,echo=0,link=/tmp/ttyV1
