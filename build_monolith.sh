#!/bin/bash
# build_monolith.sh - Build LPG-EHL Monolith (Spring Boot + React)
#
# This script creates a single executable JAR file containing:
# - lpg-ehl-core (Kotlin protocol implementation)
# - lpg-ehl-api (Spring Boot REST API)
# - lpg-web (React frontend as static files)
#
# Output: release/lpg-ehl-monolith-VERSION.jar
#
# Usage:
#   ./build_monolith.sh
#
# Requirements:
#   - Node.js 18+ (for React build)
#   - Maven 3.8+ (for Spring Boot build)
#   - Java 21 (Temurin recommended)

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  LPG-EHL Monolith Build${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Build React Frontend
echo -e "${GREEN}[1/5]${NC} Building React frontend (lpg-web)..."
cd lpg-web

if [ ! -d "node_modules" ]; then
    echo "  Installing npm dependencies..."
    npm install
else
    echo "  Dependencies already installed (skipping npm install)"
fi

echo "  Running production build..."
npm run build

# Verify build output
if [ ! -d "dist" ]; then
    echo -e "${RED}ERROR: Frontend build failed - dist/ directory not found${NC}"
    exit 1
fi

echo -e "  ${GREEN}✓${NC} Frontend built successfully"
echo ""

# Step 2: Clean old static files
echo -e "${GREEN}[2/5]${NC} Cleaning old static files from Spring Boot..."
cd "$SCRIPT_DIR"
STATIC_DIR="lpg-ehl-api/src/main/resources/static"

if [ -d "$STATIC_DIR" ]; then
    echo "  Removing $STATIC_DIR/*"
    rm -rf "$STATIC_DIR"/*
else
    echo "  Creating $STATIC_DIR"
    mkdir -p "$STATIC_DIR"
fi

echo -e "  ${GREEN}✓${NC} Static directory cleaned"
echo ""

# Step 3: Copy React build to Spring Boot static resources
echo -e "${GREEN}[3/5]${NC} Copying React build to Spring Boot static resources..."
echo "  Source: lpg-web/dist/"
echo "  Target: $STATIC_DIR/"

cp -r lpg-web/dist/* "$STATIC_DIR/"

# Verify copy
FILE_COUNT=$(find "$STATIC_DIR" -type f | wc -l | xargs)
echo -e "  ${GREEN}✓${NC} Copied $FILE_COUNT files"
echo ""

# Step 4: Build Spring Boot Fat JAR
echo -e "${GREEN}[4/5]${NC} Building Spring Boot monolith JAR..."
cd lpg-ehl-api

echo "  Running Maven package (skipping tests for faster build)..."
./mvnw clean package -DskipTests

# Find the built JAR
JAR_FILE=$(find target -name "lpg-ehl-api-*.jar" -not -name "*-plain.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found in target/${NC}"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "  ${GREEN}✓${NC} JAR built successfully: $(basename "$JAR_FILE") ($JAR_SIZE)"
echo ""

# Step 5: Move JAR to release directory
echo -e "${GREEN}[5/5]${NC} Preparing release..."
cd "$SCRIPT_DIR"

RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Extract version from pom.xml
VERSION=$(grep -m1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs)
RELEASE_JAR="$RELEASE_DIR/lpg-ehl-monolith-$VERSION.jar"

# Copy JAR to release with better naming
cp "$JAR_FILE" "$RELEASE_JAR"

echo -e "  ${GREEN}✓${NC} Release JAR created"
echo ""

# Build summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Build Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Release Information:${NC}"
echo "  File:     $(basename "$RELEASE_JAR")"
echo "  Location: $RELEASE_JAR"
echo "  Size:     $(du -h "$RELEASE_JAR" | cut -f1)"
echo "  Version:  $VERSION"
echo ""
echo -e "${YELLOW}What's included:${NC}"
echo "  ✓ lpg-ehl-core (Kotlin protocol implementation)"
echo "  ✓ lpg-ehl-api (Spring Boot REST API)"
echo "  ✓ lpg-web (React frontend)"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${YELLOW}How to run:${NC}"
echo "  Local test:"
echo "    java -jar $RELEASE_JAR"
echo "    # or"
echo "    ./$RELEASE_JAR  # (executable on Linux)"
echo ""
echo "  Deploy to ARK machine:"
echo "    scp $RELEASE_JAR user@ark-machine:/opt/lpg-ehl/"
echo "    ssh user@ark-machine"
echo "    sudo systemctl stop lpg-ehl"
echo "    sudo cp /opt/lpg-ehl/lpg-ehl-monolith-$VERSION.jar /opt/lpg-ehl/lpg-ehl.jar"
echo "    sudo systemctl start lpg-ehl"
echo ""
echo -e "${YELLOW}IntelliJ Development:${NC}"
echo "  Run LpgEhlApiApplication in IntelliJ to test the monolith locally."
echo "  Frontend will be served at: http://localhost:8080"
echo "  API endpoints at: http://localhost:8080/api/*"
echo "  Swagger UI at: http://localhost:8080/swagger-ui.html"
echo ""
echo -e "${GREEN}Ready for deployment! 🚀${NC}"
