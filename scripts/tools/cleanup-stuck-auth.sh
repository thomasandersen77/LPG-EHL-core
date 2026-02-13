#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# Cleanup Stuck Authorizations
#═══════════════════════════════════════════════════════════════════════
#
# Fjerner autorisasjoner som har hengt seg fra tidligere kjøringer.
# Kjør dette hvis du får "har allerede aktiv autorisasjon" feilmeldinger.
#
# Usage: ./scripts/tools/cleanup-stuck-auth.sh
#
#═══════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}"")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  🧹 Cleanup Stuck Authorizations${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Check if database exists
if [[ ! -f "./data/lpgdb.mv.db" ]]; then
    echo -e "${RED}❌ Database file not found: ./data/lpgdb.mv.db${NC}"
    echo -e "${YELLOW}   Nothing to clean up.${NC}"
    exit 0
fi

# Find H2 JAR (exclude sources and other classifiers)
H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" 2>/dev/null | grep -v "sources\|javadoc\|tests" | sort -V | tail -1)

if [[ -z "$H2_JAR" ]]; then
    echo -e "${RED}❌ H2 JAR not found in Maven repository${NC}"
    exit 1
fi

echo -e "${GREEN}Using H2 JAR:${NC} $H2_JAR"
echo ""

# SQL cleanup script
SQL_SCRIPT="
-- Show current stuck authorizations
SELECT 
    id, 
    dispenser_address, 
    status, 
    created_at,
    TIMESTAMPDIFF(SECOND, created_at, CURRENT_TIMESTAMP) as age_seconds
FROM pump_authorization 
WHERE status IN ('AUTHORIZED', 'PUMPING', 'PENDING')
ORDER BY created_at DESC;

-- Delete all active/stuck authorizations
DELETE FROM pump_authorization 
WHERE status IN ('AUTHORIZED', 'PUMPING', 'PENDING');

-- Show count of deleted records
SELECT '✅ Deleted authorizations' as message;
"

echo -e "${YELLOW}Executing cleanup SQL...${NC}"
echo ""

# Execute SQL using H2 Shell in non-interactive mode
echo "$SQL_SCRIPT" | java -cp "$H2_JAR" org.h2.tools.Shell \
    -url "jdbc:h2:file:./data/lpgdb" \
    -user SA \
    -password ""

echo ""
echo -e "${GREEN}✅ Cleanup completed successfully!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
