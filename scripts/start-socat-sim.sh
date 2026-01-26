#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# SOCAT + PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIM_JAR="$PROJECT_ROOT/release/pls-sim.jar"
BUILD_LOG="$PROJECT_ROOT/.build-sim.log"

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

cleanup() {
    echo ""
    echo -e "${GRAY}Shutting down...${NC}"
    [[ -n "$SIM_PID" ]] && kill "$SIM_PID" 2>/dev/null || true
    [[ -n "$SOCAT_PID" ]] && kill "$SOCAT_PID" 2>/dev/null || true
    rm -f /tmp/ttyV0 /tmp/ttyV1 "$BUILD_LOG"
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
rm -f /tmp/ttyV0 /tmp/ttyV1

# Start socat
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 &
SOCAT_PID=$!
sleep 1

if [[ ! -e /tmp/ttyV0 ]] || [[ ! -e /tmp/ttyV1 ]]; then
    echo -e "${RED}Kunne ikke opprette PTY-par${NC}"
    exit 1
fi

# Start simulator
java -jar "$SIM_JAR" --port=/tmp/ttyV0 --mode=ehl --logHex=true &
SIM_PID=$!
sleep 1

# Ready message
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ⛽ SOCAT + SIMULATOR KLAR${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BOLD}IntelliJ:${NC}"
echo -e "    • ${GREEN}Webapp (SOCAT)${NC}        → http://localhost:8080"
echo -e "    • ${GREEN}Headless (Debug API)${NC}  → curl localhost:8081"
echo ""
echo -e "  ${BOLD}JAR:${NC}"
echo -e "    java -jar release/lpg-ehl-webapp.jar \\"
echo -e "      --spring.config.location=file:./application-h2.yaml \\"
echo -e "      --ehl.transport.mode=SOCAT --ehl.serial.port=/tmp/ttyV1"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Ctrl+C for å stoppe${NC}"
echo ""

# Keep script alive - wait for any background process
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$SIM_PID" 2>/dev/null; do
    sleep 1
done

cleanup
