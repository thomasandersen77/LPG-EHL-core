#!/bin/bash
# Create ZIP archives of modules and legacy code for AI analysis
# Usage: ./scripts/zip-all-for-ai.sh

set -e

echo "🤖 Creating AI analysis archives..."
echo ""

# Core module (EHL protocol)
echo "📦 Zipping core module..."
zip -q -r lpg-ehl-core-for-ai.zip lpg-ehl-core/src \
    lpg-ehl-core/pom.xml \
    lpg-ehl-core/README.md \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"
echo "✓ Core module zipped ($(du -h lpg-ehl-core-for-ai.zip | cut -f1))"

# API module (Spring Boot REST API + Nets Cloud Connect)
echo "📦 Zipping API module..."
zip -q -r lpg-ehl-api-for-ai.zip lpg-ehl-api/src \
    lpg-ehl-api/pom.xml \
    lpg-ehl-api/README.md \
    lpg-ehl-api/.env.local.example \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"
echo "✓ API module zipped ($(du -h lpg-ehl-api-for-ai.zip | cut -f1))"

# Emulator module
echo "📦 Zipping emulator module..."
zip -q -r lpg-ehl-emulator-for-ai.zip lpg-ehl-emulator/src \
    lpg-ehl-emulator/pom.xml \
    lpg-ehl-emulator/README.md \
    -x "*/target/*" "*/.idea/*"
echo "✓ Emulator module zipped ($(du -h lpg-ehl-emulator-for-ai.zip | cut -f1))"

# PLS module (Physical Layer Support)
echo "📦 Zipping PLS module..."
zip -q -r lpg-ehl-pls-for-ai.zip lpg-ehl-pls/src \
    lpg-ehl-pls/pom.xml \
    -x "*/target/*" "*/.idea/*"
echo "✓ PLS module zipped ($(du -h lpg-ehl-pls-for-ai.zip | cut -f1))"

# CLI module (Spring Shell)
echo "📦 Zipping CLI module..."
zip -q -r lpg-ehl-cli-for-ai.zip lpg-ehl-cli/src \
    lpg-ehl-cli/pom.xml \
    -x "*/target/*" "*/.idea/*"
echo "✓ CLI module zipped ($(du -h lpg-ehl-cli-for-ai.zip | cut -f1))"

# VB6 Legacy Code
echo "📦 Zipping VB6 legacy code..."
zip -q -r norgesgass-legacy-for-ai.zip norgesgass_legacy/ \
    -x "*/bin/*" "*/obj/*" "*/.vs/*"
echo "✓ VB6 legacy zipped ($(du -h norgesgass-legacy-for-ai.zip | cut -f1))"

# Python PoC
echo "📦 Zipping Python PoC..."
zip -q -r python-legacy-for-ai.zip "more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/" \
    -x "*/__pycache__/*" "*/.pytest_cache/*" "*/venv/*"
echo "✓ Python PoC zipped ($(du -h python-legacy-for-ai.zip | cut -f1))"

# Documentation
echo "📦 Zipping documentation..."
zip -q -r docs-for-ai.zip docs/ WARP.md README.md CHANGELOG.md \
    LEGACY_ANALYSIS.md IMPLEMENTATION_ROADMAP.md
echo "✓ Documentation zipped ($(du -h docs-for-ai.zip | cut -f1))"

# Archived Baxi protocol
echo "📦 Zipping archived Baxi protocol..."
zip -q -r archived-baxi-for-ai.zip _archived/baxi-protocol/ \
    -x "*/Terminal/images/*"
echo "✓ Archived Baxi protocol zipped ($(du -h archived-baxi-for-ai.zip | cut -f1))"

echo ""
echo "✅ All archives created!"
echo ""
echo "📊 Archive sizes:"
ls -lh *-for-ai.zip | awk '{printf "   %s  %s\n", $5, $9}'
echo ""
echo "📤 Upload these files to AI (Claude, ChatGPT, Gemini) for analysis:"
echo "   • lpg-ehl-core-for-ai.zip      - Core EHL protocol implementation"
echo "   • lpg-ehl-emulator-for-ai.zip  - Virtual dispenser for testing"
echo "   • lpg-ehl-pls-for-ai.zip       - Physical Layer Support (real serial)"
echo "   • lpg-ehl-api-for-ai.zip       - REST API + Nets Cloud Connect"
echo "   • lpg-ehl-cli-for-ai.zip       - Command Line Interface (Spring Shell)"
echo "   • norgesgass-legacy-for-ai.zip - Original VB6 code"
echo "   • python-legacy-for-ai.zip     - Python PoC"
echo "   • docs-for-ai.zip              - All documentation"
echo "   • archived-baxi-for-ai.zip     - Legacy TCP/Baxi protocol"
