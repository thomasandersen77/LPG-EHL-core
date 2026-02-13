#!/bin/bash
# zip-modules-for-ai.sh - Export each module as separate zip for AI analysis
#
# Auto-discovers all Maven modules (pom.xml) and the React frontend.
# Only includes source code, config files, and README.
#
# Usage: ./scripts/zip-modules-for-ai.sh
# Output: Separate zip files per module in ai-exports/

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIR="$PROJECT_ROOT/ai-exports"

cd "$PROJECT_ROOT"

mkdir -p "$OUTPUT_DIR"

echo "🗜️  Creating AI-friendly module archives..."
echo "📦 Timestamp: $TIMESTAMP"
echo "📁 Output: $OUTPUT_DIR"
echo ""

# Zip a Maven module: only src/, pom.xml, config files, and README
zip_maven_module() {
    local module_dir=$1
    local module_name=$(basename "$module_dir")
    local output_file="$OUTPUT_DIR/${module_name}-${TIMESTAMP}.zip"

    if [ ! -d "$module_dir" ]; then
        echo "   ⚠️  Skipping $module_name (not found)"
        return
    fi

    echo "📦 Zipping $module_name..."

    cd "$module_dir"
    zip -q -r "$output_file" . \
        -i "src/*" \
        -i "pom.xml" \
        -i "README.md" \
        -i "*.yaml" \
        -i "*.yml" \
        -i "*.properties"
    cd "$PROJECT_ROOT"

    local size=$(du -h "$output_file" | cut -f1)
    echo "   ✅ $(basename "$output_file") ($size)"
}

# Zip the React frontend: only src/, config files, and README
zip_react_frontend() {
    local module_dir=$1
    local module_name=$(basename "$module_dir")
    local output_file="$OUTPUT_DIR/${module_name}-${TIMESTAMP}.zip"

    if [ ! -d "$module_dir" ]; then
        echo "   ⚠️  Skipping $module_name (not found)"
        return
    fi

    echo "📦 Zipping $module_name..."

    cd "$module_dir"
    zip -q -r "$output_file" . \
        -i "src/*" \
        -i "public/*" \
        -i "package.json" \
        -i "tsconfig*.json" \
        -i "vite.config.*" \
        -i "index.html" \
        -i "README.md" \
        -i "*.yaml" \
        -i "*.yml"
    cd "$PROJECT_ROOT"

    local size=$(du -h "$output_file" | cut -f1)
    echo "   ✅ $(basename "$output_file") ($size)"
}

# --- Auto-discover and zip all Maven modules ---
echo "=== Maven Modules ==="
for pom in */pom.xml; do
    module_dir="$(dirname "$pom")"
    zip_maven_module "$module_dir"
done

# --- React frontend ---
if [ -d "lpg-web" ]; then
    echo ""
    echo "=== React Frontend ==="
    zip_react_frontend "lpg-web"
fi

# --- Root-level project files ---
echo ""
echo "=== Root Project ==="
ROOT_ZIP="$OUTPUT_DIR/lpg-ehl-root-${TIMESTAMP}.zip"
zip -q "$ROOT_ZIP" pom.xml README.md 2>/dev/null || true
if [ -f "$ROOT_ZIP" ]; then
    local_size=$(du -h "$ROOT_ZIP" | cut -f1)
    echo "   ✅ $(basename "$ROOT_ZIP") ($local_size)"
else
    echo "   ⚠️  No root files found"
fi

echo ""
echo "📋 Created archives:"
ls -lh "$OUTPUT_DIR"/*-${TIMESTAMP}.zip 2>/dev/null | awk '{print "   " $9 " (" $5 ")"}' || echo "   No files created"

echo ""
echo "✅ Done! Archives in $OUTPUT_DIR"
echo "⚡ Each contains only source code, configs, and README - no build artifacts."
