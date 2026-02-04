#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# SOCAT + PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════
#
# Usage: ./scripts/start-socat-sim.sh [options]
#
# Options:
#   --address=<1-8>    Dispenser address (default: 1)
#   --price=<cents>    Price in cents, e.g. 1590 = 15.90 kr/L (default: 1590)
#   --baud=<rate>      Baud rate (default: 9600)
#   --parity=<type>    Parity: NONE, EVEN, ODD (default: NONE)
#   --blocked=<bool>   Initial blocked state (default: true)
#   --help             Show help
#
# Note: DEBUG logging is enabled by default to show RX/TX HEX bytes.
#
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIM_JAR="$PROJECT_ROOT/release/pls-sim.jar"
BUILD_LOG="$PROJECT_ROOT/.build-sim.log"

# Default configuration
DISPENSER_ADDRESS=1
PRICE_CENTS=1590
BAUD_RATE=9600
PARITY="NONE"
BLOCKED="true"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

# Cleanup
SOCAT_PID=""
SIM_PID=""

show_help() {
    sed -n '2,18p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --address=*) DISPENSER_ADDRESS="${1#*=}"; shift ;;
        --price=*) PRICE_CENTS="${1#*=}"; shift ;;
        --baud=*) BAUD_RATE="${1#*=}"; shift ;;
        --parity=*) PARITY="${1#*=}"; shift ;;
        --blocked=*) BLOCKED="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

cleanup() {
    echo ""
    echo -e "${CYAN}🛑 Stopping services...${NC}"
    
    if [ -n "${SIM_PID:-}" ] && kill -0 $SIM_PID 2>/dev/null; then
        kill $SIM_PID 2>/dev/null || true
        echo "  ✓ Simulator stopped"
    fi
    
    if [ -n "${SOCAT_PID:-}" ] && kill -0 $SOCAT_PID 2>/dev/null; then
        kill $SOCAT_PID 2>/dev/null || true
        echo "  ✓ socat stopped"
    fi
    
    rm -f /tmp/vserial0 /tmp/vserial1 "$BUILD_LOG"
    echo ""
    echo "Cleanup complete. Bye!"
    exit 0
}
trap cleanup SIGINT SIGTERM

# Check socat
if ! command -v socat &> /dev/null; then
    echo -e "${RED}socat ikke installert. Kjør: brew install socat${NC}"
    exit 1
fi

# Build JAR if missing
if [[ ! -f "$SIM_JAR" ]]; then
    echo -n -e "${GRAY}Bygger simulator...${NC} "
    cd "$PROJECT_ROOT"
    if ! ./mvnw -q -DskipTests package -pl lpg-ehl-serialport-sim -am > "$BUILD_LOG" 2>&1; then
        echo -e "${RED}FEILET${NC}"
        echo ""
        tail -20 "$BUILD_LOG"
        exit 1
    fi
    cp lpg-ehl-serialport-sim/target/lpg-ehl-serialport-sim-*.jar "$SIM_JAR"
    echo -e "${GREEN}✓${NC}"
fi

# Cleanup old PTYs
rm -f /tmp/vserial0 /tmp/vserial1

echo ""
echo -e "${CYAN}[1/2] Starting socat...${NC}"
echo -e "      Port 0: ${BOLD}/tmp/vserial0${NC}  ${GRAY}← Simulator${NC}"
echo -e "      Port 1: ${BOLD}/tmp/vserial1${NC}  ${GRAY}← Python/Webapp${NC}"
echo ""

# Start socat with verbose output redirected
socat -d -d \
    pty,rawer,echo=0,link=/tmp/vserial0 \
    pty,rawer,echo=0,link=/tmp/vserial1 \
    2>&1 | grep --line-buffered "N PTY" &

SOCAT_PID=$!
sleep 1

if [[ ! -e /tmp/vserial0 ]] || [[ ! -e /tmp/vserial1 ]]; then
    echo -e "${RED}Kunne ikke opprette virtuelle serial ports${NC}"
    exit 1
fi

echo -e "${GREEN}      ✓ socat running (PID: $SOCAT_PID)${NC}"
echo ""
LEGACY_ADDR=$((32 + DISPENSER_ADDRESS))
echo -e "${CYAN}[2/2] Starting PLS Simulator...${NC}"
echo -e "      Address:   ${BOLD}$DISPENSER_ADDRESS${NC} ${GRAY}(also responds to $LEGACY_ADDR)${NC}"
echo -e "      Price:     ${BOLD}$(echo "scale=2; $PRICE_CENTS / 100" | bc) kr/L${NC}"
echo -e "      Baud:      ${BOLD}$BAUD_RATE${NC}"
echo -e "      Parity:    ${BOLD}$PARITY${NC}"
echo -e "      Blocked:   ${BOLD}$BLOCKED${NC}"
echo -e "      Commands:  ${GRAY}STATE, ERROR_QUERY, VOLUME, TANKBIT${NC}"
echo ""

# Start simulator with full logging to console
java -Dsim.log.level=DEBUG -jar "$SIM_JAR" \
    --port=/tmp/vserial0 \
    --address="$DISPENSER_ADDRESS" \
    --price="$PRICE_CENTS" \
    --baud="$BAUD_RATE" \
    --parity="$PARITY" \
    --blocked="$BLOCKED" \
    --mode=ehl \
    --logHex=true &

SIM_PID=$!
sleep 2

if ! kill -0 $SIM_PID 2>/dev/null; then
    echo -e "${RED}Simulator failed to start${NC}"
    exit 1
fi

echo -e "${GREEN}      ✓ Simulator running (PID: $SIM_PID)${NC}"
echo ""

# Ready message
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Klart for testing${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}Test med Alejandros Python script:${NC}"
echo -e "  cd python-test"
echo -e "  python3 01_probe_readonly.py --port /tmp/vserial1 --addr $DISPENSER_ADDRESS"
echo -e "  ${GRAY}# Eller test legacy adresse:${NC}"
echo -e "  python3 01_probe_readonly.py --port /tmp/vserial1 --addr $LEGACY_ADDR"
echo ""
echo -e "${BOLD}Test med REST API (scan adresser som Alejandros 02_scan_addresses.py):${NC}"
echo -e "  ${GRAY}# Start webapp først, deretter:${NC}"
echo -e "  curl -X POST \"http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=1&end=40\""
echo ""
echo -e "${BOLD}Start webapp:${NC}"
echo -e "  ./scripts/start-webapp-field.sh --auto-detect"
echo -e "  ${GRAY}# eller:${NC}"
echo -e "  java -jar release/lpg-ehl-webapp.jar \\"
echo -e "    --spring.profiles.active=field \\"
echo -e "    --ehl.serial.port=/tmp/vserial1 --ehl.serial.parity=$PARITY"
echo -e "  • GUI: ${CYAN}http://localhost:8080${NC}"
echo ""
echo -e "${BOLD}Start headless med debug API:${NC}"
echo -e "  java -jar release/lpg-ehl-headless.jar \\"
echo -e "    --spring.profiles.active=field,debug-api \\"
echo -e "    --ehl.serial.port=/tmp/vserial1"
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GRAY}Logs will appear below. Press Ctrl+C to stop.${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

# Keep script alive - wait for any background process
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$SIM_PID" 2>/dev/null; do
    sleep 1
done

cleanup
