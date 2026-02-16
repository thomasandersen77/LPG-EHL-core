# 🧪 LPG-EHL Testing Guide

Komplett guide for testing med simulator + headless + curl-kommandoer.

---

## 🚀 Steg 1: Start Simulator

```bash
./scripts/start-socat-sim.sh --parity=NONE --address=2
```

Dette starter:
- **Socat** virtuelle serieporter: `/tmp/vserial0` og `/tmp/vserial1`
- **PLS Simulator** på `/tmp/vserial0` som dispenser adresse **2** (og legacy **34**)
- Baud: 9600, Parity: NONE

**Utdata:**
```
✅ Klart for testing
   Port 0: /tmp/vserial0  ← Simulator
   Port 1: /tmp/vserial1  ← Din app
```

---

## 🚀 Steg 2: Start Headless med Debug API

**I nytt terminalvindu:**

```bash
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1 \
  --ehl.serial.parity=NONE \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Eller med ekstern YAML:**

```bash
# 1. Rediger application-local.yaml først
nano application-local.yaml  # Sett port=/tmp/vserial1, parity=NONE

# 2. Start med YAML
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Sjekk at den kjører:**
```bash
curl http://localhost:8080/actuator/health
```

Forventet: `{"status":"UP"}`

---

## 📋 Alle CURL-kommandoer

### 1️⃣ **Diagnostikk / Health Check**

**Test serial kommunikasjon:**
```bash
curl "http://localhost:8080/api/debug/serial/health?address=2"
```

**Liste tilgjengelige serial ports:**
```bash
curl "http://localhost:8080/api/debug/serial/ports"
```

**Scan for addresser (finn hvilke som svarer):**
```bash
# Standard addresser (1-8)
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=1&end=8"

# Legacy addresser (33-40)
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=33&end=40"
```

**Auto-detect parity:**
```bash
curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/tmp/vserial1&address=2"
```

---

### 2️⃣ **Pumpe-kontroll (UNBLOCK / BLOCK)**

**UNBLOCK - Fri pumpe (start levering):**
```bash
curl -X POST "http://localhost:8080/api/v1/emulator/pump/2/unblock"
```

**Eller legacy adresse 34:**
```bash
curl -X POST "http://localhost:8080/api/v1/emulator/pump/34/unblock"
```

**BLOCK - Stopp pumpe:**
```bash
curl -X POST "http://localhost:8080/api/v1/emulator/pump/2/block"
```

**Se pumpe-status:**
```bash
curl "http://localhost:8080/api/v1/emulator/pump/2/status"
```

**Start pumping (simuler nozzle lift):**
```bash
curl -X POST "http://localhost:8080/api/v1/emulator/pump/2/start-pumping"
```

---

### 3️⃣ **Volum og beløp**

**Les volum under pumping (real-time):**
```bash
curl "http://localhost:8080/api/v1/emulator/pump/2/status"
```

**Polling-loop (se volum kontinuerlig):**
```bash
while true; do
  curl -s "http://localhost:8080/api/v1/emulator/pump/2/status" | jq '.volumeLitres, .amountKr'
  sleep 0.5
done
```

**Med pretty-print:**
```bash
watch -n 0.5 'curl -s http://localhost:8080/api/v1/emulator/pump/2/status | jq'
```

---

### 4️⃣ **Pris (kr/L)**

**Hent gjeldende pris:**
```bash
curl "http://localhost:8080/api/v1/prices"
```

**Oppdater pris:**
```bash
curl -X POST "http://localhost:8080/api/v1/prices/update" \
  -H "Content-Type: application/json" \
  -d '{"pricePerLiter": 15.90}'
```

**Eller med query parameter:**
```bash
curl -X POST "http://localhost:8080/api/v1/prices/update?pricePerLiter=18.50"
```

---

### 5️⃣ **Transaksjoner**

**Hent alle transaksjoner:**
```bash
curl "http://localhost:8080/api/v1/transactions"
```

**Hent pending transaksjoner:**
```bash
curl "http://localhost:8080/api/v1/transactions?status=PENDING"
```

**Settle (betal) pending transaksjon:**
```bash
curl -X POST "http://localhost:8080/api/v1/emulator/settle/2?method=CARD"
```

Metoder: `CARD`, `CASH`, `CREDIT`

---

### 6️⃣ **Komplett pumpe-syklus**

**Full test fra start til slutt:**

```bash
#!/bin/bash
# full-pump-test.sh

PUMP=2

echo "1️⃣ UNBLOCK (Fri pumpe)"
curl -X POST "http://localhost:8080/api/v1/emulator/pump/$PUMP/unblock"
echo -e "\n"

echo "⏳ Venter 2 sekunder..."
sleep 2

echo "2️⃣ Start pumping (simuler nozzle lift)"
curl -X POST "http://localhost:8080/api/v1/emulator/pump/$PUMP/start-pumping"
echo -e "\n"

echo "⏳ Pumper i 5 sekunder..."
for i in {1..5}; do
  echo "   ⛽ $(curl -s http://localhost:8080/api/v1/emulator/pump/$PUMP/status | jq -r '.volumeLitres + "L = " + (.amountKr|tostring) + " kr"')"
  sleep 1
done

echo "3️⃣ BLOCK (Stopp pumpe)"
curl -X POST "http://localhost:8080/api/v1/emulator/pump/$PUMP/block"
echo -e "\n"

echo "4️⃣ Se finalt volum"
curl -s "http://localhost:8080/api/v1/emulator/pump/$PUMP/status" | jq '{volumeLitres, amountKr, hasPendingTransaction}'
echo -e "\n"

echo "5️⃣ Settle (betal)"
curl -X POST "http://localhost:8080/api/v1/emulator/settle/$PUMP?method=CARD"
echo -e "\n"

echo "✅ Test ferdig!"
```

**Kjør:**
```bash
chmod +x full-pump-test.sh
./full-pump-test.sh
```

---

## 🔧 Testing mot ekte hardware (ARK-3360)

**For Alejandros setup med adresse 34 på /dev/ttyS3:**

```bash
# 1. Start headless på ARK-3360
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080

# 2. Test fra annen maskin på samme nettverk
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"
curl "http://ark-ip:8080/api/v1/emulator/pump/34/status"
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/block"
```

---

## 📊 Advanced: WebSocket logging (kun webapp)

**Hvis du kjører webapp i stedet for headless:**

```javascript
// I nettleser console (http://localhost:8080)
const ws = new WebSocket('ws://localhost:8080/ws/logs');
ws.onmessage = (event) => console.log(JSON.parse(event.data));
```

---

## 🐛 Feilsøking

### Problem: `Connection refused` når du curler
**Løsning:** Sjekk at headless kjører med `--spring.web.application-type=servlet`

### Problem: `404 Not Found` på /api/v1/emulator
**Løsning:** Endepunktet finnes kun i webapp. Bruk webapp i stedet:
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1
```

### Problem: Simulator svarer ikke
**Løsning:** Sjekk at du bruker riktig adresse (2 for standard, 34 for legacy)
```bash
# Test begge:
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=1&end=40"
```

### Problem: Permission denied på /dev/ttyS3
**Løsning:** Legg til bruker i dialout-gruppen:
```bash
sudo usermod -a -G dialout $USER
# Logg ut og inn igjen
```

---

## 📚 Eksempel-output

**UNBLOCK response:**
```json
{
  "success": true,
  "message": "Pumpe frigitt - levering startet",
  "state": "READY_TO_PUMP",
  "volumeLitres": 0.0,
  "amountKr": 0.0,
  "pricePerLitreKr": 15.9
}
```

**Status under pumping:**
```json
{
  "state": "PUMPING",
  "address": 2,
  "volumeLitres": 3.45,
  "amountKr": 54.86,
  "pricePerLitreKr": 15.9,
  "nozzleLifted": true,
  "hasPendingTransaction": false
}
```

**BLOCK response:**
```json
{
  "success": true,
  "message": "Levering stoppet - venter på betaling",
  "state": "PAYMENT_PENDING",
  "volumeLitres": 5.27,
  "amountKr": 83.79,
  "pricePerLitreKr": 15.9,
  "hasPendingTransaction": true
}
```

---

## 🎯 Tips for Alejandro (onsdag testing)

**1. Test at UNBLOCK/BLOCK fungerer:**
```bash
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/block"
```

**2. Real-time volum monitoring:**
```bash
watch -n 0.5 'curl -s http://ark-ip:8080/api/v1/emulator/pump/34/status | jq ".volumeLitres,.amountKr"'
```

**3. Test ADAM-boksen:**
```bash
# Test 1: Med ADAM tilkoblet
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"

# Test 2: Koble fra ADAM ethernet-kabel
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"

# Forventet: Fungerer like bra begge steder! 
# ADAM er kun for Modbus - ikke EHL protokoll
```

**4. Test med bil:**
```bash
# 1. UNBLOCK
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"

# 2. Pumpe i bil
# (overvåk volum real-time)

# 3. BLOCK
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/block"

# 4. Se resultat
curl "http://ark-ip:8080/api/v1/emulator/pump/34/status" | jq
```

---

## ✅ Quick Reference

| Kommando | Beskrivelse |
|----------|-------------|
| `curl .../pump/2/unblock` | Fri pumpe (start levering) |
| `curl .../pump/2/block` | Stopp pumpe |
| `curl .../pump/2/status` | Les volum og beløp |
| `curl .../prices/update` | Sett ny pris (kr/L) |
| `curl .../debug/serial/health` | Test serial kommunikasjon |
| `curl .../debug/serial/scan-addresses` | Finn hvilke addresser som svarer |
| `curl .../settle/2?method=CARD` | Betal pending transaksjon |

---

**Next:** Hvis noen endepunkter mangler, si fra så legger jeg dem til! 🚀
