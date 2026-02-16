# Nets Cloud Connect Testing Report

**Dato:** 2026-02-15  
**Testet av:** Warp AI Agent  
**Terminal:** 42696609  
**Miljø:** PRODUCTION

---

## 📋 Executive Summary

Vi har testet kommunikasjon med Nets Cloud Connect og identifisert den korrekte protokollen. Alle kommunikasjonslag fungerer, men terminalen er ikke online ennå.

### Status Overview

| Komponent | Status | Detaljer |
|-----------|--------|----------|
| HTTP Login | ✅ Fungerer | JWT token mottatt |
| WebSocket Connection | ✅ Fungerer | wss://connectcloud.aws.nets.eu/ws/json |
| JSON Protocol | ✅ Fungerer | Serveren forstår meldinger |
| Terminal Status | ❌ Offline | ErrorCode 8013: "Invalid terminal ID or terminal not connected" |

---

## 🔐 Credentials (VERIFIED)

**Korrekte credentials:**
```
Username: cloudberries_shared
Password: B8PnVjmVq-SMM9QD
Terminal ID: 42696609
```

**VIKTIG:** 
- ❌ `cranberries_shared` (fra e-post) = FEIL
- ✅ `cloudberries_shared` (fra server.json) = RIKTIG

**Terminal type:** PRODUCTION - EKTE PENGER! Maksimalt 1 krone i test-transaksjoner!

---

## 🌐 Protocol Details

### 1. HTTP Login

**Endpoint:** `POST https://connectcloud.aws.nets.eu/v1/login`

**Request:**
```json
{
  "username": "cloudberries_shared",
  "password": "B8PnVjmVq-SMM9QD"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "cloudberries_shared",
  "terminals": ["42696609"]
}
```

**Token utløper:** 24 timer (86400 sekunder)

---

### 2. WebSocket Connection

**Endpoint:** `wss://connectcloud.aws.nets.eu/ws/json`

**VIKTIGE DETALJER:**
- ✅ Protocol: `wss://` (WebSocket Secure)
- ✅ Authorization header: `bearer <token>` (**lowercase "bearer"!**)
- ✅ Frame type: **BINARY** (ikke TEXT)
- ✅ Content format: **JSON** (ikke XML)

**Correct Ktor setup:**
```kotlin
client.webSocket(
    request = {
        url.takeFrom("wss://connectcloud.aws.nets.eu/ws/json")
        headers.append(HttpHeaders.Authorization, "bearer $token")  // lowercase!
    }
) {
    // Handle BINARY frames, not TEXT
    for (frame in incoming) {
        when (frame) {
            is Frame.Binary -> {
                val json = frame.readBytes().decodeToString()
                // Parse JSON...
            }
        }
    }
}
```

---

### 3. JSON Message Format

**Open Request:**
```json
{
  "NetsRequest": {
    "MessageHeader": {
      "$": {
        "ECRID": "TEST-20260215182323-8ebdbdff",
        "TerminalID": "42696609",
        "VersionNumber": "1"
      }
    },
    "Open": {}
  }
}
```

**Error Response (Terminal Offline):**
```json
{
  "NetsResponse": {
    "MessageHeader": {
      "$": {
        "ECRID": "TEST-20260215182323-8ebdbdff",
        "TerminalID": "42696609",
        "VersionNumber": 1
      }
    },
    "Dfs13Error": {
      "ErrorCode": 8013,
      "ErrorString": "Error: Invalid terminal ID or terminal not connected"
    }
  }
}
```

**Expected Response (Terminal Online):**
```json
{
  "NetsResponse": {
    "MessageHeader": {
      "$": {
        "ECRID": "...",
        "TerminalID": "42696609",
        "VersionNumber": 1
      }
    },
    "Dfs13TerminalReady": {
      "TerminalId": "42696609"
    }
  }
}
```

---

### 4. ECRID Generation

ECRID må være unik per ECR-sesjon og følge dette formatet:

```
Prefix-YYYYMMDDHHMMSS-RandomHex

Eksempel: TEST-20260215182323-8ebdbdff
```

**Kotlin implementation:**
```kotlin
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    .withZone(ZoneOffset.UTC)
    .format(Instant.now())
val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
val ecrId = "POS-$timestamp-$suffix"
```

---

## 🚨 Kritiske Funn

### FEIL #1: Feil credentials
- ❌ E-posten fra Nets brukte `cranberries_shared` = FEIL
- ✅ server.json brukte `cloudberries_shared` = RIKTIG

### FEIL #2: WebSocket authorization case
- ❌ `Bearer $token` (uppercase B) = 400 Bad Request
- ✅ `bearer $token` (lowercase b) = OK

### FEIL #3: WebSocket setup
- ❌ Manual host/port/path = 400 Bad Request
- ✅ `url.takeFrom(wsUrl)` = OK

### FEIL #4: Message format
- ❌ XML format = ErrorCode 9001: "Failed to parse message content"
- ✅ JSON format = OK

### FEIL #5: Frame type
- ❌ Listening only for TEXT frames = Miss all responses
- ✅ Listening for BINARY frames = Receives JSON data

---

## 📊 Error Codes

| Code | Betydning | Årsak |
|------|-----------|-------|
| 9001 | "Failed to parse message content" | Sendte XML istedenfor JSON |
| 8013 | "Invalid terminal ID or terminal not connected" | Terminal er offline |

---

## 🎯 Konklusjon

### Hva fungerer ✅

1. **HTTP Login** - Credentials er verifisert
2. **WebSocket Connection** - Tilkobling etablert
3. **JSON Protocol** - Serveren forstår meldinger korrekt
4. **Message Format** - JSON-strukturen er korrekt

### Hva fungerer IKKE ❌

1. **Terminal 42696609 er OFFLINE**
   - Terminal må konfigureres av Nets
   - Terminal må kobles til strøm og nettverk
   - Terminal må registreres i Nets Cloud Connect

---

## 📝 Neste Steg

### 1. Kontakt Nets Support
Ring Nets og be om:
- ✅ Verifiser at terminal `42696609` er registrert
- ✅ Sjekk om terminalen er online i deres system
- ✅ Be om konfigurasjon av terminalen:
  - Host IP: `91.102.24.142`
  - Host Port: `9670`
  - ECR IP: `3.33.230.243` / `15.197.206.182`
  - ECR Port: `6001`

### 2. Test med Fysisk Terminal
Når terminal er online:
- ✅ Test Open-kommando → Dfs13TerminalReady
- ✅ Test Purchase (1 krone)
- ✅ Test Reversal
- ✅ Test Admin-kommandoer (avstemming)

### 3. Implementer Produksjonskode
Basert på Alejandro's eksisterende kode:
- ✅ `CloudConnectBaxiClient` som implementerer `BaxiClient`
- ✅ Integrer med eksisterende Spring Boot app
- ✅ Konfigurerbar via `application.yaml`

---

## 🔍 Referanser

### Alejandro's Working Implementation
```
/Users/tandersen/git/NorgesGass/BaxiExperiments/nets-cloud-solution/
PaymentTerminalNetsCloudKotlinServer/
├── connect-cloud-client/        # WebSocket + Auth
│   └── src/main/kotlin/com/cloudberries/netscloud/connectcloud/
│       ├── client/ConnectCloudWebSocketClient.kt
│       └── protocol/NetsModels.kt
├── payment-terminal-library/    # Terminal orchestration
└── payment-terminal-server/     # HTTP wrapper
```

### Test Scripts
```
/Users/tandersen/git/NorgesGass/lpg-ehl/nets-cloud-connect-scripts/
├── src/main/kotlin/
│   ├── TestLogin.kt            # ✅ PASS: HTTP login
│   └── TestWebSocket.kt        # ✅ PASS: WebSocket + JSON
├── build.gradle.kts
└── README.md
```

### Kjøre testene:
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/nets-cloud-connect-scripts

# Login test
NETS_USERNAME="cloudberries_shared" \
NETS_PASSWORD="B8PnVjmVq-SMM9QD" \
./gradlew testLogin

# WebSocket test
NETS_USERNAME="cloudberries_shared" \
NETS_PASSWORD="B8PnVjmVq-SMM9QD" \
./gradlew testWebSocket
```

---

## 📚 Dokumentasjon

### Nets Cloud Connect Endpoints

**PRODUCTION:**
- Login: `https://connectcloud.aws.nets.eu/v1/login`
- WebSocket: `wss://connectcloud.aws.nets.eu/ws/json`
- Alternative IPs: `3.33.230.243` / `15.197.206.182`
- TCP Port: `6001` (for direct TCP connection, not used in our case)

### Message Types

| Type | Purpose | Client→Server | Server→Client |
|------|---------|---------------|---------------|
| Open | Open terminal session | ✅ | Dfs13TerminalReady |
| Dfs13TransferAmount | Purchase/refund | ✅ | Dfs13TransactionConfirmed |
| Dfs13Administration | Admin operations | ✅ | Dfs13AdministrationResult |
| Dfs13Display | Display text | ❌ | ✅ (async) |
| Dfs13Error | Error message | ❌ | ✅ (on error) |

---

## ⚠️ Sikkerhet

### Production Warnings

1. **EKTE PENGER:** Terminal `42696609` er PRODUCTION
2. **Maksimalt beløp i test:** 1 krone (100 øre)
3. **Credentials:** Aldri commit credentials til Git
4. **Token expiry:** JWT utløper etter 24 timer - håndter refresh
5. **Error handling:** Robust reconnect-logikk nødvendig

### Anbefalt Konfigurasjon

Bruk environment variables:
```bash
NETS_CLOUD_URL=https://connectcloud.aws.nets.eu
NETS_USERNAME=cloudberries_shared
NETS_PASSWORD=<from secure vault>
```

Eller Spring Boot `application.yaml`:
```yaml
nets:
  cloud:
    url: https://connectcloud.aws.nets.eu
    username: ${NETS_USERNAME}
    password: ${NETS_PASSWORD}
    terminal-id: 42696609
```

---

## 🎓 Læring

### Viktige Insights

1. **NOT XML!** - `/ws/json` betyr JSON format, ikke XML-over-JSON
2. **Lowercase bearer** - Case matters!
3. **BINARY frames** - WebSocket sender binary, ikke text
4. **ECRID generation** - Must be unique per session
5. **Alejandro's code is correct** - Use it as reference!

### Forskjell: Physical vs Cloud Connect

| Aspect | Physical Terminal (baxi-kotlin) | Nets Cloud Connect |
|--------|--------------------------------|-------------------|
| Transport | Raw TCP socket | WebSocket (wss://) |
| Framing | 2-byte length prefix | WebSocket frames (auto) |
| Format | Binary DFS13 | JSON-wrapped DFS13 |
| Auth | None | JWT bearer token |
| Connection | Direct to terminal | Via Nets cloud proxy |

---

## 📞 Kontakt

**Nets Support:**
- E-post: (fra tidligere e-post-tråd)
- Kontaktperson: Jannick (fra e-post)

**Spørsmål å stille:**
1. Er terminal `42696609` online i deres system?
2. Er terminalen konfigurert med våre ECR-parametre?
3. Hva er status på terminal-registreringen?
4. Når forventes terminalen å være klar for testing?

---

**Rapport generert:** 2026-02-15T18:24:00Z  
**Versjon:** 1.0  
**Forfatter:** Warp AI Agent (based on test results)
