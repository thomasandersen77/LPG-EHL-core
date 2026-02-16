#!/usr/bin/env bash
#═══════════════════════════════════════════════════════════════════════
# BUILD WEBAPP
#═══════════════════════════════════════════════════════════════════════
#
# Builds ONLY:
#   - React frontend (lpg-web) -> copied into lpg-ehl-webapp/src/main/resources/static
#   - lpg-ehl-webapp jar -> release/lpg-ehl-webapp.jar
#
# Usage:
#   ./scripts/build-webapp.sh
#   ./scripts/build-webapp.sh --skip-tests
#   ./scripts/build-webapp.sh --verbose
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

SKIP_TESTS=true
VERBOSE=false

for arg in "$@"; do
  case $arg in
    --skip-tests) SKIP_TESTS=true ;;
    --with-tests) SKIP_TESTS=false ;;
    --verbose) VERBOSE=true ;;
  esac
done

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🔨 Build webapp${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"

# Step 1: Build React
FRONTEND_DIR="$PROJECT_ROOT/projects/lpg-ehl/lpg-web"
WEBAPP_STATIC="$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-webapp/src/main/resources/static"

if [ ! -d "$FRONTEND_DIR" ]; then
  echo -e "${RED}Missing frontend dir: $FRONTEND_DIR${NC}"
  exit 1
fi

echo ""
echo -e "${GRAY}[1/2]${NC} Building React frontend..."
(
  cd "$FRONTEND_DIR"
  npm install --silent > /dev/null 2>&1 || npm install > /dev/null 2>&1
  npm run build > /dev/null 2>&1
)

rm -rf "$WEBAPP_STATIC"/* 2>/dev/null || true
mkdir -p "$WEBAPP_STATIC"
cp -r "$FRONTEND_DIR/dist/"* "$WEBAPP_STATIC/"
STATIC_FILES=$(find "$WEBAPP_STATIC" -type f | wc -l | tr -d ' ')
echo -e "${GREEN}✓${NC} Copied static files (${STATIC_FILES})"

# Step 2: Maven package webapp
echo ""
echo -e "${GRAY}[2/2]${NC} Packaging lpg-ehl-webapp..."

MVN_ARGS=(clean package -pl projects/lpg-ehl/lpg-ehl-webapp -am)
if [[ "$SKIP_TESTS" == "true" ]]; then
  MVN_ARGS+=(-DskipTests)
fi
if [[ "$VERBOSE" == "false" ]]; then
  MVN_ARGS+=(-q)
fi

./mvnw "${MVN_ARGS[@]}"

WEBAPP_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-webapp/target" -name "lpg-ehl-webapp-*.jar" -not -name "*-plain.jar" | head -1)
if [ -z "$WEBAPP_JAR" ] || [ ! -f "$WEBAPP_JAR" ]; then
  echo -e "${RED}✗ Could not find WebApp JAR in projects/lpg-ehl/lpg-ehl-webapp/target${NC}"
  exit 1
fi

RELEASE_DIR="$PROJECT_ROOT/release"
mkdir -p "$RELEASE_DIR"
cp "$WEBAPP_JAR" "$RELEASE_DIR/lpg-ehl-webapp.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-webapp.jar"

echo ""
echo -e "${GREEN}✓ Done${NC}"
ls -lh "$RELEASE_DIR/lpg-ehl-webapp.jar" | awk '{print "  " $9 " (" $5 ")"}'
