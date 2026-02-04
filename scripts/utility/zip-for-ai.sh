#!/bin/bash
# zip-for-ai.sh - Export clean source code for AI analysis
# 
# Usage: ./scripts/zip-for-ai.sh
# Output: lpg-ehl-for-ai-YYYYMMDD-HHMMSS.zip

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_FILE="lpg-ehl-for-ai-${TIMESTAMP}.zip"

cd "$PROJECT_ROOT"

echo "🗜️  Creating AI-friendly source code archive..."
echo "📦 Output: $OUTPUT_FILE"

# Create temporary zip (quiet mode)
zip -q -r "$OUTPUT_FILE" . \
  -x "*.git/*" \
  -x "*target/*" \
  -x "*node_modules/*" \
  -x "*dist/*" \
  -x "*build/*" \
  -x "*.idea/*" \
  -x "*.vscode/*" \
  -x "*.DS_Store" \
  -x "*backup-*/*" \
  -x "*norgesgass_legacy/*" \
  -x "*more_legacy/*" \
  -x "*.class" \
  -x "*.jar" \
  -x "*.war" \
  -x "*.log" \
  -x "*.tmp" \
  -x "*~" \
  -x "*.swp" \
  -x "*lpg-ehl-for-ai-*.zip" \
  -x "*NorgesGass /*"

# Show what's included
echo ""
echo "📂 Included modules:"
unzip -l "$OUTPUT_FILE" | grep -E "(lpg-ehl-core|lpg-ehl-api|lpg-ehl-emulator|lpg-web)/" | head -3
echo "   ..."

echo ""
echo "✅ Created: $OUTPUT_FILE"
du -h "$OUTPUT_FILE"

echo ""
echo "📋 Archive contents summary:"
echo "   - lpg-ehl-core/        (Core protocol)"
echo "   - lpg-ehl-api/         (Spring Boot REST API)"
echo "   - lpg-ehl-emulator/    (Testing emulator)"
echo "   - lpg-web/             (React frontend)"
echo "   - docs/                (Documentation)"
echo "   - scripts/             (Helper scripts)"
echo "   - *.md                 (Markdown files)"
echo "   - pom.xml, package.json (Build configs)"
echo ""
echo "⚡ Ready for AI upload - excludes: build artifacts, dependencies, git history, IDE files, backups, legacy code"
