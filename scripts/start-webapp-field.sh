#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START WEBAPP IN FIELD MODE
#═══════════════════════════════════════════════════════════════════════════════
#
# Usage: ./scripts/start-webapp-field.sh [options]
#
# Options:
#   --port=<port>         Serial port (default: /tmp/vserial1)
#   --auto-detect         Auto-detect parity mode (recommended)
#   --parity=<type>       Manual parity: NONE, EVEN, ODD (default: NONE)
#   --baud=<rate>         Baud rate (default: 9600)
#   --web-port=<port>     Web server port (default: 8080)
#   --help                Show help
#
# Prerequisites:
#   1. Simulator must be running: ./scripts/start-socat-sim.sh
#   2. Database (optional): docker-compose -f docker-compose-local.yaml up -d
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default configuration
SERIAL_PORT="/tmp/vserial1"
AUTO_DETECT=false
PARITY="NONE"
BAUD_RATE=9600
WEB_PORT=8080

# JAR path
WEBAPP_JAR="$PROJECT_ROOT/release/lpg-ehl-webapp.jar"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

show_help() {
    sed -n '2,18p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --port=*) SERIAL_PORT="${1#*=}"; shift ;;
        --auto-detect) AUTO_DETECT=true; shift ;;
        --parity=*) PARITY="${1#*=}"; shift ;;
        --baud=*) BAUD_RATE="${1#*=}"; shift ;;
        --web-port=*) WEB_PORT="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  🌐 START WEBAPP (FIELD MODE)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Check JAR exists
if [[ ! -f "$WEBAPP_JAR" ]]; then
    echo -e "${RED}ERROR: JAR not found: $WEBAPP_JAR${NC}"
    echo "Build with: mvn -q -DskipTests package"
    exit 1
fi

# Check serial port exists
if [[ ! -e "$SERIAL_PORT" ]]; then
    echo -e "${RED}ERROR: Serial port not found: $SERIAL_PORT${NC}"
    echo "Start simulator first: ./scripts/start-socat-sim.sh"
    exit 1
fi

echo -e "  ${GREEN}Serial Port:${NC}  $SERIAL_PORT"
echo -e "  ${GREEN}Baud Rate:${NC}    $BAUD_RATE"

if [[ "$AUTO_DETECT" == "true" ]]; then
    echo -e "  ${GREEN}Parity:${NC}       ${CYAN}AUTO-DETECT${NC}"
else
    echo -e "  ${GREEN}Parity:${NC}       $PARITY"
fi

echo -e "  ${GREEN}Web Port:${NC}     $WEB_PORT"
echo ""
echo -e "  ${BLUE}Web GUI:${NC}       ${BOLD}http://localhost:$WEB_PORT${NC}"
echo -e "  ${BLUE}Control:${NC}       ${BOLD}http://localhost:$WEB_PORT/control${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Build Java command
JAVA_ARGS=(
    -jar "$WEBAPP_JAR"
    --spring.profiles.active=field
    --ehl.serial.port="$SERIAL_PORT"
    --ehl.serial.baud-rate="$BAUD_RATE"
    --server.port="$WEB_PORT"
)

if [[ "$AUTO_DETECT" == "true" ]]; then
    JAVA_ARGS+=(--ehl.serial.parity-auto-detect=true)
else
    JAVA_ARGS+=(--ehl.serial.parity="$PARITY")
fi

# Start webapp
exec java "${JAVA_ARGS[@]}"
