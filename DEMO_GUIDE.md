# Demo Guide: LPG-EHL Edge → Cloud Integration

Dette dokumentet forklarer hvordan du setter opp og demonstrerer full integrasjon mellom **LPG-EHL** (pump-systemet) og **MinLPG** (cloud admin-systemet).

## 🎯 Arkitektur Oversikt

```
┌─────────────────────────────────┐
│   LPG-EHL (Pump System)         │
│   - Frontend: Port 3001          │
│   - Backend API: Port 8080       │
│   - PostgreSQL: Port 5432        │
│   - Azurite Queue: Port 10001    │
└────────────┬────────────────────┘
             │ Azure Storage Queue
             ↓
┌─────────────────────────────────┐
│   MinLPG (Cloud Admin)          │
│   - Frontend: Port 3000          │
│   - Backend API: Port 8081       │
│   - PostgreSQL: Port 5433        │
└─────────────────────────────────┘
```

## 📋 Forutsetninger

- Java 21 (via SDKMAN)
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

## 🚀 Steg-for-steg Setup

### 1. Start LPG-EHL (Pump System)

```bash
cd ~/git/NorgesGass/lpg-ehl

# Start database og Azurite
docker-compose -f docker-compose-local.yaml up -d postgres azurite

# Start backend API (lokal utvikling)
cd lpg-ehl-api
mvn spring-boot:run -Dspring-boot.run.profiles=local

# I ny terminal: Start frontend
cd ~/git/NorgesGass/lpg-ehl/lpg-web
npm install
npm run dev
```

**Sjekk at alt er oppe:**
- Frontend: http://localhost:3001
- API: http://localhost:8080/actuator/health
- Database: `psql -h localhost -p 5432 -U lpg_user -d lpg_ehl`
- Azurite: http://localhost:10001

### 2. Start MinLPG (Cloud Admin)

```bash
cd ~/git/NorgesGass/MinLPG

# Start database
docker-compose up -d minlpg-db

# Start backend API (lokal utvikling)
cd backend/backend-api
mvn spring-boot:run -Dspring-boot.run.profiles=local

# I ny terminal: Start frontend
cd ~/git/NorgesGass/MinLPG/frontend
npm install
npm run dev
```

**Sjekk at alt er oppe:**
- Frontend: http://localhost:3000
- API: http://localhost:8081/api/transactions
- Database: `psql -h localhost -p 5433 -U minlpg_user -d minlpg`

### 3. Verifiser Azurite Connection

```bash
# Sjekk at MinLPG kan koble til Azurite
curl http://localhost:10001/devstoreaccount1/lpg-transactions?comp=metadata

# Se logger i MinLPG backend - skal se "Connecting to Azure Queue"
```

## 🎬 Demo-scenario

### Scenario 1: Enkel Transaksjon

1. **På LPG-EHL frontend (http://localhost:3001):**
   - Simuler en pumping/transaksjon
   - Se at transaksjon lagres lokalt

2. **Vent 5-30 sekunder** (Azure sync interval)

3. **På MinLPG frontend (http://localhost:3000):**
   - Logg inn som "STATION_OWNER"
   - Se at transaksjonen dukker opp i listen med status `PENDING`
   - Trykk på **💳 Betal** knappen
   - Status endres til `PAID`

### Scenario 2: Batch Processing

```bash
# Generer flere transaksjoner raskt
cd ~/git/NorgesGass/lpg-ehl
./scripts/simulate-traffic.sh 10  # 1 transaksjon hvert 10. sekund
```

Se at:
- Transaksjoner sendes til Azurite
- MinLPG konsumerer dem i batch (hvert 5. sekund)
- Du kan betale flere på rad i MinLPG

## 🌍 Ngrok Setup (Dual Demo)

For å vise begge GUI-ene via internett samtidig:

### 1. Start begge systemene lokalt (som over)

### 2. Start to ngrok tunnels

**Terminal 1 - LPG-EHL Frontend:**
```bash
ngrok http 3001
# Noter URL, f.eks: https://abc123.ngrok.io
```

**Terminal 2 - MinLPG Frontend:**
```bash
ngrok http 3000
# Noter URL, f.eks: https://xyz456.ngrok.io
```

### 3. Del linkene med Tobias

```
🔵 Pump System (LPG-EHL): https://abc123.ngrok.io
🟢 Cloud Admin (MinLPG):  https://xyz456.ngrok.io
```

**Demo-flow:**
1. Vis pump-systemet: Simuler pumping
2. Bytt til cloud admin: Se transaksjonen dukke opp
3. Klikk "Betal" → Status endres til PAID
4. Bytt tilbake til pump: Se at status er oppdatert

## 🛠️ Feilsøking

### Problem: MinLPG ser ingen transaksjoner

**Sjekk:**
```bash
# 1. Er Azurite oppe?
docker ps | grep azurite

# 2. Har LPG-EHL sendt noe?
# Se logger i lpg-ehl-api konsollen - søk etter "Queued transaction"

# 3. Kan MinLPG koble til Azurite?
# Se logger i MinLPG backend - søk etter "Received message"

# 4. Er meldingene i køen?
curl http://localhost:10001/devstoreaccount1/lpg-transactions/messages?numofmessages=10
```

### Problem: CORS errors

**Sjekk:**
- LPG-EHL backend: `allowed-origins` inkluderer `http://localhost:3001`
- MinLPG backend: `@CrossOrigin` inkluderer `http://localhost:3000`

### Problem: Betaling virker ikke

**Sjekk:**
```bash
# Test betaling manuelt
curl -X POST http://localhost:8081/api/transactions/{TRANSACTION_ID}/pay \
  -H "Content-Type: application/json" \
  -d '{"paymentType": "CARD"}'
```

## 📊 Monitoring

### LPG-EHL Metrics
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

### MinLPG Logs
```bash
# Backend logs
tail -f backend/backend-api/logs/application.log

# Se Azure Queue Consumer activity
grep "Received message" backend/backend-api/logs/application.log
```

### Azurite Queue Browser

```bash
# Se alle meldinger i køen (uten å fjerne dem)
./scripts/view-azurite-messages.sh

# Se meldinger i sanntid
./scripts/view-azurite-messages.sh --watch
```

## 🎯 Demo Tips for Tobias

1. **Start clean**: Restart begge databaser for fresh demo
   ```bash
   docker-compose -f docker-compose-local.yaml down -v
   docker-compose -f MinLPG/docker-compose.yml down -v
   ```

2. **Forklaring**: 
   - "LPG-EHL kjører på pumpen (edge)"
   - "MinLPG er skyløsningen (cloud admin)"
   - "De snakker sammen via Azure Storage Queue"
   - "Pumpen fungerer offline - sender når nett er tilgjengelig"

3. **Vis resilience**: 
   - Stopp MinLPG backend
   - Lag flere transaksjoner i LPG-EHL
   - Start MinLPG backend igjen
   - Se at alle transaksjoner konsumeres

4. **Multi-station (fremtidig)**: 
   - "Hvert pump-system har sin egen edge ID"
   - "Cloud kan administrere mange stasjoner"

## 📞 Support

- LPG-EHL Dokumentasjon: `~/git/NorgesGass/lpg-ehl/README.md`
- MinLPG Dokumentasjon: `~/git/NorgesGass/MinLPG/WARP.md`
- WARP AI Support: Åpne Warp terminal og spør!
