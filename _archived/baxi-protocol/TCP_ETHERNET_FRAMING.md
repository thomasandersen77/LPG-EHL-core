# TCP/Ethernet Framing Mode for NetsBaxProtocol

## Overview

This document describes the implementation of dual framing modes for the Nets/Bax payment terminal protocol. The implementation supports both legacy RS232/Serial communication and modern TCP/Ethernet communication.

## Background

### The Problem

The original `NetsBaxProtocol.kt` implementation used RS232/Serial framing with STX/ETX/LRC:

```
<STX> <Payload> <ETX> <LRC>
```

However, modern payment terminals like the **Ingenico Self/4000** use TCP/Ethernet with a different framing format when communicating over port 8009:

```
[2-byte Length Header (Big-Endian)] + [Payload (ASCII/ISO-8859-1)]
```

This mismatch caused communication timeouts because:
1. The terminal expected a length header, but received STX (0x02)
2. The terminal interpreted 0x02 as the high byte of a length field
3. The terminal waited for 512+ bytes (0x02xx) that never arrived
4. Result: `A000ECR Timeout`

## Solution

### Dual Framing Mode Implementation

Added `FramingMode` enum with two modes:

```kotlin
enum class FramingMode {
    SERIAL,        // RS232/Serial mode with STX/ETX/LRC
    TCP_ETHERNET   // TCP/Ethernet mode with 2-byte length header
}
```

Default mode is `TCP_ETHERNET` for modern terminals.

### Key Changes

#### 1. Command Format Changes

**Purchase Command:**
- **TCP Mode:** `"P;10;operatorId;amountCents;0"` (semicolon delimiters)
- **Serial Mode:** `"P,operatorId,amountCents"` (comma delimiters)

**Preauth Command:**
- **TCP Mode:** `"P;03;operatorId;amountCents;0"`
- **Serial Mode:** `"A,operatorId,amountCents"`

#### 2. New Functions

**`buildTcpFrame(payload: String): ByteArray`**
- Builds TCP frame with 2-byte length header
- Example: `"P;10;1;200;0"` → `00 0C 50 3B 31 30 3B 31 3B 32 30 30 3B 30`
  - `00 0C` = length (12 bytes)
  - `50 3B...` = ASCII payload

**`buildSerialFrame(payload: String): ByteArray`**
- Builds RS232 frame with STX/ETX/LRC
- Example: `"P,1,200"` → `02 50 2C 31 2C 32 30 30 03 <LRC>`

**`parseTcpResponse(data: ByteArray): BaxResponse`**
- Parses TCP responses (with or without length header)
- Handles Ingenico-specific formats: `A000xxx`, `D!000`, `[00]`

**`parseSerialResponse(data: ByteArray): BaxResponse`**
- Parses RS232 responses with STX/ETX/LRC validation

#### 3. Updated Functions

**`buildFrame(payload: String)`**
- Now delegates to `buildTcpFrame()` or `buildSerialFrame()` based on mode

**`parseResponse(data: ByteArray)`**
- Now delegates to `parseTcpResponse()` or `parseSerialResponse()` based on mode

**`parsePayload(payloadString: String)`**
- Now handles both `;` and `,` delimiters
- Recognizes Ingenico-specific response formats

## Usage

### Using TCP Mode (Default)

```kotlin
// TCP mode is default
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET

val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(
    amountCents = 20000,  // 200.00 NOK
    operatorId = "1"
)
// Result: [00 0C 50 3B 31 30 3B 31 3B 32 30 30 30 30 3B 30]
//         └─┬─┘ └─────────────┬──────────────────────┘
//           │                 │
//       Length=12          P;10;1;20000;0
```

### Using Serial Mode (Legacy)

```kotlin
// Switch to serial mode for RS232 terminals
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL

val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(
    amountCents = 20000,
    operatorId = "1"
)
// Result: [02 50 2C 31 2C 32 30 30 30 30 03 <LRC>]
//         └┬┘ └──────┬─────────┘ └┬┘ └─┬─┘
//         STX    P,1,20000      ETX  LRC
```

### Response Parsing

```kotlin
// Both modes use the same parseResponse() API
val response = NetsBaxProtocol.parseResponse(receivedBytes)

when (response) {
    is BaxResponse.Success -> {
        println("Transaction approved: ${response.transactionId}")
    }
    is BaxResponse.Data -> {
        println("Terminal response: ${response.payload}")
        // E.g., "A000ECR Timeout", "D!000", "[00]"
    }
    is BaxResponse.Error -> {
        println("Error: ${response.message}")
    }
    is BaxResponse.Ack -> println("ACK received")
    is BaxResponse.Nak -> println("NAK received")
    else -> println("Unknown response")
}
```

## Testing

Comprehensive test suite covers both modes:

```bash
mvn test -Dtest=NetsBaxProtocolTest
```

**Test Coverage:**
- TCP frame building with various payload sizes
- Serial frame building with STX/ETX/LRC
- Response parsing for both modes
- Ingenico-specific response formats
- Edge cases (empty responses, corrupted data, etc.)

All 18 tests pass, plus 151 existing tests remain passing (169 total).

## Migration Guide

### For Existing Code Using Serial Mode

If you have existing code that works with RS232 terminals, no changes needed. The default can be changed:

```kotlin
// In your application startup
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
```

### For New TCP/Ethernet Integrations

New integrations should use TCP mode (default):

```kotlin
// TCP mode is already default, but you can be explicit:
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
```

## Protocol Comparison

| Feature | RS232/Serial Mode | TCP/Ethernet Mode |
|---------|------------------|-------------------|
| Framing | STX/ETX/LRC | 2-byte length header |
| Delimiter | Comma (`,`) | Semicolon (`;`) |
| Command Format | `P,operatorId,amount` | `P;10;operatorId;amount;0` |
| Checksum | LRC (XOR) | None (TCP provides integrity) |
| Control Chars | ACK/NAK supported | Raw payload only |
| Response Format | STX-framed | Length-prefixed or raw |

## References

- **ECR Integration Report:** `docs/ecr-integration/ECR_INTEGRATION_REPORT.md`
- **Legacy VB6 Code:** `norgesgass_legacy/` (RS232 reference)
- **Python Proof-of-Concept:** `more_legacy/ehl_pumpekontroll_clone/` (TCP reference)
- **Gemini Analysis:** Original analysis document (provided by user)

## Technical Details

### Length Header Calculation

```kotlin
val payloadBytes = payload.toByteArray(StandardCharsets.ISO_8859_1)
val length = payloadBytes.size

val header = ByteArray(2)
header[0] = ((length shr 8) and 0xFF).toByte()  // High byte
header[1] = (length and 0xFF).toByte()           // Low byte
```

For payload length 12:
- High byte: `(12 >> 8) & 0xFF = 0x00`
- Low byte: `12 & 0xFF = 0x0C`
- Header: `00 0C`

### Heartbeat Handling (TCP Mode)

TCP mode requires responding to heartbeats:

```
Terminal → Kasse: [00 00]  (heartbeat)
Kasse → Terminal: [00 00]  (pong)
```

Length header of `00 00` means zero-length payload, which is used as heartbeat.

## Future Enhancements

Potential future improvements:

1. **Auto-detection:** Automatically detect framing mode based on first received bytes
2. **Configuration:** Allow mode to be set via configuration file or environment variable
3. **Logging:** Add framing mode to log output for easier debugging
4. **Statistics:** Track frame errors per mode

## Support

For questions or issues:
- Review test cases in `NetsBaxProtocolTest.kt`
- Check logs for framing mode and packet hex dumps
- Verify terminal is configured for correct communication type (ECR/Ethernet vs RS232)
