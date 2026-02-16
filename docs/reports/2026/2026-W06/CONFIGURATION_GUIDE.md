# LPG-EHL Konfigurasjonsveiledning

## 📋 Konfigurasjonsprinsipper

LPG-EHL følger Spring Boot best practices med **4 nivåer av konfigurasjon**:

```
┌─────────────────────────────────────────────────────────┐
│ 1. application.yaml (i JAR)                              │
│    └─ Hardkodede defaults for alle installasjoner       │
├─────────────────────────────────────────────────────────┤
│ 2. application-{profile}.yaml (i JAR)                    │
│    └─ Miljø-spesifikke defaults (field vs lab)          │
├─────────────────────────────────────────────────────────┤
│ 3. config/application.yaml (ekstern fil)                 │
│    └─ Installasjons-spesifikk config (serial port, etc.) │
├─────────────────────────────────────────────────────────┤
│ 4. Kommandolinje / Environment variables                 │
│    └─ JVM parametere, hemmeligheter, quick overrides    │
└─────────────────────────────────────────────────────────┘
         ↑ Lavere prioritet              Høyere prioritet ↑
```

---

## 🎯 Hva bør ligge hvor?

### 1️⃣ **I JAR-filen** (`application-field.yaml`, `application-lab.yaml`)

**Miljø-spesifikke defaults** som gjelder for ALLE installasjoner i det miljøet:

```yaml
# application-field.yaml
spring:
  datasource:
    url: jdbc:h2:mem:lpgdb  # Default H2 (kan overstyres)
  
logging:
  level:
    no.cloudberries.lpg: INFO

ehl:
  serial:
    baud-rate: 9600      # Standard EHL baud rate
    data-bits: 8
    stop-bits: 1
    parity: EVEN         # Standard for ekte hardware
```

✅ **Fordel:** Distribueres med JAR - fungerer out-of-the-box  
❌ **Ulempe:** Krever rebuild for å endre

---

### 2️⃣ **Ekstern config** (`config/application.yaml`)

**Installasjons-spesifikk konfigurasjon** som varierer per stasjon:

```yaml
# /opt/lpg-ehl/config/application.yaml
station:
  id: "STATION-001"
  name: "Bergen Stasjon"

ehl:
  serial:
    port: /dev/ttyUSB0   # ← Dette er UNIKT per maskin!
    parity: EVEN         # ← Kan overstyre default fra JAR
  
  dispensers:
    - address: 1         # ← Pumpenummer UNIKT per stasjon
      name: "Dispenser 1"
    - address: 2
      name: "Dispenser 2"

spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/lpgdb
    username: lpg_user
    password: ${DB_PASSWORD}  # Fra environment variable
```

✅ **Fordel:** Kan endres uten rebuild - deployment-friendly  
✅ **Fordel:** Spring Boot leser automatisk `./config/application.yaml`

---

### 3️⃣ **Kommandolinje / Oppstartskript**

**Runtime-parametere** - JVM minne, profil, hemmeligheter:

```bash
#!/bin/bash
# start-production.sh

# JVM Memory (ARK-3600: 512MB, Server: 1GB+)
JVM_MEMORY="-Xms256m -Xmx512m"

# Garbage Collection tuning
JVM_GC="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Start applikasjonen
java \
  $JVM_MEMORY \
  $JVM_GC \
  -jar lpg-ehl-webapp.jar \
  --spring.profiles.active=field
```

✅ **Fordel:** Enkelt å justere minne og GC-parametere  
✅ **Fordel:** Hemmeligheter kan settes via environment variables

---

## 📦 Produksjonsstruktur

```
/opt/lpg-ehl/
├── lpg-ehl-webapp.jar          # Applikasjonen
├── lpg-ehl-headless.jar
├── start-production.sh         # Oppstartskript med JVM-parametere
├── config/
│   └── application.yaml        # Installasjons-spesifikk config
└── logs/
    ├── lpg-ehl.log
    └── heap-dump.hprof         # Hvis OutOfMemoryError
```

---

## 🚀 Eksempel: Deployment

### Steg 1: Kopier JAR og config
```bash
scp release/lpg-ehl-webapp.jar production-server:/opt/lpg-ehl/
scp scripts/start-production.sh production-server:/opt/lpg-ehl/
```

### Steg 2: Lag config/application.yaml på serveren
```bash
ssh production-server
cd /opt/lpg-ehl
mkdir -p config

cat > config/application.yaml << 'EOF'
station:
  id: "BERGEN-001"

ehl:
  serial:
    port: /dev/ttyUSB0
  dispensers:
    - address: 1
      name: "Dispenser 1"
EOF
```

### Steg 3: Start applikasjonen
```bash
./start-production.sh webapp
```

---

## 🧪 Testing vs Produksjon

### Testing (med simulator)
```bash
# Terminal 1: Start simulator
./scripts/start-socat-sim.sh --parity=NONE

# Terminal 2: Start webapp med FIELD profil
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1 \
  --ehl.serial.parity=NONE
```

### Produksjon (ekte hardware)
```bash
# config/application.yaml inneholder serial port config
./start-production.sh headless
```

---

## ⚙️ JVM Memory Tuning

### For ARK-3600 (begrenset minne)
```bash
-Xms256m -Xmx512m    # Start med 256MB, maks 512MB
```

### For server
```bash
-Xms512m -Xmx1024m   # Start med 512MB, maks 1GB
```

### Monitoring
```bash
# Se faktisk minnebruk
jstat -gc <PID> 1s

# Hvis OutOfMemoryError:
# → Sjekk logs/heap-dump.hprof med jvisualvm
```

---

## ✅ Oppsummering - Ditt spørsmål besvart

> **Burde baud rate, data bits, parity og pumpenummer ligge i YAML?**

**JA! ✅** Men i **ekstern config** (`config/application.yaml`), IKKE i JAR:

```yaml
# config/application.yaml (per installasjon)
ehl:
  serial:
    port: /dev/ttyUSB0       # ← Unikt per maskin
    baud-rate: 9600
    parity: EVEN
    data-bits: 8
  
  dispensers:
    - address: 1             # ← Unikt per stasjon
```

> **Burde JVM minne-innstillinger ligge i YAML?**

**NEI! ❌** JVM-parametere (`-Xmx`, `-Xms`) kan IKKE settes i YAML.  
De MÅ settes på kommandolinje eller i oppstartskript:

```bash
java -Xms256m -Xmx512m -jar lpg-ehl.jar
```

> **Er dette raskeste og riktige måten?**

**JA! ✅** Dette er Spring Boot best practice og gir:
- ✅ En JAR kan deployes overalt uten rebuild
- ✅ Hver stasjon har sin egen `config/application.yaml`
- ✅ JVM-tuning skjer i oppstartskript
- ✅ Hemmeligheter i environment variables (ikke committet til Git)

---

## 📚 Les mer

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Java Memory Management](https://docs.oracle.com/en/java/javase/17/gctuning/)
