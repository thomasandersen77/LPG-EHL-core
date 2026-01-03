# REST API Attempt (Archived)

**Archived:** 2025-01-03  
**Reason:** Architectural misunderstanding

## What Happened

This folder contains an implementation attempt based on the incorrect assumption that Nets Cloud Connect was a REST API over HTTPS.

### Initial Understanding (WRONG)
- Believed Cloud Connect was REST-based: `https://api.nets.eu/terminal/v1`
- Implemented HTTP client with polling for payment status
- Created DTOs for request/response
- Used Spring WebClient for REST calls

### Actual Architecture (CORRECT)
Nets Cloud Connect is a **secure SSL/TLS socket tunnel** that uses the **Baxi protocol** internally.

- **Host:** `3.33.230.243`
- **Port:** `6001`
- **Protocol:** SSL/TLS encrypted socket
- **Payload:** Baxi protocol frames (same as direct TCP/ECR)

## What's Preserved Here

| File | Description |
|------|-------------|
| `NetsCloudClient.kt` | REST client (incorrect approach) |
| `NetsCloudConfig.kt` | Spring Configuration Properties |
| `NetsCloudPaymentGateway.kt` | Payment gateway with polling |
| `NetsCloudClientTest.kt` | WireMock tests (12 tests) |
| `NetsCloudPaymentGatewayTest.kt` | Unit tests (10 tests) |
| `.env.local.example` | Environment variable template |

## Correct Implementation

See `lpg-ehl-core` for the correct SSL socket-based implementation:
- `NetsBaxProtocol.kt` - Restored from archive
- `CloudTerminalClient.kt` - New SSL socket client

## Sources

Confirmation from:
1. **ChatGPT**: "Nets Cloud Connect is not a REST API—it's a secure SSL/TLS socket tunnel to 3.33.230.243:6001"
2. **Gemini**: "Terminal configuration: ECR IP 3.33.230.243, Port 6001, Communication: Ethernet/WIFI with TLS"

## Lesson Learned

Always verify protocol details before implementing integration layer. Cloud Connect = SSL Tunnel + Baxi Protocol, NOT REST API.
