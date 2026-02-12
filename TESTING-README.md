# Payment Terminal API Testing Tools

Quick reference for testing and mocking the payment terminal API.

## 📁 Files Overview

| File | Purpose |
|------|---------|
| `openapi-payment-terminal.yaml` | OpenAPI 3.0 specification |
| `wiremock-capture.sh` | Capture real API responses via WireMock proxy |
| `generate-wiremock-stubs.py` | Generate synthetic stubs from OpenAPI spec |
| `curl-examples.sh` | Reusable bash functions for API calls |
| `WIREMOCK-CAPTURE.md` | Detailed WireMock documentation |

## 🚀 Quick Start Guides

### 1. Test Against Real Server

```bash
# Load curl functions
source curl-examples.sh

# Check server health
health_check

# Open terminal
terminal_open

# Make a purchase (125.00 NOK)
purchase 12500
```

### 2. Capture API Responses

```bash
# Start payment terminal server first on port 8080
# Then run:
./wiremock-capture.sh

# This captures all responses to ./wiremock/mappings/
```

### 3. Generate Mock Stubs (No Server Needed)

```bash
# Install PyYAML first if needed
pip install pyyaml

# Generate stubs from OpenAPI spec
./generate-wiremock-stubs.py

# Outputs to ./wiremock/mappings/stub-*.json
```

### 4. Run Mock Server

```bash
# Start WireMock with captured/generated mappings
java -jar wiremock-standalone-3.3.1.jar \
  --port 8080 \
  --root-dir=./wiremock

# Test against mock
source curl-examples.sh
BASE_URL=http://localhost:8080 health_check
```

## 🔧 Common Tasks

### Test a Purchase Flow

```bash
source curl-examples.sh

# Complete flow: health → status → open → purchase
purchase_flow 12500
```

### Capture Production-Like Responses

```bash
# 1. Ensure terminal is in desired state (e.g., open and ready)
# 2. Run capture
./wiremock-capture.sh

# 3. Review captured mappings
ls -la wiremock/mappings/

# 4. Edit mappings to remove sensitive data if needed
```

### Create Integration Tests

```bash
# Start mock server in background
java -jar wiremock-standalone-3.3.1.jar \
  --port 8080 \
  --root-dir=./wiremock &

WIREMOCK_PID=$!

# Run your tests
mvn test
# or
./gradlew test

# Stop mock server
kill $WIREMOCK_PID
```

### Change Target Server

```bash
# For curl-examples.sh
export BASE_URL=http://localhost:18080
source curl-examples.sh
purchase 12500

# For wiremock-capture.sh
# Edit the script and change TARGET_SERVER variable
```

## 📚 API Command Reference

### Health & Status
```bash
health_check                    # GET /health
terminal_status                 # GET /v1/terminal/status
```

### Terminal Lifecycle
```bash
terminal_open                   # Open terminal
terminal_close                  # Close terminal
terminal_restart                # Complete restart flow
```

### Financial Operations
```bash
purchase [amount] [operator] [currency] [text] [request_id]
purchase 12500                  # 125.00 NOK
purchase 20000 1234            # Custom operator
purchase 15000 0000 NOK "Text" "req-123"  # With idempotency

refund [amount] [operator] [text]
refund 5000                    # 50.00 NOK refund

cashback [purchase] [cashback] [operator]
cashback 10000 5000            # 100 NOK + 50 cashback
```

### Administrative Operations
```bash
avstemming [password]          # Reconciliation
cancel_operation [password]    # Cancel current operation
reversal [password]            # Reverse last transaction
z_report [password]            # End of day Z-report
x_report [password]            # Current totals X-report
last_receipt [password]        # Print last receipt
admin_code <code> [password]   # Generic admin code
```

### Workflows
```bash
purchase_flow [amount]         # Complete purchase flow
terminal_restart               # Restart terminal
end_of_day [password]          # Full end-of-day procedure
```

### Events
```bash
poll_events [cursor]           # Poll for events
stream_events [cursor]         # Stream events via SSE
```

## 💡 Tips & Tricks

### Pretty Print JSON
```bash
# curl-examples.sh uses jq automatically
# Install jq if needed:
brew install jq

# Manual curl without functions:
curl -s http://localhost:8080/health | jq .
```

### View Captured Responses
```bash
# List all mappings
ls -la wiremock/mappings/

# View a specific mapping
cat wiremock/mappings/mapping-*.json | jq .

# Search for specific endpoint
grep -l "/v1/terminal/status" wiremock/mappings/*.json
```

### Edit Mappings for Testing
```bash
# Example: Always return terminal_not_ready error
# Edit mapping file:
{
  "request": {
    "url": "/v1/terminal/status",
    "method": "GET"
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "VendorDllLoadable": true,
      "TerminalOpen": false,
      "TerminalReady": false,
      "LastError": "Terminal initialization failed"
    }
  }
}
```

### Test Error Scenarios
```bash
# Purchase when terminal not ready → 503 error
# WireMock captures this automatically

# Create custom error mapping:
{
  "request": {
    "url": "/v1/payments/purchase",
    "method": "POST"
  },
  "response": {
    "status": 409,
    "jsonBody": {
      "Error": "Another operation is currently in progress",
      "ErrorCode": "terminal_busy"
    }
  }
}
```

### Response Templating (Dynamic Responses)
```bash
# Add to mapping for dynamic OperationId:
{
  "response": {
    "status": 200,
    "jsonBody": {
      "Success": true,
      "OperationId": "{{randomValue type='UUID'}}",
      "CallResult": 1
    },
    "transformers": ["response-template"]
  }
}
```

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
java -jar wiremock-standalone-3.3.1.jar --port 9090 ...
```

### WireMock Not Recording
- Ensure `TARGET_SERVER` is correct and reachable
- Check if server requires authentication
- Review WireMock console output for errors

### Curl Functions Not Working
```bash
# Ensure file is sourced, not executed
source curl-examples.sh   # ✓ Correct
./curl-examples.sh        # ✗ Won't load functions

# Check BASE_URL
echo $BASE_URL

# Set BASE_URL if needed
export BASE_URL=http://localhost:8080
```

### No Stubs Generated
```bash
# Check Python and PyYAML
python3 --version
pip install pyyaml

# Run with verbose output
python3 -v generate-wiremock-stubs.py
```

## 📖 More Information

- **Detailed WireMock Guide**: See [WIREMOCK-CAPTURE.md](WIREMOCK-CAPTURE.md)
- **API Specification**: See [openapi-payment-terminal.yaml](openapi-payment-terminal.yaml)
- **WireMock Documentation**: https://wiremock.org/
- **OpenAPI 3.0 Spec**: https://spec.openapis.org/oas/v3.0.3

## 🎯 Example Scenarios

### Development Without Terminal Hardware
1. Generate stubs: `./generate-wiremock-stubs.py`
2. Start WireMock: `java -jar wiremock-standalone-3.3.1.jar --port 8080 --root-dir=./wiremock`
3. Develop against mock server

### Integration Testing
1. Capture real responses: `./wiremock-capture.sh`
2. Edit mappings for edge cases
3. Run tests against WireMock
4. Validate both success and error paths

### Pre-Production Validation
1. Use `curl-examples.sh` functions
2. Test against staging environment
3. Verify all workflows: purchase, refund, end-of-day
4. Compare responses with OpenAPI spec

### Debugging Production Issues
1. Capture production-like responses
2. Replay scenarios locally with WireMock
3. Edit mappings to reproduce specific errors
4. Test fixes without impacting production
