# Headless Deployment Guide

Guide for å kjøre simulatorene på en headless Debian 64-bit server via SSH uten GUI-komponenter.

## Oversikt

Simulatorene kan kjøres i ulike moduser avhengig av miljø:

| Modus | Bruksområde | Komponenter | GUI |
|-------|-------------|------------|-----|
| **Default** | Lokal utvikling | Socat + PLS + Terminal | ✅ (JavaFX) |
| **Field** | ARK/edge produksjon | Socat + PLS | ❌ |
| **Headless** | Remote server (SSH) | Alle med terminal output | ❌ |
| **Field + Headless** | Remote field testing | Socat + PLS + Terminal | ❌ |

## Headless Mode

### Hva er headless mode?

`--headless` flagget tvinger **alle** komponenter til å kjøre med kun terminal output, uten noen GUI-komponenter (JavaFX, Swing, etc.).

Dette er perfekt for:
- Debian servere tilgjengelig kun via SSH
- CI/CD pipelines
- Docker containers
- Automatisert testing

### Quick Start (Headless)

```bash
# På Debian server via SSH:
./scripts/start-all-simulators.sh --headless

# Eller med field mode:
./scripts/start-all-simulators.sh --field --headless
```

### Kombinasjoner

#### 1. Alle komponenter (headless)
```bash
./scripts/start-all-simulators.sh --headless
```
**Starter:**
- Socat (serial port emulering)
- PLS simulator (terminal output)
- Payment Terminal simulator (headless REST API)

**Bruk:**
- Lokal testing på server
- Full stack testing uten GUI

---

#### 2. Field mode (kun PLS, headless)
```bash
./scripts/start-all-simulators.sh --field --headless
```
**Starter:**
- Socat
- PLS simulator (terminal output)

**Webapp startes separat:**
```bash
java -jar release/lpg-ehl-webapp.jar \
    --spring.profiles.active=field \
    --ehl.serial.port=/tmp/vserial1
```

**Bruk:**
- Produksjon (ARK/edge)
- Integration testing

---

#### 3. Field mode med terminal (headless)
```bash
./scripts/start-all-simulators.sh --field --with-terminal --headless
```
**Starter:**
- Socat
- PLS simulator (terminal output)
- Payment Terminal simulator (headless REST API)

**Bruk:**
- Testing av komplett flyt på remote server
- Debugging av terminal-integrasjon uten GUI

---

## Debian Setup

### 1. Installer avhengigheter

```bash
# Java 21
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem

# Socat (for virtuell serial port)
sudo apt-get update
sudo apt-get install -y socat

# Eventuelt: Maven for bygging
sdk install maven
```

### 2. Bygg simulatorene

```bash
cd /path/to/lpg-ehl/projects/lpg-ehl
./scripts/build-simulators.sh
```

### 3. Start headless

```bash
./scripts/start-all-simulators.sh --headless
```

### 4. Verifiser

Simulatorene logger til stdout. Du skal se:

```
═══════════════════════════════════════════════════════════
  🛢️  Start Simulators – HEADLESS (terminal-only)
═══════════════════════════════════════════════════════════

[1/3] Starting socat...
      /tmp/vserial0  ← PLS Simulator
      /tmp/vserial1  ← Webapp
      ✓ Socat running (PID: 12345)

[2/3] Starting PLS Simulator (pumpestyring)...
      Port: /tmp/vserial0  (adresse 1)
      ✓ PLS Simulator running (PID: 12346)

[3/3] Starting Payment Terminal Simulator (HEADLESS)...
      Port: 18080
      ✓ Terminal running (PID: 12347)
      → http://localhost:18080

═══════════════════════════════════════════════════════════
  ✅ Klart
═══════════════════════════════════════════════════════════

  Headless mode – alle komponenter kjører uten GUI
  Perfekt for Debian server via SSH.
```

---

## Testing

### Terminal Simulator (REST API)

```bash
# Health check
curl http://localhost:18080/health

# Start payment
curl -X POST http://localhost:18080/api/terminal/payment/purchase \
  -H "Content-Type: application/json" \
  -d '{"amount": 100, "currency": "NOK"}'
```

### PLS Simulator

PLS logger meldinger til stdout:

```
[PLS] Received: UNBLOCK (0x4B)
[PLS] Sending: OK
[PLS] State: UNBLOCKED
```

---

## Stopp simulatorene

```bash
# Trykk Ctrl+C i terminalen hvor skriptet kjører
^C
🛑 Stopping all services...
  ✓ PLS Simulator stopped
  ✓ Payment Terminal Simulator stopped
  ✓ Socat stopped

Bye!
```

---

## Feilsøking

### Problem: Port 18080 er opptatt

**Løsning:**
```bash
# Finn prosessen
lsof -ti:18080

# Drep prosessen
lsof -ti:18080 | xargs kill -9

# Eller start på annen port
./scripts/start-all-simulators.sh --headless --terminal-port=18081
```

### Problem: Serial port permissions

**Løsning:**
```bash
# Legg til bruker i dialout-gruppen
sudo usermod -a -G dialout $USER

# Logg ut og inn igjen for at endringen skal tre i kraft
```

### Problem: JavaFX error i headless mode

Hvis du ser `UnsupportedOperationException: Unable to open DISPLAY`:

**Årsak:** `--gui` flagget er satt samtidig med `--headless`

**Løsning:** Ikke bruk `--gui` sammen med `--headless`:
```bash
# FEIL:
./scripts/start-all-simulators.sh --headless --gui

# RIKTIG:
./scripts/start-all-simulators.sh --headless
```

---

## Eksempler

### Scenario 1: Quick test på lokal Mac/Linux

```bash
# Med GUI (standard)
./scripts/start-all-simulators.sh

# Uten GUI
./scripts/start-all-simulators.sh --headless
```

### Scenario 2: Deploy til Debian server

```bash
# SSH inn
ssh user@debian-server

# Bygg og start
cd /opt/lpg-ehl/projects/lpg-ehl
./scripts/build-simulators.sh
./scripts/start-all-simulators.sh --headless

# Start webapp i egen terminal
java -jar release/lpg-ehl-webapp.jar \
    --spring.profiles.active=field \
    --ehl.serial.port=/tmp/vserial1
```

### Scenario 3: CI/CD pipeline

```bash
#!/bin/bash
# .github/workflows/test-integration.sh

# Start simulatorene i bakgrunnen
./scripts/start-all-simulators.sh --headless &
SIM_PID=$!

# Vent til simulatorene er klare
sleep 5

# Kjør integrasjonstester
mvn verify -Pintegration-tests

# Stopp simulatorene
kill $SIM_PID
```

---

## Oppsummering

| Kommando | Bruksområde |
|----------|-------------|
| `./scripts/start-all-simulators.sh` | Lokal dev med GUI |
| `./scripts/start-all-simulators.sh --headless` | Remote server, alle komponenter |
| `./scripts/start-all-simulators.sh --field` | Produksjon, kun PLS |
| `./scripts/start-all-simulators.sh --field --headless` | Remote field testing |
| `./scripts/start-all-simulators.sh --field --with-terminal --headless` | Full stack remote testing |

**Viktig:** `--headless` tvinger **alltid** terminal output, uansett andre flagg.
