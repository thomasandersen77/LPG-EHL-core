# LPG-EHL Headless - Debug API Curl Reference

**VIKTIG:** Dette dokumentet viser BARE de endepunktene som er tilgjengelige i **headless-appen** når den kjøres med `debug-api` profilen.

## Start headless med Debug API

```bash
java -jar lpg-ehl-app-headless-*.jar --spring.profiles.active=field,debug-api
```

Dette aktiverer en lett Undertow web-server (1 IO thread, 4 worker threads) med debug-endepunkter.

---

## DebugController - Pumpe-kontroll

Alle endepunkter under `/api/debug/*`

### Health Check
```bash
curl http://localhost:8080/api/debug/health
```
Response:
```json
{
  "status": "UP",
  "mode": "FIELD",
  "serialPort": "/dev/ttyUSB0",
  "emulatorEnabled": false
}
```

### Sjekk pumpestatus (med business logic)
```bash
curl http://localhost:8080/api/debug/state/1
```
Response:
```json
{
  "address": 1,
  "state": "IDLE",
  "volumeLitres": 0.0,
  "amountKr": 0.0,
  "pricePerLitreKr": 15.90,
  "nozzleLifted": false,
  "hasPendingTransaction": false
}
```

### Les volum direkte fra EHL-protokoll
```bash
curl http://localhost:8080/api/debug/volume/1
```
Response:
```json
{
  "address": 1,
  "volumeLitres": 12.34,
  "volumeCentilitres": 1234,
  "raw": "00 04 D2 A3 FF"
}
```

### Test kommunikasjon (LINETEST)
```bash
curl -X POST http://localhost:8080/api/debug/linetest/1
```
Response:
```json
{
  "command": "LINETEST",
  "address": 1,
  "success": true,
  "message": "Kommunikasjon OK",
  "responseCode": "OK"
}
```

### FRI PUMPE (UNBLOCK)
```bash
curl -X POST http://localhost:8080/api/debug/unblock/1
```
Response:
```json
{
  "command": "UNBLOCK",
  "address": 1,
  "success": true,
  "message": "Pumpe frigjort - 60s timeout startet",
  "responseCode": "READY"
}
```

### STOPP PUMPE (BLOCK)
```bash
curl -X POST http://localhost:8080/api/debug/block/1
```
Response:
```json
{
  "command": "BLOCK",
  "address": 1,
  "success": true,
  "message": "Pumpe blokkert - volum: 12.5L, beløp: 198.75 kr",
  "responseCode": "BLOCKED"
}
```

### Simuler betaling (DEBUG only)
```bash
curl -X POST "http://localhost:8080/api/debug/settle/1?paymentMethod=CARD"
```
Response:
```json
{
  "command": "SETTLE",
  "address": 1,
  "success": true,
  "message": "Betaling simulert: 12.5L = 198.75 kr via CARD"
}
```

### Nullstill pumpe
```bash
curl -X POST http://localhost:8080/api/debug/reset/1
```
Response:
```json
{
  "command": "RESET",
  "address": 1,
  "success": true,
  "message": "Pumpe tilbakestilt til IDLE"
}
```

### Les rå STATE-kommando
```bash
curl http://localhost:8080/api/debug/raw-state/1
```
Response:
```json
{
  "address": 1,
  "command": "STATE",
  "data": "00 00 00 00 01",
  "dataSize": 5
}
```

---

## SerialDiagnosticsController - Serial Diagnostikk

Alle endepunkter under `/api/debug/serial/*`

### List tilgjengelige serial porter
```bash
curl http://localhost:8080/api/debug/serial/ports
```
Response:
```json
[
  {
    "portName": "/dev/ttyUSB0",
    "description": "USB Serial Port",
    "manufacturer": "FTDI"
  },
  {
    "portName": "/dev/ttyS0",
    "description": "Onboard Serial",
    "manufacturer": "Generic"
  }
]
```

### Health check på serial connection
```bash
curl "http://localhost:8080/api/debug/serial/health?address=1"
```
Response:
```json
{
  "connected": true,
  "testPassed": true,
  "responseTimeMs": 45,
  "error": null
}
```

### Connection status
```bash
curl http://localhost:8080/api/debug/serial/status
```
Response:
```json
{
  "connected": true,
  "transportType": "SerialTransport"
}
```

### Smart scan - finn working config automatisk
```bash
curl -X POST "http://localhost:8080/api/debug/serial/smart-scan?timeoutMs=1000&stopOnFirst=true"
```
Response:
```json
[
  {
    "portPath": "/dev/ttyUSB0",
    "baudRate": 9600,
    "parity": "NONE",
    "address": 1,
    "confidence": "HIGH",
    "responseTimeMs": 42
  }
]
```

### Scan adresser (som Alejandros 02_scan_addresses.py)
```bash
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=32&end=40&baud=9600&parity=NONE"
```
Response:
```json
{
  "portPath": "/dev/ttyUSB0",
  "addressRange": "32-40",
  "baudRate": 9600,
  "parity": "NONE",
  "respondingAddresses": [33, 34],
  "testedCount": 9
}
```

### Auto-detect parity mode
```bash
curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/dev/ttyUSB0&address=1"
```
Response:
```json
{
  "detected": true,
  "parityMode": "NONE",
  "description": "8N1 - No parity (simulator/Python)",
  "error": null
}
```

---

## Komplett testsekvens

```bash
#!/bin/bash
# Debug API felttest

API="http://localhost:8080/api/debug"
PUMP=1

echo "🔍 1. Health check..."
curl -s $API/health | jq

echo ""
echo "📡 2. List serial ports..."
curl -s $API/serial/ports | jq

echo ""
echo "🔍 3. Sjekk pumpestatus..."
curl -s $API/state/$PUMP | jq

echo ""
echo "📡 4. Test kommunikasjon (LINETEST)..."
curl -s -X POST $API/linetest/$PUMP | jq

echo ""
echo "🔓 5. Frigir pumpe (FRI PUMPE)..."
curl -s -X POST $API/unblock/$PUMP | jq

echo ""
echo "⛽ Venter på at kunde fyller (10 sekunder)..."
sleep 10

echo ""
echo "📊 6. Les volum..."
curl -s $API/volume/$PUMP | jq

echo ""
echo "🛑 7. Stopper pumpe (BLOCK)..."
curl -s -X POST $API/block/$PUMP | jq

echo ""
echo "💳 8. Registrerer betaling (simulert)..."
curl -s -X POST "$API/settle/$PUMP?paymentMethod=CARD" | jq

echo ""
echo "🔄 9. Nullstiller pumpe..."
curl -s -X POST $API/reset/$PUMP | jq

echo ""
echo "✅ Test fullført!"
```

---

## Hva er IKKE tilgjengelig i headless debug API?

Disse endepunktene er BARE i **webapp** (`lpg-ehl-webapp`), IKKE i headless:

- ❌ `/api/v1/emulator/*` - Emulator-kontroll
- ❌ `/api/v1/dispenser/*` - Dispenser-kontroll
- ❌ `/api/v1/price/*` - Prisadministrasjon
- ❌ `/api/v1/transaction/*` - Transaksjonshåndtering
- ❌ `/api/v1/reports/*` - Rapportering
- ❌ `/api/v1/sync/*` - Synkronisering
- ❌ `/api/v1/payment/*` - Betalingsterminal
- ❌ `/api/v1/config/*` - Konfigurasjon
- ❌ `/api/v1/diagnostics/*` - Webapp diagnostikk

Hvis du trenger disse endepunktene, bruk `lpg-ehl-webapp` i stedet:
```bash
java -jar lpg-ehl-webapp-*.jar --spring.profiles.active=field
```

---

## Når bruke headless vs webapp?

### Bruk `lpg-ehl-app-headless` med `debug-api`:
- ✅ Produksjonsmiljø i felten
- ✅ Lavt ressursforbruk (ingen GUI, minimal web-server)
- ✅ Field technician testing via curl
- ✅ Docker/systemd deployment
- ✅ Raspberry Pi / embedded

### Bruk `lpg-ehl-webapp`:
- ✅ Utvikling og testing med full GUI
- ✅ Komplekse admin-oppgaver
- ✅ Rapportering og visualisering
- ✅ Emulator med kontrollpanel
- ✅ Komplett REST API

---

## Se også
- [Headless README](README_HEADLESS.md)
- [DebugController source](src/main/kotlin/no/cloudberries/lpg/headless/debug/DebugController.kt)
- [SerialDiagnosticsController source](src/main/kotlin/no/cloudberries/lpg/headless/debug/SerialDiagnosticsController.kt)
- [application-debug-api.yaml](src/main/resources/application-debug-api.yaml)
