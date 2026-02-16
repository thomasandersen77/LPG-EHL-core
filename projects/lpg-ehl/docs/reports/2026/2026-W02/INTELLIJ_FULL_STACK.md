# IntelliJ Full Stack Setup

## 🎯 Mål
Start hele LPG-EHL systemet med **én knapp** i IntelliJ.

## 📦 Hva starter?

### 1. Docker Services (Automatisk)
- **PostgreSQL** (port 5432) - Database
- **Azurite** (port 10000-10002) - Azure Storage Emulator

### 2. Spring Boot Applications (IntelliJ)
- **LPG-EHL API** (port 8080)
  - REST API for transaksjoner
  - Frontend GUI (React)
  - Swagger UI: http://localhost:8080/swagger-ui.html
  
- **LPG-EHL Emulator** (port 8090, 9000)
  - Port 8090: REST API for emulator-kontroll
  - Port 9000: TCP server for Windows Dispenserkontroll
  - Mottar EHL-protokoll fra Windows

## 🚀 Oppstart

### Steg 1: Docker
Docker-tjenestene startes automatisk når du bruker compound run config.

Hvis du vil starte dem manuelt først:
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
docker-compose -f docker-compose.postgres.yaml up -d
```

### Steg 2: IntelliJ Run Configuration

I IntelliJ, velg run configuration:

**"Full Stack (API + Emulator)"**

Trykk på ▶️ Play-knappen.

Dette starter:
1. LpgEhlApiApplication (port 8080)
2. LpgEhlEmulatorApplication (port 9000)

## 🌐 URLs etter oppstart

| Service | URL | Beskrivelse |
|---------|-----|-------------|
| **Frontend GUI** | http://localhost:8080 | React-basert web-GUI |
| **API Swagger** | http://localhost:8080/swagger-ui.html | REST API dokumentasjon |
| **Emulator API** | http://localhost:8090 | Emulator kontroll-API |
| **Windows Connection** | 192.168.0.41:9000 | TCP server for Dispenserkontroll |

## 🔄 Full Testflyt

### 1. Start Windows Dispenserkontroll
- Koble til `192.168.0.41:9000`
- Trykk "Frig dispenser"
- Simuler pumping
- Trykk "Stopp dispenser"

### 2. Se transaksjon i GUI
- Åpne http://localhost:8080
- Se transaksjonen i listen
- Status: PAYMENT_PENDING

### 3. Kvitter ut transaksjon
- Klikk på transaksjonen
- Velg betalingsmåte (CASH, CARD, CREDIT)
- Trykk "Betal" / "Settle"
- Status endres til PAID

### 4. Start ny pumping
- Gå tilbake til Windows
- Trykk "Frig dispenser" igjen
- Nå tillates ny transaksjon ✅

## 🛠️ Debugging

### Se logger i IntelliJ
Begge applikasjonene logger til IntelliJ Console.

Nyttige logger:
```
📱 NEW CLIENT CONNECTION     # Windows koblet til
⛽ Update #1: 0.50 L         # Pumping pågår
🧊 Transaction frozen        # Transaksjon lagret
💾 Saving transaction        # Sendt til API
✅ Transaction saved         # Lagret i database
```

### Stopp og restart
- Trykk ⏹️ Stop i IntelliJ (stopper begge applikasjoner)
- Trykk ▶️ Play igjen for å restarte

### Enkeltmodulkjøring
Hvis du vil kjøre kun én modul:
- **LpgEhlApiApplication** - Kun API + GUI
- **LpgEhlEmulatorApplication** - Kun Emulator

## 🐛 Troubleshooting

### Port 8080 allerede i bruk
```bash
lsof -ti:8080 | xargs kill -9
```

### Port 9000 allerede i bruk
```bash
lsof -ti:9000 | xargs kill -9
```

### PostgreSQL ikke tilgjengelig
```bash
docker-compose -f docker-compose.postgres.yaml restart
```

### Frontend ikke synlig
Rebuild frontend og kopier til API:
```bash
cd lpg-web
npm run build
cd ../lpg-ehl-api
mvn process-resources
```

## 📊 Database Access

**DBeaver / DataGrip:**
```
Host: localhost
Port: 5432
Database: lpg
Username: lpg_user
Password: lpg_pass123
```

**SQL Query:**
```sql
SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 10;
```

## ✅ Success Indicators

Når alt fungerer ser du:
1. ✅ IntelliJ console viser begge applikasjoner startet
2. ✅ Windows kobler til port 9000
3. ✅ GUI viser transaksjoner på http://localhost:8080
4. ✅ Transaksjoner lagres i PostgreSQL
5. ✅ Settlement fungerer fra GUI

## 🎓 Architecture

```
┌─────────────────────┐
│   Windows           │
│   Dispenserkontroll │ (C#, Parallels)
└──────────┬──────────┘
           │ EHL Protocol (TCP 9000)
           ▼
┌─────────────────────┐
│   LPG-EHL           │
│   Emulator          │ (Kotlin, Spring Boot)
│   Port 9000, 8090   │
└──────────┬──────────┘
           │ HTTP POST /api/v1/transactions
           ▼
┌─────────────────────┐
│   LPG-EHL API       │
│   Port 8080         │ (Kotlin, Spring Boot)
│   + React Frontend  │
└──────────┬──────────┘
           │ PostgreSQL
           ▼
┌─────────────────────┐
│   PostgreSQL        │
│   Port 5432         │ (Docker)
└─────────────────────┘
```

## 🎉 Ferdig!

Du kan nå kjøre hele systemet med **én knapp** i IntelliJ! 🚀
