#!/bin/bash

# Run ECR Server
# 
# This server listens for connections from the payment terminal
# and responds to establish communication.
#
# Usage:
#   ./run-ecr-server.sh [port]
#
# Default port: 8009

cd "$(dirname "$0")"

PORT=${1:-8009}

echo "Starting ECR Server..."
echo "Port: $PORT"
echo ""

# Build if needed
mvn -q compile

# Run server
mvn -q exec:java \
    -Dexec.mainClass="no.cloudberries.lpg.payment.EcrServerApp" \
    -Dexec.args="$PORT" \
    -Dexec.cleanupDaemonThreads=false
