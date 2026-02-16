#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# H2 Console - Standalone database browser
#═══════════════════════════════════════════════════════════════════════
#
# Usage: ./scripts/tools/h2-console.sh
#
# Opens H2 web console for browsing the lpg-ehl database without
# starting the main application.
#
#═══════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}"")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  💾 H2 Database Console${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Check if database exists
if [[ ! -f "./data/lpgdb.mv.db" ]]; then
    echo -e "${YELLOW}⚠️  Database file not found: ./data/lpgdb.mv.db${NC}"
    echo -e "${YELLOW}   Database will be created on first connection${NC}"
    echo ""
fi

# Find H2 JAR (exclude sources and other classifiers)
H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" 2>/dev/null | grep -v "sources\|javadoc\|tests" | sort -V | tail -1)

if [[ -z "$H2_JAR" ]]; then
    echo -e "${YELLOW}H2 JAR not found in Maven repository${NC}"
    echo -e "${YELLOW}Attempting to download...${NC}"
    mvn dependency:get -Dartifact=com.h2database:h2:2.2.224 -q
    H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" 2>/dev/null | grep -v "sources\|javadoc\|tests" | sort -V | tail -1)
fi

if [[ -z "$H2_JAR" ]]; then
    echo -e "${RED}❌ Could not find or download H2 JAR${NC}"
    exit 1
fi

echo -e "${GREEN}Using H2 JAR:${NC} $H2_JAR"
echo ""
echo -e "${BOLD}Connection Settings:${NC}"
echo -e "  ${CYAN}JDBC URL:${NC}  jdbc:h2:file:./data/lpgdb"
echo -e "  ${CYAN}User:${NC}      SA"
echo -e "  ${CYAN}Password:${NC}  ${YELLOW}(leave empty)${NC}"
echo ""
echo -e "${BOLD}IntelliJ Connection String:${NC}"
echo -e "  ${CYAN}jdbc:h2:file:${SCRIPT_DIR}/data/lpgdb${NC}"
echo ""
echo -e "${GREEN}Console will open in browser:${NC} ${BOLD}http://localhost:8082${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Start H2 console
java -cp "$H2_JAR" org.h2.tools.Console -web -webPort 8082
