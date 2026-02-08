#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START HEADLESS APP IN PRODUCTION MODE
#═══════════════════════════════════════════════════════════════════════════════
#
# Production deployment script for lpg-ehl-headless on ARK-3360 or similar hardware.
# This is a background service without web UI, ideal for Raspberry Pi deployments.
#
# Usage: ./scripts/start-headless-production.sh [options]
#
# Options:
#   --port=<port>         Serial port (default: /dev/ttyS3)
#   --baud=<rate>         Baud rate (default: 9600)
#   --parity=<type>       Parity: NONE, EVEN, ODD (default: NONE)
#   --debug-api           Enable debug REST API on port 8080
#   --config=<file>       External config file (optional)
#   --help                Show help
#
# Production Defaults (ARK-3360):
#   Serial: /dev/ttyS3, 9600 baud, 8N1 (8 data bits, No parity, 1 stop bit)
#   Database: H2 file-based (./data/lpgdb)
#   Logging: Console + file (logs/lpg-ehl-headless.log)
#   Debug API: Disabled (enable with --debug-api)
#
# For development/testing with SOCAT simulator:
#   Use: ./scripts/start-headless-field.sh
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Production defaults (ARK-3360)
SERIAL_PORT="/dev/ttyS3"
BAUD_RATE=9600
PARITY="NONE"
DEBUG_API=false
EXTERNAL_CONFIG=""

# JAR path
HEADLESS_JAR="$PROJECT_ROOT/release/lpg-ehl-headless.jar"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

show_help() {
    sed -n '2,26p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --port=*) SERIAL_PORT="${1#*=}"; shift ;;
        --baud=*) BAUD_RATE="${1#*=}"; shift ;;
        --parity=*) PARITY="${1#*=}"; shift ;;
        --debug-api) DEBUG_API=true; shift ;;
        --config=*) EXTERNAL_CONFIG="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  🖥️  HEADLESS APP - PRODUCTION MODE${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Check JAR exists
if [[ ! -f "$HEADLESS_JAR" ]]; then
    echo -e "${RED}ERROR: JAR not found: $HEADLESS_JAR${NC}"
    echo "Build with: mvn -q -DskipTests package"
    exit 1
fi

# Check serial port exists
if [[ ! -e "$SERIAL_PORT" ]]; then
    echo -e "${YELLOW}WARNING: Serial port not found: $SERIAL_PORT${NC}"
    echo "Make sure the RS-485 adapter is connected."
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "  ${GREEN}Serial Port:${NC}  $SERIAL_PORT"
echo -e "  ${GREEN}Baud Rate:${NC}    $BAUD_RATE"
echo -e "  ${GREEN}Parity:${NC}       $PARITY"

if [[ "$DEBUG_API" == "true" ]]; then
    echo -e "  ${GREEN}Debug API:${NC}    ${BOLD}http://localhost:8080${NC}"
else
    echo -e "  ${GREEN}Debug API:${NC}    Disabled"
fi

if [[ -n "$EXTERNAL_CONFIG" ]]; then
    echo -e "  ${GREEN}Config File:${NC}  $EXTERNAL_CONFIG"
fi

echo ""
echo -e "  ${CYAN}Logs:${NC}          logs/lpg-ehl-headless.log"
echo -e "  ${CYAN}Database:${NC}      data/lpgdb"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Create logs and data directories
mkdir -p "$PROJECT_ROOT/logs"
mkdir -p "$PROJECT_ROOT/data"

# Production JVM settings (smaller footprint for headless)
JVM_OPTS=(
    -Xms128m
    -Xmx256m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=50
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=logs/heap-dump-headless.hprof
)

# Build Java command
JAVA_ARGS=(
    "${JVM_OPTS[@]}"
    -jar "$HEADLESS_JAR"
    --spring.profiles.active=field
    --ehl.serial.port="$SERIAL_PORT"
    --ehl.serial.baud-rate="$BAUD_RATE"
    --ehl.serial.parity="$PARITY"
    --logging.file.name=logs/lpg-ehl-headless.log
)

# Enable debug API if requested
if [[ "$DEBUG_API" == "true" ]]; then
    JAVA_ARGS+=(--spring.web.application-type=servlet)
    JAVA_ARGS+=(--server.port=8080)
fi

# Add external config if specified
if [[ -n "$EXTERNAL_CONFIG" ]]; then
    JAVA_ARGS+=(--spring.config.location="$EXTERNAL_CONFIG")
fi

# Start headless app
exec java "${JAVA_ARGS[@]}"
