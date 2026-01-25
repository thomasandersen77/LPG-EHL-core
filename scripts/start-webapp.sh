#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START WEBAPP (SOCAT MODE)
#═══════════════════════════════════════════════════════════════════════════════
#
# Starter webapp med full GUI og WebSocket-logging.
# Forutsetning: start-sim.sh må kjøre i egen terminal.
#
# Bruk:
#   ./scripts/start-webapp.sh [options]
#
# Opsjoner:
#   --port=<port>     Web-port (default: 8080)
#   --log-level=DEBUG Detaljert logging
#   --help            Vis hjelp
#
# GUI tilgjengelig på:
#   http://localhost:8080         - Dashboard
#   http://localhost:8080/control - Kontrollpanel med live-logging
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Konfigurasjon
WEB_PORT=8080
LOG_LEVEL=INFO
PTY_APP="/tmp/ttyV1"

# JAR-sti
WEBAPP_JAR="$PROJECT_ROOT/release/lpg-ehl-webapp.jar"

# Farger
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_header() { echo -e "\n${BLUE}═══════════════════════════════════════════════${NC}\n  ${BLUE}$1${NC}\n${BLUE}═══════════════════════════════════════════════${NC}"; }

show_help() {
    sed -n '2,20p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse argumenter
while [[ $# -gt 0 ]]; do
    case $1 in
        --port=*) WEB_PORT="${1#*=}"; shift ;;
        --log-level=*) LOG_LEVEL="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Ukjent opsjon: $1"; show_help ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────

log_header "START WEBAPP (SOCAT MODE)"

# Sjekk at JAR finnes
if [[ ! -f "$WEBAPP_JAR" ]]; then
    echo -e "${RED}FEIL: JAR ikke funnet: $WEBAPP_JAR${NC}"
    echo "Bygg med: mvn -q -DskipTests package"
    exit 1
fi

# Sjekk at PTY finnes (simulator må kjøre)
if [[ ! -e "$PTY_APP" ]]; then
    echo -e "${RED}FEIL: PTY ikke funnet: $PTY_APP${NC}"
    echo "Start først: ./scripts/start-sim.sh"
    exit 1
fi

# Sjekk om database kjører
if ! docker ps 2>/dev/null | grep -q "postgres\|lpg"; then
    echo -e "${YELLOW}ADVARSEL: Ingen PostgreSQL container funnet${NC}"
    echo "Start database: docker-compose -f docker-compose-local.yaml up -d"
    echo ""
fi

log_info "Transport: SOCAT"
log_info "Serial port: $PTY_APP"
log_info "Web port: $WEB_PORT"
log_info "Log level: $LOG_LEVEL"
echo ""

log_info "Starter webapp..."
echo ""
echo -e "  ${BLUE}GUI tilgjengelig på:${NC}"
echo -e "    Dashboard:     ${GREEN}http://localhost:$WEB_PORT${NC}"
echo -e "    Kontrollpanel: ${GREEN}http://localhost:$WEB_PORT/control${NC}"
echo -e "    WebSocket log: ${GREEN}ws://localhost:$WEB_PORT/ws/logs${NC}"
echo ""
echo -e "  ${YELLOW}Trykk Ctrl+C for å stoppe${NC}"
echo ""

# Kjør webapp
java -jar "$WEBAPP_JAR" \
    --ehl.transport.mode=SOCAT \
    --ehl.serial.port="$PTY_APP" \
    --server.port="$WEB_PORT" \
    --logging.level.no.cloudberries.lpg="$LOG_LEVEL" \
    --logging.file.name=./webapp.log
