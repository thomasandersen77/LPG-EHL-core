# ARK-3360 Testing Guide for Alejandro

## 📋 Quick Reference - Oppstarts kommandoer

### 🎯 Anbefalt kommando for ARK-3360:

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080
```

---

## 💾 Minnebegrensninger for ARK-3360

ARK-3360 har begrenset minne, så bruk disse parametrene:

| Konfigurasjon | Minne | Bruk |
|---------------|-------|------|
| **Minimal** | `-Xms64m -Xmx128m` | Hvis ARK har veldig lite RAM |
| **Anbefalt** | `-Xms128m -Xmx256m` | Standard for ARK-3360 |
| **Komfortabel** | `-Xms256m -Xmx512m` | Hvis ARK har mer RAM |

**Forklaring:**
- `-Xms128m` = Start med 128 MB RAM
- `-Xmx256m` = Maks 256 MB RAM

---

## 📁 Alternativer for ekstern YAML-fil

### Metode 1: Eksplisitt path (anbefalt)

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml
```

### Metode 2: Samme mappe som JAR

```bash
# Hvis application-field-external.yaml ligger i samme mappe:
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field
# Spring Boot finner automatisk application-field-external.yaml
```

### Metode 3: config/ undermappe

```bash
# Hvis du lager en config/ mappe:
mkdir -p config
mv application-field-external.yaml config/application.yaml

# Start:
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field
# Spring Boot søker automatisk i ./config/application.yaml
```

---

## 🔧 Alle kommando-varianter

### 1️⃣ **Headless med ekstern YAML (anbefalt)**

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Bruk:** Standard testing med debug API aktivert.

---

### 2️⃣ **Headless med override på kommandolinje**

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Bruk:** Når du vil overstyre serial port uten å endre YAML-filen.

---

### 3️⃣ **Headless produksjon (uten debug API)**

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml
```

**Bruk:** Produksjonsmodus - ingen web server, kun backend-logikk.  
**OBS:** Ingen curl-testing mulig i denne modusen!

---

### 4️⃣ **Webapp (med GUI)**

```bash
java -Xms256m -Xmx512m \
  -jar lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml
```

**Bruk:** Full web-app med React frontend.  
**Åpne:** `http://ark-ip:8080`

---

### 5️⃣ **Minimal minne (emergency)**

```bash
java -Xms64m -Xmx128m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Bruk:** Hvis ARK har MYE lite RAM tilgjengelig.

---

## 🧪 Testing på onsdag - Steg for steg

### Forberedelser:

```bash
# 1. Kopier filer til ARK
scp lpg-ehl-headless.jar ark-user@ark-ip:/opt/lpg/
scp application-field-external.yaml ark-user@ark-ip:/opt/lpg/

# 2. SSH til ARK
ssh ark-user@ark-ip
cd /opt/lpg

# 3. Rediger YAML-filen
nano application-field-external.yaml
```

**I YAML-filen, endre:**
```yaml
ehl:
  serial:
    port: /dev/ttyS3    # ARK-3360 serial port
    parity: EVEN        # For ekte hardware
```

---

### Start applikasjonen:

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Logger automatisk til:** `logs/lpg-ehl.log`

---

### Test fra laptop (annen maskin):

```bash
# UNBLOCK (Fri pumpe) - adresse 34 (legacy for dispenser 2)
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/unblock"

# Se status og volum
curl "http://ark-ip:8080/api/v1/emulator/pump/34/status"

# Real-time volum monitoring (kjører mens pumpen pumper)
watch -n 0.5 'curl -s http://ark-ip:8080/api/v1/emulator/pump/34/status | jq ".volumeLitres,.amountKr"'

# BLOCK (Stopp pumpe)
curl -X POST "http://ark-ip:8080/api/v1/emulator/pump/34/block"

# Sett pris
curl -X POST "http://ark-ip:8080/api/v1/prices/update" \
  -H "Content-Type: application/json" \
  -d '{"pricePerLiter": 15.90}'
```

---

### Se logger på ARK:

```bash
# Live logging
tail -f logs/lpg-ehl.log

# Søk i logger
grep "UNBLOCK" logs/lpg-ehl.log
grep "ERROR" logs/lpg-ehl.log
grep "pumpe" logs/lpg-ehl.log
```

---

## 🔍 Feilsøking

### Problem: OutOfMemoryError

**Løsning:** Reduser minne til minimalt:
```bash
java -Xms64m -Xmx128m -jar lpg-ehl-headless.jar ...
```

### Problem: Port ikke funnet

**Løsning:** Sjekk at serial port eksisterer:
```bash
ls -la /dev/ttyS*
```

### Problem: Permission denied

**Løsning:** Legg til bruker i dialout-gruppen:
```bash
sudo usermod -a -G dialout $USER
# Logg ut og inn igjen
```

### Problem: Connection refused (curl)

**Løsning:** Sjekk at du startet med `--spring.web.application-type=servlet`

---

## 📊 Minnebruk - Monitoring

### Se faktisk minnebruk:

```bash
# Finn PID
ps aux | grep lpg-ehl

# Overvåk minne
top -p <PID>

# Eller med jstat (hvis Java tools er installert)
jstat -gc <PID> 1s
```

---

## ✅ Quick Checklist for Alejandro

**Før testing:**
- [ ] Kopiert JAR til ARK
- [ ] Kopiert YAML til ARK  
- [ ] Redigert YAML med `/dev/ttyS3` og `EVEN` parity
- [ ] Sjekket at serial port finnes (`ls /dev/ttyS3`)

**Under testing:**
- [ ] Startet headless med debug API
- [ ] Testet UNBLOCK fra laptop
- [ ] Overvåket volum real-time
- [ ] Testet BLOCK
- [ ] Sjekket logger på ARK

**Eksperiment:**
- [ ] Testet med ADAM tilkoblet
- [ ] Testet med ADAM frakoblet
- [ ] Verifisert at pumping fungerer like bra begge steder

---

## 💡 Pro Tips

1. **Start alltid med debug API aktivert** (`--spring.web.application-type=servlet`) for testing
2. **Bruk minimal minne** (`128m/256m`) for ARK-3360
3. **Bruk ekstern YAML** for enkel konfigurasjon
4. **Logger til fil automatisk** - ingen ekstra setup nødvendig
5. **Test fra laptop** med curl - enklere enn å være på ARK

---

## 🎯 Den ENKLESTE kommandoen (copy-paste)

```bash
java -Xms128m -Xmx256m \
  -jar lpg-ehl-headless.jar \
  --spring.profiles.active=field \
  --spring.config.location=application-field-external.yaml \
  --ehl.serial.port=/dev/ttyS3 \
  --ehl.serial.parity=EVEN \
  --spring.web.application-type=servlet \
  --server.port=8080
```

**Dette starter:**
- ✅ Headless app
- ✅ Med 128-256 MB RAM (perfekt for ARK)
- ✅ Leser config fra ekstern YAML
- ✅ Kobler til /dev/ttyS3 med EVEN parity
- ✅ Aktiverer debug REST API på port 8080
- ✅ Logger automatisk til `logs/lpg-ehl.log`

---

**Lykke til med testingen i morgen, Alejandro! 🚀**
