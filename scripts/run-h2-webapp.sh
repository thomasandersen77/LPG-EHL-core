#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# RUN WEBAPP WITH EXTERNAL H2 CONFIG
#═══════════════════════════════════════════════════════════════════════════════
#
# Runs the webapp JAR with external application-h2.yaml configuration.
# This allows changing serial parameters without rebuilding.
#
# Usage:
#   ./scripts/run-h2-webapp.sh              # Default (EMULATOR mode)
#   ./scripts/run-h2-webapp.sh --hardware   # HARDWARE mode (real serial port)
#   ./scripts/run-h2-webapp.sh --socat      # SOCAT mode (virtual PTY)
#   ./scripts/run-h2-webapp.sh --help       # Show help
#
# GUI endpoints:
#   http://localhost:8080          - Dashboard
#   http://localhost:8080/control  - Control panel with live logging
#   http://localhost:8080/h2-console - H2 database console
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
CONFIG_FILE="$PROJECT_ROOT/application-h2.yaml"
JAR_FILE="$PROJECT_ROOT/release/lpg-ehl-webapp.jar"
TRANSPORT_MODE=""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_header() { 
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

show_help() {
    echo "Usage: $(basename "$0") [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --emulator   Use in-memory emulator (default)"
    echo "  --hardware   Use real serial port (HARDWARE mode)"
    echo "  --socat      Use virtual PTY (SOCAT mode, requires socat running)"
    echo "  --help, -h   Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  PORT                Web port (default: 8080)"
    echo "  EHL_SERIAL_PORT     Serial port path (for HARDWARE/SOCAT)"
    echo "  EHL_TRANSPORT_MODE  Override transport mode"
    echo ""
    echo "Configuration file: $CONFIG_FILE"
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --emulator) TRANSPORT_MODE="EMULATOR"; shift ;;
        --hardware) TRANSPORT_MODE="HARDWARE"; shift ;;
        --socat) TRANSPORT_MODE="SOCAT"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────────────

log_header "LPG EHL WEBAPP (H2 Config)"

# Check JAR exists
if [[ ! -f "$JAR_FILE" ]]; then
    echo -e "${RED}ERROR: JAR not found: $JAR_FILE${NC}"
    echo ""
    echo "Build with:"
    echo "  mvn -q -DskipTests package"
    echo "  cp lpg-ehl-webapp/target/lpg-ehl-webapp-*.jar release/lpg-ehl-webapp.jar"
    exit 1
fi

# Check config file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo -e "${RED}ERROR: Config file not found: $CONFIG_FILE${NC}"
    exit 1
fi

log_info "JAR: $JAR_FILE"
log_info "Config: $CONFIG_FILE"

# Build Java arguments
JAVA_ARGS=(
    -jar "$JAR_FILE"
    "--spring.config.location=file:$CONFIG_FILE"
    "--spring.profiles.active=h2"
)

# Add transport mode override if specified
if [[ -n "$TRANSPORT_MODE" ]]; then
    log_info "Transport: $TRANSPORT_MODE"
    JAVA_ARGS+=("--ehl.transport.mode=$TRANSPORT_MODE")
    
    # Warnings for specific modes
    if [[ "$TRANSPORT_MODE" == "HARDWARE" ]]; then
        log_info "Serial port: ${EHL_SERIAL_PORT:-/dev/ttyS0}"
        echo -e "${YELLOW}⚠️  HARDWARE MODE: Ensure serial device is connected${NC}"
    elif [[ "$TRANSPORT_MODE" == "SOCAT" ]]; then
        log_info "Serial port: ${EHL_SERIAL_PORT:-/tmp/ttyV1}"
        if [[ ! -e "${EHL_SERIAL_PORT:-/tmp/ttyV1}" ]]; then
            echo -e "${YELLOW}⚠️  SOCAT MODE: PTY not found. Start socat first:${NC}"
            echo "    ./scripts/start-socat-sim.sh"
        fi
    fi
else
    log_info "Transport: EMULATOR (default)"
fi

echo ""
log_info "Starting webapp..."
echo ""
echo -e "  ${BLUE}GUI endpoints:${NC}"
echo -e "    Dashboard:      ${GREEN}http://localhost:${PORT:-8080}${NC}"
echo -e "    Control panel:  ${GREEN}http://localhost:${PORT:-8080}/control${NC}"
echo -e "    H2 Console:     ${GREEN}http://localhost:${PORT:-8080}/h2-console${NC}"
echo -e "    Swagger UI:     ${GREEN}http://localhost:${PORT:-8080}/swagger-ui.html${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Run
exec java "${JAVA_ARGS[@]}"
