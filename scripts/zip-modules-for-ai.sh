#!/bin/bash
# zip-modules-for-ai.sh - Export each module as separate zip for AI analysis
# 
# Usage: ./scripts/zip-modules-for-ai.sh
# Output: Six separate zip files (core, api, emulator, pls, cli, web)

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
zip_module "lpg-ehl-emulator"
zip_module "lpg-ehl-pls"
zip_module "lpg-ehl-api"
zip_module "lpg-ehl-cli"
zip_module "lpg-web"

echo ""
echo "📋 Created archives:"
ls -lh lpg-ehl-*-${TIMESTAMP}.zip | awk '{print "   " $9 " (" $5 ")"}'

echo ""
echo "✅ Done! Six module archives created:"
echo "   • lpg-ehl-core      - EHL protocol implementation (Kotlin)"
echo "   • lpg-ehl-emulator  - Virtual dispenser for testing (Kotlin)"
echo "   • lpg-ehl-pls       - Physical Layer Support for real serial ports (Kotlin)"
echo "   • lpg-ehl-api       - REST API + Spring Boot (Kotlin)"
echo "   • lpg-ehl-cli       - Command Line Interface with Spring Shell (Kotlin)"
echo "   • lpg-web           - React frontend (TypeScript)"
echo ""
echo "⚡ Each contains only source code, configs, and docs - no build artifacts or dependencies."
