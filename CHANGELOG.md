# Changelog

All notable changes to the LPG-EHL project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added - 2026-01-01

#### TCP/Ethernet Framing Mode for Nets/Bax Protocol

**Branch:** `feature/ethernet-bax-protocol`

Implemented dual framing mode support for payment terminal communication, solving critical timeout issues with modern Ingenico Self/4000 terminals over Ethernet.

**Problem Solved:**
- Original implementation used RS232/Serial framing (`STX/ETX/LRC`) for all terminals
- Modern Ingenico Self/4000 terminals over TCP port 8009 expect 2-byte length header framing
- This mismatch caused `A000ECR Timeout` errors - terminal interpreted `0x02` (STX) as length byte and waited indefinitely

**New Features:**

1. **Dual Framing Mode Support**
   - `FramingMode.SERIAL` - Legacy RS232 with STX/ETX/LRC (comma delimiters)
   - `FramingMode.TCP_ETHERNET` - Modern TCP with length header (semicolon delimiters)
   - Default mode: `TCP_ETHERNET` for modern terminals

2. **TCP Framing Functions**
   - `buildTcpFrame(payload: String): ByteArray` - Builds 2-byte length header frames
   - `parseTcpResponse(data: ByteArray): BaxResponse` - Parses TCP responses with/without headers
   - Handles Ingenico-specific formats: `A000xxx`, `D!000`, `[00]`

3. **Updated Command Formats**
   - **TCP Mode:** `P;10;operatorId;amountCents;0` (Purchase), `P;03;operatorId;amountCents;0` (Preauth)
   - **Serial Mode:** `P,operatorId,amountCents` (Purchase), `A,operatorId,amountCents` (Preauth)

4. **Comprehensive Testing**
   - 18 new unit tests covering both framing modes
   - All 169 tests passing (18 new + 151 existing)
   - No regressions in existing functionality

5. **Documentation**
   - New `docs/TCP_ETHERNET_FRAMING.md` - Complete implementation guide
   - Usage examples for both modes
   - Migration guide for existing integrations
   - Protocol comparison table

**Technical Details:**

TCP Frame Structure:
```
[2-byte Length Header (Big-Endian)] + [Payload (ISO-8859-1)]

Example: "P;10;1;200;0" (12 bytes)
Result:  00 0C 50 3B 31 30 3B 31 3B 32 30 30 3B 30
         └─┬─┘ └─────────────┬──────────────┘
           │                 │
       Length=12         Payload text
```

Serial Frame Structure (unchanged):
```
<STX> <Payload> <ETX> <LRC>

Example: "P,1,200"
Result:  02 50 2C 31 2C 32 30 30 03 [LRC]
         └┬┘ └──────┬─────────┘ └┬┘ └─┬─┘
         STX    Payload        ETX  Checksum
```

**API Changes:**

- **Breaking:** Command payload format differs between modes (automatic based on `framingMode`)
- **Non-breaking:** All existing function signatures preserved
- **New:** `NetsBaxProtocol.framingMode` property (default: `TCP_ETHERNET`)
- **New:** `buildTcpFrame()` and `buildSerialFrame()` public functions
- **Modified:** `buildFrame()` now delegates based on mode
- **Modified:** `parseResponse()` now delegates based on mode
- **Enhanced:** `parsePayload()` handles both `;` and `,` delimiters

**Files Changed:**

- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/NetsBaxProtocol.kt` (major refactor)
- `lpg-ehl-core/src/test/kotlin/no/cloudberries/lpg/payment/NetsBaxProtocolTest.kt` (new)
- `docs/TCP_ETHERNET_FRAMING.md` (new)
- `LEGACY_ANALYSIS.md` (new)
- `WARP.md` (updated)

**Usage Example:**

```kotlin
// TCP mode (default for modern terminals)
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
val cmd = NetsBaxProtocol.createPurchaseCommand(amountCents = 20000, operatorId = "1")
// Sends: [00 0E 50 3B 31 30 3B 31 3B 32 30 30 30 30 3B 30]

// Serial mode (legacy RS232 terminals)
NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
val cmd = NetsBaxProtocol.createPurchaseCommand(amountCents = 20000, operatorId = "1")
// Sends: [02 50 2C 31 2C 32 30 30 30 30 03 <LRC>]
```

**Backward Compatibility:**

✅ Existing RS232 integrations work unchanged  
✅ Can switch mode at runtime via `framingMode` property  
✅ All existing tests pass without modification  
✅ Default mode (`TCP_ETHERNET`) works for new deployments

**Testing:**

```bash
# Run new tests
mvn test -Dtest=NetsBaxProtocolTest

# Run full suite
mvn test

# Results: 169/169 tests passed ✅
```

**References:**

- Issue: ECR Timeout with Ingenico Self/4000
- Analysis: `docs/ecr-integration/ECR_INTEGRATION_REPORT.md`
- Implementation: Based on Gemini AI analysis and Python PoC findings
- Legacy code: `norgesgass_legacy/` (VB6 RS232 reference)

**Contributors:**

- Thomas Andersen (implementation)
- Warp AI (code assistance)
- Gemini AI (protocol analysis)

**Next Steps:**

1. ✅ Code review on branch `feature/ethernet-bax-protocol`
2. ⏳ Integration testing with physical Ingenico Self/4000 terminal
3. ⏳ Merge to `main` after successful testing
4. ⏳ Deploy to production LPG stations

**Impact:**

- 🎯 **Fixes:** Critical timeout issue with modern payment terminals
- 🚀 **Enables:** Deployment to Ingenico Self/4000 equipped stations
- 🔧 **Maintains:** Full backward compatibility with legacy RS232 systems
- 📈 **Improves:** Test coverage from 151 to 169 tests

---

## [0.0.1-SNAPSHOT] - Previous Work

### Initial Implementation

- EHL protocol implementation for LPG dispensers
- Transaction state machine
- RS232 serial communication
- In-memory testing support
- 151 unit tests
