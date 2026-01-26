#!/bin/bash
# =============================================================================
# Build Fat JAR with Frontend
# =============================================================================
# This script:
# 1. Builds the React frontend
# 2. Copies static files to emulator's resources
# 3. Builds the Fat JAR with Maven
# =============================================================================

set -euo pipefail  # Exit on error, undefined vars, and pipe failures
IFS=$'\n\t'        # Safe field separator

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/lpg-web"
EMULATOR_DIR="$PROJECT_ROOT/lpg-ehl-emulator"
STATIC_DIR="$EMULATOR_DIR/src/main/resources/static"

echo "=============================================="
echo "🔨 Building LPG-EHL Edge Fat JAR"
echo "=============================================="

# Step 1: Build Frontend
echo ""
echo "📦 Step 1: Building React frontend..."
cd "$FRONTEND_DIR"
npm install --silent
npm run build

# Step 2: Copy to static resources
echo ""
echo "📁 Step 2: Copying frontend to static resources..."
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR" || { echo "❌ Failed to create $STATIC_DIR"; exit 1; }
if [ ! -d "$FRONTEND_DIR/dist" ]; then
    echo "❌ Frontend build output not found at $FRONTEND_DIR/dist"
    exit 1
fi
cp -r "$FRONTEND_DIR/dist/"* "$STATIC_DIR/"
echo "   Copied $(find "$STATIC_DIR" -type f | wc -l | tr -d ' ') files"

# Step 3: Build Maven project
echo ""
echo "🔧 Step 3: Building Maven project..."
cd "$PROJECT_ROOT"
./mvnw clean package -DskipTests -q

# Step 4: Show result
echo ""
echo "=============================================="
echo "✅ Build complete!"
echo "=============================================="

# List all built JARs
echo ""
echo "📦 Built JAR files:"
find "$PROJECT_ROOT" -path "*/target/*.jar" -not -name "*-sources.jar" -not -name "original-*" 2>/dev/null | while read -r jar; do
    jar_size=$(du -h "$jar" | cut -f1)
    echo "   $jar_size  $(basename "$jar")"
done

# Show main emulator JAR for quick reference
JAR_FILE=$(find "$EMULATOR_DIR/target" -name "*.jar" -not -name "*-sources.jar" -not -name "original-*" 2>/dev/null | head -1)
if [ -f "$JAR_FILE" ]; then
    echo ""
    echo "🚀 To run emulator:"
    echo "   java -jar $JAR_FILE"
fi
