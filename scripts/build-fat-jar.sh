#!/bin/bash
# =============================================================================
# Build Fat JAR with Frontend
# =============================================================================
# This script:
# 1. Builds the React frontend
# 2. Copies static files to emulator's resources
# 3. Builds the Fat JAR with Maven
# =============================================================================

set -e  # Exit on error

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
mkdir -p "$STATIC_DIR"
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
JAR_FILE=$(find "$EMULATOR_DIR/target" -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo "📦 JAR: $JAR_FILE"
    echo "📏 Size: $JAR_SIZE"
    echo ""
    echo "🚀 To run:"
    echo "   java -jar $JAR_FILE"
fi
