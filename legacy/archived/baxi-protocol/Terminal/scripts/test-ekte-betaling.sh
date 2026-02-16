#!/bin/bash

# Test Ekte Betaling
# 
# Kobler til ekte terminal og venter på at du tapper kortet

cd "$(dirname "$0")"

echo ""
echo "══════════════════════════════════════════════════"
echo "  TEST MED EKTE BETALINGSTERMINAL"
echo "══════════════════════════════════════════════════"
echo ""
echo "⚠️  ADVARSEL: Dette vil tappe kortet ditt for 3 øre!"
echo ""
echo "Terminal: 192.168.0.4:8009"
echo "Beløp: 3 øre (0.03 kr)"
echo ""
read -p "Hold kortet klart og trykk Enter for å fortsette..."
echo ""

# Compile
mvn -q compile

# Run
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  no.cloudberries.lpg.payment.InteractivePaymentDemo
