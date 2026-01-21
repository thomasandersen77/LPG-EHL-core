#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# SOCAT + PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════════════
#
# Starter socat virtuell seriell port og PLS simulator.
# Headless-appen kan deretter startes separat (terminal eller IntelliJ).
#
# Bruk:
#   ./start-socat-sim.sh              # Standard (stille modus)
#   ./start-socat-sim.sh --debug      # Debug logging
#
# Headless kan deretter startes med:
#   Terminal:  java -jar release/lpg-ehl-headless.jar \
#                --spring.profiles.active=debug-api,local \
#                --lpg.mode=FIELD --ehl.serial.port=/tmp/ttyV1
#
#   IntelliJ:  Kjør HeadlessApplicationKt med VM options:
#              -Dspring.profiles.active=debug-api,local
#              -Dlpg.mode=FIELD
#              -Dehl.serial.port=/tmp/ttyV1
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Konfigurasjon
PTY_SIM="/tmp/ttyV0"
PTY_APP="/tmp/ttyV1"
SIM_JAR="$SCRIPT_DIR/release/pls-sim.jar"
DEBUG_MODE=false

# Farger
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse argumenter
while [[ $# -gt 0 ]]; do
    case $1 in
        --debug|-d)
            DEBUG_MODE=true
            shift
            ;;
        --help|-h)
            head -25 "$0" | tail -20 | sed 's/^# //' | sed 's/^#//'
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

cleanup() {
    echo ""
    echo -e "${YELLOW}Stopper...${NC}"
    pkill -f "socat.*ttyV0.*ttyV1" 2>/dev/null || true
    pkill -f "pls-sim.jar" 2>/dev/null || true
    rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true
    echo -e "${GREEN}Stoppet.${NC}"
}

trap cleanup EXIT INT TERM

# Sjekk avhengigheter
if ! command -v socat &> /dev/null; then
    echo "socat ikke installert. Installer med: brew install socat"
    exit 1
fi

if [[ ! -f "$SIM_JAR" ]]; then
    echo "Finner ikke $SIM_JAR"
    echo "Bygg først: ./mvnw -pl lpg-ehl-serialport-sim package -DskipTests -q"
    exit 1
fi

# Rydd opp gamle prosesser
pkill -f "socat.*ttyV0.*ttyV1" 2>/dev/null || true
pkill -f "pls-sim.jar" 2>/dev/null || true
rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true
sleep 0.5

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  ⛽ SOCAT + PLS SIMULATOR${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Start socat
echo -e "${GREEN}▶ Starter socat...${NC}"
socat -d -d pty,raw,echo=0,link="$PTY_SIM" pty,raw,echo=0,link="$PTY_APP" 2>/dev/null &
SOCAT_PID=$!
sleep 1

if [[ ! -e "$PTY_SIM" ]] || [[ ! -e "$PTY_APP" ]]; then
    echo "Feil: Kunne ikke opprette virtuelle porter"
    exit 1
fi
echo -e "  ✅ socat kjører (PID: $SOCAT_PID)"
echo -e "     $PTY_SIM ↔ $PTY_APP"

# Start simulator
echo ""
echo -e "${GREEN}▶ Starter PLS Simulator...${NC}"

SIM_ARGS="--port=$PTY_SIM --mode=ehl --address=1 --price=1590 --blocked=true"
if [[ "$DEBUG_MODE" == "true" ]]; then
    java -Dsim.log.level=DEBUG -jar "$SIM_JAR" $SIM_ARGS &
else
    java -jar "$SIM_JAR" $SIM_ARGS &
fi
SIM_PID=$!
sleep 2

if ! kill -0 "$SIM_PID" 2>/dev/null; then
    echo "Feil: Simulator startet ikke"
    exit 1
fi
echo -e "  ✅ Simulator kjører (PID: $SIM_PID)"

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ KLAR FOR HEADLESS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Simulator lytter på: ${YELLOW}$PTY_SIM${NC}"
echo -e "  Headless kobler til: ${YELLOW}$PTY_APP${NC}"
echo ""
echo -e "  ${GREEN}Start headless i nytt terminalvindu:${NC}"
echo ""
echo -e "  java -jar release/lpg-ehl-headless.jar \\"
echo -e "    --spring.profiles.active=debug-api,local \\"
echo -e "    --lpg.mode=FIELD --ehl.serial.port=$PTY_APP"
echo ""
echo -e "  ${GREEN}Eller kjør i IntelliJ med VM options:${NC}"
echo ""
echo -e "  -Dspring.profiles.active=debug-api,local"
echo -e "  -Dlpg.mode=FIELD"
echo -e "  -Dehl.serial.port=$PTY_APP"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Trykk ${YELLOW}Ctrl+C${NC} for å stoppe"
echo ""

# Vent på prosessene
wait
