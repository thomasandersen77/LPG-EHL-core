# Getting Started - LPG EHL System

## 📚 Innholdsfortegnelse

1. [Oversikt](#oversikt)
2. [Lokal Utvikling](#lokal-utvikling)
3. [Demo og Testing](#demo-og-testing)
4. [Field Deployment (Produksjon)](#field-deployment-produksjon)
5. [Debugging og Diagnostikk](#debugging-og-diagnostikk)
6. [REST API for Serial Port Discovery](#rest-api-for-serial-port-discovery)

---

## Oversikt

Dette systemet består av flere moduler:

- **lpg-ehl-core**: EHL-protokoll implementasjon (IKKE RØR)
- **lpg-ehl-service**: Forretningslogikk, auto-detect, diagnostikk
- **lpg-ehl-webapp**: Web-applikasjon med GUI
- **lpg-ehl-app-headless**: Headless-app for produksjon
- **lpg-ehl-serialport-sim**: PLS simulator for testing

### Modes

Systemet støtter to modes:

1. **LAB Mode**: In-memory emulator (ingen hardware nødvendig)
2. **FIELD Mode**: Kommuniserer med ekte hardware eller simulator via serial port

---

## Lokal Utvikling

### Forutsetninger

```bash
# Java 21
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem

# Maven
sdk install maven

# socat (for virtual serial ports)
brew install socat

# Docker (for database)
brew install docker
```

### Quick Start - LAB Mode (IntelliJ)

Dette er **raskeste** måten å komme i gang på:

1. **Åpne prosjektet i IntelliJ**
   ```
   File > Open > Velg pom.xml
   ```

2. **Velg run configuration**
   - `WebApp (LAB)` - Start webapp med emulator
   - `HeadlessApp (LAB)` - Start headless med emulator

3. **Klikk Debug (🐞)**

4. **Åpne browser**
   ```
   http://localhost:8080
   http://localhost:8080/control
   ```

✅ **Fordeler:**
- Ingen eksterne avhengigheter
- Rask oppstart
- Perfekt for business logic testing

❌ **Limitasjoner:**
- Tester ikke ekte serial port kommunikasjon
- Ingen test av parity/timing issues

---

## Demo og Testing

For å teste med **simulator** (mer realistisk):

### Steg 1: Bygg det du trenger

```bash
# Simulatorer (PLS + betalingsterminal)
./scripts/build-simulators.sh

# (Valgfritt) Webapp som JAR
./scripts/build-webapp.sh
```

Dette lager artifacts i `release/` (bl.a. `pls-sim.jar`, `payment-terminal-*.jar`, `lpg-ehl-webapp.jar`).

### Steg 2: Start Database (valgfritt)

```bash
docker-compose -f docker-compose-local.yaml up -d
```

### Steg 3: Start PLS-simulator (SOCAT)

```bash
./scripts/sim-pls.sh
```

**Avansert bruk:**
```bash
./scripts/sim-pls.sh \
  --address=2 \
  --price=2100 \
  --baud=19200 \
  --parity=EVEN \
  --blocked=false
```

Dette starter:
- ✅ SOCAT (virtual serial port pair)
- ✅ PLS Simulator (koblet til PTY0)
- ✅ Tilgjengelig port: `/tmp/vserial1` (for webapp/IntelliJ/Python)

### Steg 4: Start Webapp (FIELD) og koble til `/tmp/vserial1`

**Anbefalt:** start webapp i IntelliJ.

Run args (FIELD + SOCAT):
- `--spring.profiles.active=field`
- `--ehl.serial.port=/tmp/vserial1`
- (valgfritt) `--ehl.serial.parity-auto-detect=true`

Hvis du vil starte fra JAR:
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1 \
  --ehl.serial.parity-auto-detect=true
```

### Steg 5: Test systemet

Åpne browser:
```
Dashboard:     http://localhost:8080
Control Panel: http://localhost:8080/control
```

Test operasjoner:
1. "Simuler kortdragning"
2. "Fri dispenser"
3. Se pumpe gå til READY_TO_PUMP
4. Start pumping

---

## Field Deployment (Produksjon)

For deployment på bensinstasjon med **ekte hardware**:

### Scenario A: Du vet port og parity

```bash
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity=EVEN \
  --ehl.serial.baud-rate=9600
```

### Scenario B: Du vet bare port (auto-detect parity)

```bash
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity-auto-detect=true
```

**Auto-detect vil:**
1. ✅ Teste NONE parity (8N1)
2. ✅ Teste EVEN parity (8E1)
3. ✅ Teste ODD parity (8O1)
4. ✅ Velge første som fungerer
5. ✅ Logge resultatet

### Scenario C: Du vet INGENTING (smart scan)

Bruk webapp med REST API for å finne riktig port:

```bash
# Start webapp først
java -jar lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --server.port=8080

# I egen terminal, kjør smart scan
curl -X POST http://localhost:8080/api/debug/serial/smart-scan
```

**Output:**
```json
[
  {
    "port": {
      "path": "/dev/ttyS0",
      "description": "USB Serial",
      "vendorId": 1234,
      "productId": 5678
    },
    "baudRate": 9600,
    "parity": "NONE",
    "dispenserAddress": 1,
    "confidence": 195
  }
]
```

Bruk så konfigurasjonen som ble funnet:

```bash
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity=NONE \
  --ehl.serial.baud-rate=9600
```

### Systemd Service (anbefalt for produksjon)

Opprett `/etc/systemd/system/lpg-ehl.service`:

```ini
[Unit]
Description=LPG EHL Headless Application
After=network.target

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity-auto-detect=true
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Start:
```bash
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl
sudo systemctl status lpg-ehl
```

---

## Debugging og Diagnostikk

### IntelliJ Debugging

Se [INTELLIJ_DEBUG_GUIDE.md](docs/INTELLIJ_DEBUG_GUIDE.md) for detaljert guide.

**Quick summary:**
1. Velg run configuration (f.eks. "WebApp (FIELD - Auto-detect)")
2. Sett breakpoints i koden
3. Klikk debug (🐞)
4. Trigger operasjon via GUI
5. Inspiser variabler i debugger

### Python Test Scripts (Alejandros scripts)

Test kommunikasjon med Python først (baseline):

```bash
cd projects/python-test

# Test mot simulator
python3 01_probe_readonly.py \
  --port /tmp/vserial1 \
  --addr 1 \
  --debug
```

Hvis Python fungerer, vet du at:
- ✅ Serial port er korrekt
- ✅ Simulator responderer
- ✅ Parity er sannsynligvis NONE (8N1)

### REST API Diagnostikk

Start webapp med REST API:

```bash
./scripts/start-webapp-field.sh --auto-detect
```

**Liste tilgjengelige ports:**
```bash
curl http://localhost:8080/api/debug/serial/ports
```

**Output:**
```json
[
  {
    "path": "/dev/ttyS0",
    "description": "USB Serial Converter",
    "location": "USB1",
    "vendorId": 1027,
    "productId": 24577
  },
  {
    "path": "/tmp/vserial1",
    "description": "Unknown",
    "location": "Unknown",
    "vendorId": 0,
    "productId": 0
  }
]
```

**Health check (test aktiv connection):**
```bash
curl http://localhost:8080/api/debug/serial/health
```

**Output:**
```json
{
  "connected": true,
  "testPassed": true,
  "responseTimeMs": 45,
  "error": null,
  "responseCommand": "STATE"
}
```

**Connection status:**
```bash
curl http://localhost:8080/api/debug/serial/status
```

**Smart scan (finn working config):**
```bash
curl -X POST http://localhost:8080/api/debug/serial/smart-scan
```

**Manuel parity detection:**
```bash
curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/dev/ttyS0"
```

---

## REST API for Serial Port Discovery

Webapp har kraftige diagnostikk-endpoints for field deployment.

### GET `/api/debug/serial/ports`

Liste alle tilgjengelige serial ports på systemet.

**Request:**
```bash
curl http://localhost:8080/api/debug/serial/ports
```

**Response:**
```json
[
  {
    "path": "/dev/ttyS0",
    "description": "Built-in Serial Port",
    "location": "Internal",
    "vendorId": 0,
    "productId": 0
  },
  {
    "path": "/dev/ttyUSB0",
    "description": "USB-to-Serial Adapter",
    "location": "USB1",
    "vendorId": 1234,
    "productId": 5678
  }
]
```

### POST `/api/debug/serial/smart-scan`

Automatisk finn working serial port configuration. Stopper på første match (production mode).

**Request:**
```bash
# Default: 1 sekund timeout, stopp på første match
curl -X POST http://localhost:8080/api/debug/serial/smart-scan

# Full scan (finn alle working configs)
curl -X POST "http://localhost:8080/api/debug/serial/smart-scan?stopOnFirst=false"

# Custom timeout (raskere, men mindre pålitelig)
curl -X POST "http://localhost:8080/api/debug/serial/smart-scan?timeoutMs=500"
```

**Response:**
```json
[
  {
    "port": {
      "path": "/dev/ttyS0",
      "description": "Built-in Serial Port",
      "vendorId": 0,
      "productId": 0
    },
    "baudRate": 9600,
    "parity": "NONE",
    "dispenserAddress": 33,
    "confidence": 200
  }
]
```

**Confidence score:**
- 200+ : Høy (9600 baud + NONE/EVEN parity + address 33 legacy)
- 180-199 : Høy (standard address 1)
- 150-179 : Medium
- <150 : Lav (uvanlig konfigurasjon)

**Adresser som testes:**
- Standard: 1, 2, 3, 4
- Legacy format (32 + pump_number): 33, 34, 35, 36

### POST `/api/debug/serial/scan-addresses`

Skan en range av adresser på en spesifikk port. Tilsvarende Alejandros `02_scan_addresses.py`.

**Request:**
```bash
# Scan standard + legacy range (1-40)
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=40"

# Scan bare legacy addresses (basert på Alejandros funn: 32 + pumpenummer)
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=32&end=40"

# Med custom parity
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&parity=EVEN&start=1&end=10"
```

**Response:**
```json
{
  "portPath": "/dev/ttyUSB0",
  "addressRange": "32-40",
  "baudRate": 9600,
  "parity": "NONE",
  "respondingAddresses": [
    {
      "address": 33,
      "description": "(legacy: 32 + pump 1)",
      "responseTimeMs": 45
    },
    {
      "address": 34,
      "description": "(legacy: 32 + pump 2)",
      "responseTimeMs": 52
    }
  ],
  "testedCount": 9
}
```

**Basert på Alejandros felttesting:**
- Real pumper responderer ofte på adresse 32 + pumpenummer
- F.eks. pumpe 1 = adresse 33, pumpe 2 = adresse 34
- STATE, ERROR_QUERY, VOLUME, TANKBIT kommandoer fungerer

### GET `/api/debug/serial/health`

Test aktiv serial connection.

**Request:**
```bash
curl http://localhost:8080/api/debug/serial/health

# Test med spesifikk address
curl "http://localhost:8080/api/debug/serial/health?address=2"
```

**Response (success):**
```json
{
  "connected": true,
  "testPassed": true,
  "responseTimeMs": 42,
  "error": null,
  "responseCommand": "STATE"
}
```

**Response (failure):**
```json
{
  "connected": true,
  "testPassed": false,
  "responseTimeMs": 2001,
  "error": "Timeout waiting for response",
  "responseCommand": null
}
```

### POST `/api/debug/serial/auto-detect`

Manuel parity detection for en spesifikk port.

**⚠️ ADVARSEL:** Dette vil midlertidig overstyre aktiv connection!

**Request:**
```bash
curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/dev/ttyS0&address=1"
```

**Response:**
```json
{
  "detected": true,
  "parityMode": "NONE",
  "description": "8N1 - No parity (simulator/Python)",
  "error": null
}
```

### GET `/api/debug/serial/status`

Enkel connection status.

**Request:**
```bash
curl http://localhost:8080/api/debug/serial/status
```

**Response:**
```json
{
  "connected": true,
  "transportType": "SerialPortManager"
}
```

---

## Headless med Debug API

Headless-applikasjonen kan også eksponere REST-endepunkter for diagnostikk ved å bruke `debug-api` profilen:

```bash
# Start headless med debug API (Undertow web server)
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field,debug-api \
  --ehl.serial.port=/dev/ttyUSB0
```

Nå er de samme endepunktene tilgjengelige som i webapp:

```bash
# Liste serial porter
curl http://localhost:8080/api/debug/serial/ports

# Scan adresser (som Alejandros Python script)
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=32&end=40"

# Smart scan
curl -X POST http://localhost:8080/api/debug/serial/smart-scan

# Pumpe-kontroll
curl http://localhost:8080/api/debug/state/33
curl -X POST http://localhost:8080/api/debug/unblock/33
```

Dette er spesielt nyttig for felt-diagnostikk når du trenger å debugge uten GUI.

---

## Feilsøking

### Problem: "Address already in use"

**Løsning:** En annen instans kjører allerede. Stopp eksisterende prosesser.

```bash
# Finn prosess
lsof -i :8080

# Drep prosess
kill <PID>
```

### Problem: "Failed to open serial port"

**Sjekk:**

1. Er socat kjørende?
   ```bash
   ps aux | grep socat
   ```

2. Er simulator kjørende?
   ```bash
   ps aux | grep pls-sim
   ```

3. Er port-navnet riktig?
   ```bash
   ls -la /tmp/vserial*
   ```

### Problem: "Could not auto-detect parity"

**Sjekk:**

1. Er pumpen på og tilkoblet?
2. Test med Python script først:
   ```bash
   cd projects/python-test
   python3 01_probe_readonly.py --port /dev/ttyS0 --addr 1 --debug
   ```
3. Prøv smart scan via REST API

### Problem: Maven import feiler i IntelliJ

**Løsning:**

1. `File > Invalidate Caches / Restart`
2. Høyreklikk på `pom.xml` > `Maven > Reload Project`
3. Sjekk at Java SDK er satt til 21

---

## Oppsummering - Quick Commands

### Lokal Utvikling (LAB Mode)

```bash
# IntelliJ: Velg "WebApp (LAB)" og klikk debug
# Eller via JAR:
mvn package -DskipTests
java -jar release/lpg-ehl-webapp.jar --spring.profiles.active=lab
```

### Demo/Testing (Simulator)

```bash
# Terminal 1: Start simulator
./scripts/start-socat-sim.sh

# Terminal 2: Start webapp
./scripts/start-webapp-field.sh --auto-detect

# Browser: http://localhost:8080
```

### Produksjon (Ekte Hardware)

```bash
# Med kjent konfigurasjon
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity=EVEN

# Med auto-detect
java -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.parity-auto-detect=true

# Med smart scan (via webapp API)
java -jar lpg-ehl-webapp.jar --spring.profiles.active=field
curl -X POST http://localhost:8080/api/debug/serial/smart-scan
```

---

## Videre Lesning

- [INTELLIJ_DEBUG_GUIDE.md](docs/INTELLIJ_DEBUG_GUIDE.md) - IntelliJ debugging
- [IMPLEMENTATION_MODE_PARITY_FIX.md](docs/IMPLEMENTATION_MODE_PARITY_FIX.md) - Parity konfigurasjon
- [PYTHON_INTEGRATION_ANALYSIS.md](docs/PYTHON_INTEGRATION_ANALYSIS.md) - Python/Kotlin integrasjon
- [COMPLETE_ARCHITECTURE_GUIDE.md](docs/COMPLETE_ARCHITECTURE_GUIDE.md) - Fullstendig arkitektur

---

**Lykke til! 🚀**
