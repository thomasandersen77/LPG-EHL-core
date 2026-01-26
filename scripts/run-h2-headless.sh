#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# RUN HEADLESS WITH EXTERNAL H2 CONFIG
#═══════════════════════════════════════════════════════════════════════════════
#
# Runs the headless JAR with external application-h2.yaml configuration.
# This allows changing serial parameters without rebuilding.
#
# Usage:
#   ./scripts/run-h2-headless.sh           # Headless only (no web server)
#   ./scripts/run-h2-headless.sh --api     # With debug REST API on port 8080
#   ./scripts/run-h2-headless.sh --help    # Show help
#
# Environment variables:
#   PORT              - API port when using --api (default: 8080)
#   EHL_SERIAL_PORT   - Override serial port path
#   EHL_TRANSPORT_MODE - Override transport mode (EMULATOR, SOCAT, HARDWARE)
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
CONFIG_FILE="$PROJECT_ROOT/application-h2.yaml"
JAR_FILE="$PROJECT_ROOT/release/lpg-ehl-headless.jar"
ENABLE_API=false

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
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
    echo "Options:"
    echo "  --api       Enable debug REST API on port \${PORT:-8080}"
    echo "  --help, -h  Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  PORT                API port (default: 8080)"
    echo "  EHL_SERIAL_PORT     Serial port path (default: /dev/ttyS0)"
    echo "  EHL_TRANSPORT_MODE  Transport mode: EMULATOR, SOCAT, HARDWARE"
    echo "  LPG_MODE            Application mode: LAB or FIELD"
    echo ""
    echo "Configuration file: $CONFIG_FILE"
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --api) ENABLE_API=true; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

# ─────────────────────────────────────────────────────────────────────────────

log_header "LPG EHL HEADLESS (H2 Config)"

# Check JAR exists
if [[ ! -f "$JAR_FILE" ]]; then
    echo -e "${RED}ERROR: JAR not found: $JAR_FILE${NC}"
    echo ""
    echo "Build with:"
    echo "  mvn -q -DskipTests package"
    echo "  cp lpg-ehl-app-headless/target/lpg-ehl-app-headless-*.jar release/lpg-ehl-headless.jar"
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
)

# Determine profiles
PROFILES="h2"
if [[ "$ENABLE_API" == "true" ]]; then
    PROFILES="h2,debug-api"
    log_info "Debug API: http://localhost:${PORT:-8080}"
    log_info "  GET  /api/debug/health"
    log_info "  GET  /api/debug/state/{addr}"
    log_info "  POST /api/debug/unblock/{addr}"
    log_info "  POST /api/debug/block/{addr}"
else
    log_info "Debug API: Disabled (use --api to enable)"
fi

JAVA_ARGS+=("--spring.profiles.active=$PROFILES")

echo ""
log_info "Starting headless application..."
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Run
exec java "${JAVA_ARGS[@]}"
