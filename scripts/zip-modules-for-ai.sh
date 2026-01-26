#!/bin/bash
# zip-modules-for-ai.sh - Export each module as separate zip for AI analysis
# 
# Usage: ./scripts/zip-modules-for-ai.sh
# Output: Separate zip files for each Maven module + React frontend

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIR="$PROJECT_ROOT/ai-exports"

cd "$PROJECT_ROOT"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "🗜️  Creating AI-friendly module archives..."
echo "📦 Timestamp: $TIMESTAMP"
echo "📁 Output: $OUTPUT_DIR"
echo ""

# Function to zip a module
zip_module() {
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
        -x "*.git/*" \
        -x "*target/*" \
        -x "*node_modules/*" \
        -x "*dist/*" \
        -x "*build/*" \
        -x "*.idea/*" \
        -x "*.vscode/*" \
        -x "*.DS_Store" \
        -x "*.class" \
        -x "*.jar" \
        -x "*.war" \
        -x "*.log" \
        -x "*.tmp" \
        -x "*~" \
        -x "*.swp" \
        -x "*.iml"
    
    cd "$PROJECT_ROOT"
    
    local size=$(du -h "$output_file" | cut -f1)
    echo "   ✅ $(basename "$output_file") ($size)"
}

# Zip all Maven modules (Kotlin/Java)
echo "=== Maven Modules ==="
zip_module "lpg-ehl-core"
zip_module "lpg-ehl-service"
zip_module "lpg-transport"
zip_module "lpg-ehl-emulator"
zip_module "lpg-ehl-serialport-sim"
zip_module "lpg-ehl-webapp"
zip_module "lpg-ehl-app-headless"
zip_module "lpg-ehl-cli"

echo ""
echo "=== React Frontend ==="
zip_module "lpg-web"

echo ""
echo "📋 Created archives:"
ls -lh "$OUTPUT_DIR"/*-${TIMESTAMP}.zip 2>/dev/null | awk '{print "   " $9 " (" $5 ")"}' || echo "   No files created"

echo ""
echo "✅ Done! Module archives created in $OUTPUT_DIR:"
echo ""
echo "   Maven Modules (Kotlin):"
echo "   • lpg-ehl-core          - EHL protocol (NO Spring dependencies)"
echo "   • lpg-ehl-service       - Business logic + JPA + Liquibase migrations"
echo "   • lpg-transport         - Serial/TCP transport layer"
echo "   • lpg-ehl-emulator      - LAB mode dispenser simulator"
echo "   • lpg-ehl-serialport-sim- Serial port simulator"
echo "   • lpg-ehl-webapp        - Web API + React frontend (thin wrapper)"
echo "   • lpg-ehl-app-headless  - Headless production (no web server)"
echo "   • lpg-ehl-cli           - Spring Shell CLI"
echo ""
echo "   React Frontend (TypeScript):"
echo "   • lpg-web               - Vite + React + TanStack Query"
echo ""
echo "⚡ Each contains only source code, configs, and docs - no build artifacts."
