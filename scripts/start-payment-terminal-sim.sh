#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# PAYMENT TERMINAL SIMULATOR
#═══════════════════════════════════════════════════════════════════════
#
# Usage: ./scripts/start-payment-terminal-sim.sh [options]
#
# Options:
#   --port=<port>         HTTP port (default: 18080, same as Debian production)
#   --help, -h            Show this help message
#
# Note: When you stop the script (Ctrl+C), the simulator process is
#       automatically killed to avoid multiple instances running.
#
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIM_JAR="$PROJECT_ROOT/release/payment-terminal-sim.jar"
BUILD_LOG="$PROJECT_ROOT/.build-payment-sim.log"

# Default configuration
HTTP_PORT=18080  # Same as Debian production for easy local/remote switching

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
SIM_PID=""

show_help() {
    sed -n '2,14p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --port=*) HTTP_PORT="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

cleanup() {
    echo ""
    echo -e "${CYAN}🛑 Stopping payment terminal simulator...${NC}"
    
    # Kill simulator by PID first
    if [ -n "${SIM_PID:-}" ] && kill -0 "$SIM_PID" 2>/dev/null; then
        kill "$SIM_PID" 2>/dev/null || true
        sleep 0.5
    fi
    
    # Force kill any remaining payment-terminal-sim.jar processes
    if pgrep -f "payment-terminal-sim.jar" > /dev/null 2>&1; then
        echo -e "${YELLOW}  ⚠️  Found leftover payment-terminal-sim.jar processes. Killing...${NC}"
        pkill -f "payment-terminal-sim.jar" 2>/dev/null || true
        sleep 0.5
    fi
    
    # Verify all payment-terminal-sim.jar processes are dead; hard kill if needed
    if pgrep -f "payment-terminal-sim.jar" > /dev/null 2>&1; then
        echo -e "${RED}  💀 Force killing remaining payment-terminal-sim.jar processes...${NC}"
        pkill -9 -f "payment-terminal-sim.jar" 2>/dev/null || true
        sleep 0.2
    fi
    
    # Also kill any process on the port
    if lsof -ti:$HTTP_PORT > /dev/null 2>&1; then
        echo -e "${YELLOW}  ⚠️  Killing process on port $HTTP_PORT...${NC}"
        lsof -ti:$HTTP_PORT | xargs kill -9 2>/dev/null || true
    fi
    
    echo "  ✓ Simulator stopped"
    rm -f "$BUILD_LOG" 2>/dev/null || true
    echo ""
    echo "Cleanup complete. Bye!"
}
trap cleanup EXIT SIGINT SIGTERM

# Build JAR if missing
if [[ ! -f "$SIM_JAR" ]]; then
    echo -n -e "${GRAY}Bygger payment terminal simulator...${NC} "
    cd "$PROJECT_ROOT"
    if ! ./mvnw -q -DskipTests package -pl lpg-ehl-payment-terminal-sim -am > "$BUILD_LOG" 2>&1; then
        echo -e "${RED}FEILET${NC}"
        echo ""
        tail -20 "$BUILD_LOG"
        exit 1
    fi
    cp lpg-ehl-payment-terminal-sim/target/lpg-ehl-payment-terminal-sim-*.jar "$SIM_JAR"
    echo -e "${GREEN}✓${NC}"
fi

# Kill any existing instances on the port
if lsof -ti:$HTTP_PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port $HTTP_PORT is already in use. Killing existing process...${NC}"
    lsof -ti:$HTTP_PORT | xargs kill -9 2>/dev/null || true
    sleep 1
fi

echo ""
echo -e "${CYAN}Starting Payment Terminal Simulator...${NC}"
echo -e "      Port:         ${BOLD}$HTTP_PORT${NC}"
echo -e "      JAR:          ${GRAY}$SIM_JAR${NC}"
echo ""

# Start simulator
java -jar "$SIM_JAR" --server.port=$HTTP_PORT &
SIM_PID=$!
sleep 2

if ! kill -0 "$SIM_PID" 2>/dev/null; then
    echo -e "${RED}Simulator failed to start${NC}"
    exit 1
fi

echo -e "${GREEN}      ✓ Simulator running (PID: $SIM_PID)${NC}"
echo ""

# Ready message
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Payment Terminal Simulator Ready${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}Base URL:${NC}"
echo -e "  http://localhost:$HTTP_PORT"
echo ""
echo -e "${BOLD}Test scenarios:${NC}"
echo -e "  ${GRAY}# Approved transaction${NC}"
echo -e "  curl -X POST http://localhost:$HTTP_PORT/v1/payments/purchase \\\\"
echo -e "    -H \"Content-Type: application/json\" \\\\"
echo -e "    -H \"X-Terminal-Scenario: APPROVED\" \\\\"
echo -e "    -d '{\"AmountMinor\": 10000, \"Currency\": \"NOK\", \"OperatorId\": \"0000\"}' | jq"
echo ""
echo -e "  ${GRAY}# Declined${NC}"
echo -e "  curl -X POST http://localhost:$HTTP_PORT/v1/payments/purchase \\\\"
echo -e "    -H \"X-Terminal-Scenario: DECLINED\" \\\\"
echo -e "    -d '{\"AmountMinor\": 10000, \"Currency\": \"NOK\"}' | jq"
echo ""
echo -e "  ${GRAY}# Terminal busy${NC}"
echo -e "  curl -X POST http://localhost:$HTTP_PORT/v1/payments/purchase \\\\"
echo -e "    -H \"X-Terminal-Scenario: BUSY\" \\\\"
echo -e "    -d '{\"AmountMinor\": 10000, \"Currency\": \"NOK\"}' | jq"
echo ""
echo -e "${BOLD}Available scenarios:${NC}"
echo -e "  APPROVED, DECLINED, WRONG_PIN, USER_CANCEL, TIMEOUT, BUSY, NOT_READY"
echo ""
echo -e "${BOLD}Stop simulator:${NC}  ${GRAY}(Ctrl+C)${NC}"
echo -e "${YELLOW}──────────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GRAY}Logs will appear below. Press Ctrl+C to stop.${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

# Keep script alive - wait for simulator process
# Cleanup is guaranteed by trap
wait $SIM_PID
