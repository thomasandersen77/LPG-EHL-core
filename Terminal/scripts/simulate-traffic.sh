#!/bin/bash

# Configuration
API_URL="http://localhost:8080/api/v1/transactions"
INTERVAL=${1:-3600} # Default 3600 seconds (1 hour)

echo "🚗 Starting LPG Traffic Simulator"
echo "⏱️  Interval: $INTERVAL seconds (1 hour)"
echo "📍 API URL: $API_URL"
echo "---------------------------------------------------"

while true; do
    CURRENT_TIME=$(date "+%Y-%m-%d %H:%M:%S")
    echo "[$CURRENT_TIME] 🔄 New customer arriving..."
    
    # Random volume between 10 and 60 liters
    VOL_LITERS=$((10 + RANDOM % 51))
    # Price 15.90 kr/L -> 1590 øre/L
    PRICE_ORE=1590
    
    # Calculate totals
    VOL_DL=$((VOL_LITERS * 10))
    AMOUNT_ORE=$((VOL_LITERS * PRICE_ORE))
    
    # Format for display (using awk for basic float division fallback if bc missing)
    if command -v bc &> /dev/null; then
        AMOUNT_KR=$(echo "scale=2; $AMOUNT_ORE / 100" | bc)
    else
        AMOUNT_KR=$(awk "BEGIN {printf \"%.2f\", $AMOUNT_ORE/100}")
    fi
    
    echo "⛽ Filling $VOL_LITERS liters ($AMOUNT_KR NOK)..."
    
    # 1. Create Transaction (STOP dispensing)
    RESPONSE=$(curl -s -X POST "$API_URL" \
      -H "Content-Type: application/json" \
      -d "{
        \"dispenserAddress\": 1,
        \"nozzleNumber\": 1,
        \"volumeDeciliters\": $VOL_DL,
        \"amountOre\": $AMOUNT_ORE,
        \"pricePerLiter\": $PRICE_ORE,
        \"paymentType\": \"PENDING\",
        \"productCode\": \"LPG\",
        \"includesRoadTax\": true
      }")
      
    # Extract ID (tries grep first, then jq)
    TX_ID=$(echo "$RESPONSE" | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$TX_ID" ] || [ "$TX_ID" == "null" ]; then
        echo "❌ Failed to create transaction. Is API running?"
        echo "Response: $RESPONSE"
    else
        echo "✅ Transaction created: $TX_ID"
        echo "⏳ Walking to register..."
        
        sleep 5
        
        # 2. Pay Transaction
        echo "💳 Paying with CARD..."
        PAY_RESPONSE=$(curl -s -X PATCH "$API_URL/$TX_ID/payment?paymentMethod=CARD&paymentStatus=PAID")
        
        echo "✅ Payment successful! Transaction settled."
    fi
    
    echo "💤 Sleeping for $INTERVAL seconds (Next customer in 1 hour)..."
    echo "---------------------------------------------------"
    sleep $INTERVAL
done
