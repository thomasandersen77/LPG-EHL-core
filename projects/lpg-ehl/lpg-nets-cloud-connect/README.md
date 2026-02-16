# Nets Cloud Connect - Kotlin Terminal Client

✅ **Status:** Implementert og klar for testing

## 📋 Oversikt

Dette er en Kotlin-implementasjon av `TerminalClient` interface for Nets Cloud Connect. Implementasjonen bruker:
- **Ktor** for HTTP og WebSocket kommunikasjon
- **Kotlin Coroutines** for asynkron håndtering
- **Spring Boot** for konfigurasjon og dependency injection

## 🚀 Quick Start

### 1. Sett miljøvariabler

```bash
export NETS_USERNAME=cloudberries_shared
export NETS_PASSWORD="B8PnVjmVq-SMM9QD"
export NETS_TERMINAL_ID="42696609"
```

### 2. Start lpg-ehl-service med Nets Cloud Connect

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service
mvn spring-boot:run -Dspring-boot.run.profiles=nets-cloud
```

### 3. Test med curl (fra annen terminal)

```bash
# Health check
curl http://localhost:8080/api/v1/terminal/health

# Open terminal
curl -X POST http://localhost:8080/api/v1/terminal/open

# Status
curl http://localhost:8080/api/v1/terminal/status

# Purchase (1 krone - TEST MED EGET KORT!)
curl -X POST http://localhost:8080/api/v1/terminal/purchase \
  -H "Content-Type: application/json" \
  -d '{"amountMinor":100,"operatorId":"0001"}'

# Close terminal
curl -X POST http://localhost:8080/api/v1/terminal/close
```

## 🏗️ Arkitektur

```
NetsCloudConnectTerminalClient (implements TerminalClient)
  ├── NetsCloudAuthClient          # HTTP login → JWT
  ├── NetsCloudWebSocketClient     # WebSocket med bearer token
  ├── NetsMessageBuilder           # JSON message builder
  └── NetsResponseParser           # Response parser
```

## ⚙️ Konfigurasjon

### application.yaml

```yaml
terminal:
  provider: nets-cloud-connect

nets-cloud-connect:
  base-url: https://connectcloud.aws.nets.eu
  username: ${NETS_USERNAME}
  password: ${NETS_PASSWORD}
  terminal-id: ${NETS_TERMINAL_ID}
  
  websocket:
    ping-interval-ms: 20000
    reconnect-delay-ms: 5000
    max-reconnect-attempts: 10
    
  timeouts:
    login-timeout-ms: 10000
    open-terminal-timeout-ms: 30000
    purchase-timeout-ms: 120000
    reversal-timeout-ms: 60000
```

## 🧪 Testing

### Unit Test (med production credentials)

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-nets-cloud-connect
mvn test -Dtest=NetsCloudAuthClientTest
```

### Integration Test

Kjør lpg-ehl-service og test full flow:

```bash
# Terminal 1: Start service
mvn spring-boot:run -Dspring-boot.run.profiles=nets-cloud

# Terminal 2: Test endpoints
./test-terminal-flow.sh
```

## 📝 Logg Output

Ved vellykket Open kommando:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔌 Opening Nets Cloud Connect terminal...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 1/3: Login
   ✅ Got JWT token
   ✅ Terminals: [42696609]
STEP 2/3: WebSocket Connect
   ✅ WebSocket connected
STEP 3/3: Send Open command
   ✅ Open command sent
⏳ Waiting for TerminalReady...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ TERMINAL READY!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Ved vellykket Purchase:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💳 Starting purchase: 100 øre (1.0 kr)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Purchase command sent
⏳ Waiting for card tap...
📺 Display: INSERT CARD
📺 Display: APPROVED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ TRANSACTION APPROVED!
   Result: 1
   Amount: 100 øre
   Response Code: 00
   Duration: 8234ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## ⚠️ VIKTIG - SIKKERHET

### 1. PROD Terminal - EKTE PENGER!

Terminal `42696609` er en **produksjonsterminal**. Alle transaksjoner er **ekte** og vil belaste faktiske kort.

**Testing-regler:**
- ✅ Bruk kun TESTBELØP (maks 1-10 kroner)
- ✅ Bruk kun ditt eget testkort
- ❌ ALDRI test med kundens kort
- ❌ ALDRI test med høye beløp

### 2. Credentials

**ALDRI commit credentials til Git!** Bruk alltid miljøvariabler:

```bash
export NETS_USERNAME=cloudberries_shared
export NETS_PASSWORD="<from-email>"
export NETS_TERMINAL_ID="42696609"
```

## 🔧 Troubleshooting

### Problem: Login feiler med 401

**Årsak:** Feil credentials  
**Løsning:** Verifiser NETS_USERNAME og NETS_PASSWORD miljøvariabler

### Problem: WebSocket connection failed

**Årsak:** Firewall eller nettverksproblemer  
**Løsning:**
```bash
# Test connectivity
curl -v https://connectcloud.aws.nets.eu/v1/login
```

### Problem: Timeout waiting for TerminalReady

**Årsak:** Terminal er utilgjengelig eller opptatt  
**Løsning:** Vent noen sekunder og prøv igjen

### Problem: Transaction timeout

**Årsak:** Bruker tappet ikke kortet i tide  
**Løsning:** Standard timeout er 120 sekunder - sjekk at terminal er klar

## 📚 Relaterte Dokumenter

- `../nets-cloud-connect-scripts/README.md` - Test-scripts
- `../nets-cloud-connect-scripts/IMPLEMENTATION_GUIDE_COMPLETE.md` - Full implementasjonsguide
- `../lpg-ehl-core/WARP.md` - Core dokumentasjon

## ✅ Implementasjons Status

- ✅ NetsCloudAuthClient (HTTP login)
- ✅ NetsCloudWebSocketClient (WebSocket)
- ✅ NetsMessageBuilder (JSON messages)
- ✅ NetsResponseParser (Response parsing)
- ✅ NetsCloudConnectTerminalClient (Main implementation)
- ✅ Spring Boot integration
- ✅ Konfigurasjon
- ✅ Kompilerer uten feil
- ⏳ Integration test mot production (next step)

## 🚀 Neste Steg

1. Start lpg-ehl-service med nets-cloud profil
2. Test Open kommando
3. Test Purchase med 1 krone
4. Test Close kommando
5. Verifiser at alt fungerer som forventet

**Estimert testtid:** 10-15 minutter

---

**Sist oppdatert:** 2026-02-15  
**Status:** Klar for testing! 🎉
