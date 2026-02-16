# 🧪 Feltesting av LPG-EHL mot ekte serialporter

Hei Alejandro og Tobias!

Dette er verdt å prøve fordi dere trenger bare å kjøre JAR-filen med fire forskjellige konfigurasjoner for å finne ut hvilken serialport som faktisk fungerer. Bygger dere én gang, så kan dere teste alle varianter uten å bygge om.

## 🚀 Quickstart

1. **Bygg artefaktene:**
   ```bash
   git checkout main
   ./build_monolith.sh
   ```

   Dette lager tre JAR-filer i `release/`-katalogen, men kun to er interessante for feltesting:
   - `lpg-ehl-webapp.jar` → GUI med WebSocket logging
   - `lpg-ehl-headless.jar` → Background service (kan kjøres med eller uten debug-api)

2. **Kopier konfigurasjonsfilen:**
   ```bash
   cp application-h2.yaml release/
   cd release/
   ```

Nu är ni redo att testa!

## 🔌 Finn riktig serialport på Linux

På Windows bruker vi `COM2` eller `COM3`. På Linux må vi finne tilsvarende:

```bash
# List tilgjengelige serial ports
ls -l /dev/ttyS*
ls -l /dev/ttyUSB*
ls -l /dev/ttyAMA*  # Raspberry Pi

# Vanlige kandidater:
# /dev/ttyS0  → COM1
# /dev/ttyS1  → COM2  ← SANNSYNLIG
# /dev/ttyS2  → COM3  ← SANNSYNLIG
# /dev/ttyUSB0 → USB-til-seriell adapter
```

**Tips:** Kjør kommandoen før og etter du plugger inn USB-enheten for å se hvilken port som dukker opp.

## 🧪 Testscenario 1: Headless + curl (ingen GUI)

Perfekt for ARK-3600 med bare SSH-tilgang.

### Start applikasjonen:
```bash
java -jar lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,debug-api \
  --ehl.serial.port=/dev/ttyS1
```

### Test via curl:
```bash
# Helsesjekk
curl http://localhost:8080/api/debug/health

# Les pumpetilstand (adresse 1)
curl http://localhost:8080/api/debug/state/1

# Frigi pumpe (adresse 1)
curl -X POST http://localhost:8080/api/debug/unblock/1

# Blokker pumpe
curl -X POST http://localhost:8080/api/debug/block/1
```

## 🖥️ Testscenario 2: WebApp + GUI

Perfekt hvis dere har tilgang til nettleser på Linux-maskinen, eller kan port-forwarde.

### Start applikasjonen:
```bash
java -jar lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2 \
  --lpg.mode=FIELD \
  --ehl.transport.mode=HARDWARE \
  --ehl.serial.port=/dev/ttyS1
```

### Åpne i nettleser:
```
http://localhost:8080
```

Her ser du:
- 🟢 Live logging av TX/RX bytes
- 🎛️ Kontrollpanel for å frigjøre kort
- 📊 Transaksjonshistorikk

Når du frigir kort i GUI-en, sendes EHL-protokollmeldinger over den konfigurerte serialporten og pumpa skal frigjøres.

## 🔍 Testing av alle serialporter (systematisk tilnærming)

Kjør disse fire kommandoene etter hverandre. Den som fungerer vil gi respons fra pumpa:

### Test 1: /dev/ttyS0
```bash
java -jar lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,debug-api \
  --ehl.serial.port=/dev/ttyS0

# I annen terminal:
curl -X POST http://localhost:8080/api/debug/unblock/1
```

### Test 2: /dev/ttyS1 (mest sannsynlig = COM2)
```bash
java -jar lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,debug-api \
  --ehl.serial.port=/dev/ttyS1

curl -X POST http://localhost:8080/api/debug/unblock/1
```

### Test 3: /dev/ttyS2 (mest sannsynlig = COM3)
```bash
java -jar lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,debug-api \
  --ehl.serial.port=/dev/ttyS2

curl -X POST http://localhost:8080/api/debug/unblock/1
```

### Test 4: /dev/ttyUSB0 (hvis USB-adapter)
```bash
java -jar lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,debug-api \
  --ehl.serial.port=/dev/ttyUSB0

curl -X POST http://localhost:8080/api/debug/unblock/1
```

## ⚙️ Konfigurere serial parameters

Hvis dere trenger å endre baud rate, data bits eller parity, **rediger `application-h2.yaml`** i release-katalogen:

```yaml
ehl:
  serial:
    port: /dev/ttyS1
    baud-rate: 9600        # ← Endre her (vanlig: 4800, 9600, 19200, 38400)
    data-bits: 8           # ← Standard: 8
    parity: EVEN           # ← NONE, ODD, EVEN, MARK, SPACE
    stop-bits: 1           # ← 1 eller 2
```

Standard EHL-protokoll er **9600 baud, 8E1** (8 data bits, Even parity, 1 stop bit).

## 🔑 Viktige detaljer

### Lab vs Field mode
- **LAB mode** (`--lpg.mode=LAB`): Aktiverer emulator, ingen ekte serialport
- **FIELD mode** (`--lpg.mode=FIELD`): Ekte hardware via serialport ✅

### Transport modes
- `EMULATOR`: In-memory emulator (ingen hardware)
- `SOCAT`: Virtuell PTY for testing med simulator
- `HARDWARE`: Ekte RS-485 serialport ✅

### For feltesting må dere sette:
```bash
--lpg.mode=FIELD \
--ehl.transport.mode=HARDWARE \
--ehl.serial.port=/dev/ttySX
```

## 🐛 Troubleshooting

### "Permission denied" på /dev/ttyS*
```bash
# Gi rettigheter til brukeren
sudo chmod 666 /dev/ttyS1

# Eller legg til brukeren i dialout-gruppen
sudo usermod -a -G dialout $USER
# (krever logout/login)
```

### Ingen respons fra pumpa?
1. Sjekk at kabelen er koblet til riktig port
2. Prøv alle fire serialporter systematisk
3. Verifiser baud rate (9600 er standard)
4. Sjekk loggene: `tail -f /tmp/lpg-ehl/headless.log`

### Se detaljert protokoll-logging
Rediger `application-h2.yaml`:
```yaml
logging:
  level:
    no.cloudberries.lpg.communication: DEBUG  # TX/RX bytes
    no.cloudberries.lpg.protocol: DEBUG       # EHL packet parsing
```

## 📋 Oppsummering

Det dere trenger å gjøre:

1. ✅ Checkout main branch
2. ✅ Kjør `./build_monolith.sh`
3. ✅ Kopier `application-h2.yaml` til `release/`
4. ✅ Test de fire serialportene systematisk
5. ✅ Når dere finner riktig port → oppdater konfigurasjonsfilen

Ingen ombygging nødvendig mellom testene! Bare stopp JAR-en (Ctrl+C) og start på nytt med ny `--ehl.serial.port` parameter.

**Lykke til! 🚀**

--- 
Spørsmål? Ring meg (men jeg er syk, så jeg håper dette holder 😊)
