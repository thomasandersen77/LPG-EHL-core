#!/bin/bash

# Script to view messages in Azurite queue
# Usage: ./scripts/view-azurite-messages.sh [OPTIONS]
#
# OPTIONS:
#   --peek N     Peek at N messages without removing them (default: 10)
#   --receive N  Receive and remove N messages (default: 1)
#   --count      Show message count only
#   --watch      Continuously watch for new messages (Ctrl+C to stop)
#   --clear      Clear all messages from queue

QUEUE_NAME="lpg-transactions"
AZURITE_URL="http://localhost:10001/devstoreaccount1"
CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Azure Storage Tools are installed
if ! command -v az &> /dev/null; then
    echo -e "${RED}❌ Azure CLI not found${NC}"
    echo "Install with: brew install azure-cli"
    echo ""
    echo "Alternative: Use Azure Storage Explorer GUI"
    echo "  Download from: https://azure.microsoft.com/en-us/products/storage/storage-explorer/"
    echo "  Connect to: Local Emulator (Azurite)"
    exit 1
fi

# Function to peek at messages
peek_messages() {
    local count=${1:-10}
    echo -e "${BLUE}📬 Peeking at $count message(s) in queue: $QUEUE_NAME${NC}"
    echo ""
    
    az storage message peek \
        --queue-name "$QUEUE_NAME" \
        --num-messages "$count" \
        --connection-string "$CONNECTION_STRING" \
        --output json 2>/dev/null | jq -r '.[] | "\n=== Message ===\nInsertion Time: \(.insertionTime)\nContent:\n" + (.content | fromjson | tostring)'
}

# Function to receive (and delete) messages
receive_messages() {
    local count=${1:-1}
    echo -e "${YELLOW}📥 Receiving $count message(s) from queue: $QUEUE_NAME${NC}"
    echo ""
    
    az storage message get \
        --queue-name "$QUEUE_NAME" \
        --num-messages "$count" \
        --connection-string "$CONNECTION_STRING" \
        --output json 2>/dev/null | jq -r '.[] | "\n=== Message (REMOVED FROM QUEUE) ===\nInsertion Time: \(.insertionTime)\nContent:\n" + (.content | fromjson | tostring)'
}

# Function to count messages
count_messages() {
    echo -e "${BLUE}🔢 Counting messages in queue: $QUEUE_NAME${NC}"
    
    local metadata=$(az storage queue metadata show \
        --name "$QUEUE_NAME" \
        --connection-string "$CONNECTION_STRING" \
        --output json 2>/dev/null)
    
    local count=$(echo "$metadata" | jq -r '.approximateMessageCount // 0')
    echo -e "${GREEN}Total messages: $count${NC}"
}

# Function to watch for new messages
watch_messages() {
    echo -e "${BLUE}👁️  Watching queue: $QUEUE_NAME (Ctrl+C to stop)${NC}"
    echo ""
    
    while true; do
        clear
        echo -e "${BLUE}=== Azurite Queue Monitor - $(date) ===${NC}\n"
        count_messages
        echo ""
        peek_messages 5
        sleep 2
    done
}

# Function to clear all messages
clear_queue() {
    echo -e "${RED}🗑️  Clearing all messages from queue: $QUEUE_NAME${NC}"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        az storage queue clear \
            --name "$QUEUE_NAME" \
            --connection-string "$CONNECTION_STRING"
        echo -e "${GREEN}✅ Queue cleared${NC}"
    else
        echo "Cancelled"
    fi
}

# Parse arguments
case "${1:-}" in
    --peek)
        peek_messages "${2:-10}"
        ;;
    --receive)
        receive_messages "${2:-1}"
        ;;
    --count)
        count_messages
        ;;
    --watch)
        watch_messages
        ;;
    --clear)
        clear_queue
        ;;
    --help|-h|"")
        echo "Usage: $0 [OPTION]"
        echo ""
        echo "Options:"
        echo "  --peek N     Peek at N messages without removing them (default: 10)"
        echo "  --receive N  Receive and remove N messages (default: 1)"
        echo "  --count      Show message count only"
        echo "  --watch      Continuously watch for new messages"
        echo "  --clear      Clear all messages from queue"
        echo "  --help       Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 --peek 5          # Show 5 latest messages"
        echo "  $0 --watch           # Monitor queue in real-time"
        echo "  $0 --count           # Show message count"
        ;;
    *)
        echo "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac
