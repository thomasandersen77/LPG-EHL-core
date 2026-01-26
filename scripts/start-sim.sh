#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# START SOCAT + PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════════════
#
# Starter socat og PLS Simulator. App må startes separat.
#
# Bruk:
#   ./scripts/start-sim.sh [options]
#
# Opsjoner:
#   --address=<1-8>   Dispenser-adresse (default: 1)
#   --price=<cents>   Pris i øre, f.eks. 1590 = 15.90 kr/L (default: 1590)
#   --blocked=<bool>  Start blokkert (default: true)
#   --help            Vis hjelp
#
# Etter oppstart, start app i egen terminal:
#   java -jar release/lpg-ehl-headless.jar --ehl.transport.mode=SOCAT --ehl.serial.port=/tmp/ttyV1
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default konfigurasjon
DISPENSER_ADDRESS=1
PRICE_CENTS=1590
BLOCKED=true

# PTY-stier
PTY_SIM="/tmp/ttyV0"    # Simulator kobler her
PTY_APP="/tmp/ttyV1"    # App kobler her

# JAR-sti
SIM_JAR="$PROJECT_ROOT/release/pls-sim.jar"

# Farger
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_header() { echo -e "\n${BLUE}═══════════════════════════════════════════════${NC}\n  ${BLUE}$1${NC}\n${BLUE}═══════════════════════════════════════════════${NC}"; }

show_help() {
    sed -n '2,19p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

cleanup() {
    echo -e "\n${YELLOW}Stopper...${NC}"
    [[ -n "$SIM_PID" ]] && kill "$SIM_PID" 2>/dev/null || true
    [[ -n "$SOCAT_PID" ]] && kill "$SOCAT_PID" 2>/dev/null || true
    rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true
    echo -e "${GREEN}Stoppet.${NC}"
}
trap cleanup EXIT INT TERM

# Parse argumenter
while [[ $# -gt 0 ]]; do
    case $1 in
        --address=*) DISPENSER_ADDRESS="${1#*=}"; shift ;;
        --price=*) PRICE_CENTS="${1#*=}"; shift ;;
        --blocked=*) BLOCKED="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Ukjent opsjon: $1"; show_help ;;
    esac
done

#───────────────────────────────────────────────────────────────────────────────

log_header "START SOCAT + SIMULATOR"

# Sjekk avhengigheter
if ! command -v socat &> /dev/null; then
    echo -e "${RED}FEIL: socat er ikke installert${NC}"
    echo "Installer med: brew install socat"
    exit 1
fi

if [[ ! -f "$SIM_JAR" ]]; then
    echo -e "${RED}FEIL: JAR ikke funnet: $SIM_JAR${NC}"
    echo "Bygg med: mvn -q -DskipTests package"
    exit 1
fi

# Fjern gamle PTY-er
rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true

# Start socat
log_info "Starter socat..."
socat -d -d pty,raw,echo=0,link="$PTY_SIM" pty,raw,echo=0,link="$PTY_APP" 2>&1 &
SOCAT_PID=$!
sleep 1

if [[ ! -e "$PTY_SIM" ]] || [[ ! -e "$PTY_APP" ]]; then
    echo -e "${RED}FEIL: Kunne ikke opprette PTY-par${NC}"
    exit 1
fi
log_info "✅ socat kjører (PID: $SOCAT_PID)"

# Start simulator
log_info "Starter PLS Simulator..."
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
    echo -e "${RED}FEIL: Simulator feilet oppstart${NC}"
    exit 1
fi
log_info "✅ Simulator kjører (PID: $SIM_PID)"

#───────────────────────────────────────────────────────────────────────────────

log_header "KLAR FOR TILKOBLING"

echo ""
echo -e "  ${GREEN}Simulator:${NC}  $PTY_SIM"
echo -e "  ${GREEN}App-port:${NC}   $PTY_APP"
echo -e "  ${GREEN}Pris:${NC}       $(echo "scale=2; $PRICE_CENTS / 100" | bc) kr/L"
echo -e "  ${GREEN}Blokkert:${NC}   $BLOCKED"
echo ""
echo -e "  ${BLUE}Start app i ny terminal:${NC}"
echo ""
echo -e "  ${YELLOW}# Headless (uten web):${NC}"
echo -e "  java -jar release/lpg-ehl-headless.jar \\"
echo -e "    --ehl.transport.mode=SOCAT \\"
echo -e "    --ehl.serial.port=$PTY_APP \\"
echo -e "    --logging.file.name=./headless.log"
echo ""
echo -e "  ${YELLOW}# Headless med debug API (curl):${NC}"
echo -e "  java -jar release/lpg-ehl-headless.jar \\"
echo -e "    --spring.profiles.active=debug-api \\"
echo -e "    --ehl.transport.mode=SOCAT \\"
echo -e "    --ehl.serial.port=$PTY_APP \\"
echo -e "    --logging.file.name=./headless.log"
echo ""
echo -e "  ${YELLOW}# Full webapp med GUI:${NC}"
echo -e "  java -jar release/lpg-ehl-webapp.jar \\"
echo -e "    --ehl.transport.mode=SOCAT \\"
echo -e "    --ehl.serial.port=$PTY_APP"
echo ""
echo -e "  ${YELLOW}Trykk Ctrl+C for å stoppe${NC}"
echo ""

wait $SIM_PID
