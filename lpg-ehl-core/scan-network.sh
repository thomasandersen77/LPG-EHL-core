#!/bin/bash

echo "🔍 Skanner 192.168.0.x-nettverket for betalingsterminal..."
echo "Dette kan ta 2-3 minutter..."
echo ""

# Vanlige ECR-porter å teste
ECR_PORTS=(8009 9001 10001 8080)

echo "Finner aktive enheter..."
echo "================================"

for i in {1..254}; do
  IP="192.168.0.$i"
  
  # Rask ping-test (timeout 0.2 sekunder)
  if ping -c 1 -W 200 $IP >/dev/null 2>&1; then
    echo ""
    echo "✅ Enhet funnet: $IP"
    
    # Test ECR-porter
    for PORT in "${ECR_PORTS[@]}"; do
      if timeout 0.5 bash -c "echo > /dev/tcp/$IP/$PORT" 2>/dev/null; then
        echo "   🎯 Port $PORT er ÅPEN!"
      fi
    done
  fi
done

echo ""
echo "================================"
echo "✅ Ferdig!"
