#!/bin/bash
# zip-modules-for-ai.sh - Export each module as separate zip for AI analysis
# 
# Usage: ./scripts/zip-modules-for-ai.sh
# Output: Four separate zip files (core, api, emulator, web)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

cd "$PROJECT_ROOT"

echo "🗜️  Creating AI-friendly module archives..."
echo "📦 Timestamp: $TIMESTAMP"
echo ""

# Function to zip a module
zip_module() {
    local module_dir=$1
    local module_name=$(basename "$module_dir")
    local output_file="${module_name}-${TIMESTAMP}.zip"
    
    echo "📦 Zipping $module_name..."
    
    cd "$module_dir"
    zip -q -r "../$output_file" . \
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
        -x "*.swp"
    
    cd "$PROJECT_ROOT"
    
    echo "   ✅ $output_file ($(du -h "$output_file" | cut -f1))"
}

# Zip each module
zip_module "lpg-ehl-core"
zip_module "lpg-ehl-api"
zip_module "lpg-ehl-emulator"
zip_module "lpg-web"

echo ""
echo "📋 Created archives:"
ls -lh lpg-ehl-*-${TIMESTAMP}.zip | awk '{print "   " $9 " (" $5 ")"}'

echo ""
echo "✅ Done! Four separate module archives created."
echo "⚡ Each contains only source code, configs, and docs - no build artifacts or dependencies."
