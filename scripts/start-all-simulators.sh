#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# START ALL SIMULATORS – Socat + Terminal + PLS
#═══════════════════════════════════════════════════════════════════════
#
# Starter simulatorene for terminal/pumpe-integrasjon:
#   1. Socat       – virtuell seriell kobling (vserial0 <-> vserial1)
#   2. Terminal    – Payment Terminal Simulator med GUI (port 18080)
#   3. PLS         – Pumpe-simulator med GUI (vserial0)
#
# Port-fordeling:
#   - /tmp/vserial0  → PLS Simulator (pumpestyring)
#   - /tmp/vserial1  → Webapp
#
# Usage:
#   ./scripts/start-all-simulators.sh
#   ./scripts/start-all-simulators.sh --build   # Bygg JARs først
#
# Stop: Ctrl+C
#
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# JAR paths (release/ or target/)
RELEASE="$PROJECT_ROOT/release"
PAYMENT_TERMINAL_GUI_JAR="$RELEASE/payment-terminal-gui.jar"
PLS_SIM_JAR="$RELEASE/pls-sim.jar"

# PIDs
SOCAT_PID=""
TERMINAL_PID=""
PLS_PID=""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

DO_BUILD=false
for arg in "$@"; do
    case $arg in
        --build) DO_BUILD=true ;;
    esac
done

cleanup() {
    echo ""
    echo -e "${CYAN}🛑 Stopping all services...${NC}"

    # PLS Simulator
    if [ -n "${PLS_PID:-}" ] && kill -0 "$PLS_PID" 2>/dev/null; then
        kill "$PLS_PID" 2>/dev/null || true
        sleep 0.3
        echo -e "  ✓ PLS Simulator stopped"
    fi
    pkill -f "pls-sim.jar" 2>/dev/null || true

    # Payment Terminal GUI
    if [ -n "${TERMINAL_PID:-}" ] && kill -0 "$TERMINAL_PID" 2>/dev/null; then
        kill "$TERMINAL_PID" 2>/dev/null || true
        echo -e "  ✓ Payment Terminal Simulator stopped"
    fi
    pkill -f "payment-terminal-gui" 2>/dev/null || true

    # Socat
    if [ -n "${SOCAT_PID:-}" ] && kill -0 "$SOCAT_PID" 2>/dev/null; then
        kill "$SOCAT_PID" 2>/dev/null || true
        echo -e "  ✓ Socat stopped"
    fi

    rm -f /tmp/vserial0 /tmp/vserial1 2>/dev/null || true
    echo ""
    echo "Bye!"
}
trap cleanup EXIT SIGINT SIGTERM

# Check socat
if ! command -v socat &> /dev/null; then
    echo -e "${RED}socat ikke installert. Kjør: brew install socat${NC}"
    exit 1
fi

# Build if requested or JARs missing
build_if_needed() {
    local missing=""
    [ ! -f "$PAYMENT_TERMINAL_GUI_JAR" ] && missing="$missing payment-terminal-gui"
    [ ! -f "$PLS_SIM_JAR" ] && missing="$missing pls-sim"

    if [ -n "$missing" ] || [ "$DO_BUILD" = true ]; then
        echo -e "${YELLOW}Bygger manglende JARs...${NC}"
        cd "$PROJECT_ROOT"
        ./build_monolith.sh
        cd - >/dev/null
    fi
}
build_if_needed

# Verify JARs exist
for j in "$PAYMENT_TERMINAL_GUI_JAR" "$PLS_SIM_JAR"; do
    if [ ! -f "$j" ]; then
        echo -e "${RED}Manglende JAR: $j${NC}"
        echo -e "Kjør: ./build_monolith.sh"
        exit 1
    fi
done

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🛢️  Start All Simulators${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""

# 1. Socat
rm -f /tmp/vserial0 /tmp/vserial1
echo -e "${CYAN}[1/3] Starting socat...${NC}"
echo -e "      ${BOLD}/tmp/vserial0${NC}  ${GRAY}← PLS Simulator${NC}"
echo -e "      ${BOLD}/tmp/vserial1${NC}  ${GRAY}← Webapp${NC}"

SOCAT_OUTPUT=$(mktemp)
socat -d -d \
    pty,rawer,echo=0,link=/tmp/vserial0 \
    pty,rawer,echo=0,link=/tmp/vserial1 \
    2>"$SOCAT_OUTPUT" &
SOCAT_PID=$!
sleep 1

if [[ ! -e /tmp/vserial0 ]] || [[ ! -e /tmp/vserial1 ]]; then
    echo -e "${RED}Kunne ikke opprette virtuelle serial ports${NC}"
    cat "$SOCAT_OUTPUT"
    rm -f "$SOCAT_OUTPUT"
    exit 1
fi

PTY0=$(grep "N PTY is" "$SOCAT_OUTPUT" | head -1 | sed 's/.*PTY is //')
cat "$SOCAT_OUTPUT"
rm -f "$SOCAT_OUTPUT"

if [[ -e "$PTY0" ]]; then
    chmod 666 "$PTY0" 2>/dev/null || true
fi
echo -e "${GREEN}      ✓ Socat running (PID: $SOCAT_PID)${NC}"
echo ""

# 2. Payment Terminal Simulator
echo -e "${CYAN}[2/3] Starting Payment Terminal Simulator (GUI)...${NC}"
echo -e "      Port: ${BOLD}18080${NC}"

java -jar "$PAYMENT_TERMINAL_GUI_JAR" &
TERMINAL_PID=$!
sleep 2
echo -e "${GREEN}      ✓ Terminal running (PID: $TERMINAL_PID)${NC}"
echo -e "      ${GRAY}→ http://localhost:18080${NC}"
echo ""

# 3. PLS Simulator
echo -e "${CYAN}[3/3] Starting PLS Simulator (pumpestyring)...${NC}"
echo -e "      Port: ${BOLD}/tmp/vserial0${NC}  (adresse 1, GUI)";

java -Xms64m -Xmx64m -XX:+UseSerialGC \
    -jar "$PLS_SIM_JAR" \
    --port="$PTY0" \
    --address=1 \
    --mode=ehl \
    --gui \
    &
PLS_PID=$!
sleep 2

if ! kill -0 "$PLS_PID" 2>/dev/null; then
    echo -e "${RED}PLS Simulator startet ikke${NC}"
    exit 1
fi
echo -e "${GREEN}      ✓ PLS Simulator running (PID: $PLS_PID)${NC}"
echo ""

# Ready
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Klart for testing${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BOLD}Flyt:${NC}"
echo -e "  1. Åpne terminal i Payment Terminal Simulator"
echo -e "  2. Trykk «Trekke kort» (scenario APPROVED)"
echo -e "  3. PLS får UNBLOCK → trykk START i PLS GUI"
echo -e "  4. Start webapp separat for å fullføre testen"
echo ""
echo -e "  ${GRAY}Stop alt: Ctrl+C${NC}"
echo ""

# Keep alive
while kill -0 "$SOCAT_PID" 2>/dev/null; do
    sleep 1
done
