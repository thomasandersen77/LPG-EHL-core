#!/bin/bash

# Baxi Terminal Test Runner
# Compiles and runs the Kotlin test

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Find baxi-kotlin JAR
BAXI_JAR="$HOME/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/0.1.0-SNAPSHOT/baxi-kotlin-0.1.0-SNAPSHOT.jar"

if [ ! -f "$BAXI_JAR" ]; then
    echo "❌ Error: baxi-kotlin JAR not found"
    echo "Expected: $BAXI_JAR"
    echo ""
    echo "Please install baxi-kotlin first"
    exit 1
fi

# Compile
echo "📦 Compiling..."
kotlinc -cp "$BAXI_JAR" "$SCRIPT_DIR/BaxiQuickTest.kt" -include-runtime -d "$SCRIPT_DIR/baxi-test.jar"

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

echo ""
echo "🚀 Running test..."
echo ""

# Run
kotlin -cp "$SCRIPT_DIR/baxi-test.jar:$BAXI_JAR" BaxiQuickTestKt

# Cleanup
rm -f "$SCRIPT_DIR/baxi-test.jar"
