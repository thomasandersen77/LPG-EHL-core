#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# LPG-EHL Startup Script
#═══════════════════════════════════════════════════════════════════════════════
#
# TO MODES:
#   LAB   - In-memory emulator (ingen hardware)
#   FIELD - Real serial port (hardware eller socat PTY)
#
# Bruk:
#   ./start-lpg-ehl.sh              # LAB MODE (default)
#   ./start-lpg-ehl.sh --lab        # LAB MODE (explicit)
#   ./start-lpg-ehl.sh --field      # FIELD MODE
#   ./start-lpg-ehl.sh --headless   # HEADLESS (LAB mode)
#   ./start-lpg-ehl.sh --help       # Vis hjelp
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

# Default mode
MODE="LAB"
PORT=8080

# Parse arguments
for arg in "$@"; do
    case $arg in
        --lab) MODE="LAB" ;;
        --field) MODE="FIELD" ;;
        --headless) MODE="HEADLESS" ;;
        --port=*) PORT="${arg#*=}" ;;
        --help|-h)
            sed -n '2,13p' "$0" | sed 's/^# //' | sed 's/^#//'
            exit 0
            ;;
        *)
            echo -e "${RED}Ukjent opsjon: $arg${NC}"
            echo "Bruk --help for hjelp"
            exit 1
            ;;
    esac
done

# Cleanup function for FIELD mode
cleanup_field() {
    echo ""
    echo -e "${YELLOW}Stopper simulator og socat...${NC}"
    [ -n "$SOCAT_PID" ] && kill "$SOCAT_PID" 2>/dev/null || true
    [ -n "$SIM_PID" ] && kill "$SIM_PID" 2>/dev/null || true
    rm -f /tmp/ttyV0 /tmp/ttyV1
    echo -e "${GREEN}✓ Cleanup complete${NC}"
}

# Trap Ctrl+C for FIELD mode
trap cleanup_field SIGINT SIGTERM

#═══════════════════════════════════════════════════════════════════════════════
# LAB MODE - In-memory emulator
#═══════════════════════════════════════════════════════════════════════════════
if [ "$MODE" = "LAB" ]; then
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  🧪 LPG-EHL LAB MODE${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${CYAN}In-memory emulator - ingen hardware kreves${NC}"
    echo -e "  ${CYAN}Perfekt for utvikling og testing!${NC}"
    echo ""
    
    if [ ! -f "release/lpg-ehl-webapp.jar" ]; then
        echo -e "${RED}FEIL: release/lpg-ehl-webapp.jar ikke funnet${NC}"
        echo "Bygg først: ./build_monolith.sh --skip-tests"
        exit 1
    fi
    
    echo -e "  ${GRAY}Port:${NC}      $PORT"
    echo -e "  ${GRAY}Mode:${NC}      LAB (emulator)"
    echo -e "  ${GRAY}Database:${NC}  H2 in-memory"
    echo ""
    echo -e "  ${GREEN}GUI:${NC}       http://localhost:$PORT"
    echo -e "  ${GREEN}Control:${NC}   http://localhost:$PORT/control"
    echo -e "  ${GREEN}API:${NC}       http://localhost:$PORT/swagger-ui.html"
    echo ""
    echo -e "${YELLOW}Starting webapp...${NC}"
    echo ""
    
    java -jar release/lpg-ehl-webapp.jar \
        --spring.config.location=file:./application-h2.yaml \
        --server.port="$PORT"
fi

#═══════════════════════════════════════════════════════════════════════════════
# FIELD MODE - Simulator + Virtual PTY + Webapp
#═══════════════════════════════════════════════════════════════════════════════
if [ "$MODE" = "FIELD" ]; then
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  🔌 LPG-EHL FIELD MODE${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${CYAN}Simulator + Virtual PTY (socat) + Webapp${NC}"
    echo -e "  ${CYAN}For testing med seriell kommunikasjon${NC}"
    echo ""
    
    # Check prerequisites
    if ! command -v socat &> /dev/null; then
        echo -e "${RED}FEIL: socat ikke installert${NC}"
        echo "Installer med: brew install socat"
        exit 1
    fi
    
    if [ ! -f "release/pls-sim.jar" ] || [ ! -f "release/lpg-ehl-webapp.jar" ]; then
        echo -e "${RED}FEIL: JAR-filer ikke funnet${NC}"
        echo "Bygg først: ./build_monolith.sh --skip-tests"
        exit 1
    fi
    
    echo -e "  ${GRAY}Port:${NC}      $PORT"
    echo -e "  ${GRAY}Mode:${NC}      FIELD (virtual PTY via socat)"
    echo -e "  ${GRAY}Database:${NC}  H2 in-memory"
    echo ""
    
    # Cleanup old PTYs
    rm -f /tmp/ttyV0 /tmp/ttyV1
    
    # Step 1: Start socat
    echo -e "${YELLOW}[1/3] Starting socat virtual PTY bridge...${NC}"
    socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 > /tmp/socat.log 2>&1 &
    SOCAT_PID=$!
    sleep 1
    
    if ! kill -0 $SOCAT_PID 2>/dev/null; then
        echo -e "${RED}FEIL: socat failed to start${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}      ✓ Socat running (PID: $SOCAT_PID)${NC}"
    echo -e "        /tmp/ttyV0 ← Simulator"
    echo -e "        /tmp/ttyV1 ← Webapp"
    echo ""
    
    # Step 2: Start simulator
    echo -e "${YELLOW}[2/3] Starting PLS Simulator...${NC}"
    java -jar release/pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true > /tmp/simulator.log 2>&1 &
    SIM_PID=$!
    sleep 2
    
    if ! kill -0 $SIM_PID 2>/dev/null; then
        echo -e "${RED}FEIL: Simulator failed to start${NC}"
        cleanup_field
        exit 1
    fi
    
    echo -e "${GREEN}      ✓ Simulator running (PID: $SIM_PID)${NC}"
    echo ""
    
    # Step 3: Start webapp
    echo -e "${YELLOW}[3/3] Starting webapp...${NC}"
    echo ""
    echo -e "  ${GREEN}GUI:${NC}       http://localhost:$PORT"
    echo -e "  ${GREEN}Control:${NC}   http://localhost:$PORT/control"
    echo -e "  ${GREEN}API:${NC}       http://localhost:$PORT/swagger-ui.html"
    echo ""
    echo -e "  ${GRAY}Logs:${NC}"
    echo -e "    Socat:     tail -f /tmp/socat.log"
    echo -e "    Simulator: tail -f /tmp/simulator.log"
    echo ""
    echo -e "${GREEN}Press Ctrl+C to stop all services${NC}"
    echo ""
    
    # Start webapp (foreground - will block)
    java -jar release/lpg-ehl-webapp.jar \
        --spring.config.location=file:./application-h2.yaml \
        --server.port="$PORT" \
        --lpg.mode=FIELD
    
    # Cleanup when webapp exits
    cleanup_field
fi

#═══════════════════════════════════════════════════════════════════════════════
# HEADLESS MODE - Background service
#═══════════════════════════════════════════════════════════════════════════════
if [ "$MODE" = "HEADLESS" ]; then
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  🤖 LPG-EHL HEADLESS MODE${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${CYAN}Background service - ingen web UI${NC}"
    echo -e "  ${CYAN}For produksjon på Raspberry Pi / server${NC}"
    echo ""
    
    if [ ! -f "release/lpg-ehl-headless.jar" ]; then
        echo -e "${RED}FEIL: release/lpg-ehl-headless.jar ikke funnet${NC}"
        echo "Bygg først: ./build_monolith.sh --skip-tests"
        exit 1
    fi
    
    echo -e "  ${GRAY}Mode:${NC}      HEADLESS (in-memory emulator)"
    echo -e "  ${GRAY}Database:${NC}  H2 in-memory"
    echo -e "  ${GRAY}API:${NC}       Disabled"
    echo ""
    echo -e "${YELLOW}Starting headless service...${NC}"
    echo ""
    
    java -jar release/lpg-ehl-headless.jar \
        --spring.config.location=file:./application-h2.yaml
fi
