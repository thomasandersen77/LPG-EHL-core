#!/usr/bin/env bash
# Payment Terminal API - Curl Examples
# Based on openapi-payment-terminal.yaml
#
# Usage:
#   1. Set BASE_URL to your server (default: http://localhost:8080)
#   2. Source this file: source curl-examples.sh
#   3. Call functions: health_check, terminal_status, purchase 12500, etc.

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}==>${NC} $1"
}

# ============================================================================
# Health & Status
# ============================================================================

health_check() {
    log "GET /health"
    curl -s -X GET "${BASE_URL}/health" | jq .
}

terminal_status() {
    log "GET /v1/terminal/status"
    curl -s -X GET "${BASE_URL}/v1/terminal/status" | jq .
}

# ============================================================================
# Terminal Lifecycle
# ============================================================================

terminal_open() {
    log "POST /v1/terminal/open"
    curl -s -X POST "${BASE_URL}/v1/terminal/open" \
        -H "Content-Type: application/json" | jq .
}

terminal_close() {
    log "POST /v1/terminal/close"
    curl -s -X POST "${BASE_URL}/v1/terminal/close" \
        -H "Content-Type: application/json" | jq .
}

# ============================================================================
# Financial Operations
# ============================================================================

purchase() {
    local amount_minor="${1:-12500}"  # Default 125.00 NOK
    local operator_id="${2:-0000}"
    local currency="${3:-NOK}"
    local optional_data="${4:-LPG Autogas}"
    local client_request_id="${5}"
    
    log "POST /v1/payments/purchase (Amount: ${amount_minor} øre)"
    
    local body=$(cat <<EOF
{
  "AmountMinor": ${amount_minor},
  "OperatorId": "${operator_id}",
  "Currency": "${currency}",
  "OptionalData": "${optional_data}"
EOF
)
    
    if [ -n "$client_request_id" ]; then
        body+=",\n  \"ClientRequestId\": \"${client_request_id}\""
    fi
    
    body+="\n}"
    
    echo -e "$body" | curl -s -X POST "${BASE_URL}/v1/payments/purchase" \
        -H "Content-Type: application/json" \
        -d @- | jq .
}

purchase_with_preavst() {
    local amount_minor="${1:-12500}"
    local password="${2:-0000}"
    
    log "POST /v1/payments/purchase with pre-avstemming"
    
    curl -s -X POST "${BASE_URL}/v1/payments/purchase" \
        -H "Content-Type: application/json" \
        -d "{
          \"AmountMinor\": ${amount_minor},
          \"OperatorId\": \"0000\",
          \"Currency\": \"NOK\",
          \"OptionalData\": \"Purchase with pre-avstemming\",
          \"PreAvstemming\": {
            \"Enabled\": true,
            \"Password\": \"${password}\"
          }
        }" | jq .
}

refund() {
    local amount_minor="${1:-5000}"  # Default 50.00 NOK
    local operator_id="${2:-0000}"
    local optional_data="${3:-Refund LPG}"
    
    log "POST /v1/payments/refund (Amount: ${amount_minor} øre)"
    
    curl -s -X POST "${BASE_URL}/v1/payments/refund" \
        -H "Content-Type: application/json" \
        -d "{
          \"AmountMinor\": ${amount_minor},
          \"OperatorId\": \"${operator_id}\",
          \"OptionalData\": \"${optional_data}\"
        }" | jq .
}

cashback() {
    local purchase_minor="${1:-10000}"  # Default 100.00 NOK
    local cashback_minor="${2:-5000}"   # Default 50.00 NOK
    local operator_id="${3:-4321}"
    
    log "POST /v1/payments/cashback (Purchase: ${purchase_minor}, Cashback: ${cashback_minor})"
    
    curl -s -X POST "${BASE_URL}/v1/payments/cashback" \
        -H "Content-Type: application/json" \
        -d "{
          \"PurchaseMinor\": ${purchase_minor},
          \"CashbackMinor\": ${cashback_minor},
          \"Currency\": \"NOK\",
          \"OperatorId\": \"${operator_id}\"
        }" | jq .
}

# ============================================================================
# Administrative Operations
# ============================================================================

avstemming() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/avstemming"
    
    curl -s -X POST "${BASE_URL}/v1/admin/avstemming" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

cancel_operation() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/cancel"
    
    curl -s -X POST "${BASE_URL}/v1/admin/cancel" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

reversal() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/reversal"
    echo -e "${YELLOW}WARNING: This is not reversible!${NC}"
    
    curl -s -X POST "${BASE_URL}/v1/admin/reversal" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

z_report() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/z-report"
    
    curl -s -X POST "${BASE_URL}/v1/admin/z-report" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

x_report() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/code (X-report: 12598)"
    
    curl -s -X POST "${BASE_URL}/v1/admin/code" \
        -H "Content-Type: application/json" \
        -d "{\"Code\": 12598, \"Password\": \"${password}\"}" | jq .
}

last_receipt() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/last-receipt"
    
    curl -s -X POST "${BASE_URL}/v1/admin/last-receipt" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

empty_printer_buffer() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/code (Empty printer buffer: 12593)"
    
    curl -s -X POST "${BASE_URL}/v1/admin/code" \
        -H "Content-Type: application/json" \
        -d "{\"Code\": 12593, \"Password\": \"${password}\"}" | jq .
}

software_download() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/software"
    echo -e "${YELLOW}WARNING: Long-running operation${NC}"
    
    curl -s -X POST "${BASE_URL}/v1/admin/software" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

dataset_download() {
    local password="${1:-0000}"
    
    log "POST /v1/admin/dataset"
    echo -e "${YELLOW}WARNING: Long-running operation${NC}"
    
    curl -s -X POST "${BASE_URL}/v1/admin/dataset" \
        -H "Content-Type: application/json" \
        -d "{\"Password\": \"${password}\"}" | jq .
}

admin_code() {
    local code="$1"
    local password="${2:-0000}"
    
    if [ -z "$code" ]; then
        echo "Usage: admin_code <code> [password]"
        echo "Example: admin_code 12598 0000  # X-report"
        return 1
    fi
    
    log "POST /v1/admin/code (Code: ${code})"
    
    curl -s -X POST "${BASE_URL}/v1/admin/code" \
        -H "Content-Type: application/json" \
        -d "{\"Code\": ${code}, \"Password\": \"${password}\"}" | jq .
}

# ============================================================================
# Events
# ============================================================================

poll_events() {
    local since="${1:-0}"
    
    log "GET /v1/events?since=${since}"
    
    curl -s -X GET "${BASE_URL}/v1/events?since=${since}" | jq .
}

stream_events() {
    local since="${1:-0}"
    
    log "GET /v1/events/stream?since=${since} (SSE)"
    echo "Press Ctrl+C to stop"
    echo ""
    
    curl -N -X GET "${BASE_URL}/v1/events/stream?since=${since}"
}

# ============================================================================
# Diagnostics (disabled by default)
# ============================================================================

diag_schema() {
    log "GET /v1/diag/schema"
    
    curl -s -X GET "${BASE_URL}/v1/diag/schema" | jq .
}

diag_send_json() {
    local json_str="$1"
    
    if [ -z "$json_str" ]; then
        echo "Usage: diag_send_json '<json_string>'"
        return 1
    fi
    
    log "POST /v1/diag/sendjson"
    
    curl -s -X POST "${BASE_URL}/v1/diag/sendjson" \
        -H "Content-Type: application/json" \
        -d "{\"json\": \"${json_str}\"}" | jq .
}

# ============================================================================
# Common Workflows
# ============================================================================

# Complete purchase flow
purchase_flow() {
    local amount="${1:-12500}"
    
    echo -e "\n${GREEN}=== Complete Purchase Flow ===${NC}\n"
    
    echo "1. Check health..."
    health_check
    echo ""
    
    echo "2. Check terminal status..."
    terminal_status
    echo ""
    
    echo "3. Open terminal (if not already open)..."
    terminal_open
    echo ""
    
    echo "4. Perform purchase (${amount} øre)..."
    purchase "$amount"
    echo ""
    
    echo -e "${GREEN}=== Flow complete ===${NC}"
}

# Terminal restart flow
terminal_restart() {
    echo -e "\n${GREEN}=== Terminal Restart ===${NC}\n"
    
    echo "1. Close terminal..."
    terminal_close
    echo ""
    
    echo "2. Wait 3 seconds..."
    sleep 3
    
    echo "3. Open terminal..."
    terminal_open
    echo ""
    
    echo "4. Check status..."
    terminal_status
    echo ""
    
    echo -e "${GREEN}=== Restart complete ===${NC}"
}

# End of day flow
end_of_day() {
    local password="${1:-0000}"
    
    echo -e "\n${GREEN}=== End of Day Procedure ===${NC}\n"
    
    echo "1. X-report (current totals)..."
    x_report "$password"
    echo ""
    
    echo "2. Avstemming (reconciliation)..."
    avstemming "$password"
    echo ""
    
    echo "3. Z-report (end of day)..."
    z_report "$password"
    echo ""
    
    echo -e "${GREEN}=== End of day complete ===${NC}"
}

# ============================================================================
# Help
# ============================================================================

payment_terminal_help() {
    cat <<'EOF'
Payment Terminal API - Curl Command Reference

Environment:
  BASE_URL    Server URL (default: http://localhost:8080)

Health & Status:
  health_check                    - GET /health
  terminal_status                 - GET /v1/terminal/status

Terminal Lifecycle:
  terminal_open                   - POST /v1/terminal/open
  terminal_close                  - POST /v1/terminal/close
  terminal_restart                - Complete restart flow

Financial Operations:
  purchase [amount] [operator_id] [currency] [optional_data] [client_request_id]
  purchase_with_preavst [amount] [password]
  refund [amount] [operator_id] [optional_data]
  cashback [purchase] [cashback] [operator_id]

Administrative Operations:
  avstemming [password]           - Reconciliation
  cancel_operation [password]     - Cancel current operation
  reversal [password]             - Reverse last transaction (NOT REVERSIBLE!)
  z_report [password]             - End of day Z-report
  x_report [password]             - Current totals X-report
  last_receipt [password]         - Print last receipt
  empty_printer_buffer [password] - Clear printer buffer
  software_download [password]    - Download software update
  dataset_download [password]     - Download dataset
  admin_code <code> [password]    - Generic admin code

Events:
  poll_events [cursor]            - Poll for events since cursor
  stream_events [cursor]          - Stream events via SSE (Ctrl+C to stop)

Diagnostics (disabled by default):
  diag_schema                     - Get terminal schema
  diag_send_json <json_string>    - Send raw JSON to terminal

Workflows:
  purchase_flow [amount]          - Complete purchase flow
  terminal_restart                - Restart terminal
  end_of_day [password]           - End of day procedure

Examples:
  # Basic purchase of 125.00 NOK
  purchase 12500

  # Purchase with custom operator ID
  purchase 20000 1234

  # Purchase with idempotency key
  purchase 15000 0000 NOK "LPG Autogas" "session-12345"

  # Refund 50.00 NOK
  refund 5000

  # End of day procedure
  end_of_day 0000

  # Stream events
  stream_events 0

Notes:
  - All amounts are in minor units (øre for NOK)
  - Default password is "0000"
  - Default operator ID is "0000" (or "4321" for cashback)
  - Use jq to format JSON output (install: brew install jq)

EOF
}

# Show help if sourced interactively
if [ -n "$PS1" ]; then
    echo "Payment Terminal API functions loaded."
    echo "Type 'payment_terminal_help' for usage information."
fi
