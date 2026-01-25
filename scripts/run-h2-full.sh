#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# RUN WEBAPP + EMULATOR TCP SERVER (FULL TESTING MODE)
#═══════════════════════════════════════════════════════════════════════════════
#
# Runs both the webapp and the emulator TCP server together.
# This allows testing with Windows "Dispenser Control" application.
#
# Architecture:
#   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
#   │ Windows         │────▶│ Emulator        │────▶│ Webapp          │
#   │ Dispenser       │TCP  │ TCP Server      │HTTP │ (GUI + API)     │
#   │ Control         │9000 │ (Text commands) │8080 │ H2 Database     │
#   └─────────────────┘     └─────────────────┘     └─────────────────┘
#
# Usage:
#   ./scripts/run-h2-full.sh         # Start both in foreground
#   ./scripts/run-h2-full.sh --help  # Show help
#
# Windows Dispenser Control: Connect to [HOST_IP]:9000
# Webapp GUI: http://localhost:8080
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
CONFIG_FILE="$PROJECT_ROOT/application-h2.yaml"
WEBAPP_JAR="$PROJECT_ROOT/release/lpg-ehl-webapp.jar"
EMULATOR_JAR="$PROJECT_ROOT/lpg-ehl-emulator/target/lpg-ehl-emulator-0.0.1-SNAPSHOT.jar"

# Fallback to release folder
if [[ ! -f "$EMULATOR_JAR" ]]; then
    EMULATOR_JAR="$PROJECT_ROOT/release/lpg-ehl-emulator.jar"
fi

# Ports
WEBAPP_PORT=${PORT:-8080}
EMULATOR_PORT=${EMULATOR_PORT:-9000}

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_header() { 
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

show_help() {
    echo "Usage: $(basename "$0") [OPTIONS]"
    echo ""
    echo "Starts both the webapp (GUI + API) and the emulator TCP server."
    echo "The emulator handles text commands from Windows Dispenser Control."
    echo ""
    echo "Options:"
    echo "  --help, -h   Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  PORT           Webapp port (default: 8080)"
    echo "  EMULATOR_PORT  TCP server port (default: 9000)"
    echo ""
    echo "Configuration file: $CONFIG_FILE"
    exit 0
}

cleanup() {
    echo ""
    log_info "Shutting down..."
    
    # Kill background processes
    if [[ -n "$WEBAPP_PID" ]] && kill -0 "$WEBAPP_PID" 2>/dev/null; then
        log_info "Stopping webapp (PID: $WEBAPP_PID)"
        kill "$WEBAPP_PID" 2>/dev/null || true
    fi
    
    if [[ -n "$EMULATOR_PID" ]] && kill -0 "$EMULATOR_PID" 2>/dev/null; then
        log_info "Stopping emulator (PID: $EMULATOR_PID)"
        kill "$EMULATOR_PID" 2>/dev/null || true
    fi
    
    # Wait for clean shutdown
    sleep 2
    
    log_info "Cleanup complete"
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────────────

trap cleanup SIGINT SIGTERM

log_header "LPG EHL FULL TESTING MODE"

# Check config
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo -e "${RED}ERROR: Config file not found: $CONFIG_FILE${NC}"
    exit 1
fi

# Check webapp JAR
if [[ ! -f "$WEBAPP_JAR" ]]; then
    echo -e "${RED}ERROR: Webapp JAR not found: $WEBAPP_JAR${NC}"
    echo ""
    echo "Build with:"
    echo "  mvn -q -DskipTests package"
    exit 1
fi

# Check emulator JAR
if [[ ! -f "$EMULATOR_JAR" ]]; then
    echo -e "${RED}ERROR: Emulator JAR not found: $EMULATOR_JAR${NC}"
    echo ""
    echo "Build with:"
    echo "  mvn -q -DskipTests package -pl lpg-ehl-emulator -am"
    exit 1
fi

log_info "Config: $CONFIG_FILE"
log_info "Webapp JAR: $WEBAPP_JAR"
log_info "Emulator JAR: $EMULATOR_JAR"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# Start Webapp
# ─────────────────────────────────────────────────────────────────────────────

echo -e "${CYAN}[1/2] Starting Webapp...${NC}"

java -jar "$WEBAPP_JAR" \
    "--spring.config.location=file:$CONFIG_FILE" \
    "--spring.profiles.active=h2" \
    "--server.port=$WEBAPP_PORT" \
    > >(sed 's/^/  [WEBAPP] /') 2>&1 &

WEBAPP_PID=$!
log_info "Webapp started (PID: $WEBAPP_PID)"

# Wait for webapp to be ready
echo -n "  Waiting for webapp to start..."
for i in {1..30}; do
    if curl -s "http://localhost:$WEBAPP_PORT/actuator/health" > /dev/null 2>&1; then
        echo " Ready!"
        break
    fi
    echo -n "."
    sleep 1
done
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# Start Emulator TCP Server
# ─────────────────────────────────────────────────────────────────────────────

echo -e "${CYAN}[2/2] Starting Emulator TCP Server...${NC}"

java -jar "$EMULATOR_JAR" \
    "--spring.config.location=file:$CONFIG_FILE" \
    "--emulator.port=$EMULATOR_PORT" \
    "--lpg-api.base-url=http://localhost:$WEBAPP_PORT" \
    > >(sed 's/^/  [EMULATOR] /') 2>&1 &

EMULATOR_PID=$!
log_info "Emulator started (PID: $EMULATOR_PID)"

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────

echo ""
log_header "SERVICES RUNNING"
echo ""
echo -e "  ${BLUE}Webapp (GUI + API):${NC}"
echo -e "    Dashboard:      ${GREEN}http://localhost:$WEBAPP_PORT${NC}"
echo -e "    Control panel:  ${GREEN}http://localhost:$WEBAPP_PORT/control${NC}"
echo -e "    H2 Console:     ${GREEN}http://localhost:$WEBAPP_PORT/h2-console${NC}"
echo -e "    Swagger UI:     ${GREEN}http://localhost:$WEBAPP_PORT/swagger-ui.html${NC}"
echo ""
echo -e "  ${BLUE}Emulator TCP Server (for Windows Dispenser Control):${NC}"
echo -e "    Host:           ${GREEN}0.0.0.0:$EMULATOR_PORT${NC}"
echo -e "    Protocol:       Legacy text commands (<TANK_DISP_UNBLOCK>, etc.)"
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  Press Ctrl+C to stop both services${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Wait for either process to exit
wait -n "$WEBAPP_PID" "$EMULATOR_PID" 2>/dev/null || true

# If one dies, clean up the other
cleanup
