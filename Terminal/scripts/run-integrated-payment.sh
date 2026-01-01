#!/bin/bash

# Run Integrated Payment Demo
# 
# Complete flow:
# - Dispenser emulator (1 øre/liter)
# - Simulate delivery (max 3 liters = 3 øre)
# - Payment terminal integration
# - Transaction completion
#
# Usage:
#   ./run-integrated-payment.sh          - Use simulated terminal
#   ./run-integrated-payment.sh --real   - Use real terminal (192.168.0.4:8009)

cd "$(dirname "$0")"

echo "==================================================="
echo "  INTEGRATED DISPENSER + PAYMENT DEMO"
echo "==================================================="
echo ""
echo "Price: 1 øre/liter (for safe testing)"
echo "Max delivery: ~3 liters = 3 øre"
echo ""

if [ "$1" = "--real" ]; then
    echo "Mode: REAL payment terminal"
    echo "Terminal: 192.168.0.4:8009"
    echo ""
    echo "⚠️  WARNING: This will charge a real card!"
    echo "   Amount: Maximum 3 øre (0.03 kr)"
    echo ""
    read -p "Press Enter to continue or Ctrl+C to cancel..."
    echo ""
else
    echo "Mode: SIMULATED payment terminal"
    echo ""
fi

# Build if needed
mvn -q compile

# Run demo
if [ "$1" = "--real" ]; then
    mvn exec:java \
        -Dexec.mainClass="no.cloudberries.lpg.payment.IntegratedPaymentDemo" \
        -Dexec.args="--real" \
        -Dexec.cleanupDaemonThreads=false 2>&1 | grep -v "^\[INFO\]"
else
    mvn exec:java \
        -Dexec.mainClass="no.cloudberries.lpg.payment.IntegratedPaymentDemo" \
        -Dexec.cleanupDaemonThreads=false 2>&1 | grep -v "^\[INFO\]"
fi
