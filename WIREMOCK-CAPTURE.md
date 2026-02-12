# WireMock Capture for Payment Terminal API

This setup captures real requests/responses from the payment terminal API and saves them in WireMock format for testing and development.

## Prerequisites

- Java 11+ (for running WireMock)
- curl (for triggering API requests)
- Payment terminal server running on `http://localhost:8080` (or update `TARGET_SERVER` in the script)

## Quick Start

You have two options:

### Option 1: Capture from Real Server (Recommended)

Capture actual request/response pairs from a running payment terminal server:

```bash
# Run the capture script
./wiremock-capture.sh
```

The script will:
1. Download WireMock standalone JAR (if not present)
2. Start WireMock in proxy/recording mode on port 9090
3. Trigger curl requests for all API endpoints through the proxy
4. Save captured request/response mappings to `./wiremock/mappings/`
5. Stop WireMock

**Requirements:** Payment terminal server must be running on `http://localhost:8080`

### Option 2: Generate Stubs from OpenAPI Spec

Generate WireMock stubs directly from the OpenAPI spec (no server needed):

```bash
# Generate stub mappings
./generate-wiremock-stubs.py
```

This generates synthetic responses based on the schema definitions in `openapi-payment-terminal.yaml`.

**Requirements:** Python 3 with PyYAML (`pip install pyyaml`)

## What Gets Captured

The script captures the following endpoints from `openapi-payment-terminal.yaml`:

### Health & Status
- `GET /health` - Health check
- `GET /v1/terminal/status` - Terminal status

### Terminal Lifecycle
- `POST /v1/terminal/open` - Open terminal
- `POST /v1/terminal/close` - Close terminal

### Financial Operations
- `POST /v1/payments/purchase` - Card purchase
- `POST /v1/payments/refund` - Refund/return
- `POST /v1/payments/cashback` - Purchase with cashback

### Administration
- `POST /v1/admin/avstemming` - End-of-day reconciliation
- `POST /v1/admin/cancel` - Cancel operation
- `POST /v1/admin/reversal` - Reverse last transaction
- `POST /v1/admin/z-report` - Z-report
- `POST /v1/admin/last-receipt` - Print last receipt
- `POST /v1/admin/code` - Generic admin code (e.g., X-report)

### Events
- `GET /v1/events?since=0` - Poll for events

### Diagnostics
- `GET /v1/diag/schema` - Terminal schema (may be disabled)

## Output Structure

```
wiremock/
├── mappings/              # Request/response mappings (JSON)
│   ├── mapping-*.json
│   └── ...
└── __files/              # Response body files
    └── body-*.json
```

Each mapping file contains:
- Request matcher (URL, method, headers, body)
- Response template (status, headers, body)
- Metadata (timestamps, etc.)

## Using Captured Mappings

### 1. Run WireMock in Standalone Mode

```bash
java -jar wiremock-standalone-3.3.1.jar --port 8080 --root-dir=./wiremock
```

This starts a mock server on port 8080 that replays captured responses.

### 2. Test Against Mock Server

```bash
# Test the mock server
curl http://localhost:8080/health

curl -X GET http://localhost:8080/v1/terminal/status

curl -X POST http://localhost:8080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{"AmountMinor": 100, "OperatorId": "0000"}'
```

### 3. Integrate with Tests

Use WireMock in your integration tests:

**Maven dependency:**
```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.3.1</version>
    <scope>test</scope>
</dependency>
```

**Example test:**
```java
@Test
public void testPurchase() {
    // WireMock will use mappings from ./wiremock/mappings/
    WireMockServer wireMock = new WireMockServer(
        options().port(8080).usingFilesUnderDirectory("wiremock")
    );
    wireMock.start();
    
    // Your test code here
    
    wireMock.stop();
}
```

## Configuration

Edit `wiremock-capture.sh` to customize:

```bash
# WireMock proxy port (where to send test requests)
WIREMOCK_PORT=9090

# Target server (the real payment terminal API)
TARGET_SERVER="http://localhost:8080"

# Output directory
WIREMOCK_DIR="./wiremock"
```

## Troubleshooting

### No mappings captured
- Ensure the payment terminal server is running at `$TARGET_SERVER`
- Check if the server is accessible: `curl http://localhost:8080/health`
- Review WireMock logs in the script output

### Some requests fail
This is expected! The script captures both success and error responses. Error responses (409 Conflict, 503 Service Unavailable, etc.) are useful for testing error handling.

### WireMock won't start
- Check if port 9090 is already in use: `lsof -i :9090`
- Verify Java is installed: `java -version`

### Captured responses are too generic
For dynamic responses, you may need to manually edit the mapping files to add response templating or request matching logic.

## Advanced Usage

### Manual Request Capture

Start WireMock in recording mode manually:

```bash
java -jar wiremock-standalone-3.3.1.jar \
  --port 9090 \
  --proxy-all="http://localhost:8080" \
  --record-mappings \
  --root-dir=./wiremock \
  --verbose
```

Then make requests through the proxy:

```bash
curl http://localhost:9090/health
curl http://localhost:9090/v1/terminal/status
# etc.
```

Press Ctrl+C to stop WireMock and save mappings.

### Editing Mappings

Mapping files are JSON and can be edited manually. Example:

```json
{
  "request": {
    "url": "/v1/terminal/status",
    "method": "GET"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "VendorDllLoadable": true,
      "TerminalOpen": true,
      "TerminalReady": true,
      "LastError": null
    }
  }
}
```

### Response Templating

Add dynamic responses using Handlebars templates:

```json
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

See [WireMock docs](https://wiremock.org/docs/response-templating/) for more.

## Integration with CI/CD

Use captured mappings in automated tests:

```bash
# Start WireMock in background
java -jar wiremock-standalone-3.3.1.jar --port 8080 --root-dir=./wiremock &
WIREMOCK_PID=$!

# Run tests
mvn test

# Stop WireMock
kill $WIREMOCK_PID
```

## References

- [WireMock Documentation](https://wiremock.org/)
- [WireMock Request Matching](https://wiremock.org/docs/request-matching/)
- [WireMock Response Templating](https://wiremock.org/docs/response-templating/)
- [OpenAPI Spec](./openapi-payment-terminal.yaml)

## Notes

- Captured responses reflect the actual state of the payment terminal at capture time
- For financial operations (purchase, refund), captured responses may show rejections if terminal wasn't ready
- Pre-configure the terminal to be in a known state before capture for consistent results
- Consider creating multiple mapping sets for different scenarios (success, errors, edge cases)
