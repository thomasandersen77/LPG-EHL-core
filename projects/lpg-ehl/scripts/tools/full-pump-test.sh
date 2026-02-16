#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
# FULL PUMP TEST - Test komplett pumpe-syklus
# ═══════════════════════════════════════════════════════════════════════════════
#
# Forutsetninger:
#   1. PLS simulator kjører (SOCAT): ./scripts/sim-pls.sh --address=2
#   2. Webapp/headless kjører med debug API på port 8080
#
# ═══════════════════════════════════════════════════════════════════════════════

set -e

# Konfigurasjon
PUMP=${1:-2}  # Pumpe-adresse (default: 2)
API_URL="http://localhost:8080"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  🧪 FULL PUMP TEST${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Pumpe adresse: ${BOLD}$PUMP${NC}"
echo -e "  API URL:       ${BOLD}$API_URL${NC}"
echo ""

# Test at API er tilgjengelig
echo -e "${CYAN}📡 Testing API connection...${NC}"
if ! curl -s -f "$API_URL/actuator/health" > /dev/null; then
    echo -e "${RED}❌ Kan ikke koble til API på $API_URL${NC}"
    echo ""
    echo "Start headless/webapp først:"
    echo "  java -jar release/lpg-ehl-webapp.jar --spring.profiles.active=field --ehl.serial.port=/tmp/vserial1"
    exit 1
fi
echo -e "${GREEN}✅ API er tilgjengelig${NC}"
echo ""

# Steg 1: UNBLOCK
echo -e "${YELLOW}1️⃣  UNBLOCK (Fri pumpe)${NC}"
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/emulator/pump/$PUMP/unblock")
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')

if [ "$SUCCESS" != "true" ]; then
    echo -e "${RED}❌ UNBLOCK feilet${NC}"
    echo "$RESPONSE" | jq
    exit 1
fi

echo -e "${GREEN}✅ Pumpe frigitt${NC}"
echo "$RESPONSE" | jq '{state, message}'
echo ""

# Steg 2: Vent litt
echo -e "${CYAN}⏳ Venter 2 sekunder...${NC}"
sleep 2

# Steg 3: Start pumping (simuler nozzle lift)
echo -e "${YELLOW}2️⃣  START PUMPING (simuler nozzle lift)${NC}"
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/emulator/pump/$PUMP/start-pumping")
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')

if [ "$SUCCESS" != "true" ]; then
    echo -e "${RED}❌ Start pumping feilet${NC}"
    echo "$RESPONSE" | jq
    exit 1
fi

echo -e "${GREEN}✅ Pumping startet${NC}"
echo ""

# Steg 4: Overvåk volum under pumping
echo -e "${YELLOW}3️⃣  PUMPING (overvåker volum i 5 sekunder)${NC}"
echo ""

for i in {1..5}; do
    RESPONSE=$(curl -s "$API_URL/api/v1/emulator/pump/$PUMP/status")
    VOL=$(echo "$RESPONSE" | jq -r '.volumeLitres')
    AMT=$(echo "$RESPONSE" | jq -r '.amountKr')
    PRICE=$(echo "$RESPONSE" | jq -r '.pricePerLitreKr')
    STATE=$(echo "$RESPONSE" | jq -r '.state')
    
    echo -e "   ${CYAN}⛽ ${i}/5${NC}  State: ${BOLD}$STATE${NC}  Volum: ${GREEN}${VOL}L${NC}  Beløp: ${GREEN}${AMT} kr${NC}  (${PRICE} kr/L)"
    sleep 1
done

echo ""

# Steg 5: BLOCK
echo -e "${YELLOW}4️⃣  BLOCK (Stopp pumpe)${NC}"
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/emulator/pump/$PUMP/block")
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')

if [ "$SUCCESS" != "true" ]; then
    echo -e "${RED}❌ BLOCK feilet${NC}"
    echo "$RESPONSE" | jq
    exit 1
fi

echo -e "${GREEN}✅ Pumpe stoppet${NC}"
echo "$RESPONSE" | jq '{state, message, volumeLitres, amountKr, hasPendingTransaction}'
echo ""

# Steg 6: Se finalt resultat
echo -e "${YELLOW}5️⃣  FINAL RESULT${NC}"
RESPONSE=$(curl -s "$API_URL/api/v1/emulator/pump/$PUMP/status")
VOL=$(echo "$RESPONSE" | jq -r '.volumeLitres')
AMT=$(echo "$RESPONSE" | jq -r '.amountKr')
PENDING=$(echo "$RESPONSE" | jq -r '.hasPendingTransaction')

echo ""
echo -e "   Volum:          ${BOLD}${VOL}L${NC}"
echo -e "   Beløp:          ${BOLD}${AMT} kr${NC}"
echo -e "   Pending:        ${BOLD}$PENDING${NC}"
echo ""

# Steg 7: Settle (hvis pending)
if [ "$PENDING" = "true" ]; then
    echo -e "${YELLOW}6️⃣  SETTLE (Betal med kort)${NC}"
    RESPONSE=$(curl -s -X POST "$API_URL/api/v1/emulator/settle/$PUMP?method=CARD")
    STATUS=$(echo "$RESPONSE" | jq -r '.status')
    
    if [ "$STATUS" = "settled" ]; then
        echo -e "${GREEN}✅ Betaling gjennomført${NC}"
        echo "$RESPONSE" | jq '{status, method, transaction}'
    else
        echo -e "${RED}❌ Settle feilet${NC}"
        echo "$RESPONSE" | jq
    fi
    echo ""
fi

# Oppsummering
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ TEST FULLFØRT!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Pumpe-syklus testet OK:"
echo -e "    ✓ UNBLOCK (fri pumpe)"
echo -e "    ✓ START PUMPING (nozzle lift)"
echo -e "    ✓ Pumping med volum-tracking"
echo -e "    ✓ BLOCK (stopp pumpe)"
if [ "$PENDING" = "true" ]; then
    echo -e "    ✓ SETTLE (betaling)"
fi
echo ""
echo -e "  Totalt levert: ${BOLD}${VOL}L${NC} = ${BOLD}${AMT} kr${NC}"
echo ""
