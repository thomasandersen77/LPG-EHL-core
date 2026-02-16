# LPG-EHL Quick Start

## 🚀 Enkel oppstart (1-2-3)

### 1️⃣ Bygg JAR-filen (en gang)
```bash
mvn clean package -DskipTests
cp lpg-ehl-webapp/target/lpg-ehl-webapp-*.jar release/lpg-ehl-webapp.jar
cp lpg-ehl-app-headless/target/lpg-ehl-app-headless-*.jar release/lpg-ehl-headless.jar
```

### 2️⃣ Rediger `application-local.yaml`
```bash
nano application-local.yaml  # Endre serial port
```

### 3️⃣ Start applikasjonen
```bash
# Se kommandoer under 👇
```

---

## 📋 Kommandoer for å starte JAR

### ✅ Webapp (med GUI)

**Enkleste:**
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

**Med minnebegrensning:**
```bash
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

**Med ekstra parametere:**
```bash
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml \
  --server.port=8080
```

→ Åpne: **http://localhost:8080**

---

### ✅ Headless (uten GUI)

**Enkleste:**
```bash
java -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

**Med minnebegrensning (for ARK-3360):**
```bash
java -Xms128m -Xmx256m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

**Med debug REST API (for testing):**
```bash
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

---

## 🧪 Testing med Simulator

### Terminal 1: Start PLS simulator (SOCAT)
```bash
./scripts/sim-pls.sh --address=2 --parity=NONE
```

### Terminal 2: Start webapp
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

Eller **quick override** uten å endre YAML:
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1 \
  --ehl.serial.parity=NONE
```

---

## 🏭 Produksjon (ARK-3360 / Ekte hardware)

### For Alejandros setup (ARK-3360, adresse 34)

**Rediger `application-local.yaml`:**
```yaml
ehl:
  serial:
    port: /dev/ttyS3    # ARK-3360 serial port
    parity: EVEN        # Standard for EHL hardware
```

**Start headless (lav minne for ARK):**
```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```

**Test UNBLOCK/BLOCK til adresse 34:**
```bash
# UNBLOCK (Fri pumpe)
curl -X POST "http://localhost:8080/api/v1/pumps/34/unblock"

# BLOCK (Stopp pumpe)
curl -X POST "http://localhost:8080/api/v1/pumps/34/block"

# Les volum
curl "http://localhost:8080/api/v1/pumps/34/volume"
```

---

## 📊 Minnebegrensninger

| Plattform | Anbefalt | Kommando |
|-----------|----------|----------|
| **ARK-3360** (lite minne) | 256 MB | `-Xms128m -Xmx256m` |
| **Server / PC** | 512 MB | `-Xms256m -Xmx512m` |
| **Utvikling** | 1 GB | `-Xms512m -Xmx1024m` |

---

## 🔧 Alternativer for YAML-fil

### Metode 1: Eksplisitt path (anbefalt)
```bash
java -jar app.jar --spring.config.location=application-local.yaml
```

### Metode 2: I samme mappe som JAR
```
/opt/lpg/
├── lpg-ehl-webapp.jar
└── application-local.yaml
```
```bash
cd /opt/lpg
java -jar lpg-ehl-webapp.jar --spring.profiles.active=field
# Spring Boot finner automatisk application-local.yaml
```

### Metode 3: config/ undermappe
```
/opt/lpg/
├── lpg-ehl-webapp.jar
└── config/
    └── application.yaml
```
```bash
cd /opt/lpg
java -jar lpg-ehl-webapp.jar --spring.profiles.active=field
# Spring Boot søker automatisk i ./config/
```

---

## 💡 Tips for Alejandro

### ✅ Fungerer allerede:
- UNBLOCK / BLOCK til adresse 34 ✓
- ARK-3360 via /dev/ttyS3 ✓

### 📝 Neste steg (onsdag):

**1. Les volum under pumping:**
```bash
# Start en polling-loop
while true; do
  curl -s "http://localhost:8080/api/v1/pumps/34/volume"
  sleep 0.5
done
```

**2. Sett pris (kr/L):**
```bash
curl -X POST "http://localhost:8080/api/v1/prices/update?pricePerLiter=15.90"
```

**3. Full pumpe-flow:**
```bash
# 1. UNBLOCK
curl -X POST "http://localhost:8080/api/v1/pumps/34/unblock"

# 2. Pumpe (se volum real-time)
curl "http://localhost:8080/api/v1/pumps/34/volume"

# 3. BLOCK når ferdig
curl -X POST "http://localhost:8080/api/v1/pumps/34/block"
```

### 🔍 Om ADAM-boksen:
ADAM er sannsynligvis kun for **Modbus TCP** kommunikasjon med SCADA/overvåkning.  
For **pumpe-kontroll** bruker dere **direkte RS-485** til pumpen - ADAM er IKKE involvert der.

**Test:** Koble fra ADAM ethernet-kabelen mens dere kjører Java-koden.  
Forventet resultat: Pumping fungerer fortsatt! ✅

---

## ❓ Feilsøking

**Problem:** `java.io.IOException: Port not found`  
**Løsning:** Sjekk at serial port finnes:
```bash
ls -la /dev/ttyS*   # ARK-3360
ls -la /dev/ttyUSB* # USB-adapter
ls -la /tmp/vserial* # Simulator
```

**Problem:** `OutOfMemoryError` på ARK-3360  
**Løsning:** Reduser minne:
```bash
java -Xms64m -Xmx128m -jar lpg-ehl-headless.jar ...
```

**Problem:** Ingen respons fra pumpe  
**Løsning:** Test parity auto-detect:
```bash
java -jar app.jar ... --ehl.serial.parity-auto-detect=true
```

---

## 📦 Komplett eksempel (copy-paste)

```bash
# 1. Start simulator (testing)
./scripts/sim-pls.sh --address=2 --parity=NONE &

# 2. Start webapp med ekstern YAML
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml

# 3. Test i nettleser
open http://localhost:8080
```

**Eller for ARK-3360:**
```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-local.yaml
```
