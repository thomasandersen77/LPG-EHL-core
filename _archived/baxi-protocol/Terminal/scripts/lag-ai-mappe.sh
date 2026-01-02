#!/bin/bash

# Lag AI-mappe
# 
# Kopierer kun kildekode og konfigurasjon til en egen mappe

cd "$(dirname "$0")"

DEST_DIR="../lpg-ehl-for-ai"

echo "══════════════════════════════════════════════════"
echo "  LAG MAPPE FOR AI-OPPLASTING"
echo "══════════════════════════════════════════════════"
echo ""

# 1. Remove old if exists
if [ -d "$DEST_DIR" ]; then
    echo "1. Fjerner gammel mappe..."
    rm -rf "$DEST_DIR"
    echo "   ✓ Fjernet"
    echo ""
fi

# 2. Create new directory
echo "2. Lager ny mappe: $DEST_DIR"
mkdir -p "$DEST_DIR"
echo "   ✓ Opprettet"
echo ""

# 3. Copy source files
echo "3. Kopierer filer..."

# Copy src directory
echo "   - Kopierer src/"
cp -r src "$DEST_DIR/"

# Copy pom.xml
echo "   - Kopierer pom.xml"
cp pom.xml "$DEST_DIR/"

# Copy markdown files
echo "   - Kopierer *.md"
cp *.md "$DEST_DIR/" 2>/dev/null || true

# Copy shell scripts
echo "   - Kopierer *.sh"
cp *.sh "$DEST_DIR/" 2>/dev/null || true

# Copy SDK config
if [ -f ".sdkmanrc" ]; then
    echo "   - Kopierer .sdkmanrc"
    cp .sdkmanrc "$DEST_DIR/"
fi

echo "   ✓ Filer kopiert"
echo ""

# 4. Clean up any build artifacts
echo "4. Cleaner build-artifakter..."
find "$DEST_DIR" -name "*.class" -delete
find "$DEST_DIR" -name ".DS_Store" -delete
echo "   ✓ Cleaned"
echo ""

# 5. Count files
FILE_COUNT=$(find "$DEST_DIR" -type f | wc -l | tr -d ' ')

echo "══════════════════════════════════════════════════"
echo "  FERDIG!"
echo "══════════════════════════════════════════════════"
echo ""
echo "Mappe: $DEST_DIR"
echo "Antall filer: $FILE_COUNT"
echo ""
echo "Struktur:"
tree -L 2 "$DEST_DIR" 2>/dev/null || find "$DEST_DIR" -maxdepth 2 -type d
echo ""
echo "══════════════════════════════════════════════════"
echo ""
echo "📤 KLAR FOR OPPLASTING!"
echo ""
echo "Last opp hele mappen til Gemini/ChatGPT:"
echo "1. Dra mappen til AI-grensesnittet"
echo "2. Legg ved bilder av terminalen"
echo "3. Still spørsmålene fra OPPSUMMERING_FOR_AI.md"
echo ""
