#!/bin/bash

TERMINAL_IP="192.168.0.200"

echo "Skanner betalingsterminal på $TERMINAL_IP..."
echo "Dette tar ca 30 sekunder..."
echo ""

# Vanlige ECR/betalingsterminal porter
PORTS=(
  80      # HTTP
  443     # HTTPS
  8008    # Alternative HTTP
  8009    # Verifone ECR (vanlig)
  8080    # HTTP proxy
  8443    # HTTPS alternative
  9000    # Generic
  9001    # Nets/BBS
  9100    # Raw printing
  10001   # Verifone administration
  20002   # Ingenico
  23      # Telnet
  22      # SSH
)

echo "Tester ${#PORTS[@]} porter..."
echo "================================"

for PORT in "${PORTS[@]}"; do
  echo -n "Port $PORT: "
  if timeout 1 bash -c "echo > /dev/tcp/$TERMINAL_IP/$PORT" 2>/dev/null; then
    echo "✅ ÅPEN"
  else
    echo "❌ Stengt"
  fi
done

echo ""
echo "================================"
echo "Ferdig!"
