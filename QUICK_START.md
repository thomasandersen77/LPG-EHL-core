# LPG-EHL Quick Start Guide

## 🚀 Rask Oppstart

### 1. Bygg Prosjektet

```bash
./build_monolith.sh --skip-tests
```

Dette bygger:
- `release/lpg-ehl-webapp.jar` - Web UI + REST API
- `release/lpg-ehl-headless.jar` - Background service
- `release/pls-sim.jar` - PLS Simulator

**Build tid:** ~30 sekunder

---

## 🎯 To Måter å Kjøre På

### Mode 1: LAB MODE (⚡ Standard - For utvikling)

**Ingen hardware kreves!** In-memory emulator.

```bash
./start-lpg-ehl.sh
```

**Åpne i nettleser:**
- GUI: http://localhost:8080
- Control Panel: http://localhost:8080/control
- API Docs: http://localhost:8080/swagger-ui.html

**Perfekt for:**
- ✅ Rask utvikling
- ✅ GUI-testing
- ✅ API-testing
- ✅ Ingen dependencies

---

### Mode 2: FIELD MODE (🔌 For testing med hardware)

**Real serial port** - Hardware eller virtual PTY (socat)

**Variant A: Med simulator (socat)** - Alt i ett terminalvindu!

```bash
./start-lpg-ehl.sh --field
```

Scriptet starter automatisk:
1. Socat virtual PTY bridge
2. PLS Simulator
3. Webapp

**Ctrl+C stopper alle tre.**

**Variant B: Med ekte hardware**

```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --lpg.mode=FIELD \
  --ehl.serial.port=/dev/ttyUSB0
```

**GUI:** http://localhost:8080

**Logs:**
```bash
tail -f /tmp/simulator.log  # Simulator (kun med socat)
tail -f /tmp/socat.log      # Socat (kun med socat)
```

**Perfekt for:**
- ✅ Testing av seriell kommunikasjon
- ✅ Debugging av EHL-protokoll
- ✅ Simulere ekte dispenser-oppførsel
- ✅ Produksjon med ekte hardware

---

### Mode 3: HEADLESS MODE (🤖 Background service)

**Ingen web UI** - Background service

```bash
./start-lpg-ehl.sh --headless
```

**Perfekt for:**
- ✅ Raspberry Pi deployment
- ✅ Server-kjøring
- ✅ Produksjonsmiljø

---

## 📋 Vanlige Kommandoer

### LAB MODE (default)
```bash
./start-lpg-ehl.sh
# eller explicit:
./start-lpg-ehl.sh --lab
```

### FIELD MODE (med simulator)
```bash
./start-lpg-ehl.sh --field
```

### Start med spesifikk port
```bash
./start-lpg-ehl.sh --lab --port=8081
```

### Se alle opsjoner
```bash
./start-lpg-ehl.sh --help
```

### Rebuild uten tests
```bash
./build_monolith.sh --skip-tests
```

### Rebuild med verbose output
```bash
./build_monolith.sh --verbose
```

---

## 🧪 Testing av Kortdragning-Flyt

### I LAB MODE:

1. Start webapp:
   ```bash
   ./start-lpg-ehl.sh
   ```

2. Åpne Control Panel: http://localhost:8080/control

3. Test flyt:
   - Trykk "Kortdragning" (simulerer kortsveip)
   - Trykk "Fri Dispenser" (unblock)
   - Vent 5 sekunder (pumping simulator)
   - Trykk "Stopp" (block)
   - **NÅ NYTT:** Autorisasjon bekreftes automatisk!
   - Pumpe resettes til IDLE
   - Klar for neste kunde ✅

### Forventet oppførsel:
- ✅ Kortdragning → Authorization opprettet
- ✅ Fri Dispenser → Pumping starter
- ✅ Stopp → Volume fryses, PAYMENT_PENDING
- ✅ **AUTO-SETTLE** → Betaling bekreftes automatisk
- ✅ Pumpe IDLE → Klar for neste

---

## 🔧 Alternative Metoder

### Manuell oppstart (separate terminaler)

Hvis du foretrekker separate terminaler:

**Terminal 1: Start simulator**
```bash
./start-simulator.sh
```

**Terminal 2: Start webapp**
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --lpg.mode=FIELD
```

---

## 🐛 Feilsøking

### Problem: `release/lpg-ehl-webapp.jar` ikke funnet

**Løsning:**
```bash
./build_monolith.sh --skip-tests
```

### Problem: Port 8080 allerede i bruk

**Løsning:**
```bash
./start-lpg-ehl.sh --port=8081
```

### Problem: socat not found

**Løsning:**
```bash
brew install socat
```

### Problem: Webapp henger i PAYMENT_PENDING

**Løsning:** Dette er fikset! Auto-settlement kjører nå automatisk etter BLOCK.
Hvis problemet fortsatt skjer:
1. Sjekk at du har bygget nyeste versjon
2. Restart webapp
3. Test kortdragning-flyten på nytt

---

## 📚 Mer Dokumentasjon

- [WARP.md](lpg-ehl-core/WARP.md) - Detaljert utviklerguide
- [ARCHITECTURE.md](ARCHITECTURE.md) - Systemarkitektur
- [EMULATOR.md](lpg-ehl-core/docs/EMULATOR.md) - Emulator-dokumentasjon
- [RUNNING.md](RUNNING.md) - IntelliJ run configurations

---

## ✅ Verifiser At Alt Fungerer

```bash
# 1. Bygg
./build_monolith.sh --skip-tests

# 2. Start LAB MODE
./start-lpg-ehl.sh

# 3. Åpne http://localhost:8080/control

# 4. Test kortdragning-flyt:
#    - Kortdragning
#    - Fri Dispenser
#    - Vent 5s
#    - Stopp
#    - Sjekk at pumpe går automatisk til IDLE

# ✅ Hvis alt fungerer: SUCCESS!
```

---

## 🎉 Happy Coding!

For spørsmål eller problemer, sjekk logs eller se detaljert dokumentasjon i README.md
