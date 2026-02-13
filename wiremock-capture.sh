#!/usr/bin/env bash
set -euo pipefail

# WireMock capture script for payment terminal API
# This script:
# 1. Downloads WireMock standalone if not present
# 2. Starts WireMock in proxy/recording mode
# 3. Generates curl requests from OpenAPI spec
# 4. Saves captured request/response mappings

WIREMOCK_VERSION="3.3.1"
WIREMOCK_JAR="wiremock-standalone-${WIREMOCK_VERSION}.jar"
WIREMOCK_URL="https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/${WIREMOCK_VERSION}/${WIREMOCK_JAR}"

# Configuration
WIREMOCK_PORT=9090
TARGET_SERVER="http://localhost:8080"  # The actual payment terminal server
WIREMOCK_DIR="./wiremock"
MAPPINGS_DIR="${WIREMOCK_DIR}/mappings"
FILES_DIR="${WIREMOCK_DIR}/__files"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Download WireMock if not present
download_wiremock() {
    if [ -f "$WIREMOCK_JAR" ]; then
        log_info "WireMock already downloaded: $WIREMOCK_JAR"
    else
        log_info "Downloading WireMock ${WIREMOCK_VERSION}..."
        curl -L -o "$WIREMOCK_JAR" "$WIREMOCK_URL"
        log_info "Downloaded WireMock successfully"
    fi
}

# Create directories
setup_directories() {
    mkdir -p "$MAPPINGS_DIR"
    mkdir -p "$FILES_DIR"
    log_info "Created WireMock directories"
}

# Start WireMock in recording mode
start_wiremock() {
    log_info "Starting WireMock in recording/proxy mode..."
    log_info "  Proxy target: $TARGET_SERVER"
    log_info "  WireMock port: $WIREMOCK_PORT"
    
    java -jar "$WIREMOCK_JAR" \
        --port $WIREMOCK_PORT \
        --proxy-all="$TARGET_SERVER" \
        --record-mappings \
        --root-dir="$WIREMOCK_DIR" \
        --verbose &
    
    WIREMOCK_PID=$!
    log_info "WireMock started with PID: $WIREMOCK_PID"
    
    # Wait for WireMock to start
    sleep 3
    
    # Check if WireMock is running
    if ! kill -0 $WIREMOCK_PID 2>/dev/null; then
        log_error "WireMock failed to start"
        exit 1
    fi
}

# Stop WireMock
stop_wiremock() {
    if [ -n "${WIREMOCK_PID:-}" ]; then
        log_info "Stopping WireMock (PID: $WIREMOCK_PID)..."
        kill $WIREMOCK_PID 2>/dev/null || true
        wait $WIREMOCK_PID 2>/dev/null || true
        log_info "WireMock stopped"
    fi
}

# Trigger API calls through WireMock proxy
trigger_requests() {
    local proxy_url="http://localhost:${WIREMOCK_PORT}"
    
    log_info "Triggering API requests through WireMock proxy..."
    log_warn "Note: Some requests may fail if the payment terminal is not ready"
    log_warn "      This is expected - we're capturing both success and error responses"
    echo ""
    
    # Health check
    log_info "1. GET /health"
    curl -s -X GET "${proxy_url}/health" -w "\n  Status: %{http_code}\n\n" || true
    
    # Terminal status
    log_info "2. GET /v1/terminal/status"
    curl -s -X GET "${proxy_url}/v1/terminal/status" -w "\n  Status: %{http_code}\n\n" || true
    
    # Terminal open
    log_info "3. POST /v1/terminal/open"
    curl -s -X POST "${proxy_url}/v1/terminal/open" \
        -H "Content-Type: application/json" \
        -w "\n  Status: %{http_code}\n\n" || true
    
    sleep 2
    
    # Purchase (small amount to avoid actual transaction)
    log_info "4. POST /v1/payments/purchase"
    curl -s -X POST "${proxy_url}/v1/payments/purchase" \
        -H "Content-Type: application/json" \
        -d '{
          "AmountMinor": 100,
          "OperatorId": "0000",
          "Currency": "NOK",
          "OptionalData": "Test WireMock Capture",
          "ClientRequestId": "wiremock-test-001"
        }' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Refund
    log_info "5. POST /v1/payments/refund"
    curl -s -X POST "${proxy_url}/v1/payments/refund" \
        -H "Content-Type: application/json" \
        -d '{
          "AmountMinor": 50,
          "OperatorId": "0000",
          "OptionalData": "Test Refund"
        }' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Cashback
    log_info "6. POST /v1/payments/cashback"
    curl -s -X POST "${proxy_url}/v1/payments/cashback" \
        -H "Content-Type: application/json" \
        -d '{
          "PurchaseMinor": 100,
          "CashbackMinor": 50,
          "Currency": "NOK",
          "OperatorId": "4321"
        }' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Admin operations
    log_info "7. POST /v1/admin/avstemming"
    curl -s -X POST "${proxy_url}/v1/admin/avstemming" \
        -H "Content-Type: application/json" \
        -d '{"Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    log_info "8. POST /v1/admin/cancel"
    curl -s -X POST "${proxy_url}/v1/admin/cancel" \
        -H "Content-Type: application/json" \
        -d '{"Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    log_info "9. POST /v1/admin/reversal"
    curl -s -X POST "${proxy_url}/v1/admin/reversal" \
        -H "Content-Type: application/json" \
        -d '{"Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    log_info "10. POST /v1/admin/z-report"
    curl -s -X POST "${proxy_url}/v1/admin/z-report" \
        -H "Content-Type: application/json" \
        -d '{"Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    log_info "11. POST /v1/admin/last-receipt"
    curl -s -X POST "${proxy_url}/v1/admin/last-receipt" \
        -H "Content-Type: application/json" \
        -d '{"Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    log_info "12. POST /v1/admin/code (X-report)"
    curl -s -X POST "${proxy_url}/v1/admin/code" \
        -H "Content-Type: application/json" \
        -d '{"Code": 12598, "Password": "0000"}' \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Events
    log_info "13. GET /v1/events?since=0"
    curl -s -X GET "${proxy_url}/v1/events?since=0" \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Diagnostics (may be disabled)
    log_info "14. GET /v1/diag/schema"
    curl -s -X GET "${proxy_url}/v1/diag/schema" \
        -w "\n  Status: %{http_code}\n\n" || true
    
    # Terminal close
    log_info "15. POST /v1/terminal/close"
    curl -s -X POST "${proxy_url}/v1/terminal/close" \
        -H "Content-Type: application/json" \
        -w "\n  Status: %{http_code}\n\n" || true
    
    echo ""
    log_info "All requests triggered"
}

# Main execution
main() {
    log_info "=== WireMock Capture Script ==="
    log_info "This will capture requests/responses from the payment terminal API"
    echo ""
    
    # Setup
    download_wiremock
    setup_directories
    
    # Start WireMock
    start_wiremock
    
    # Trap to ensure WireMock is stopped
    trap stop_wiremock EXIT
    
    # Wait a bit for WireMock to be fully ready
    sleep 2
    
    # Trigger requests
    trigger_requests
    
    # Give WireMock time to write mappings
    sleep 2
    
    log_info "=== Capture Complete ==="
    log_info "WireMock mappings saved to: ${MAPPINGS_DIR}"
    log_info "Response files saved to: ${FILES_DIR}"
    echo ""
    log_info "You can now use these mappings to run WireMock in standalone mode:"
    log_info "  java -jar ${WIREMOCK_JAR} --port 8080 --root-dir=${WIREMOCK_DIR}"
    echo ""
    
    # List captured mappings
    if [ -d "$MAPPINGS_DIR" ] && [ "$(ls -A $MAPPINGS_DIR)" ]; then
        log_info "Captured mappings:"
        ls -lh "$MAPPINGS_DIR"
    else
        log_warn "No mappings were captured. Check if the payment terminal server is running at $TARGET_SERVER"
    fi
}

# Run main
main
