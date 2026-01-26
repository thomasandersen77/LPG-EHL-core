#!/bin/bash
#
# felttest.sh - Komplett felttest for LPG-pumpe
#
# Bruk: ./felttest.sh [pumpe-adresse] [api-url]
#
# Eksempel:
#   ./felttest.sh              # Pumpe 1, localhost:8080
#   ./felttest.sh 2            # Pumpe 2, localhost:8080
#   ./felttest.sh 1 http://192.168.1.100:8080
#

set -e

# Konfigurasjon
PUMP=${1:-1}
API_BASE=${2:-"http://localhost:8080"}
API="$API_BASE/api/v1"

# Farger for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}$1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

echo "═══════════════════════════════════════════════════════════"
echo "   LPG PUMPE FELTTEST"
echo "   Pumpe: $PUMP"
echo "   API: $API"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Sjekk at API er tilgjengelig
print_step "🔌 Sjekker API-tilkobling..."
if curl -sf "$API_BASE/actuator/health" > /dev/null 2>&1; then
    print_success "API er tilgjengelig"
else
    print_error "Kunne ikke koble til API på $API_BASE"
    echo "   Sjekk at applikasjonen kjører"
    exit 1
fi
echo ""

# Steg 1: Sjekk pumpestatus
print_step "🔍 1. Sjekker pumpestatus..."
STATUS=$(curl -sf "$API/emulator/pump/$PUMP/status" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$STATUS" | python3 -m json.tool 2>/dev/null || echo "$STATUS"
    print_success "Status mottatt"
else
    print_error "Kunne ikke hente status"
    exit 1
fi
echo ""

# Steg 2: FRI PUMPE (Unblock)
print_step "🔓 2. FRI PUMPE - Frigir pumpen..."
UNBLOCK=$(curl -sf -X POST "$API/emulator/pump/$PUMP/unblock" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$UNBLOCK" | python3 -m json.tool 2>/dev/null || echo "$UNBLOCK"
    print_success "Pumpe frigitt!"
else
    print_error "Kunne ikke frigjøre pumpen"
fi
echo ""

# Vent på fylling
print_step "⛽ Pumpen er nå klar for fylling"
echo "   Løft pistolen og fyll drivstoff..."
echo ""
read -p "   Trykk ENTER når fylling er ferdig (eller Ctrl+C for å avbryte)..."
echo ""

# Steg 3: Stopp pumpe
print_step "🛑 3. Stopper pumpen..."
BLOCK=$(curl -sf -X POST "$API/emulator/pump/$PUMP/block" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$BLOCK" | python3 -m json.tool 2>/dev/null || echo "$BLOCK"
    print_success "Pumpe stoppet"
else
    print_error "Kunne ikke stoppe pumpen"
fi
echo ""

# Steg 4: Velg betalingsmetode
print_step "💳 4. Registrer betaling"
echo "   Velg betalingsmetode:"
echo "   1) CARD (standard)"
echo "   2) CASH"
echo "   3) CREDIT"
read -p "   Valg [1]: " PAYMENT_CHOICE

case $PAYMENT_CHOICE in
    2) PAYMENT_METHOD="CASH" ;;
    3) PAYMENT_METHOD="CREDIT" ;;
    *) PAYMENT_METHOD="CARD" ;;
esac

SETTLE=$(curl -sf -X POST "$API/emulator/settle/$PUMP?method=$PAYMENT_METHOD" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$SETTLE" | python3 -m json.tool 2>/dev/null || echo "$SETTLE"
    print_success "Betaling registrert med $PAYMENT_METHOD"
else
    print_error "Kunne ikke registrere betaling"
fi
echo ""

# Steg 5: Nullstill pumpe
print_step "🔄 5. Nullstiller pumpe..."
RESET=$(curl -sf -X POST "$API/emulator/pump/$PUMP/reset" 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$RESET" | python3 -m json.tool 2>/dev/null || echo "$RESET"
    print_success "Pumpe nullstilt"
else
    print_warning "Kunne ikke nullstille (kan være OK hvis allerede IDLE)"
fi
echo ""

# Steg 6: Bekreft endelig status
print_step "🔍 6. Verifiserer endelig status..."
FINAL_STATUS=$(curl -sf "$API/emulator/pump/$PUMP/status" 2>/dev/null)
echo "$FINAL_STATUS" | python3 -m json.tool 2>/dev/null || echo "$FINAL_STATUS"
echo ""

echo "═══════════════════════════════════════════════════════════"
print_success "FELTTEST FULLFØRT!"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📊 Neste steg:"
echo "   • Sjekk database: psql -h localhost -U lpg_user -d lpg_ehl"
echo "   • SQL: SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 5;"
echo ""
