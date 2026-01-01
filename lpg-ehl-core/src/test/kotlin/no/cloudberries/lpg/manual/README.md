# Manual Terminal Testing

This directory contains manual tests for real-world payment terminal communication.

## ManualTerminalTest.kt

### Purpose

Test TCP/Ethernet communication with Ingenico Self/4000 payment terminal without needing full Spring infrastructure or deployment.

### Prerequisites

1. **Terminal Configuration:**
   - Komm type: **IP ETHERNET**
   - ECR IP: Your machine's IP (e.g., `192.168.0.41`)
   - ECR IP PORT: **8009**
   - ECR/TLS: **Nei**

2. **Network Setup:**
   - Terminal and computer on same network
   - Firewall allows incoming TCP on port 8009

3. **Terminal State:**
   - Powered on
   - Connected to network
   - Ready to connect to ECR

### How to Run in IntelliJ

#### Method 1: Direct Run (Easiest)

1. Open `ManualTerminalTest.kt` in IntelliJ
2. Right-click anywhere in the file
3. Select **"Run 'ManualTerminalTest'"**
4. Watch the console output

#### Method 2: Run Configuration

1. **Run → Edit Configurations**
2. Click **+** → **Kotlin**
3. Set:
   - Name: `Manual Terminal Test`
   - Main class: `no.cloudberries.lpg.manual.ManualTerminalTest`
   - Module: `lpg-ehl-core.test`
4. Click **OK**
5. Click **Run** button (green triangle)

### Expected Output

```
═══════════════════════════════════════════════════════
  Manual Ingenico Self/4000 Terminal Test
  TCP/Ethernet Mode - Port 8009
═══════════════════════════════════════════════════════

[00:12:34.567] ✓ Set framing mode: TCP_ETHERNET
[00:12:34.568] ✓ Server listening on port 8009
[00:12:34.568] ⏳ Waiting for terminal to connect...
   (Make sure terminal is powered on and configured)

[00:12:35.123] ✓ Terminal connected from: 192.168.0.43:54321

[00:12:35.124] 📥 Listening for initial message from terminal...
[00:12:35.234] 📥 RECEIVED (4 bytes):
   HEX: 00 02 49 31
   TXT: I1
   Parsed: Data(I1)

[00:12:36.235] 💳 Sending Purchase command: 200 øre (2.0 NOK)
[00:12:36.235] 📤 SENT (14 bytes):
   HEX: 00 0C 50 3B 31 30 3B 31 3B 32 30 30 3B 30
   CMD: P;10;1;200;0

[00:12:36.236] 📥 Listening for terminal responses...
   (Waiting for transaction result or timeout...)

[00:12:36.345] 📥 RESPONSE #1 (6 bytes):
   HEX: 00 04 5B 30 30 5D
   TXT: [00]
   Parsed: Data([00])
   Status: [00]

[00:12:37.456] 📥 RESPONSE #2 (17 bytes):
   HEX: 00 0F 41 30 30 30 45 43 52 20 54 69 6D 65 6F 75 74
   TXT: A000ECR Timeout
   Parsed: Data(A000ECR Timeout)
   Status: A000ECR Timeout
   ⚠️  Transaction timed out (no card inserted)

Session ended. Press Enter to exit...
```

### What the Test Does

1. **Starts Server:** Listens on TCP port 8009
2. **Accepts Connection:** Waits for terminal to connect
3. **Reads Initial Message:** Usually `I1` (terminal identification)
4. **Sends Purchase:** Command for 2.00 NOK
5. **Monitors Responses:** 
   - `[00]` = Command acknowledged
   - `A000ECR Timeout` = No card inserted within timeout
   - Or actual transaction result if card is used

### Interpreting Results

#### ✅ Success Indicators

- Terminal connects
- Receives `[00]` acknowledgment
- Gets response (even if timeout)

This proves:
- TCP framing is correct
- Protocol format is accepted
- Communication works

#### ⚠️ Timeout (Expected)

```
A000ECR Timeout
```

**Normal!** This means:
- Terminal received and understood command
- Terminal waited for card
- No card was inserted
- Terminal timed out after ~30 seconds

**This is actually SUCCESS** - the protocol works!

#### ✅ Transaction Success (If Card Used)

```
✅ TRANSACTION APPROVED!
   Transaction ID: 12345678
   Auth Code: ABCD1234
```

This would appear if you insert a card during the test.

### Common Issues

#### Port Already in Use

```
✗ Error: Address already in use
```

**Solution:**
```bash
# Find process using port 8009
lsof -i :8009

# Kill the process
kill -9 <PID>
```

#### Terminal Not Connecting

**Check:**
1. Terminal IP configuration (should match your machine's IP)
2. Both on same network/subnet
3. Terminal is actually powered on
4. No firewall blocking port 8009

**Debug:**
```bash
# Check if server is listening
netstat -an | grep 8009

# Should show:
tcp4       0      0  *.8009                 *.*                    LISTEN
```

#### No Initial Message

If terminal connects but sends nothing:
- Terminal might be in wrong mode (not ECR mode)
- Check terminal configuration: Komm type should be "ECR/Kasse"

### Testing Different Commands

Edit the test to try other operations:

```kotlin
// Test Refund
val refundCmd = NetsBaxProtocol.createRefundCommand(
    amountCents = 100,
    operatorId = "1"
)

// Test Status
val statusCmd = NetsBaxProtocol.createStatusCommand()

// Test with Transaction ID
val refundWithId = NetsBaxProtocol.createRefundCommand(
    amountCents = 100,
    operatorId = "1",
    transactionId = "TX123"
)
```

### Next Steps

After successful manual test:

1. **Integration Testing:** Use findings to build production ECR server
2. **Error Handling:** Implement retry logic, connection management
3. **Transaction Flow:** Complete purchase flow with confirmation
4. **Deployment:** Deploy to actual LPG station

### Troubleshooting Tips

#### See Raw Bytes

All communication is logged in HEX format:

```
HEX: 00 0C 50 3B 31 30 3B 31 3B 32 30 30 3B 30
     └─┬─┘ └─────────────┬──────────────┘
       │                 │
   Length=12        P;10;1;200;0
```

Verify:
- First 2 bytes = length in big-endian
- Payload matches expected command format

#### Compare with Python PoC

If this works but Python didn't (or vice versa):
- Compare HEX output byte-by-byte
- Check delimiter (`;` vs `,`)
- Verify length header calculation

### References

- **Implementation:** `NetsBaxProtocol.kt`
- **Unit Tests:** `NetsBaxProtocolTest.kt`
- **Documentation:** `docs/TCP_ETHERNET_FRAMING.md`
- **ECR Report:** `docs/ecr-integration/ECR_INTEGRATION_REPORT.md`
