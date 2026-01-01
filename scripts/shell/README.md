# Shell Scripts

Bash-skript for LPG-EHL Edge system administrasjon og testing.

---

## 📋 Skriptoversikt

### `start-local.sh` 🚀
**Hva:** Starter LPG-EHL Edge systemet i lokal utviklingsmodus.  
**Bruk:** Utviklingstesting på Mac/Linux uten Docker.  
**Kjøring:**
```bash
./start-local.sh
```

**Funksjonalitet:**
- Starter Spring Boot applikasjon
- Kobler til lokal database
- Logger til konsoll
- Hot-reload støtte (hvis konfigurert)

**Forutsetninger:**
- Java 21 installert
- Maven bygget prosjektet (`mvn clean install`)
- Database kjører (se docker-compose)

**Environment Variables:**
```bash
STATION_ID=S001          # Station identifier
EDGE_ID=EDGE-LOCAL       # Edge device ID
DISPENSER_ID=D001        # Dispenser ID
```

---

### `start-system.sh` 🖥️
**Hva:** Starter komplett LPG-EHL system med alle komponenter.  
**Bruk:** Produksjonslignende oppstart.  
**Kjøring:**
```bash
./start-system.sh
```

**Funksjonalitet:**
- Starter database (via Docker)
- Starter Edge applikasjon
- Starter API server (hvis inkludert)
- Health checks

**Stopp system:**
```bash
# Ctrl+C eller:
docker-compose down
```

---

### `enq_ping.sh` 📡
**Hva:** ENQ (Enquiry) ping-test for legacy VB6 dispenserkontroll.  
**Bruk:** Teste forbindelse til Windows Dispenserkontroll.  
**Kjøring:**
```bash
./enq_ping.sh [IP] [PORT]

# Eksempel:
./enq_ping.sh 192.168.1.100 9000
```

**Funksjonalitet:**
- Sender ENQ byte (`0x05`) til dispenser
- Venter på ACK (`0x06`) respons
- Timeout etter 5 sekunder
- Returnerer exit code 0 for suksess

**Output:**
```bash
Pinging dispenser at 192.168.1.100:9000...
✅ ACK received (Connected)
```

eller

```bash
❌ No response (Timeout)
```

**Bruksområder:**
- Verifisere at Dispenserkontroll kjører
- Sjekke nettverksforbindelse
- Automatisk health monitoring

---

### `test_vb6_protocols.sh` 🧪
**Hva:** Komplett testscript for VB6 Dispenserkontroll protokoller.  
**Bruk:** Testing av alle dispenser-kommandoer.  
**Kjøring:**
```bash
./test_vb6_protocols.sh [DISPENSER_IP] [PORT]

# Eksempel:
./test_vb6_protocols.sh 192.168.1.100 9000
```

**Tester:**
1. **ENQ Ping** - Basis forbindelsestest
2. **Status Query** - Henter dispenserstatus
3. **Start Fueling** - Initierer fylling
4. **Stop Fueling** - Stopper fylling
5. **Get Totals** - Henter totaler
6. **Reset** - Resetter dispenser

**Output Eksempel:**
```bash
==================================================
VB6 Dispenserkontroll Protocol Tester
==================================================
Target: 192.168.1.100:9000

Test 1: ENQ Ping
--------------------------------------------------
✅ PASS - ACK received

Test 2: Status Query
--------------------------------------------------
Response: STATUS OK IDLE
✅ PASS

Test 3: Start Fueling (Pump 1, 50L)
--------------------------------------------------
✅ PASS - Fueling started

...

==================================================
Test Summary
==================================================
Passed: 5/6
Failed: 1/6
```

**Forutsetninger:**
- `netcat` (nc) installert
- Nettverkstilgang til dispenser
- VB6 Dispenserkontroll kjører

---

## 🚀 Quick Start

### Lokal Utvikling
```bash
# 1. Start database
docker-compose -f docker-compose-local.yaml up -d

# 2. Start applikasjon
./start-local.sh
```

### Produksjon
```bash
# Start alt
./start-system.sh
```

### Testing
```bash
# Test dispenser
./enq_ping.sh 192.168.1.100 9000

# Kjør full test-suite
./test_vb6_protocols.sh 192.168.1.100 9000
```

---

## 🔧 Konfigurasjon

### Environment Variables

Alle skript respekterer følgende miljøvariabler:

```bash
# Station Identifiers
export STATION_ID="S001"
export EDGE_ID="EDGE-S001-01"
export DISPENSER_ID="D001"

# Database
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_NAME="lpg_ehl"

# API
export API_BASE_URL="http://localhost:8081"

# Dispenser
export DISPENSER_HOST="192.168.1.100"
export DISPENSER_PORT="9000"
```

### Application Properties

Skriptene bruker konfigurasjon fra:
```
../lpg-ehl-emulator/src/main/resources/application-local.yaml
```

---

## 📖 Relatert Dokumentasjon

- **Multi-Station Setup:** `../../docs/general/MULTI-STATION-SETUP.md`
- **Deployment Guide:** `../../docs/general/DEPLOYMENT_QUICKSTART.md`
- **Developer Guide:** `../../docs/general/DEVELOPER_GUIDE.md`

---

## 🆘 Feilsøking

### Problem: `start-local.sh` - "Java not found"
**Løsning:**
```bash
# Installer Java 21 via SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.7-tem
sdk use java 21.0.7-tem
```

### Problem: `enq_ping.sh` - "netcat not found"
**Løsning:**
```bash
# MacOS
brew install netcat

# Linux
sudo apt-get install netcat
```

### Problem: Database Connection Refused
**Løsning:**
```bash
# Start database
docker-compose -f docker-compose-local.yaml up -d

# Verifiser
docker ps | grep postgres
```

### Problem: Port Already in Use
**Løsning:**
```bash
# Finn prosess på port 8080
lsof -i :8080

# Drep prosess
kill -9 <PID>
```

---

## 🔐 Sikkerhet

⚠️ **Viktig:** Disse skriptene er for utvikling/testing.

**Produksjonsanbefalinger:**
- Bruk miljøvariabler for secrets (ikke hardkode)
- Kjør med begrenset brukerrettigheter
- Valider input
- Logg alle operasjoner
- Bruk TLS for eksterne forbindelser

---

## 📝 Utvikling

### Legge til Nytt Skript

1. Opprett `.sh` fil i denne mappen
2. Legg til shebang: `#!/bin/bash`
3. Dokumenter i denne README
4. Gjør kjørbar: `chmod +x script.sh`
5. Test grundig

### Skript Struktur

```bash
#!/bin/bash
# script_name.sh - Kort beskrivelse

set -e  # Exit on error
set -u  # Exit on undefined variable

# Konfigurasjon
DEFAULT_VALUE="something"

# Funksjoner
function main() {
    # Din kode her
}

# Kjør
main "$@"
```

---

## 📞 Support

For hjelp med skriptene:
- **Intern dokumentasjon:** `../../docs/general/`
- **Issues:** GitHub Issues
- **Team:** LPG-EHL Development Team

---

**Sist oppdatert:** Januar 2026  
**Vedlikeholdt av:** LPG-EHL Development Team
