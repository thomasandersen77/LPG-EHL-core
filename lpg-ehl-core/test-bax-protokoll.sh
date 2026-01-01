#!/bin/bash

# Test Bax Protokoll
# 
# Tester Nets/Bax-protokoll med ekte terminal

cd "$(dirname "$0")"

echo ""
echo "══════════════════════════════════════════════════"
echo "  TEST NETS/BAX PROTOKOLL MED EKTE TERMINAL"
echo "══════════════════════════════════════════════════"
echo ""
echo "Basert på analyse av Thomas (Persona 2):"
echo "- Terminal: Verifone P400/V400"
echo "- Protokoll: Nets/Bax over TCP"
echo "- Format: STX + P,OpID,Amount + ETX + LRC"
echo ""
echo "⚠️  TESTING MED 3 ØRE!"
echo ""
read -p "Trykk Enter for å starte..."
echo ""

# Compile
mvn -q compile

# Run
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  no.cloudberries.lpg.payment.InteractivePaymentDemo --real
