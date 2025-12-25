#!/bin/bash

# Simple script to view messages in Azurite using curl
# Works directly with Azurite REST API

QUEUE_NAME="lpg-transactions"
ACCOUNT="devstoreaccount1"
BASE_URL="http://localhost:10001/$ACCOUNT/$QUEUE_NAME"

echo "=== Azurite Queue Monitor - $(date) ==="
echo ""
echo "🔢 Checking queue: $QUEUE_NAME"
echo ""

# Peek at messages (doesn't remove them)
echo "📬 Peeking at messages..."
echo ""

response=$(curl -s "$BASE_URL/messages?peekonly=true&numofmessages=10")

if [ -z "$response" ] || [ "$response" == "<?xml version=\"1.0\" encoding=\"utf-8\"?><QueueMessagesList />" ]; then
    echo "⚠️  No messages in queue"
else
    # Extract MessageText content (macOS compatible)
    echo "$response" | sed -n 's/.*<MessageText>\(.*\)<\/MessageText>.*/\1/p' | while read -r msg; do
        echo "=== Message ==="
        echo "$msg" | jq '.' 2>/dev/null || echo "$msg"
        echo ""
    done
fi

# Show approximate count
echo ""
echo "💡 To view messages with Azure CLI:"
echo "   ./scripts/view-azurite-messages.sh --peek 5"
echo ""
echo "💡 To watch continuously:"
echo "   ./scripts/view-azurite-messages.sh --watch"
