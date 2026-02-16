#!/bin/bash
set -e

# Clean Web for AI Upload
# Removes build artifacts and dependencies, then creates a zip for AI upload
# Keeps only source code and configuration files

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WEB_DIR="$PROJECT_ROOT/projects/lpg-ehl/lpg-web"
OUTPUT_DIR="$PROJECT_ROOT/ai-exports"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

mkdir -p "$OUTPUT_DIR"

echo "🧹 Cleaning lpg-web for AI upload..."
echo "📁 Target: $WEB_DIR"
echo ""

# Count files before cleanup
BEFORE=$(find "$WEB_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
echo "📊 Files before cleanup: $BEFORE"
echo ""

# Remove node_modules
if [ -d "$WEB_DIR/node_modules" ]; then
    echo "🗑️  Removing node_modules..."
    rm -rf "$WEB_DIR/node_modules"
    echo "✅ node_modules removed"
fi

# Remove dist
if [ -d "$WEB_DIR/dist" ]; then
    echo "🗑️  Removing dist..."
    rm -rf "$WEB_DIR/dist"
    echo "✅ dist removed"
fi

# Remove package-lock.json (can be regenerated)
if [ -f "$WEB_DIR/package-lock.json" ]; then
    echo "🗑️  Removing package-lock.json..."
    rm -f "$WEB_DIR/package-lock.json"
    echo "✅ package-lock.json removed"
fi

# Remove any .vite or build cache directories
if [ -d "$WEB_DIR/.vite" ]; then
    echo "🗑️  Removing .vite cache..."
    rm -rf "$WEB_DIR/.vite"
    echo "✅ .vite cache removed"
fi

# Count files after cleanup
AFTER=$(find "$WEB_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
echo ""
echo "📊 Files after cleanup: $AFTER"
echo "♻️  Removed: $((BEFORE - AFTER)) files"
echo ""

# Create zip file
OUTPUT_FILE="$OUTPUT_DIR/lpg-web-${TIMESTAMP}.zip"
echo "📦 Creating zip archive..."

cd "$WEB_DIR"
zip -q -r "$OUTPUT_FILE" . \
    -x "*.git/*" \
    -x "*.DS_Store" \
    -x "*~" \
    -x "*.swp"
cd "$PROJECT_ROOT"

SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
echo "✅ Created: $(basename "$OUTPUT_FILE") ($SIZE)"

echo ""
echo "✨ Cleanup and zip complete!"
echo ""
echo "📦 Archive location: $OUTPUT_FILE"
echo ""
echo "📋 Contents include:"
echo "   - Source code (src/)"
echo "   - Configuration (vite.config.ts, tsconfig.json, tailwind.config.js)"
echo "   - OpenAPI spec (openapi.yaml)"
echo "   - Package definition (package.json)"
echo "   - README.md"
echo ""
echo "🚀 Ready for upload to Gemini or other AI tools"
