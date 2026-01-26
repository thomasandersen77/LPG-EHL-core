#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START HEADLESS APP (SOCAT MODE)
#═══════════════════════════════════════════════════════════════════════════════
#
# Starter headless-appen mot virtuell PTY (krever at start-sim.sh kjører).
#
# Bruk:
#   ./scripts/start-headless.sh [options]
#
# Opsjoner:
#   --debug-api       Aktiver REST API på port 8080 (for curl-testing)
#   --port=<port>     Web-port hvis debug-api (default: 8080)
#   --help            Vis hjelp
#
# Forutsetning:
#   Kjør først: ./scripts/start-sim.sh
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Konfigurasjon
DEBUG_API=false
WEB_PORT=8080
PTY_APP="/tmp/ttyV1"

# JAR-sti
HEADLESS_JAR="$PROJECT_ROOT/release/lpg-ehl-headless.jar"

# Farger
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_header() { echo -e "\n${BLUE}═══════════════════════════════════════════════${NC}\n  ${BLUE}$1${NC}\n${BLUE}═══════════════════════════════════════════════${NC}"; }

show_help() {
    sed -n '2,18p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse argumenter
while [[ $# -gt 0 ]]; do
    case $1 in
        --debug-api) DEBUG_API=true; shift ;;
        --port=*) WEB_PORT="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Ukjent opsjon: $1"; show_help ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────

log_header "START HEADLESS APP"

# Sjekk at JAR finnes
if [[ ! -f "$HEADLESS_JAR" ]]; then
    echo -e "${RED}FEIL: JAR ikke funnet: $HEADLESS_JAR${NC}"
    echo "Bygg med: mvn -q -DskipTests package"
    exit 1
fi

# Sjekk at PTY finnes (simulator må kjøre)
if [[ ! -e "$PTY_APP" ]]; then
    echo -e "${RED}FEIL: PTY ikke funnet: $PTY_APP${NC}"
    echo "Start først: ./scripts/start-sim.sh"
    exit 1
fi

log_info "Transport: SOCAT"
log_info "Serial port: $PTY_APP"

# Bygg kommandolinje
JAVA_ARGS=(
    -jar "$HEADLESS_JAR"
    --ehl.transport.mode=SOCAT
    --ehl.serial.port="$PTY_APP"
    --logging.file.name=./headless.log
)

if [[ "$DEBUG_API" == "true" ]]; then
    log_info "Debug API: http://localhost:$WEB_PORT"
    JAVA_ARGS+=(
        --spring.profiles.active=debug-api
        --server.port="$WEB_PORT"
    )
else
    log_info "Debug API: Disabled (bruk --debug-api for å aktivere)"
fi

echo ""
log_info "Starter..."
echo ""

# Kjør
java "${JAVA_ARGS[@]}"
