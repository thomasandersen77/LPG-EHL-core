#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# LPG-EHL SOCAT + SIMULATOR STARTUP SCRIPT
#═══════════════════════════════════════════════════════════════════════════════
#
# This script sets up a complete SOCAT testing environment:
#   1. Creates a virtual serial port pair using socat
#   2. Starts the PLS Simulator on one end
#   3. Optionally starts the webapp or headless app on the other end
#
# Usage:
#   ./scripts/start-socat-sim.sh [options]
#
# Options:
#   --webapp          Start lpg-ehl-webapp after simulator (default)
#   --headless        Start lpg-ehl-headless instead of webapp
#   --sim-only        Only start socat + simulator, no app
#   --address=<1-8>   Dispenser address (default: 1)
#   --price=<cents>   Price in cents, e.g. 1590 = 15.90 kr/L (default: 1590)
#   --blocked=<bool>  Initial blocked state (default: true)
#   --debug-api       Enable debug-api profile for headless (curl access)
#   --port=<port>     Web server port (default: 8080)
#   --help            Show this help message
#
# Examples:
#   ./scripts/start-socat-sim.sh                       # Default: webapp
#   ./scripts/start-socat-sim.sh --sim-only            # Just socat + simulator
#   ./scripts/start-socat-sim.sh --headless --debug-api  # Headless with curl access
#   ./scripts/start-socat-sim.sh --price=2100 --blocked=false
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default configuration
APP_MODE="webapp"
DISPENSER_ADDRESS=1
PRICE_CENTS=1590
BLOCKED=true
DEBUG_API=false
WEB_PORT=8080

# Virtual PTY paths
PTY_SIM="/tmp/ttyV0"    # PLS Simulator connects here
PTY_APP="/tmp/ttyV1"    # Application connects here

# JAR paths
SIM_JAR="$PROJECT_ROOT/lpg-ehl-serialport-sim/target/pls-sim.jar"
WEBAPP_JAR="$PROJECT_ROOT/lpg-ehl-webapp/target/lpg-ehl-webapp.jar"
HEADLESS_JAR="$PROJECT_ROOT/lpg-ehl-app-headless/target/lpg-ehl-headless.jar"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

#───────────────────────────────────────────────────────────────────────────────
# Helper functions
#───────────────────────────────────────────────────────────────────────────────

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
}

show_help() {
    head -40 "$0" | tail -35 | sed 's/^# //' | sed 's/^#//'
    exit 0
}

check_dependency() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 is not installed. Please install it first."
        exit 1
    fi
}

check_jar() {
    if [[ ! -f "$1" ]]; then
        log_error "JAR not found: $1"
        log_info "Run: mvn -q -DskipTests package"
        exit 1
    fi
}

cleanup() {
    log_info "Cleaning up..."
    
    # Kill background processes
    if [[ -n "$SOCAT_PID" ]] && kill -0 "$SOCAT_PID" 2>/dev/null; then
        log_info "Stopping socat (PID: $SOCAT_PID)"
        kill "$SOCAT_PID" 2>/dev/null || true
    fi
    
    if [[ -n "$SIM_PID" ]] && kill -0 "$SIM_PID" 2>/dev/null; then
        log_info "Stopping simulator (PID: $SIM_PID)"
        kill "$SIM_PID" 2>/dev/null || true
    fi
    
    if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
        log_info "Stopping app (PID: $APP_PID)"
        kill "$APP_PID" 2>/dev/null || true
    fi
    
    # Remove PTY symlinks
    rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true
    
    log_info "Cleanup complete"
}

trap cleanup EXIT INT TERM

#───────────────────────────────────────────────────────────────────────────────
# Parse arguments
#───────────────────────────────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case $1 in
        --webapp)
            APP_MODE="webapp"
            shift
            ;;
        --headless)
            APP_MODE="headless"
            shift
            ;;
        --sim-only)
            APP_MODE="none"
            shift
            ;;
        --address=*)
            DISPENSER_ADDRESS="${1#*=}"
            shift
            ;;
        --price=*)
            PRICE_CENTS="${1#*=}"
            shift
            ;;
        --blocked=*)
            BLOCKED="${1#*=}"
            shift
            ;;
        --debug-api)
            DEBUG_API=true
            shift
            ;;
        --port=*)
            WEB_PORT="${1#*=}"
            shift
            ;;
        --help|-h)
            show_help
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────
# Main script
#───────────────────────────────────────────────────────────────────────────────

log_header "LPG-EHL SOCAT Testing Environment"

# Check dependencies
check_dependency "socat"
check_dependency "java"
check_jar "$SIM_JAR"

if [[ "$APP_MODE" == "webapp" ]]; then
    check_jar "$WEBAPP_JAR"
elif [[ "$APP_MODE" == "headless" ]]; then
    check_jar "$HEADLESS_JAR"
fi

# Show configuration
echo ""
log_info "Configuration:"
log_info "  App Mode:     $APP_MODE"
log_info "  Dispenser:    Address $DISPENSER_ADDRESS"
log_info "  Price:        $(echo "scale=2; $PRICE_CENTS / 100" | bc) kr/L"
log_info "  Blocked:      $BLOCKED"
log_info "  Debug API:    $DEBUG_API"
log_info "  Web Port:     $WEB_PORT"
log_info "  PTY (Sim):    $PTY_SIM"
log_info "  PTY (App):    $PTY_APP"

#───────────────────────────────────────────────────────────────────────────────
# Step 1: Start socat
#───────────────────────────────────────────────────────────────────────────────

log_header "Step 1: Starting socat"

# Remove old symlinks if they exist
rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true

log_info "Creating virtual serial port pair..."
socat -d -d pty,raw,echo=0,link="$PTY_SIM" pty,raw,echo=0,link="$PTY_APP" &
SOCAT_PID=$!

# Wait for socat to create the symlinks
sleep 1

if [[ ! -e "$PTY_SIM" ]] || [[ ! -e "$PTY_APP" ]]; then
    log_error "Failed to create PTY pair"
    exit 1
fi

log_info "✅ socat running (PID: $SOCAT_PID)"
log_info "   $PTY_SIM <-> $PTY_APP"

#───────────────────────────────────────────────────────────────────────────────
# Step 2: Start PLS Simulator
#───────────────────────────────────────────────────────────────────────────────

log_header "Step 2: Starting PLS Simulator"

log_info "Launching simulator on $PTY_SIM..."

java -jar "$SIM_JAR" \
    --port="$PTY_SIM" \
    --mode=ehl \
    --logHex=true \
    --address="$DISPENSER_ADDRESS" \
    --price="$PRICE_CENTS" \
    --blocked="$BLOCKED" &
SIM_PID=$!

sleep 2

if ! kill -0 "$SIM_PID" 2>/dev/null; then
    log_error "Simulator failed to start"
    exit 1
fi

log_info "✅ Simulator running (PID: $SIM_PID)"

#───────────────────────────────────────────────────────────────────────────────
# Step 3: Start Application (optional)
#───────────────────────────────────────────────────────────────────────────────

if [[ "$APP_MODE" != "none" ]]; then
    log_header "Step 3: Starting Application ($APP_MODE)"
    
    if [[ "$APP_MODE" == "webapp" ]]; then
        log_info "Launching webapp on port $WEB_PORT..."
        
        java -jar "$WEBAPP_JAR" \
            --ehl.transport.mode=SOCAT \
            --ehl.serial.port="$PTY_APP" \
            --server.port="$WEB_PORT" &
        APP_PID=$!
        
    elif [[ "$APP_MODE" == "headless" ]]; then
        PROFILES=""
        if [[ "$DEBUG_API" == "true" ]]; then
            PROFILES="--spring.profiles.active=debug-api"
            log_info "Launching headless with debug-api profile on port $WEB_PORT..."
        else
            log_info "Launching headless (no web server)..."
        fi
        
        java -jar "$HEADLESS_JAR" \
            --ehl.transport.mode=SOCAT \
            --ehl.serial.port="$PTY_APP" \
            --server.port="$WEB_PORT" \
            $PROFILES &
        APP_PID=$!
    fi
    
    sleep 3
    
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        log_error "Application failed to start"
        exit 1
    fi
    
    log_info "✅ Application running (PID: $APP_PID)"
fi

#───────────────────────────────────────────────────────────────────────────────
# Summary
#───────────────────────────────────────────────────────────────────────────────

log_header "Environment Ready"

echo ""
log_info "Running processes:"
log_info "  socat:     PID $SOCAT_PID"
log_info "  simulator: PID $SIM_PID"
if [[ -n "$APP_PID" ]]; then
    log_info "  app:       PID $APP_PID"
fi

echo ""
if [[ "$APP_MODE" == "webapp" ]]; then
    log_info "Web UI:     http://localhost:$WEB_PORT"
    log_info "Control:    http://localhost:$WEB_PORT/control"
    log_info "Tester:     http://localhost:$WEB_PORT/protocol-tester"
elif [[ "$APP_MODE" == "headless" ]] && [[ "$DEBUG_API" == "true" ]]; then
    log_info "Debug API endpoints:"
    log_info "  Health:   curl http://localhost:$WEB_PORT/api/debug/health"
    log_info "  State:    curl http://localhost:$WEB_PORT/api/debug/state/1"
    log_info "  Unblock:  curl -X POST http://localhost:$WEB_PORT/api/debug/unblock/1"
    log_info "  Block:    curl -X POST http://localhost:$WEB_PORT/api/debug/block/1"
fi

echo ""
log_info "Press Ctrl+C to stop all processes"
echo ""

# Wait for all background processes
wait
