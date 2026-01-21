# LPG EHL Headless Application

Bakgrunnsservice for LPG-dispenserstyring **uten web-server eller GUI**. Perfekt for produksjonsmiljøer på bensinstasjoner.

## ✨ Features

- ✅ **NO WEB SERVER** - Ingen HTTP overhead, kun forretningslogikk
- ✅ **Automatic Hardware Communication** - Kobler automatisk til dispensere via serial port
- ✅ **Database Persistence** - Lagrer transaksjoner i PostgreSQL
- ✅ **Azure Cloud Sync** - Automatisk synkronisering til sky
- ✅ **Scheduled Tasks** - Polling, watchdog, og synkronisering kjører automatisk
- ✅ **Systemd Compatible** - Klar for Linux service deployment
- ✅ **Docker Ready** - Fungerer perfekt i containers

## 🚀 Quick Start

### Build

```bash
mvn clean package -pl lpg-ehl-app-headless -am
```

### Run

```bash
# LAB Mode (for testing med emulator)
java -jar target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar

# FIELD Mode (produksjon med ekte hardware)
java -jar target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --EHL_EMULATOR_ENABLED=false \
  --EHL_SERIAL_PORT=/dev/ttyUSB0 \
  --DB_HOST=localhost \
  --AZURE_ENABLED=true
```

## 🐳 Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/lpg-ehl-app-headless-*.jar app.jar

# For serial port access
RUN apk add --no-cache udev

ENV DB_HOST=postgres
ENV EHL_EMULATOR_ENABLED=false

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Run Container

```bash
docker run -d \
  --name lpg-ehl-headless \
  --device=/dev/ttyUSB0 \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=secret \
  -e AZURE_CONNECTION_STRING=$AZURE_CS \
  lpg-ehl-headless:latest
```

## 🐧 Systemd Service

Create `/etc/systemd/system/lpg-ehl.service`:

```ini
[Unit]
Description=LPG EHL Headless Service
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-app-headless.jar
Restart=on-failure
RestartSec=10s

Environment="DB_HOST=localhost"
Environment="EHL_EMULATOR_ENABLED=false"
Environment="EHL_SERIAL_PORT=/dev/ttyUSB0"
Environment="AZURE_ENABLED=true"

[Install]
WantedBy=multi-user.target
```

**Enable and start:**

```bash
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl
sudo systemctl status lpg-ehl
```

## ⚙️ Configuration

### Environment Variables

### Database
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | lpg_ehl | Database name |
| `DB_USER` | lpg_user | Database username |
| `DB_PASSWORD` | - | Database password (**required**) |

### Serial Port (RS-485)
| Variable | Default | Description |
|----------|---------|-------------|
| `EHL_EMULATOR_ENABLED` | false | Use emulator (true) or real hardware (false) |
| `EHL_SERIAL_PORT` | /dev/ttyS0 | Serial port device path |
| `EHL_BAUD_RATE` | 9600 | Baud rate (9600, 19200, 115200) |
| `EHL_DATA_BITS` | 8 | Data bits (5, 6, 7, 8) |
| `EHL_PARITY` | EVEN | Parity: NONE, ODD, EVEN, MARK, SPACE |
| `EHL_STOP_BITS` | 1 | Stop bits (1, 2) |

### Azure Sync
| Variable | Default | Description |
|----------|---------|-------------|
| `AZURE_ENABLED` | true | Enable Azure sync |
| `AZURE_CONNECTION_STRING` | - | Azure Storage connection string |

### Logging
| Variable | Default | Description |
|----------|---------|-------------|
| `LOG_FILE` | /var/log/lpg-ehl/headless.log | Log file path |

### Serial Port Device Names

| Platform | Common Device Paths |
|----------|--------------------|
| Linux (onboard) | `/dev/ttyS0`, `/dev/ttyS1` |
| Linux (USB adapter) | `/dev/ttyUSB0`, `/dev/ttyACM0` |
| Raspberry Pi | `/dev/ttyAMA0`, `/dev/serial0` |
| macOS | `/dev/cu.usbserial-*` |
| Windows | `COM1`, `COM3` |

### application.yaml

Default configuration i `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: lpg-ehl-headless

ehl:
  emulator:
    enabled: false  # FIELD mode by default for headless
  serial:
    port: /dev/ttyS0
    baud-rate: 9600
    data-bits: 8
    parity: EVEN  # 8E1 format
    stop-bits: 1

logging:
  file:
    name: /var/log/lpg-ehl/headless.log
```

## 📊 Monitoring

### Logs

```bash
# Follow logs
tail -f /var/log/lpg-ehl/headless.log

# Systemd logs
journalctl -u lpg-ehl -f
```

### Expected Startup Output

```
═══════════════════════════════════════════════════════════
   LPG EHL HEADLESS APPLICATION
   Mode: HEADLESS (No Web Server)
═══════════════════════════════════════════════════════════

🚀 HEADLESS STARTUP SEQUENCE

🔌 Initializing hardware communication...
   Mode: FIELD
   🏭 FIELD MODE: Using real hardware
   ✅ Hardware communication initialized

🐕 Starting hardware watchdog service...
   ✅ Watchdog service active

☁️  Starting Azure sync services...
   ✅ Azure queue reader active
   ✅ Transaction sync active

📊 SYSTEM STATUS:
   • Dispensers: Ready for operation
   • Database: Connected
   • Scheduled tasks: Running
   • Pump #1 State: IDLE
   • Current Price: 15.90 kr/L

═══════════════════════════════════════════════════════════
✅ HEADLESS APPLICATION READY
═══════════════════════════════════════════════════════════

💡 System is now running in background mode
💡 Transactions will be saved to database automatically
💡 Azure sync is active (if configured)
💡 Check logs for dispenser activity

Press Ctrl+C to stop
```

## 🎯 Use Cases

### Production Deployment
Kjør headless-appen på en server ved siden av pumpen. Ingen GUI nødvendig - alt skjer automatisk.

### Raspberry Pi
Perfekt for embedded deployment direkte på bensinstasjonen.

### Docker Swarm / Kubernetes
Skalerbar deployment for multi-stasjon setup.

### Testing
Bruk LAB mode med emulator for integrasjonstesting uten hardware.

## 🔧 Troubleshooting

### Serial Port Permission Denied

```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Or change permissions (not recommended)
sudo chmod 666 /dev/ttyUSB0
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Test connection
psql -h localhost -U lpg_user -d lpg_ehl
```

### No Transactions Being Saved

- Check hardware is connected and powered
- Verify serial port configuration
- Check logs for errors
- Ensure database credentials are correct

## 📚 See Also

- [Main Architecture Documentation](../ARCHITECTURE.md)
- [Web Application README](../lpg-ehl-webapp/README.md)
- [Protocol Documentation](../lpg-ehl-core/README.md)

---

## 🛠️ Feltguide: Testing med ekte pumpe

Denne seksjonen beskriver hvordan du tester headless-applikasjonen i felten med en ekte LPG-pumpe på en ny Linux-installasjon.

### Forutsetninger

1. **Debian/Ubuntu Linux installert** (headless eller med skjerm)
2. **Java 21 installert**
   ```bash
   # Installer Java via SDKMAN (anbefalt)
   curl -s "https://get.sdkman.io" | bash
   source ~/.sdkman/sdkman-init.sh
   sdk install java 21.0.7-tem
   
   # Eller via apt
   sudo apt update && sudo apt install -y openjdk-21-jre-headless
   ```
3. **PostgreSQL kjørende** (lokal eller Docker)
   ```bash
   # Docker (enklest)
   docker run -d --name lpg-postgres \
     -e POSTGRES_USER=lpg_user \
     -e POSTGRES_PASSWORD=lpg_password \
     -e POSTGRES_DB=lpg_ehl \
     -p 5432:5432 postgres:15
   ```
4. **RS-485 adapter tilkoblet** (typisk `/dev/ttyUSB0`)
5. **Bruker i dialout-gruppe**
   ```bash
   sudo usermod -a -G dialout $USER
   # Logg ut og inn igjen for at endringen skal tre i kraft
   ```

### Steg 1: Kopier JAR-filen til maskinen

```bash
# Fra utviklingsmaskin (bygg først med: mvn clean package -pl lpg-ehl-app-headless -am)
scp lpg-ehl-app-headless/target/lpg-ehl-app-headless-*.jar bruker@pump-maskin:/opt/lpg-ehl/

# Eller last ned direkte
wget https://your-artifact-server/lpg-ehl-app-headless.jar -O /opt/lpg-ehl/app.jar
```

### Steg 2: Start headless-applikasjonen

```bash
# Naviger til applikasjonsmappen
cd /opt/lpg-ehl

# Start med ekte hardware (FIELD mode)
java -jar lpg-ehl-app-headless-*.jar \
  --spring.profiles.active=field \
  --EHL_EMULATOR_ENABLED=false \
  --EHL_SERIAL_PORT=/dev/ttyUSB0 \
  --DB_HOST=localhost \
  --DB_USER=lpg_user \
  --DB_PASSWORD=lpg_password
```

**Forventet output:**
```
═══════════════════════════════════════════════════════════
   LPG EHL HEADLESS APPLICATION
   Mode: HEADLESS (No Web Server)
═══════════════════════════════════════════════════════════
✅ Headless application started successfully
📡 Listening for dispenser events...
💾 Database connection active
☁️  Azure sync service running

Press Ctrl+C to stop
```

### Steg 3: Finn serial port

```bash
# List tilgjengelige porter
ls -la /dev/ttyUSB* /dev/ttyACM* /dev/ttyS* 2>/dev/null

# Se dmesg for nylig tilkoblet adapter
dmesg | grep -i tty | tail -10

# Typiske porter:
# - /dev/ttyUSB0  (USB-til-RS485 adapter)
# - /dev/ttyACM0  (Arduino-basert adapter)
# - /dev/ttyS0    (Onboard seriell port)
```

### Steg 4: Test pumpekommandoer med curl

Når applikasjonen kjører, kan du teste pumpen med curl-kommandoer.

> **MERK:** Headless-appen har IKKE web-server. For å teste via curl, må du enten:
> - Bruke `lpg-ehl-webapp` (full app med REST API)
> - Eller bruke CLI-verktøyet `lpg-ehl-cli`

#### Alternativ A: Kjør webapp i stedet for headless

```bash
# Start webapp med REST API
java -jar lpg-ehl-webapp-*.jar \
  --EHL_EMULATOR_ENABLED=false \
  --EHL_SERIAL_PORT=/dev/ttyUSB0
```

Nå kan du teste:

```bash
# 1. Sjekk pumpestatus
curl http://localhost:8080/api/v1/emulator/pump/1/status

# 2. FRI PUMPE - Åpne pumpen (UNBLOCK)
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/unblock

# --> Nå kan kunden fylle drivstoff <--

# 3. STOPP PUMPE - Blokkér pumpen (BLOCK)
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/block

# 4. REGISTRER BETALING - Settle transaksjonen
curl -X POST "http://localhost:8080/api/v1/emulator/settle/1?method=CARD"

# 5. Nullstill pumpe til IDLE
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/reset
```

#### Alternativ B: Bruk CLI-verktøyet

```bash
# Bygg CLI
mvn clean package -pl lpg-ehl-cli -am

# Kjør interaktiv modus
java -jar lpg-ehl-cli/target/lpg-ehl-cli-*.jar --port /dev/ttyUSB0

# CLI kommandoer:
# > status 1         - Vis status for pumpe 1
# > unblock 1        - FRI PUMPE
# > block 1          - STOPP PUMPE
# > volume 1         - Les volum
```

### Steg 5: Komplett testsekvens i felten

Her er en komplett testsekvens for å verifisere at systemet fungerer:

```bash
#!/bin/bash
# felttest.sh - Komplett felttest

API="http://localhost:8080/api/v1"
PUMP=1

echo "🔍 1. Sjekker pumpestatus..."
curl -s $API/emulator/pump/$PUMP/status | jq

echo ""
echo "🔓 2. Frigir pumpe (FRI PUMPE)..."
curl -s -X POST $API/emulator/pump/$PUMP/unblock | jq

echo ""
echo "⛽ Venter på at kunde fyller (10 sekunder)..."
sleep 10

echo ""
echo "🛑 3. Stopper pumpe..."
curl -s -X POST $API/emulator/pump/$PUMP/block | jq

echo ""
echo "💳 4. Registrerer betaling (CARD)..."
curl -s -X POST "$API/emulator/settle/$PUMP?method=CARD" | jq

echo ""
echo "🔄 5. Nullstiller pumpe..."
curl -s -X POST $API/emulator/pump/$PUMP/reset | jq

echo ""
echo "✅ Test fullført!"
```

Lagre som `felttest.sh` og kjør:
```bash
chmod +x felttest.sh
./felttest.sh
```

### Steg 6: Verifiser i databasen

```bash
# Koble til PostgreSQL
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl

# Eller direkte
psql -h localhost -U lpg_user -d lpg_ehl
```

```sql
-- Vis siste transaksjoner
SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 10;

-- Vis pumpestatus
SELECT * FROM dispenser_status;

-- Vis usynkroniserte transaksjoner (venter på Azure)
SELECT * FROM unsynced_transactions;
```

### Feilsøking i felten

#### Serial port "Permission denied"
```bash
sudo usermod -a -G dialout $USER
# Logg ut og inn igjen

# Eller midlertidig:
sudo chmod 666 /dev/ttyUSB0
```

#### Ingen respons fra pumpe
```bash
# Sjekk at kabelen er tilkoblet
ls -la /dev/ttyUSB*

# Test med minicom
sudo apt install minicom
minicom -D /dev/ttyUSB0 -b 9600

# Verifiser RS-485 A/B polaritet (bytt om nødvendig)
```

#### Database connection refused
```bash
# Sjekk at PostgreSQL kjører
docker ps | grep postgres

# Start på nytt
docker start lpg-postgres
```

#### Se logger i sanntid
```bash
# Hvis kjører som systemd service
journalctl -u lpg-ehl -f

# Hvis kjører i forgrunnen
# Logger vises direkte i terminalen

# Loggfil (hvis konfigurert)
tail -f /var/log/lpg-ehl/headless.log
```

### Hurtigsjekkliste for feltinstallasjon

- [ ] Java 21 installert (`java -version`)
- [ ] PostgreSQL kjører (`docker ps` eller `systemctl status postgresql`)
- [ ] RS-485 adapter tilkoblet (`ls /dev/ttyUSB*`)
- [ ] Bruker i dialout-gruppe (`groups` - skal vise `dialout`)
- [ ] JAR-fil kopiert til `/opt/lpg-ehl/`
- [ ] Miljøvariabler konfigurert (DB, serial port)
- [ ] Systemd service aktivert (valgfritt)
- [ ] Test med `curl` vellykket
- [ ] Transaksjon lagret i database

---

## 🤝 Support

For spørsmål eller problemer, kontakt utviklingsteamet.
