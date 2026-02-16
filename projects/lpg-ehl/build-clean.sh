#!/bin/bash
# LPG-EHL Clean Build Script
# Fixes common Kotlin daemon issues and builds the project

set -e

echo "═══════════════════════════════════════════════════════════"
echo "  LPG-EHL Clean Build"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Step 1: Kill any hanging Kotlin daemons
echo "🧹 Cleaning Kotlin daemon processes..."
pkill -f kotlin 2>/dev/null || true
pkill -f KotlinCompileDaemon 2>/dev/null || true

# Step 2: Remove Kotlin daemon cache
echo "🗑️  Removing Kotlin daemon cache..."
rm -rf "$HOME/Library/Application Support/kotlin/daemon" 2>/dev/null || true
rm -rf /var/folders/*/*/*/kotlin-compiler-client-*-is-running 2>/dev/null || true

echo "✅ Kotlin daemon cleaned"
echo ""

# Step 3: Build with in-process compiler
echo "🔨 Building project (in-process compiler)..."
echo ""

mvn clean install -Dkotlin.compiler.execution.strategy=in-process "$@"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✅ Build complete!"
echo "═══════════════════════════════════════════════════════════"
