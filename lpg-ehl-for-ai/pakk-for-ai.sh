#!/bin/bash

# Pakk prosjekt for AI-analyse
# 
# Lager en ren pakke med kun kildekode og konfigurasjon

cd "$(dirname "$0")"

echo "══════════════════════════════════════════════════"
echo "  PAKK PROSJEKT FOR AI-ANALYSE"
echo "══════════════════════════════════════════════════"
echo ""

# 1. Clean Maven
echo "1. Cleaner Maven..."
mvn clean -q
echo "   ✓ Maven cleaned"

# 2. Remove any other build artifacts
echo ""
echo "2. Fjerner build-artifakter..."
find . -name "*.class" -delete
find . -name "*.jar" -delete
find . -name ".DS_Store" -delete
echo "   ✓ Build-artifakter fjernet"

# 3. Create archive
echo ""
echo "3. Lager arkiv..."
ARCHIVE_NAME="lpg-ehl-core-$(date +%Y%m%d-%H%M%S).zip"

zip -r "../$ARCHIVE_NAME" \
  src/ \
  pom.xml \
  *.md \
  *.sh \
  .sdkmanrc \
  -x "*/target/*" "*/node_modules/*" "*/.git/*" "*.class" "*.jar"

echo "   ✓ Arkiv opprettet: $ARCHIVE_NAME"

# 4. Show summary
echo ""
echo "══════════════════════════════════════════════════"
echo "  FERDIG!"
echo "══════════════════════════════════════════════════"
echo ""
echo "Fil: ../$ARCHIVE_NAME"
echo ""
echo "Inneholder:"
echo "- src/           (all kildekode)"
echo "- pom.xml        (Maven konfigurasjon)"
echo "- *.md           (dokumentasjon)"
echo "- *.sh           (scripts)"
echo ""
echo "Ekskludert:"
echo "- target/        (kompilerte filer)"
echo "- .git/          (versjonskontroll)"
echo "- *.class        (bytecode)"
echo ""
echo "Last opp til Gemini/ChatGPT sammen med:"
echo "- OPPSUMMERING_FOR_AI.md"
echo "- Bilder av terminalen"
echo ""
