# Ekstern konfigurasjon - 2 filer for alt

## 📋 Oversikt

Du har **2 eksterne YAML-filer** som fungerer for **både webapp OG headless**:

```
lpg-ehl/
├── application-local.yaml              # LAB mode (emulator)
├── application-field-external.yaml     # FIELD mode (ekte hardware)
├── release/
│   ├── lpg-ehl-webapp.jar
│   └── lpg-ehl-headless.jar
└── logs/
    └── lpg-ehl.log                     # Logges automatisk hit
```

---

## 🎯 Hva er forskjellen?

| Fil | Modus | Brukes til |
|-----|-------|------------|
| `application-local.yaml` | **LAB** | Emulator, in-memory H2, ingen hardware |
| `application-field-external.yaml` | **FIELD** | Ekte hardware eller simulator (socat) |

**Begge:**
- ✅ Bruker H2 database (in-memory)
- ✅ Logger til `logs/lpg-ehl.log` automatisk
- ✅ Alle parametere kan overstyres fra kommandolinje
- ✅ Fungerer for både webapp og headless

---

## 🚀 Hvordan bruke

### 1️⃣ **LAB mode** (emulator)

**Webapp:**
```bash
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=lab \
  --spring.config.location=application-local.yaml
```

**Headless (med debug API):**
```bash
java -Xms128m -Xmx256m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=lab \
  --spring.config.location=application-local.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

---

### 2️⃣ **FIELD mode** (simulator eller ekte hardware)

**Med simulator (socat):**

```bash
# Terminal 1: Start simulator
./scripts/start-socat-sim.sh --parity=NONE --address=2

# Terminal 2: Start webapp
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml
```

**Headless på ARK-3360:**
```bash
java -Xms128m -Xmx256m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080
```

---

## 📝 Redigere YAML-filene

### LAB mode (`application-local.yaml`)

**Trenger normalt IKKE endres** - fungerer out-of-the-box med emulator.

### FIELD mode (`application-field-external.yaml`)

**Rediger disse linjer:**

```yaml
ehl:
  serial:
    port: /tmp/vserial1        # ← ENDRE TIL DIN PORT
    parity: NONE               # ← ENDRE TIL EVEN for hardware
```

**Eksempler:**
```yaml
# For socat simulator
port: /tmp/vserial1
parity: NONE

# For ARK-3360
port: /dev/ttyS3
parity: EVEN

# For USB-adapter på Linux
port: /dev/ttyUSB0
parity: EVEN

# For Windows
port: COM3
parity: EVEN
```

---

## 📊 Logging

**Automatisk logging til fil - ingen ekstra kommandoer nødvendig!**

```bash
# Se live logging
tail -f logs/lpg-ehl.log

# Eller med farger
tail -f logs/lpg-ehl.log | ccze -A

# Søk i logger
grep "ERROR" logs/lpg-ehl.log
grep "UNBLOCK" logs/lpg-ehl.log
```

**Logg-rotasjon:**
- Maks filstørrelse: 10 MB
- Antall historiske filer: 30
- Total cap: 300 MB
- Automatisk cleanup av gamle logger

**Logg-filer:**
```
logs/
├── lpg-ehl.log              # Aktiv logg
├── lpg-ehl.log.1            # Forrige
├── lpg-ehl.log.2
└── ...
```

---

## 🔧 Override fra kommandolinje

**Alle YAML-parametere kan overstyres:**

```bash
java -jar lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --ehl.serial.port=/dev/ttyS3 \           # Override serial port
  --ehl.serial.parity=EVEN \               # Override parity
  --server.port=9090 \                     # Override web port
  --logging.level.no.cloudberries.lpg=DEBUG  # Override log level
```

---

## 📚 Komplett eksempel - Fra start til slutt

### Testing lokalt med simulator:

```bash
# 1. Start simulator
./scripts/start-socat-sim.sh --parity=NONE --address=2

# 2. Start headless med FIELD mode
java -Xms256m -Xmx512m \
  -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080

# 3. Test med curl
curl -X POST "http://localhost:8080/api/v1/emulator/pump/2/unblock"
curl "http://localhost:8080/api/v1/emulator/pump/2/status"
curl -X POST "http://localhost:8080/api/v1/emulator/pump/2/block"

# 4. Se logger
tail -f logs/lpg-ehl.log
```

---

### På ARK-3360 (produksjon):

```bash
# 1. Kopier filer til ARK
scp release/lpg-ehl-headless.jar ark-user@ark-ip:/opt/lpg/
scp application-field-external.yaml ark-user@ark-ip:/opt/lpg/

# 2. SSH til ARK og rediger YAML
ssh ark-user@ark-ip
cd /opt/lpg
nano application-field-external.yaml  # Endre port til /dev/ttyS3, parity til EVEN

# 3. Start headless
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080

# 4. Test fra laptop
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"
curl "http://ark-ip:8080/api/v1/emulator/pump/34/status"
```

---

## ✅ Oppsummering

**Du har nå:**
- ✅ 2 enkle YAML-filer (lab og field)
- ✅ Automatisk logging til fil
- ✅ H2 database som default
- ✅ Fungerer for både webapp og headless
- ✅ Alle parametere kan overstyres
- ✅ Ingen komplekse skript nødvendig

**For Alejandro (onsdag):**
```bash
# På ARK-3360
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080

# Test fra laptop
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"
watch -n 0.5 'curl -s http://ark-ip:8080/api/v1/emulator/pump/34/status | jq'
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/block"

# Se logger på ARK
tail -f logs/lpg-ehl.log
```

🎉 **Klart for testing!**
