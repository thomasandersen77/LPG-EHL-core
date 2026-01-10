# 🚀 LPG-EHL Edge System - Deployment Guide

## Oversikt

Dette dokumentet beskriver hvordan du deployer LPG-EHL Edge-systemet til en produksjonsmaskin (Debian/Linux).

## Systemkrav

- **OS:** Debian 11+ eller Ubuntu 20.04+
- **Java:** OpenJDK 11 eller nyere (Java 21 anbefalt for x64)
- **RAM:** Minimum 512 MB
- **Disk:** Minimum 100 MB
- **Nettverk:** Port 9001 (web/API), Port 9000 (EHL-protokoll)

## Quick Start

```bash
# 1. Kopier JAR-filen
sudo mkdir -p /opt/lpg-ehl
sudo cp lpg-ehl-edge.jar /opt/lpg-ehl/

# 2. Opprett bruker
sudo useradd -r -s /bin/false lpg

# 3. Sett rettigheter
sudo chown -R lpg:lpg /opt/lpg-ehl

# 4. Installer systemd unit
sudo cp systemd/lpg-ehl.service /etc/systemd/system/
sudo systemctl daemon-reload

# 5. Start tjenesten
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl

# 6. Sjekk status
sudo systemctl status lpg-ehl
```

## Tilgang

Etter oppstart er systemet tilgjengelig på:

- **Web GUI:** `http://<ip>:9001/`
- **Kontrollpanel:** `http://<ip>:9001/control`
- **API:** `http://<ip>:9001/api/v1/emulator/...`
- **WebSocket Logger:** `ws://<ip>:9001/ws/logs`
- **EHL-protokoll:** TCP port 9000

## Logger

```bash
# Se logger i sanntid
journalctl -u lpg-ehl -f

# Se siste 100 linjer
journalctl -u lpg-ehl -n 100

# Filtrer etter nivå
journalctl -u lpg-ehl -p err
```

## Fri Pumpe - Felt-testing

Når du er ute i felt og trenger å teste pumpen uten PLS/terminal:

1. Åpne `http://<ip>:9001/control` i nettleser (mobil eller PC)
2. Trykk **🔓 FRI PUMPE** for å starte levering
3. Trykk **🛑 STOPP** for å avslutte
4. Trykk **💳 SIMULER BETALING** for å nullstille

Logger vises i sanntid i høyre panel.

## Konfigurasjon

Systemet bruker minimal konfigurasjon via miljøvariabler:

| Variabel | Standard | Beskrivelse |
|----------|----------|-------------|
| `PORT` | 9001 | HTTP-port for web/API |
| `EMULATOR_PORT` | 9000 | TCP-port for EHL-protokoll |
| `EMULATOR_PRICE_PER_LITRE_CENTS` | 1590 | Pris i øre (15.90 kr) |
| `SPRING_PROFILES_ACTIVE` | - | Sett til `production` |

For å endre, rediger `/etc/systemd/system/lpg-ehl.service`:

```ini
Environment=EMULATOR_PRICE_PER_LITRE_CENTS=1690
```

Deretter:
```bash
sudo systemctl daemon-reload
sudo systemctl restart lpg-ehl
```

## Feilsøking

### Tjenesten starter ikke

```bash
# Sjekk logger
journalctl -u lpg-ehl -n 50

# Kjør manuelt for debugging
sudo -u lpg java -jar /opt/lpg-ehl/lpg-ehl-edge.jar
```

### Port allerede i bruk

```bash
# Finn prosess
sudo lsof -i :9001
sudo lsof -i :9000

# Drep prosess
sudo kill -9 <PID>
```

### Java ikke funnet

```bash
# Installer Java 11
sudo apt-get update
sudo apt-get install openjdk-11-jre-headless

# Verifiser
java -version
```

## Bygging fra kilde

```bash
# Klon repository
git clone https://github.com/cloudberries/lpg-ehl.git
cd lpg-ehl

# Bygg Fat JAR med frontend
./scripts/build-fat-jar.sh

# Resultat: lpg-ehl-emulator/target/lpg-ehl-emulator-*.jar
```

## Arkitektur

```
┌─────────────────────────────────────────────────────────────────┐
│                     lpg-ehl-edge.jar                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   React     │  │    API      │  │   Emulator/Protokoll    │ │
│  │  (static)   │  │ Controllers │  │   EhlDispenserEmulator  │ │
│  │  :9001/     │  │ :9001/api/  │  │   TCP :9000             │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
│                          │                    │                 │
│                    WebSocket                  │                 │
│                    :9001/ws/logs              │                 │
│                          │                    │                 │
│                    ┌─────┴────────────────────┴─────┐           │
│                    │      PostgreSQL (lokal)        │           │
│                    └────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## Kontakt

Ved spørsmål, kontakt Cloudberries AS.
