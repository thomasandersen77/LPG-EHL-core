#!/bin/bash

# Run Payment Terminal Demo
# 
# Usage:
#   ./run-payment-demo.sh          - Run with simulated terminal
#   ./run-payment-demo.sh --real   - Run with real terminal (192.168.0.4:8009)

cd "$(dirname "$0")"

# Build if needed
mvn -q compile

# Run demo
if [ "$1" = "--real" ]; then
    echo "Running with REAL payment terminal..."
    mvn -q exec:java \
        -Dexec.args="--payment --real" \
        -Dexec.cleanupDaemonThreads=false
else
    echo "Running with SIMULATED payment terminal..."
    mvn -q exec:java \
        -Dexec.args="--payment" \
        -Dexec.cleanupDaemonThreads=false
fi
