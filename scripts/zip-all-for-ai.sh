#!/bin/bash
# Create ZIP archives of modules and legacy code for AI analysis
# Usage: ./scripts/zip-all-for-ai.sh
#
# Output: All archives are stored in ./ai-exports/

set -e

# Output directory
OUTPUT_DIR="ai-exports"

echo "🤖 Creating AI analysis archives..."
echo "📁 Output directory: ${OUTPUT_DIR}/"
echo ""

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

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
        zip -q -r "${OUTPUT_DIR}/${zip_name}" "${files_to_zip[@]}" "${exclude_args[@]}"
        echo "✓ ${zip_name} ($(du -h "${OUTPUT_DIR}/${zip_name}" | cut -f1))"
    else
        echo "⚠️  Skipping ${zip_name} (no matching files found)"
    fi
}

echo "═══════════════════════════════════════════════════════════"
echo "📦 MAVEN MODULES"
echo "═══════════════════════════════════════════════════════════"

# Core module (EHL protocol, commands, state machine)
echo "📦 lpg-ehl-core (EHL protocol implementation)..."
safe_zip lpg-ehl-core-for-ai.zip lpg-ehl-core/src \
    lpg-ehl-core/pom.xml \
    lpg-ehl-core/README.md \
    lpg-ehl-core/*.md \
    -x "*/target/*" "*/.idea/*"

# Service module (business logic, repositories, domain services)
echo "📦 lpg-ehl-service (business logic, JPA, services)..."
safe_zip lpg-ehl-service-for-ai.zip lpg-ehl-service/src \
    lpg-ehl-service/pom.xml \
    lpg-ehl-service/README.md \
    lpg-ehl-service/*.md \
    -x "*/target/*" "*/.idea/*"

# Transport module (serial port abstraction)
echo "📦 lpg-transport (serial port transport layer)..."
safe_zip lpg-transport-for-ai.zip lpg-transport/src \
    lpg-transport/pom.xml \
    lpg-transport/README.md \
    lpg-transport/*.md \
    -x "*/target/*" "*/.idea/*"

# Emulator module (virtual dispenser)
echo "📦 lpg-ehl-emulator (virtual dispenser for testing)..."
safe_zip lpg-ehl-emulator-for-ai.zip lpg-ehl-emulator/src \
    lpg-ehl-emulator/pom.xml \
    lpg-ehl-emulator/README.md \
    lpg-ehl-emulator/*.md \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"

# Webapp module (Spring Boot REST API + React frontend)
echo "📦 lpg-ehl-webapp (REST API + React frontend)..."
safe_zip lpg-ehl-webapp-for-ai.zip lpg-ehl-webapp/src \
    lpg-ehl-webapp/pom.xml \
    lpg-ehl-webapp/README.md \
    lpg-ehl-webapp/*.md \
    lpg-ehl-webapp/.env.local.example \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*" "*/static/*"

# Headless app module (production deployment without web UI)
echo "📦 lpg-ehl-app-headless (headless production app)..."
safe_zip lpg-ehl-app-headless-for-ai.zip lpg-ehl-app-headless/src \
    lpg-ehl-app-headless/pom.xml \
    lpg-ehl-app-headless/README.md \
    lpg-ehl-app-headless/*.md \
    -x "*/target/*" "*/.idea/*"

# CLI module (Spring Shell)
echo "📦 lpg-ehl-cli (command line interface)..."
safe_zip lpg-ehl-cli-for-ai.zip lpg-ehl-cli/src \
    lpg-ehl-cli/pom.xml \
    lpg-ehl-cli/README.md \
    lpg-ehl-cli/*.md \
    -x "*/target/*" "*/.idea/*"

# Serial port simulator module
echo "📦 lpg-ehl-serialport-sim (serial port simulator)..."
safe_zip lpg-ehl-serialport-sim-for-ai.zip lpg-ehl-serialport-sim/src \
    lpg-ehl-serialport-sim/pom.xml \
    lpg-ehl-serialport-sim/README.md \
    lpg-ehl-serialport-sim/*.md \
    -x "*/target/*" "*/.idea/*"

# Parent POM
echo "📦 Parent POM..."
safe_zip lpg-ehl-parent-for-ai.zip pom.xml

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "📚 DOCUMENTATION & LEGACY"
echo "═══════════════════════════════════════════════════════════"

# Documentation
echo "📦 docs (all documentation)..."
safe_zip docs-for-ai.zip docs/ WARP.md README.md CHANGELOG.md \
    LEGACY_ANALYSIS.md IMPLEMENTATION_ROADMAP.md *.md \
    -x "*/target/*"

# VB6 Legacy Code
echo "📦 norgesgass_legacy (original VB6 code)..."
safe_zip norgesgass-legacy-for-ai.zip norgesgass_legacy/ \
    -x "*/bin/*" "*/obj/*" "*/.vs/*"

# Python PoC
echo "📦 python-legacy (Python PoC)..."
safe_zip python-legacy-for-ai.zip "more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/" \
    -x "*/__pycache__/*" "*/.pytest_cache/*" "*/venv/*"

# Archived Baxi protocol
echo "📦 archived-baxi (legacy TCP/Baxi protocol)..."
safe_zip archived-baxi-for-ai.zip _archived/baxi-protocol/ \
    -x "*/Terminal/images/*"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✅ All archives created in ${OUTPUT_DIR}/"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📊 Archive sizes:"
ls -lh "${OUTPUT_DIR}"/*-for-ai.zip 2>/dev/null | awk '{printf "   %-8s %s\n", $5, $9}'
echo ""
echo "📤 Upload these files to AI (Claude, ChatGPT, Gemini) for analysis:"
echo ""
echo "   MAVEN MODULES:"
echo "   • lpg-ehl-core-for-ai.zip         - Core EHL protocol implementation"
echo "   • lpg-ehl-service-for-ai.zip      - Business logic, JPA, domain services"
echo "   • lpg-transport-for-ai.zip        - Serial port transport abstraction"
echo "   • lpg-ehl-emulator-for-ai.zip     - Virtual dispenser for testing"
echo "   • lpg-ehl-webapp-for-ai.zip       - REST API + React frontend"
echo "   • lpg-ehl-app-headless-for-ai.zip - Headless production deployment"
echo "   • lpg-ehl-cli-for-ai.zip          - Command Line Interface (Spring Shell)"
echo "   • lpg-ehl-serialport-sim-for-ai.zip - Serial port simulator"
echo "   • lpg-ehl-parent-for-ai.zip       - Parent POM with dependencies"
echo ""
echo "   DOCUMENTATION & LEGACY:"
echo "   • docs-for-ai.zip                 - All documentation"
echo "   • norgesgass-legacy-for-ai.zip    - Original VB6 code"
echo "   • python-legacy-for-ai.zip        - Python PoC"
echo "   • archived-baxi-for-ai.zip        - Legacy TCP/Baxi protocol"
