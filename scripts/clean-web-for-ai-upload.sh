#!/bin/bash
set -e

# Clean Web for AI Upload
# Removes build artifacts and dependencies to prepare for upload to Gemini/AI tools
# Keeps only source code and configuration files

WEB_DIR="/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-web"

echo "🧹 Cleaning lpg-web for AI upload..."
echo "📁 Target: $WEB_DIR"
echo ""

# Count files before cleanup
BEFORE=$(find "$WEB_DIR" -type f | wc -l | tr -d ' ')
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
AFTER=$(find "$WEB_DIR" -type f | wc -l | tr -d ' ')
echo ""
echo "📊 Files after cleanup: $AFTER"
echo "♻️  Removed: $((BEFORE - AFTER)) files"
echo ""
echo "✨ Cleanup complete!"
echo ""
echo "📦 Remaining structure includes:"
echo "   - Source code (src/)"
echo "   - Configuration files (vite.config.ts, tsconfig.json, etc.)"
echo "   - OpenAPI spec (openapi.yaml)"
echo "   - Package definition (package.json)"
echo ""
echo "🚀 Ready for upload to Gemini or other AI tools"
