#!/bin/bash

# Lytt til Terminal
# 
# Starter en server på port 8009 som fanger ALT terminalen sender.
# Dette viser oss hvilken protokoll den bruker.

cd "$(dirname "$0")"

echo ""
echo "══════════════════════════════════════════════════"
echo "  LYTT TIL TERMINAL-MELDINGER"
echo "══════════════════════════════════════════════════"
echo ""
echo "Dette programmet vil:"
echo "1. Starte en server på port 8009"
echo "2. Vente på at terminalen kobler til"
echo "3. Logge ALLE meldinger fra terminalen"
echo "4. Svare med ACK på hver melding"
echo ""
echo "⚠️  VIKTIG:"
echo "- Terminalen MÅ være på og på nettverket"
echo "- Den vil automatisk prøve å koble til"
echo "- SE NØYE PÅ LOGGENE!"
echo ""
echo "Trykk Ctrl+C for å stoppe"
echo ""
read -p "Trykk Enter for å starte..."
echo ""

# Compile
mvn -q compile

# Run
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  no.cloudberries.lpg.payment.SimpleEcrListener
