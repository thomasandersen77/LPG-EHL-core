# EHL Protocol - Human-Readable Logging Examples

This document demonstrates the improved human-readable logging in the EHL protocol implementation.

## Overview

The logging has been enhanced to make protocol communication easy to understand for humans, not just machines. Every operation is logged with:

- 📤 📥 **Direction indicators** (sending/receiving)
- 🎯 **Command descriptions** in plain English
- 📊 **Decoded data** (not just hex dumps)
- 🔄 **State transitions** with reasons
- ⛽ **Delivery progress** in liters and kr
- ❌ **Error messages** with context
- ✅ **Success/failure** indicators

## Example Log Output

### 1. Simple STATE Query

```
INFO  📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Query | Bytes: [20 06 01 4B 6C 36] | Checksum: 0x6C ✓
INFO  📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0 (IDLE (Ready for new transaction)) | Bytes: [20 07 01 4B 00 6D 36] | Checksum: 0x6D ✓
```

### 2. Complete Delivery Cycle (From Emulator)

```
================================================================================
INFO  📥 RECEIVING ← Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [20 06 01 77 55 36] | Checksum: 0x55 ✓
INFO  📊 Processing UNBLOCK command
INFO  🔄 STATE CHANGE: IDLE → DELIVERING | Reason: UNBLOCK command received
INFO  🚀 DELIVERY STARTED: Price=10.00 kr/L | Flow rate=1.00 L/s
INFO  📤 SENDING → Dispenser #1 | OK (Command acknowledgement) | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
INFO  📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | State=2 (DELIVERING (Fuel flowing)) | Bytes: [20 07 01 4B 02 6F 36] | Checksum: 0x6F ✓
================================================================================

... [wait 1.5 seconds] ...

================================================================================
INFO  📥 RECEIVING ← Dispenser #1 | STOP (Stop the dispenser) | Bytes: [20 06 01 2F 1D 36] | Checksum: 0x1D ✓
INFO  📊 Processing STOP command
INFO  🔄 STATE CHANGE: DELIVERING → FINISHED | Reason: STOP command received
INFO  ⛽ DELIVERY: 1.50 L × 10.00 kr/L = 15.00 kr
INFO  🏁 DELIVERY FINISHED: 1.50 L delivered for 15.00 kr
INFO  📤 SENDING → Dispenser #1 | OK (Command acknowledgement) | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
INFO  📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | State=3 (FINISHED (Transaction complete)) | Bytes: [20 07 01 4B 03 6E 36] | Checksum: 0x6E ✓
INFO  📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.50 L | Amount=15.00 kr (1500 øre) | Bytes: [20 0A 01 45 00 0F 05 DC 85 36] | Checksum: 0x85 ✓
================================================================================
```

### 3. Price Programming

```
================================================================================
INFO  📥 RECEIVING ← Dispenser #1 | PROG_PRC (Programming of fuel price) | Price=15.90 kr/L | Bytes: [20 0A 01 A9 30 39 35 31 8E 36] | Checksum: 0x8E ✓
INFO  📊 Processing PROG_PRC command
INFO  💰 PRICE PROGRAMMED: 10.00 kr/L → 15.90 kr/L
INFO  📤 SENDING → Dispenser #1 | OK (Command acknowledgement) | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
INFO  📤 SENDING → Dispenser #1 | PRICE (Give/take the fuel price) | Price=15.90 kr/L | Bytes: [20 0A 01 5C 30 39 35 31 C3 36] | Checksum: 0xC3 ✓
================================================================================
```

### 4. Error Handling - Checksum Error

```
ERROR ❌ ERROR: Checksum Mismatch | Expected 0x6C, got 0xFF (data corrupted in transmission)
DEBUG 🔧 Recovery: Found next STX at offset 8, skipping corrupt packet
```

### 5. Error Handling - Wrong Address

```
WARN  📪 IGNORED: Packet addressed to #2 (I am #1)
```

### 6. Buffer Management

```
DEBUG 🟢 Buffer received data | Size: 45 bytes
DEBUG 🔵 Buffer after parsing packet | Size: 0 bytes
WARN  ❌ ERROR: Buffer Overflow | Buffer exceeded 1024 bytes (1087), clearing oldest data
```

### 7. State Transitions

```
INFO  🔄 STATE CHANGE: IDLE → DELIVERING | Reason: UNBLOCK command received
INFO  🔄 STATE CHANGE: DELIVERING → FINISHED | Reason: STOP command received
INFO  🔄 STATE CHANGE: FINISHED → IDLE | Reason: ZER (Reset) command received
```

### 8. Live Delivery Progress (DEBUG level)

```
DEBUG ⛽ DELIVERY: 0.50 L × 10.00 kr/L = 5.00 kr
DEBUG ⛽ DELIVERY: 1.00 L × 10.00 kr/L = 10.00 kr
DEBUG ⛽ DELIVERY: 1.50 L × 10.00 kr/L = 15.00 kr
```

### 9. Communication Test (LINETEST)

```
INFO  📥 RECEIVING ← Dispenser #1 | LINETEST (Transmission channel test) | Bytes: [20 06 01 6A 48 36] | Checksum: 0x48 ✓
INFO  🔌 Processing LINETEST - communication OK
INFO  📤 SENDING → Dispenser #1 | OK (Command acknowledgement) | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
```

### 10. Reset Command

```
INFO  📥 RECEIVING ← Dispenser #1 | ZER (Reset the calculator) | Bytes: [20 06 01 81 A7 36] | Checksum: 0xA7 ✓
INFO  🔄 STATE CHANGE: DELIVERING → IDLE | Reason: ZER (Reset) command received
INFO  🔄 DISPENSER RESET: All counters cleared, state → IDLE
INFO  📤 SENDING → Dispenser #1 | OK (Command acknowledgement) | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
INFO  📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | State=0 (IDLE (Ready for new transaction)) | Bytes: [20 07 01 4B 00 6D 36] | Checksum: 0x6D ✓
```

## Log Level Guide

### INFO Level (Production)
- ✅ All packet send/receive with decoded data
- ✅ State transitions with reasons
- ✅ Operation outcomes (delivery started/finished, price changed, etc.)
- ✅ Important warnings (wrong address, unsupported commands)

### DEBUG Level (Development/Troubleshooting)
- 🔧 Buffer status and synchronization details
- 🔧 Recovery operations (finding next STX after errors)
- ⛽ Live delivery progress updates
- 🔧 Raw byte dumps alongside human-readable logs

### ERROR Level
- ❌ Protocol violations (checksum errors, invalid format)
- ❌ Buffer overflows and data corruption
- ❌ Parsing failures

## Key Features

### 1. **Emoji Icons for Quick Visual Scanning**
- 📤 = Sending to dispenser
- 📥 = Receiving from dispenser
- ✅ = Success
- ❌ = Error
- ⚠️ = Warning
- 🔄 = State change
- ⛽ = Delivery/fuel
- 💰 = Price/money
- 🔌 = Communication
- 🔧 = Recovery/debug
- 📊 = Data query

### 2. **Decoded Protocol Data**
Instead of:
```
DEBUG Sent 7 bytes: 20 07 01 4B 05 6F 36
```

You get:
```
INFO  📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | State=5 (UNKNOWN) | Bytes: [20 07 01 4B 05 6F 36] | Checksum: 0x6F ✓
```

### 3. **Human-Readable State Names**
- State 0 = IDLE (Ready for new transaction)
- State 1 = READY (Authorized, waiting for nozzle)
- State 2 = DELIVERING (Fuel flowing)
- State 3 = FINISHED (Transaction complete)
- State 9 = ERROR (Dispenser error)

### 4. **Automatic Unit Conversions**
- Volume: deciliters → liters (e.g., "1.50 L" instead of "15 deciliters")
- Amount: øre → kr (e.g., "15.00 kr" instead of "1500 øre")
- Price: Shows both (e.g., "15.90 kr/L")

### 5. **Context-Aware Error Messages**
Instead of:
```
ERROR Checksum error: expected 0x6C, got 0xFF
```

You get:
```
ERROR ❌ ERROR: Checksum Mismatch | Expected 0x6C, got 0xFF (data corrupted in transmission)
DEBUG 🔧 Recovery: Found next STX at offset 8, skipping corrupt packet
```

## Configuration

### Logback Configuration (logback.xml)

For production:
```xml
<root level="INFO">
    <appender-ref ref="FILE"/>
</root>
```

For development/troubleshooting:
```xml
<root level="DEBUG">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
</root>
```

For specific package control:
```xml
<logger name="no.cloudberries.lpg.protocol" level="INFO"/>
<logger name="no.cloudberries.lpg.communication" level="DEBUG"/>
<logger name="no.cloudberries.lpg.emulator" level="INFO"/>
```

## Benefits

### For Development
- **Faster debugging**: Immediately see what's happening without hex decoding
- **Clear error context**: Know exactly what went wrong and why
- **State tracking**: Follow the dispenser state machine visually

### For Operations
- **Easy troubleshooting**: Operators can read logs without protocol knowledge
- **Quick problem identification**: Emoji icons and clear messages highlight issues
- **Audit trail**: Complete human-readable record of all transactions

### For Support
- **Remote diagnosis**: Support team can understand logs without access to system
- **Pattern recognition**: Similar issues show similar log patterns
- **Documentation**: Logs serve as documentation of system behavior

## Example Troubleshooting Session

**Problem**: Dispenser not responding

**Log Analysis**:
```
INFO  📤 SENDING → Dispenser #1 | STATE Query | Bytes: [20 06 01 4B 6C 36] | Checksum: 0x6C ✓
DEBUG 🟢 Buffer received data | Size: 0 bytes
DEBUG ⏳ Incomplete packet: 0 bytes received, waiting for more data
... [5 seconds] ...
ERROR ⏱️ TIMEOUT: receive | Waited 5000ms with no response
```

**Diagnosis**: Dispenser #1 is not responding at all - check physical connection

---

**Problem**: Data corruption

**Log Analysis**:
```
INFO  📤 SENDING → Dispenser #1 | UNBLOCK | Bytes: [20 06 01 77 55 36] | Checksum: 0x55 ✓
ERROR ❌ ERROR: Checksum Mismatch | Expected 0x6C, got 0x3A (data corrupted in transmission)
DEBUG 🔧 Recovery: Found next STX at offset 3, skipping corrupt packet
INFO  📥 RECEIVING ← Dispenser #1 | OK | Bytes: [20 06 01 1E 0D 36] | Checksum: 0x0D ✓
```

**Diagnosis**: Intermittent electrical noise on RS-485 line - check cable shielding

---

**Problem**: Wrong dispenser responding

**Log Analysis**:
```
INFO  📤 SENDING → Dispenser #1 | STATE Query | Bytes: [20 06 01 4B 6C 36] | Checksum: 0x6C ✓
WARN  📪 IGNORED: Packet addressed to #2 (I am #1)
INFO  📥 RECEIVING ← Dispenser #2 | STATE | State=0 (IDLE) | Bytes: [20 07 02 4B 00 6E 36] | Checksum: 0x6E ✓
```

**Diagnosis**: Dispenser addressing configuration incorrect - dispenser set to address #2 but should be #1
