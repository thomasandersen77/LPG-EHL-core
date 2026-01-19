#!/bin/bash
# Create ZIP archives of modules and legacy code for AI analysis
# Usage: ./scripts/zip-all-for-ai.sh

set -e

echo "🤖 Creating AI analysis archives..."
echo ""

# Helper function to zip only if files exist
safe_zip() {
    local zip_name=$1
    shift
    local files_to_zip=()
    local exclude_args=()
    local in_exclude=false

    for arg in "$@"; do
        if [[ "$arg" == "-x" ]]; then
            in_exclude=true
            exclude_args+=("-x")
            continue
        fi

        if $in_exclude; then
            exclude_args+=("$arg")
        else
            if [[ -e "$arg" ]]; then
                files_to_zip+=("$arg")
            fi
        fi
    done

    if [ ${#files_to_zip[@]} -gt 0 ]; then
        zip -q -r "$zip_name" "${files_to_zip[@]}" "${exclude_args[@]}"
        echo "✓ ${zip_name} zipped ($(du -h "$zip_name" | cut -f1))"
    else
        echo "⚠️  Skipping ${zip_name} (no matching files found)"
    fi
}

# Core module (EHL protocol)
echo "📦 Zipping core module..."
safe_zip lpg-ehl-core-for-ai.zip lpg-ehl-core/src \
    lpg-ehl-core/pom.xml \
    lpg-ehl-core/README.md \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"

# API module (Spring Boot REST API + Nets Cloud Connect)
echo "📦 Zipping API module..."
safe_zip lpg-ehl-api-for-ai.zip lpg-ehl-api/src \
    lpg-ehl-api/pom.xml \
    lpg-ehl-api/README.md \
    lpg-ehl-api/.env.local.example \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"

# Emulator module
echo "📦 Zipping emulator module..."
safe_zip lpg-ehl-emulator-for-ai.zip lpg-ehl-emulator/src \
    lpg-ehl-emulator/pom.xml \
    lpg-ehl-emulator/README.md \
    -x "*/target/*" "*/.idea/*"

# PLS module (Physical Layer Support)
echo "📦 Zipping PLS module..."
safe_zip lpg-ehl-pls-for-ai.zip lpg-ehl-pls/src \
    lpg-ehl-pls/pom.xml \
    -x "*/target/*" "*/.idea/*"

# CLI module (Spring Shell)
echo "📦 Zipping CLI module..."
safe_zip lpg-ehl-cli-for-ai.zip lpg-ehl-cli/src \
    lpg-ehl-cli/pom.xml \
    -x "*/target/*" "*/.idea/*"

# VB6 Legacy Code
echo "📦 Zipping VB6 legacy code..."
safe_zip norgesgass-legacy-for-ai.zip norgesgass_legacy/ \
    -x "*/bin/*" "*/obj/*" "*/.vs/*"

# Python PoC
echo "📦 Zipping Python PoC..."
safe_zip python-legacy-for-ai.zip "more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/" \
    -x "*/__pycache__/*" "*/.pytest_cache/*" "*/venv/*"

# Documentation
echo "📦 Zipping documentation..."
safe_zip docs-for-ai.zip docs/ WARP.md README.md CHANGELOG.md \
    LEGACY_ANALYSIS.md IMPLEMENTATION_ROADMAP.md

# Archived Baxi protocol
echo "📦 Zipping archived Baxi protocol..."
safe_zip archived-baxi-for-ai.zip _archived/baxi-protocol/ \
    -x "*/Terminal/images/*"

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
