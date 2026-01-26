# LPG EHL Headless Application - Brukerveiledning

## Oversikt

Headless-applikasjonen er en Spring Boot daemon uten web-server som kommuniserer med LPG-dispensere via EHL-protokoll. Den er designet for å kjøre kontinuerlig på produksjonsservere, Raspberry Pi, eller Docker containers.

**Funksjoner:**
- 🔄 Kontinuerlig polling av dispenser status
- 💾 Automatisk lagring av transaksjoner til PostgreSQL
- ☁️  Azure sync for cloud backup
- 📊 Scheduled tasks for monitoring og watchdog
- 🧪 LAB mode med emulator for testing
- 🏭 FIELD mode for ekte hardware

---

## Arkitektur

### Drifts-moder

| Mode    | Beskrivelse | Transport | Bruksområde |
|---------|-------------|-----------|-------------|
| **LAB** | Emulator | `InMemorySerialPort` + `EhlDispenserEmulator` | Testing, utvikling, demo |
| **FIELD** | Ekte hardware | `RealSerialTransport` (serial port/TCP) | Produksjon på bensinstasjon |

### Komponenter

```
┌─────────────────────────────────────────────────────────────┐
│ HeadlessApplication (Main Entry Point)                      │
├─────────────────────────────────────────────────────────────┤
│ • WebApplicationType.NONE (ingen web-server)                │
│ • @EnableScheduling aktivert                                 │
│ • Component scanning for services                            │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
  ┌─────▼─────┐      ┌──────▼──────┐    ┌──────▼─────────┐
  │ Headless  │      │  Headless   │    │   Dispenser    │
  │Startup    │      │  Polling    │    │   Service      │
  │Runner     │      │  Service    │    │                │
  └───────────┘      └─────────────┘    └────────────────┘
       │                    │                    │
       │ Test @ startup     │ Poll every 2s      │ State machine
       │                    │                    │
       └────────────────────┴────────────────────┘
                            │
                     ┌──────▼──────┐
                     │     Ehl     │
                     │Communicator │
                     └──────┬──────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
       ┌──────▼──────┐           ┌────────▼────────┐
       │ LAB MODE    │           │  FIELD MODE     │
       │ InMemory    │           │  Real Serial    │
       │ Serial Port │           │  Transport      │
       │      +      │           │                 │
       │ Emulator    │           │ /dev/ttyS0      │
       └─────────────┘           └─────────────────┘
```

---

## Installasjon

### Bygg applikasjonen

```bash
cd /path/til/lpg-ehl

# Bygg headless-modulen
mvn clean install -pl lpg-ehl-app-headless -am -DskipTests

# Eller bruk build-clean.sh for å fikse Kotlin daemon issues
./build-clean.sh
```

JAR-filen blir bygget til:
```
lpg-ehl-app-headless/target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar
```

### Krav

**Software:**
- Java 21 eller høyere
- PostgreSQL database (kjørende og tilgjengelig)

**Hardware (FIELD mode):**
- Serial port (RS-485/RS-232)
- LPG dispenser med EHL-protokoll support

---

## Kjøring

### LAB Mode (Emulator - Anbefalt for testing)

LAB mode bruker en in-memory emulator i stedet for ekte hardware. Dette er ideelt for testing, utvikling, og demo.

#### Kommando:

```bash
java -jar lpg-ehl-app-headless/target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --lpg.mode=LAB \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/lpg_ehl \
  --spring.datasource.username=lpg_user \
  --spring.datasource.password=lpg_dev_password
```

#### Med Maven (for utvikling):

```bash
cd lpg-ehl-app-headless
mvn spring-boot:run -Dspring-boot.run.arguments="--lpg.mode=LAB"
```

#### Forventet output:

```
═══════════════════════════════════════════════════════════
   LPG EHL HEADLESS APPLICATION
   Mode: HEADLESS (No Web Server)
═══════════════════════════════════════════════════════════

🧪 LAB MODE: Configuring DispenserEmulator
   Dispenser Address: 1
   Default Price: 15.9 kr/L
✅ InMemorySerialPort connected to emulator
✅ Creating EhlCommunicator with InMemorySerialPort

═══════════════════════════════════════════════════════════
🚀 HEADLESS STARTUP SEQUENCE
═══════════════════════════════════════════════════════════

🔌 Initializing hardware communication...
   Mode: LAB
   Dispenser Address: 1
   🧪 LAB MODE: Using emulator
   📡 Testing communication with dispenser...
   ✅ Communication test successful
   Response: address=1, command=STATE, data=1 bytes

═══════════════════════════════════════════════════════════
✅ HEADLESS APPLICATION READY
═══════════════════════════════════════════════════════════

💡 System is now running in background mode
💡 Transactions will be saved to database automatically
💡 Check logs for dispenser activity

🚀 Starting dispenser polling loop...
📊 [19:45:10] Polling #10 - Dispenser 1 state: IDLE
💓 [19:45:20] Headless app is alive - Poll count: 15
📊 [19:45:30] Polling #20 - Dispenser 1 state: IDLE
```

#### Konfigurasjon (application.yaml):

```yaml
lpg:
  mode: LAB  # LAB eller FIELD
  dispenser:
    address: 1  # Dispenser address å polle
  polling:
    interval-ms: 2000  # Poll-intervall (millisekunder)

ehl:
  emulator:
    dispenser-address: 1
    price-per-liter-cents: 1590  # 15.90 kr/L
```

---

### FIELD Mode (Ekte Hardware)

⚠️ **MERK:** FIELD mode er ikke ferdig implementert ennå. Bruk LAB mode for testing.

Når FIELD mode er klar:

```bash
java -jar lpg-ehl-app-headless/target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --lpg.mode=FIELD \
  --ehl.serial.port=/dev/ttyS0 \
  --ehl.serial.baud-rate=9600 \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/lpg_ehl \
  --spring.datasource.username=lpg_user \
  --spring.datasource.password=lpg_dev_password
```

---

## Konfigurasjon

### Environment Variables

Du kan overstyre konfigurasjon med environment variables:

```bash
export LPG_MODE=LAB
export LPG_DISPENSER_ADDRESS=1
export LPG_POLLING_INTERVAL_MS=2000
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=lpg_ehl
export DB_USER=lpg_user
export DB_PASSWORD=lpg_dev_password

java -jar lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar
```

### Konfigurasjonsfil (application.yaml)

Se `lpg-ehl-app-headless/src/main/resources/application.yaml` for full konfigurasjon.

**Viktige parametre:**

| Parameter | Default | Beskrivelse |
|-----------|---------|-------------|
| `lpg.mode` | `LAB` | Drift mode: `LAB` eller `FIELD` |
| `lpg.dispenser.address` | `1` | Dispenser address (1-255) |
| `lpg.polling.interval-ms` | `2000` | Polling-intervall (ms) |
| `ehl.emulator.price-per-liter-cents` | `1590` | Pris i øre (15.90 kr/L) |
| `ehl.serial.port` | `/dev/ttyS0` | Serial port for FIELD mode |
| `ehl.serial.baud-rate` | `9600` | Baud rate for serial kommunikasjon |

---

## Logging

Headless-appen logger til konsoll og (valgfritt) til fil.

### Log-nivåer

```yaml
logging:
  level:
    root: INFO
    no.cloudberries.lpg.headless: INFO  # Headless-spesifikt
    no.cloudberries.lpg.service: INFO   # Business logic
    no.cloudberries.lpg.protocol: INFO  # EHL protokoll
    no.cloudberries.lpg.communication: INFO  # Kommunikasjon (TX/RX)
```

### Debug logging

For å aktivere debug-logging:

```bash
java -jar lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --lpg.mode=LAB \
  --logging.level.no.cloudberries.lpg=DEBUG
```

### Logg-fil (valgfritt)

```yaml
logging:
  file:
    name: /var/log/lpg-ehl/headless.log
    max-size: 10MB
    max-history: 30
```

---

## Systemd Service (Linux)

For å kjøre headless-appen som en systemd service på Linux:

### 1. Opprett service-fil

`/etc/systemd/system/lpg-ehl-headless.service`:

```ini
[Unit]
Description=LPG EHL Headless Application
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment="LPG_MODE=LAB"
Environment="SPRING_PROFILES_ACTIVE=production"

[Install]
WantedBy=multi-user.target
```

### 2. Aktiver og start service

```bash
sudo systemctl daemon-reload
sudo systemctl enable lpg-ehl-headless
sudo systemctl start lpg-ehl-headless
```

### 3. Sjekk status og logger

```bash
# Status
sudo systemctl status lpg-ehl-headless

# Logger (live)
sudo journalctl -u lpg-ehl-headless -f

# Logger (siste 100 linjer)
sudo journalctl -u lpg-ehl-headless -n 100
```

---

## Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar app.jar

ENV LPG_MODE=LAB
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Bygg og kjør

```bash
# Bygg Docker image
docker build -t lpg-ehl-headless:latest .

# Kjør container
docker run -d \
  --name lpg-ehl-headless \
  -e LPG_MODE=LAB \
  -e DB_HOST=postgres \
  -e DB_NAME=lpg_ehl \
  -e DB_USER=lpg_user \
  -e DB_PASSWORD=lpg_dev_password \
  lpg-ehl-headless:latest

# Sjekk logger
docker logs -f lpg-ehl-headless
```

---

## Feilsøking

### Problem: Applikasjonen stopper etter oppstart

**Symptom:** Appen starter men avslutter etter noen sekunder.

**Løsning:**
- Sjekk at `@EnableScheduling` er aktivert i HeadlessApplication
- Sjekk at HeadlessPollingService er scannet og kjører
- Se etter feil i loggene under oppstart

```bash
# Kjør med debug logging
java -jar lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --logging.level.no.cloudberries.lpg=DEBUG
```

### Problem: Database connection errors

**Symptom:** `Cannot create PoolableConnectionFactory`

**Løsning:**
1. Sjekk at PostgreSQL er kjørende:
   ```bash
   docker ps | grep postgres
   ```

2. Test databaseforbindelse:
   ```bash
   psql -h localhost -p 5432 -U lpg_user -d lpg_ehl
   ```

3. Korriger connection string:
   ```bash
   --spring.datasource.url=jdbc:postgresql://localhost:5432/lpg_ehl
   ```

### Problem: "FIELD MODE serial transport not yet implemented"

**Symptom:** Exception ved oppstart med `--lpg.mode=FIELD`

**Løsning:**
FIELD mode er ikke ferdig implementert. Bruk `--lpg.mode=LAB` for testing med emulator.

---

## Kontakt og Support

For problemer eller spørsmål:
- Se `TROUBLESHOOTING.md` for vanlige feil
- Se `FIXES_SUMMARY.md` for siste endringer

---

## Eksempler

### Eksempel 1: Test med emulator (LAB mode)

```bash
cd lpg-ehl-app-headless

# Start med Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--lpg.mode=LAB"

# Eller med JAR
java -jar target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar --lpg.mode=LAB
```

### Eksempel 2: Custom polling interval

```bash
java -jar lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --lpg.mode=LAB \
  --lpg.polling.interval-ms=5000  # Poll every 5 seconds
```

### Eksempel 3: Custom dispenser address

```bash
java -jar lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --lpg.mode=LAB \
  --lpg.dispenser.address=2 \
  --ehl.emulator.dispenser-address=2
```

### Eksempel 4: Production deployment (LAB mode for demo)

```bash
#!/bin/bash
# start-headless.sh

export LPG_MODE=LAB
export LPG_DISPENSER_ADDRESS=1
export DB_HOST=production-db.example.com
export DB_NAME=lpg_ehl_prod
export DB_USER=lpg_prod_user
export DB_PASSWORD="$(cat /secure/db-password)"
export STATION_ID=STATION-001
export STATION_NAME="Bergen Vest"

java -jar /opt/lpg-ehl/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar
```

---

**Status:** ✅ Headless application er klar for bruk i LAB mode. FIELD mode kommer i neste versjon.
