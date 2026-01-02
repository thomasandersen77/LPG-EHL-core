# Nets Cloud Connect Integration

## Overview

**Nets Cloud Connect** is a cloud-based payment terminal integration that replaces the legacy Baxi TCP/ECR protocol. Instead of managing TCP sockets and binary protocols, our application communicates with Nets via a modern REST API.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│                 │   REST  │                 │   ECR   │                 │
│  LPG-EHL API    │────────▶│  Nets Cloud     │────────▶│  Payment        │
│  (Our App)      │   API   │  (3.33.230.243) │  Proto  │  Terminal       │
│                 │◀────────│                 │◀────────│  (Ingenico)     │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

### Key Benefits

✅ **No TCP Socket Management** - No need to handle low-level networking  
✅ **No Binary Protocol** - No hex encoding/decoding or checksum calculations  
✅ **Cloud Reliability** - Nets handles terminal connectivity and failover  
✅ **Simplified Testing** - Easy to mock REST API responses  
✅ **Better Monitoring** - Nets provides logging and analytics

## Terminal Setup

Configure the terminal via **Merchant Menu**:

1. **Access Merchant Menu**
   - Swipe merchant card OR
   - Press `Menu` → `8` → Enter merchant code

2. **Navigate to Parameters**
   - Press `6` (Parameters)
   - Press `1` (Change)

3. **Configure Communication**
   - Press `2` (Communication)
   - Set **Komm. Type** to:
     - `Ethernet` (for DESK3500, Lane3000/IPP350)
     - `WIFI` or `Ethernet` (for MOVE 3500)

4. **Configure ECR Settings**
   - Press `3` (ECR/Kasse menu)
   - Set the following:
     - **ECR/TLS**: `Ja` (Yes)
     - **ECR IP**: `3.33.230.243` (Nets Cloud)
     - **ECR Port**: `6001`
     - **Kommstype**: Match communication type from step 3

5. **Save and Test**
   - Press green button to confirm
   - Press red button twice to exit
   - Download card agreement: Swipe auth card → `1` → `1`

**Important:** The terminal connects TO Nets cloud, not to your local server!

## Application Configuration

### 1. Get Credentials from Nets

Contact Nets support to receive:
- API base URL
- Username
- Password
- Terminal ID (TID)
- Merchant ID

### 2. Configure Environment

Copy `.env.local.example` to `.env.local` and fill in:

```bash
# Enable Nets Cloud Connect
NETS_CLOUD_ENABLED=true

# Credentials from Nets
NETS_CLOUD_URL=https://api.nets.eu/terminal/v1
NETS_CLOUD_USERNAME=your_username
NETS_CLOUD_PASSWORD=your_password
NETS_TERMINAL_ID=42696609
NETS_MERCHANT_ID=your_merchant_id
```

### 3. Optional Tuning

```bash
# Timeout for HTTP requests (seconds)
NETS_TIMEOUT_SECONDS=30

# Interval between status polls (milliseconds)
NETS_POLLING_INTERVAL_MS=500

# Maximum polling attempts (120 * 500ms = 60 seconds)
NETS_MAX_POLL_ATTEMPTS=120
```

## Payment Flow

### 1. Initiate Payment

```kotlin
val request = PaymentRequest(
    amountCents = 10000, // 100.00 NOK
    method = PaymentMethod.CARD,
    reference = "TXN-12345"
)

val payment = paymentGateway.startPayment(request)
```

### 2. Behind the Scenes

```
Application                     Nets Cloud                  Terminal
    │                               │                           │
    ├─── POST /sale ────────────────▶│                           │
    │    {amount: 10000}             │                           │
    │                                ├─── Push payment ─────────▶│
    │                                │                           │
    │◀── 202 {paymentId: "xyz"} ────┤                           │
    │                                │                           │
    ├─── GET /payments/xyz ─────────▶│                           │
    │◀── {status: "PENDING"} ────────┤                           │
    │                                │                           │
    │   (polling every 500ms)        │                           │
    │                                │                           │
    │                                │   [Customer inserts card] │
    │                                │                           │
    │                                │◀── Card response ─────────┤
    │                                │                           │
    ├─── GET /payments/xyz ─────────▶│                           │
    │◀── {status: "APPROVED"} ───────┤                           │
    │                                │                           │
```

### 3. Status Polling

The application automatically polls for payment completion:

- **PENDING** / **PROCESSING**: Continue polling
- **APPROVED**: Payment successful
- **DECLINED**: Payment rejected
- **CANCELLED**: Payment cancelled by user or timeout

## API Implementation

### Components

#### NetsCloudConfig
Spring Configuration Properties class that loads Nets settings from environment variables.

**Location:** `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/NetsCloudConfig.kt`

#### NetsCloudClient
REST client that communicates with Nets Cloud API using Spring's RestClient.

**Location:** `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/integration/NetsCloudClient.kt`

**Methods:**
- `initiateSale(amountCents, reference)` - Start a payment
- `initiateRefund(amountCents, originalTxId)` - Refund a transaction
- `checkPaymentStatus(paymentId)` - Poll payment status
- `cancelPayment(paymentId)` - Cancel ongoing payment

#### NetsCloudPaymentGateway
Implementation of the `PaymentGateway` interface using Nets Cloud Connect.

**Location:** `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/payment/NetsCloudPaymentGateway.kt`

**Features:**
- Implements existing `PaymentGateway` interface (no API changes!)
- Handles polling logic
- Maps Nets status codes to internal `PaymentStatus` enum
- Timeout and error handling

## Testing

### Local Development (Without Terminal)

Use `SimulatedPaymentGateway` for development:

```yaml
nets:
  cloud-connect:
    enabled: false  # Use simulated gateway
```

### Integration Testing (With Terminal)

1. Configure terminal as described above
2. Enable Nets Cloud Connect in `.env.local`
3. Start the API: `mvn spring-boot:run`
4. Test payment via API:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-token-12345" \
  -d '{
    "amountCents": 10000,
    "method": "CARD",
    "reference": "TEST-001"
  }'
```

5. Terminal should display payment request
6. Insert test card to complete

## Troubleshooting

### Terminal Not Receiving Payments

**Check terminal ECR settings:**
- Is ECR enabled? (ECR/TLS = Yes)
- Is ECR IP correct? (3.33.230.243)
- Is terminal online? (Check network connection)

**Check logs:**
```bash
# Enable debug logging
export LOGGING_LEVEL_NO_CLOUDBERRIES_LPG_API_INTEGRATION=DEBUG
mvn spring-boot:run
```

### API Errors

**401 Unauthorized:**
- Verify `NETS_CLOUD_USERNAME` and `NETS_CLOUD_PASSWORD`
- Contact Nets to verify credentials are active

**404 Not Found:**
- Verify `NETS_CLOUD_URL` is correct
- Check endpoint paths in documentation from Nets

**Timeout:**
- Increase `NETS_TIMEOUT_SECONDS`
- Increase `NETS_MAX_POLL_ATTEMPTS`
- Check terminal is powered on and connected

## Migration from Baxi Protocol

The old TCP/Baxi implementation has been archived to `_archived/baxi-protocol/`.

**What was removed:**
- `NetsBaxProtocol.kt` - Binary protocol implementation
- `PaymentTerminal.kt` - TCP socket wrapper
- `PaymentTerminalClient.kt` - High-level TCP client
- All hex encoding/decoding logic
- STX/ETX/LRC framing logic

**What stayed the same:**
- `PaymentGateway` interface (no breaking changes!)
- `PaymentRequest` / `Payment` domain models
- REST API endpoints
- Database schema

## Support

For Nets Cloud Connect support:
- **Nets Customer Service:** [Contact details from Nets]
- **Technical Documentation:** Provided by Nets via email or portal
- **Terminal Issues:** Contact Nets terminal support

For questions about this integration:
- See `WARP.md` for system architecture
- Check `CHANGELOG.md` for recent changes
- Review code in `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/`
