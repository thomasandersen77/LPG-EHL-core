# Nets Connect@Cloud Configuration

## 📡 Endpoint Information

### Production Environment
```yaml
Environment: PROD
Base URL: connectcloud.aws.nets.eu
Port: 443 (HTTPS/WSS)
WebSocket Path: /ws/json
```

### QA/Test Environment
```yaml
Environment: QA
Base URL: (check with Nets for QA endpoint)
Port: 443
```

---

## 🔐 Credentials

**Found in:** `/Users/tandersen/git/NorgesGass/BaxiExperiments/nets-cloud-solution/PaymentTerminalNetsCloudMonoServer/server.json`

```json
{
  "connectCloud": {
    "environment": "PROD",
    "username": "cloudberries_shared",
    "password": "B8PnVjmVq-SMM9QD",
    "terminalId": "42696609",
    "ecrIdPrefix": "POS-",
    "operatorIdDefault": "4321"
  }
}
```

### Important Notes
- ⚠️  **Security:** Credentials are in plaintext in server.json
- 🔄 **Shared Account:** Username `cloudberries_shared` suggests this is a shared test account
- 🔑 **Password may expire:** If you get 401 Unauthorized, credentials may need updating
- 📞 **Contact Nets:** For production credentials or if these expire

---

## 🧪 Testing with Kotlin Scripts

### Quick Connectivity Test
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/scripts
kotlin test-baxi-quick.kts
```

### Full Purchase Test
```bash
kotlin test-baxi-terminal.kts
```

### Custom Host/Port
```bash
NETS_HOST=192.168.1.100 NETS_PORT=7200 kotlin test-baxi-quick.kts
```

---

## 🔧 baxi-kotlin Library Configuration

The `baxi-kotlin` library uses `BaxiIniConfig`:

```kotlin
val config = BaxiIniConfig(
    hostIpAddress = "connectcloud.aws.nets.eu",  // Nets Cloud endpoint
    hostPort = 443,                               // HTTPS/WSS port
    vendorInfoExtended = "LPG-EHL-SERVICE",      // Your app identifier
    socketListenerEnabled = false,                // Not needed for client
    socketListenerPort = null
)
```

**Note:** The library handles:
- WebSocket connection to Nets
- Authentication with username/password
- Protocol encoding/decoding
- Event callbacks (onTerminalReady, onLocalMode, etc.)

You do **NOT** need to:
- Manually specify username/password in BaxiIniConfig
- Handle WebSocket protocol directly
- Parse Nets messages

The library wraps all of that internally!

---

## 📊 Architecture: baxi-kotlin vs C# Server

```
┌─────────────────────────────────────────────────────────────┐
│                     Nets Connect@Cloud                       │
│              connectcloud.aws.nets.eu:443                    │
│                    (WebSocket /ws/json)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
           ┌───────────┴───────────┐
           │                       │
    ┌──────▼─────────┐      ┌─────▼──────────────┐
    │  baxi-kotlin   │      │  C# MonoServer     │
    │   Library      │      │  (reference impl)  │
    │                │      │                    │
    │  - Kotlin/JVM  │      │  - .NET 8          │
    │  - WebSocket   │      │  - SQLite          │
    │  - Events      │      │  - HTTP REST API   │
    └──────┬─────────┘      └────────────────────┘
           │
    ┌──────▼─────────┐
    │ BaxiTerminal   │
    │    Client      │
    │ (your code)    │
    └────────────────┘
```

**Key Difference:**
- **C# Server:** Standalone HTTP REST API server (port 18081)
- **baxi-kotlin:** Library you embed directly in your app (no HTTP overhead)

---

## ⚙️ Spring Configuration

In `application.yaml`:

```yaml
payment:
  terminal:
    enabled: true
    implementation: baxi  # Use baxi-kotlin library
    baxi:
      host: connectcloud.aws.nets.eu  # For production
      port: 443
```

Or for local testing against simulator:

```yaml
payment:
  terminal:
    enabled: true
    implementation: simulated  # Use HTTP simulator
    base-url: http://localhost:18080
```

---

## 🐛 Troubleshooting

### "401 Unauthorized" during open
**Cause:** Credentials expired or invalid  
**Fix:** Contact Nets for updated credentials

### "Timeout waiting for terminal ready"
**Possible causes:**
1. Network connectivity issues (firewall blocking port 443)
2. Wrong hostname/port
3. Terminal is offline
4. Credentials invalid (check logs for 401)

**Debug steps:**
```bash
# Test network connectivity
ping connectcloud.aws.nets.eu
telnet connectcloud.aws.nets.eu 443

# Run with verbose logging
export ORG_SLF4J_SIMPLELOGGER_LOG_NO_CLOUDBERRIES=TRACE
kotlin test-baxi-quick.kts
```

### "callResult != 1"
**Cause:** baxi-kotlin rejected the operation  
**Fix:** Check `methodRejectCode` and `methodRejectInfo` in output

---

## 📝 Terminal ID

**Current Terminal ID:** `42696609`

This identifies the physical/virtual terminal in Nets system. Each terminal has a unique ID.

If you need to test with a different terminal:
```bash
TERMINAL_ID=12345678 kotlin test-baxi-terminal.kts
```

---

## 🔗 Related Files

- `test-baxi-quick.kts` - Quick connectivity test
- `test-baxi-terminal.kts` - Full purchase flow test
- `BaxiTerminalClient.kt` - Production Spring integration
- `BaxiIntegrationTest.kt` - Automated test with simulator

---

## ✅ Next Steps

1. ✅ Verify network connectivity to `connectcloud.aws.nets.eu:443`
2. ✅ Run `test-baxi-quick.kts` to test authentication
3. ✅ Run `test-baxi-terminal.kts` to test full purchase
4. ✅ Update `application.yaml` with production config
5. ✅ Test via Spring Boot app with real terminal
