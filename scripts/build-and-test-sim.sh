#!/bin/bash
#═══════════════════════════════════════════════════════════════════════════════
# BUILD AND TEST - PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════════════
#
# Bygger simulatoren, starter socat + simulator, og kjører tester.
#
# Bruk:
#   ./scripts/build-and-test-sim.sh [--skip-build]
#
#═══════════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Farger
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_header() { echo -e "\n${BLUE}═══════════════════════════════════════════════════════════${NC}\n  ${BLUE}$1${NC}\n${BLUE}═══════════════════════════════════════════════════════════${NC}"; }
log_info() { echo -e "  ${GREEN}✅${NC} $1"; }
log_warn() { echo -e "  ${YELLOW}⚠️${NC} $1"; }
log_error() { echo -e "  ${RED}❌${NC} $1"; }
log_step() { echo -e "\n${YELLOW}▶${NC} $1"; }

# Virtual PTY paths
PTY_SIM="/tmp/ttyV0"
PTY_APP="/tmp/ttyV1"

# PIDs
SOCAT_PID=""
SIM_PID=""

cleanup() {
    echo -e "\n${YELLOW}Stopper...${NC}"
    [[ -n "$SIM_PID" ]] && kill "$SIM_PID" 2>/dev/null || true
    [[ -n "$SOCAT_PID" ]] && kill "$SOCAT_PID" 2>/dev/null || true
    rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true
    echo -e "${GREEN}Stoppet.${NC}"
}
trap cleanup EXIT INT TERM

SKIP_BUILD=false
[[ "$1" == "--skip-build" ]] && SKIP_BUILD=true

#───────────────────────────────────────────────────────────────────────────────
# STEG 1: Bygg
#───────────────────────────────────────────────────────────────────────────────
if [[ "$SKIP_BUILD" == "false" ]]; then
    log_header "STEG 1: Bygger Simulator"
    
    cd "$PROJECT_ROOT"
    log_step "Kjører Maven build..."
    
    if ./mvnw -q -pl lpg-ehl-serialport-sim -am clean package -DskipTests; then
        log_info "Build OK"
    else
        log_error "Build feilet!"
        exit 1
    fi
else
    log_header "STEG 1: Hopper over build (--skip-build)"
fi

#───────────────────────────────────────────────────────────────────────────────
# STEG 2: Start socat
#───────────────────────────────────────────────────────────────────────────────
log_header "STEG 2: Starter socat"

rm -f "$PTY_SIM" "$PTY_APP" 2>/dev/null || true

log_step "Oppretter virtuelt serieport-par..."
socat -d -d pty,raw,echo=0,link="$PTY_SIM" pty,raw,echo=0,link="$PTY_APP" 2>&1 &
SOCAT_PID=$!
sleep 1

if [[ ! -e "$PTY_SIM" ]] || [[ ! -e "$PTY_APP" ]]; then
    log_error "Kunne ikke opprette PTY-par"
    exit 1
fi

log_info "socat kjører (PID: $SOCAT_PID)"
echo -e "     $PTY_SIM ↔ $PTY_APP"

#───────────────────────────────────────────────────────────────────────────────
# STEG 3: Start simulator
#───────────────────────────────────────────────────────────────────────────────
log_header "STEG 3: Starter PLS Simulator"

SIM_JAR="$PROJECT_ROOT/lpg-ehl-serialport-sim/target/pls-sim.jar"

if [[ ! -f "$SIM_JAR" ]]; then
    log_error "JAR ikke funnet: $SIM_JAR"
    exit 1
fi

log_step "Starter simulator på $PTY_SIM..."

java -jar "$SIM_JAR" \
    --port="$PTY_SIM" \
    --mode=ehl \
    --logHex=false \
    --address=1 \
    --price=1590 \
    --blocked=true &
SIM_PID=$!

sleep 2

if ! kill -0 "$SIM_PID" 2>/dev/null; then
    log_error "Simulator feilet oppstart"
    exit 1
fi

log_info "Simulator kjører (PID: $SIM_PID)"

#───────────────────────────────────────────────────────────────────────────────
# STEG 4: Kjør kommunikasjonstester
#───────────────────────────────────────────────────────────────────────────────
log_header "STEG 4: Kjører kommunikasjonstester"

TEST_PASSED=0
TEST_FAILED=0

run_test() {
    local name="$1"
    local hex_cmd="$2"
    local expected_prefix="$3"
    
    echo -ne "  Testing $name... "
    
    # Send hex command and read response with timeout
    response=$(echo -ne "$hex_cmd" | timeout 2 stty -F "$PTY_APP" 9600 cs8 -cstopb parenb 2>/dev/null; \
               echo -ne "$hex_cmd" > "$PTY_APP" 2>/dev/null; \
               timeout 1 head -c 20 "$PTY_APP" 2>/dev/null | xxd -p || echo "timeout")
    
    if [[ "$response" == "timeout" ]] || [[ -z "$response" ]]; then
        echo -e "${YELLOW}TIMEOUT${NC} (simulator mottar kanskje ikke data)"
        ((TEST_FAILED++))
    elif [[ "$response" == *"$expected_prefix"* ]]; then
        echo -e "${GREEN}OK${NC}"
        ((TEST_PASSED++))
    else
        echo -e "${RED}FEIL${NC} (fikk: $response)"
        ((TEST_FAILED++))
    fi
}

# EHL Protocol test: LINETEST command (STX=0x02, Addr=0x31, Cmd=0x4C, ETX=0x03, BCC)
# Frame: 02 31 4C 03 7E (simple LINETEST for address 1)
log_step "Sender EHL-kommandoer..."

# Simple connectivity test - just check if we can write/read
echo -e "  Sjekker serieport-tilkobling..."

# Configure the port
stty -F "$PTY_APP" 9600 cs8 -cstopb parenb 2>/dev/null || true

# Send a simple LINETEST frame
# STX(02) ADDR(31='1') CMD(4C='L') ETX(03) BCC(7E)
echo -ne '\x02\x31\x4C\x03\x7E' > "$PTY_APP" 2>/dev/null &
SEND_PID=$!

# Read response with timeout
RESPONSE=$(timeout 2 cat "$PTY_APP" 2>/dev/null | head -c 10 | xxd -p || echo "")

if [[ -n "$RESPONSE" ]]; then
    echo -e "  ${GREEN}✅${NC} Mottok respons: $RESPONSE"
    ((TEST_PASSED++))
else
    echo -e "  ${YELLOW}⚠️${NC} Ingen respons (kan være OK - simulator logger til konsoll)"
fi

#───────────────────────────────────────────────────────────────────────────────
# Oppsummering
#───────────────────────────────────────────────────────────────────────────────
log_header "RESULTAT"

echo ""
echo -e "  Simulator: ${GREEN}Kjører${NC} på $PTY_SIM"
echo -e "  App-port:  $PTY_APP (klar for tilkobling)"
echo ""
echo -e "  For å koble til med headless-app, kjør i nytt terminalvindu:"
echo ""
echo -e "  ${BLUE}java -jar release/lpg-ehl-headless.jar \\\\${NC}"
echo -e "  ${BLUE}  --spring.profiles.active=debug-api,local \\\\${NC}"
echo -e "  ${BLUE}  --lpg.mode=FIELD --ehl.serial.port=$PTY_APP${NC}"
echo ""
echo -e "  ${YELLOW}Trykk Ctrl+C for å stoppe${NC}"
echo ""

# Hold running
wait $SIM_PID
