#!/bin/bash

# Baxi Terminal Quick Test Runner
# Runs the Kotlin test script with proper classpath setup

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🔍 Baxi Terminal Quick Test"
echo "═══════════════════════════════════════════════════════════"

# Check if baxi-kotlin JAR exists
BAXI_JAR="$HOME/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/0.1.0-SNAPSHOT/baxi-kotlin-0.1.0-SNAPSHOT.jar"
if [ ! -f "$BAXI_JAR" ]; then
    echo "❌ Error: baxi-kotlin JAR not found at: $BAXI_JAR"
    echo ""
    echo "Please install baxi-kotlin first:"
    echo "  cd /path/to/baxi-kotlin"
    echo "  mvn clean install"
    exit 1
fi

# Build classpath
CLASSPATH="$BAXI_JAR"

# Add slf4j-simple for logging
SLF4J_JAR="$HOME/.m2/repository/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"
SLF4J_API_JAR="$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"

if [ -f "$SLF4J_JAR" ]; then
    CLASSPATH="$CLASSPATH:$SLF4J_JAR:$SLF4J_API_JAR"
else
    echo "⚠️  Warning: slf4j-simple not found, continuing without logging..."
fi

# Run with kotlinc
echo "Running test..."
echo ""

kotlinc -classpath "$CLASSPATH" -script "$SCRIPT_DIR/test-baxi-quick.kts"
