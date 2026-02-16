# Nets Cloud Connect Integration

## Overview

**Nets Cloud Connect** is a secure SSL/TLS tunnel service that enables payment terminal communication over encrypted channels. Our application uses the **Baxi protocol** (same as direct TCP/ECR) but sends it through an encrypted SSL connection to Nets cloud infrastructure.

**IMPORTANT:** Cloud Connect is NOT a REST API. It is an SSL/TLS socket tunnel using the standard Baxi protocol.

## Architecture

```
┌─────────────────┌         ┌────────────────────────────┌         ┌─────────────────┌
│                 │         │                            │         │                 │
│  LPG-EHL Edge    ├─────────▶│  Nets Cloud Connect     ├─────────▶│  Payment        │
│  (Our App)       │ SSL/TLS │  3.33.230.243:6001      │   ECR   │  Terminal       │
│                 │◀─────────│  (Baxi Protocol Frames) │◀─────────│  (Ingenico)     │
└─────────────────┘         └────────────────────────────┘         └─────────────────┘
      CloudTerminalClient              Encrypted Tunnel                  Baxi Protocol
```

### Key Benefits

✅ **Encrypted Communication** - All data secured via TLS 1.2/1.3  
✅ **Cloud Routing** - Nets handles network routing and failover  
✅ **Same Protocol** - Uses existing Baxi protocol (no protocol changes)  
✅ **Proven Architecture** - Same as direct TCP/ECR, just encrypted  
✅ **No Firewall Configuration** - Terminal connects TO Nets (outbound only)

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

### Cloud Connect Parameters

**No credentials or API keys required!** The SSL/TLS connection uses standard Java SSL libraries.

**Configuration:**

```kotlin
val client = CloudTerminalClient(
    host = "3.33.230.243",  // Nets Cloud Connect endpoint
    port = 6001,             // SSL/TLS port
    connectTimeoutMs = 10000, // 10 seconds for SSL handshake
    readTimeoutMs = 30000     // 30 seconds for terminal response
)
```

### SSL/TLS Details

- **Protocol:** TLS 1.2 / TLS 1.3
- **Certificate:** Nets provides valid SSL certificate (no custom certs needed)
- **Authentication:** None required at socket level (handled by terminal/Nets)
- **Cipher Suites:** Default Java SSL cipher suites

### Environment Variables (Optional)

If you want to override defaults:

```bash
# Nets Cloud Connect endpoint (default: 3.33.230.243)
NETS_CLOUD_HOST=3.33.230.243

# Port (default: 6001)
NETS_CLOUD_PORT=6001

# SSL handshake timeout in milliseconds (default: 10000)
NETS_CONNECT_TIMEOUT_MS=10000

# Response read timeout in milliseconds (default: 30000)
NETS_READ_TIMEOUT_MS=30000
```

## Payment Flow

### 1. Initiate Payment

```kotlin
CloudTerminalClient().use { client ->
    // Connect to Nets Cloud Connect
    client.connect()
    
    // Create purchase command (100.00 NOK)
    val command = NetsBaxProtocol.createPurchaseCommand(
        amountCents = 10000,
        operatorId = "1"
    )
    
    // Send command and wait for response
    val response = client.sendCommand(command)
    
    // Parse response
    val result = response.parse()
    when (result) {
        is BaxResponse.Success -> println("Payment approved!")
        is BaxResponse.Error -> println("Payment failed: ${result.message}")
        else -> println("Unexpected response")
    }
}
```

### 2. Behind the Scenes

```
LPG Edge                     Nets Cloud Connect           Terminal
  (Our App)                     (SSL Tunnel)               (Ingenico)
    │                               │                           │
    ├─── SSL Connect ─────────────▶│                           │
    │◀── TLS Handshake (OK) ────────┤                           │
    │                                │                           │
    ├─── Send Baxi Frame ───────────▶│                           │
    │    [Encrypted Payload]         ├─── Forward to Terminal ──▶│
    │    P;10;1;10000;0              │    [Baxi Protocol]         │
    │                                │                           │
    │                                │       [Customer Action]    │
    │                                │       Insert/Tap Card      │
    │                                │                           │
    │                                │◀── Response Frame ──────┤
    │◀── Receive Frame ───────────┤    [Baxi Protocol]         │
    │    [Encrypted Response]        │                           │
    │    00;TXN-ID;AUTH-CODE         │                           │
    │                                │                           │
```

### 3. Response Handling

Baxi protocol responses:

- **00** or **OK**: Payment approved
- **Error codes** (e.g., 01, 02): Payment declined
- **ACK**: Command received
- **NAK**: Command rejected

## Implementation

### Components

#### NetsBaxProtocol
Core protocol implementation for creating and parsing Baxi frames.

**Location:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/NetsBaxProtocol.kt`

**Key Methods:**
- `createPurchaseCommand(amountCents, operatorId)` - Create payment frame
- `createPreauthCommand(amountCents, operatorId)` - Create pre-auth frame
- `createRefundCommand(amountCents, operatorId)` - Create refund frame
- `createCancelCommand()` - Create cancel frame
- `parseResponse(data)` - Parse terminal response

**Framing Modes:**
- `TCP_ETHERNET` - 2-byte length header (default for Cloud Connect)
- `SERIAL` - STX/ETX/LRC framing (legacy)

#### CloudTerminalClient
SSL/TLS socket client for Nets Cloud Connect communication.

**Location:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/CloudTerminalClient.kt`

**Key Methods:**
- `connect()` - Establish SSL/TLS connection
- `sendCommand(command)` - Send Baxi frame and wait for response
- `sendAck()` / `sendNak()` - Protocol control
- `close()` - Clean up connection

**Features:**
- SSL/TLS 1.2/1.3 support
- Automatic handshake
- Timeout handling
- Response accumulation and frame detection

## Testing

### Unit Testing

Test protocol framing:

```kotlin
@Test
fun `test purchase command framing`() {
    NetsBaxProtocol.framingMode = FramingMode.TCP_ETHERNET
    
    val command = NetsBaxProtocol.createPurchaseCommand(
        amountCents = 10000,
        operatorId = "1"
    )
    
    val payload = String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)
    assertEquals("P;10;1;10000;0", payload)
}
```

### Integration Testing (With Terminal)

1. Configure terminal as described in Terminal Setup section
2. Run integration test:

```kotlin
CloudTerminalClient().use { client ->
    client.connect()
    
    val command = NetsBaxProtocol.createPurchaseCommand(100)
    val response = client.sendCommand(command)
    
    println("Response: ${response.toHexString()}")
    val result = response.parse()
    println("Result: $result")
}
```

3. Insert test card on terminal to complete transaction

### Local Development (Without Terminal)

Use in-memory emulator (see `lpg-ehl-emulator`):

```kotlin
val emulator = TerminalEmulator()
emulator.start()

// Connect to local emulator instead of Nets Cloud
val client = CloudTerminalClient(
    host = "localhost",
    port = 9000
)
```

## Troubleshooting

### Terminal Not Receiving Payments

**Check terminal ECR settings:**
- Is ECR enabled? (ECR/TLS = Yes)
- Is ECR IP correct? (3.33.230.243)
- Is ECR Port correct? (6001)
- Is terminal online? (Check network connection)

**Check SSL connection:**
```bash
# Test SSL connection to Nets Cloud
openssl s_client -connect 3.33.230.243:6001
```

If connection succeeds, you should see:
- SSL handshake details
- Certificate chain
- "Verify return code: 0 (ok)"

**Check logs:**
```bash
# Enable debug logging in lpg-ehl-core
export LOGGING_LEVEL_NO_CLOUDBERRIES_LPG_PAYMENT=DEBUG
mvn spring-boot:run
```

### SSL/TLS Errors

**Connection Timeout:**
- Verify firewall allows outbound connections to port 6001
- Check network connectivity: `ping 3.33.230.243`
- Increase `connectTimeoutMs` in CloudTerminalClient

**SSL Handshake Failed:**
- Verify Java has up-to-date CA certificates
- Check Java version supports TLS 1.2/1.3
- Try: `keytool -list -keystore $JAVA_HOME/lib/security/cacerts`

**Certificate Verification Failed:**
- Contact Nets support - certificate may have changed
- Verify system time is correct (certificates are time-sensitive)

### Protocol Errors

**Terminal sends NAK:**
- Command format may be incorrect
- Check framing mode: `NetsBaxProtocol.framingMode = FramingMode.TCP_ETHERNET`
- Verify amount is in cents/øre (100 = 1.00 NOK)

**No Response / Timeout:**
- Increase `readTimeoutMs` (cloud latency may be higher)
- Terminal may be offline or unresponsive
- Check terminal display for errors

## Architecture History

### Current: SSL/TLS Socket (2025-01-03)

**Active Implementation:**
- `CloudTerminalClient.kt` - SSL/TLS socket client
- `NetsBaxProtocol.kt` - Baxi protocol (TCP_ETHERNET mode)
- Connection: `3.33.230.243:6001` via SSL/TLS

### Previous: REST API Attempt (2025-01-02 - ARCHIVED)

**Why it was wrong:**
- Incorrectly assumed Cloud Connect was a REST API
- Implemented HTTP client with polling
- Archived to `legacy/archived/rest-api-attempt/`

**Lesson learned:**
- "Cloud Connect" is a marketing term for SSL tunnel service
- Always verify protocol details before implementation

### Legacy: Direct TCP/ECR (Pre-2025)

**Original implementation:**
- Direct TCP socket to terminal IP:8009
- Baxi protocol with STX/ETX/LRC framing
- Archived to `legacy/archived/baxi-protocol/`

**Why it changed:**
- Required static IP configuration
- Firewall/NAT complications
- No encryption
- Cloud Connect provides secure routing

## Support

For Nets Cloud Connect support:
- **Nets Customer Service:** [Contact details from Nets]
- **Technical Documentation:** Provided by Nets via email or portal
- **Terminal Issues:** Contact Nets terminal support

For questions about this integration:
- See `WARP.md` for system architecture
- Check `CHANGELOG.md` for recent changes
- Review code in `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/`
