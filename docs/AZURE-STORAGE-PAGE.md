# Azure Storage Visualisering 

En komplett løsning for å visualisere transaksjoner lagret i Azure Storage Queue med persistent data.

## 🎉 Hva har blitt implementert?

### 1. **Persistent Data i Azurite**
- Azure Storage Queue (Azurite) har nå persistent lagring via Docker volume
- Data forsvinner IKKE når containeren restartes
- Volume: `azurite-data` montert til `/data` i containeren

### 2. **Dobbel Lagring**
Alle transaksjoner lagres nå på **to steder**:
- ✅ **PostgreSQL** - Primær database for applikasjonen
- ✅ **Azure Storage Queue (Azurite)** - Kø for skysynkronisering

### 3. **Backend API**
Nye endepunkter:
- `GET /api/v1/sync/queue/messages` - Hent meldinger fra køen
- `GET /api/v1/sync/queue/by-date` - Hent meldinger gruppert på dato

### 4. **Frontend Side - Azure Storage**
Ny side tilgjengelig på `/azure-storage` med:
- 📊 **Dashboard** med statistikk (totalt, per dag, unike dager)
- 📅 **Gruppering per dato** - Transaksjoner organisert på dager
- 📂 **Ekspander/kollaps** - Klikk på en dag for å se detaljer
- 🔄 **Live oppdatering** - Auto-refresh hvert 10. sekund
- 🎨 **Brukervennlig GUI** - Farger, ikoner og responsivt design

### 5. **Navigasjon**
- ☁️ Azure Storage-kortet på hovedsiden er nå klikkbart
- Nytt navigasjonskort i "Systemmoduler"-seksjonen
- Direkte lenke til Azure Storage-siden

## 🚀 Kom i gang

### 1. Start systemet
```bash
# Terminal 1: Start alle tjenester (inkl. Azurite med persistent data)
cd /Users/tandersen/git/NorgesGass/lpg-ehl
docker-compose -f docker-compose-local.yaml up

# Terminal 2: Start backend (hvis ikke Docker-versjon)
./gradlew :lpg-ehl-api:bootRun

# Terminal 3: Start frontend
cd lpg-web && npm run dev
```

### 2. Åpne Azure Storage-siden
Gå til: http://localhost:5173/azure-storage

Eller klikk på:
- ☁️ Azure Storage-kortet på hovedsiden
- Azure Storage i navigasjonsmenyen

### 3. Se transaksjoner
1. Opprett transaksjoner via Windows Dispenserkontroll eller emulator
2. Transaksjoner vil automatisk synkroniseres til Azure Storage Queue
3. Se dem visualisert på Azure Storage-siden

## 📊 Azure Storage Side - Funksjoner

### Statistikk Dashboard
```
┌─────────────────────────────────────────────┐
│  📊 Azure Storage Queue                     │
├─────────────────────────────────────────────┤
│  ╔═══════════╗  ╔═══════════╗  ╔═══════════╗│
│  ║    42     ║  ║     7     ║  ║    12     ║│
│  ║  Totalt   ║  ║ Unike dager║  ║  I dag   ║│
│  ╚═══════════╝  ╚═══════════╝  ╚═══════════╝│
└─────────────────────────────────────────────┘
```

### Transaksjoner gruppert per dato
```
📁 Fredag 24. desember 2024              12 meldinger ▶
   (Klikk for å ekspandere)

📂 Fredag 24. desember 2024              12 meldinger ▼
   ┌──────────────────────────────────────────────┐
   │ SYNCED  15:32:10  TRANSACTION • ID: 123abc...│
   │ ├─ Pumpe: #1                                 │
   │ ├─ Volum: 25.50 L                            │
   │ ├─ Beløp: 471.75 kr                          │
   │ └─ Betaling: CARD                            │
   └──────────────────────────────────────────────┘
   
   ┌──────────────────────────────────────────────┐
   │ PENDING  15:30:05  TRANSACTION • ID: 456def...│
   │ ├─ Pumpe: #2                                 │
   │ ├─ Volum: 30.00 L                            │
   │ ├─ Beløp: 555.00 kr                          │
   │ └─ Betaling: CREDIT                          │
   │ ⚠️ Retry count: 1                            │
   └──────────────────────────────────────────────┘
```

### Statusfarger
- 🟢 **SYNCED** - Vellykket synkronisert (grønn)
- 🟡 **PENDING** - Venter på synkronisering (gul)
- 🔵 **IN_PROGRESS** - Pågående (blå)
- 🔴 **FAILED** - Feilet (rød)

## 🛠️ Teknisk Arkitektur

### Dataflyt
```
1. Transaksjon opprettet (Windows/Emulator/API)
          ↓
2. Lagres i PostgreSQL (primær)
          ↓
3. AzureSyncQueue-entry opprettet (status=PENDING)
          ↓
4. AzureSyncService kjører (hvert 30. sek)
          ↓
5. Sender melding til Azurite Queue
          ↓
6. Status oppdateres til SYNCED
          ↓
7. Melding blir i køen (persistent i volume)
          ↓
8. Frontend leser fra køen via API (peek, ikke remove)
          ↓
9. Vises på Azure Storage-siden
```

### Backend-komponenter

#### Nye filer
- **`AzureQueueReaderService.kt`** - Service for å lese meldinger fra Azure Queue
  - `peekMessages()` - Hent meldinger uten å fjerne dem
  - `getMessagesByDate()` - Grupper meldinger på dato
  
- **`SyncController.kt`** (utvidet)
  - `GET /api/v1/sync/queue/messages` - Hent meldinger
  - `GET /api/v1/sync/queue/by-date` - Hent meldinger gruppert

- **`ApiResponses.kt`** (utvidet)
  - `AzureQueueMessageDto` - DTO for queue-meldinger
  - `AzureQueueByDateResponse` - Response med dato-gruppering

### Frontend-komponenter

#### Nye filer
- **`pages/AzureStoragePage.tsx`** - Hovedside for Azure Storage
  - Dashboard med statistikk
  - Dato-gruppering med ekspander/kollaps
  - Live oppdatering (10 sek interval)
  
- **`api/sync.ts`** (utvidet)
  - `fetchQueueMessages()` - Hent meldinger
  - `fetchQueueMessagesByDate()` - Hent meldinger gruppert

#### Oppdaterte filer
- **`App.tsx`** - Route til `/azure-storage`
- **`HomePage.tsx`** - Klikkbart kort og navigasjonslenke

### Docker Compose-endringer

```yaml
azurite:
  image: mcr.microsoft.com/azure-storage/azurite
  command: "azurite-queue --queueHost 0.0.0.0 --queuePort 10001 --loose --location /data"
  volumes:
    - azurite-data:/data  # ← PERSISTENT STORAGE

volumes:
  postgres-data:
    driver: local
  azurite-data:  # ← NYE VOLUME
    driver: local
```

## 🧪 Testing

### Test persistent data
```bash
# 1. Start systemet
docker-compose -f docker-compose-local.yaml up

# 2. Opprett noen transaksjoner

# 3. Se dem i Azure Storage-siden
# http://localhost:5173/azure-storage

# 4. Stopp containerne
docker-compose -f docker-compose-local.yaml down

# 5. Start på nytt
docker-compose -f docker-compose-local.yaml up

# 6. Gå til Azure Storage-siden igjen
# ✅ Data er fortsatt der!
```

### Test dobbel lagring
```bash
# 1. Opprett en transaksjon via API eller emulator

# 2. Sjekk PostgreSQL
psql -U lpg_user -d lpg_ehl
SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1;

# 3. Sjekk Azurite Queue via CLI
./scripts/view-azurite-messages.sh --peek 1

# 4. Sjekk Azure Storage-siden
# http://localhost:5173/azure-storage

# ✅ Transaksjonen skal være synlig i alle tre!
```

## 📋 API Dokumentasjon

### GET /api/v1/sync/queue/messages
Hent meldinger fra Azure Queue uten å fjerne dem.

**Query Parameters:**
- `maxMessages` (optional, default=32, max=1000)

**Response:**
```json
[
  {
    "messageId": "abc123...",
    "insertionTime": "2024-12-24T15:32:10",
    "expirationTime": "2024-12-31T15:32:10",
    "dequeueCount": 0,
    "entityType": "TRANSACTION",
    "entityId": "uuid-123",
    "status": "SYNCED",
    "retryCount": 0,
    "transaction": {
      "dispenserAddress": 1,
      "volumeLiters": 25.5,
      "amountKr": 471.75,
      "pricePerLiter": 18.50,
      "paymentType": "CARD",
      "paymentStatus": "PAID",
      "timestamp": "2024-12-24T15:30:00"
    }
  }
]
```

### GET /api/v1/sync/queue/by-date
Hent meldinger gruppert på dato.

**Response:**
```json
{
  "dates": {
    "2024-12-24": [
      { /* message 1 */ },
      { /* message 2 */ }
    ],
    "2024-12-23": [
      { /* message 3 */ }
    ]
  },
  "totalMessages": 3
}
```

## 🎨 Frontend Features

### Auto-refresh
Siden oppdateres automatisk hvert 10. sekund for å vise nye meldinger.

### Responsive Design
- Desktop: 3-kolonner statistikk
- Mobile: 1-kolonne statistikk
- Alle enheter: Touch-vennlig ekspander/kollaps

### Farger og status
Hver melding får farge basert på status:
- **Grønn** = SYNCED (vellykket)
- **Gul** = PENDING (venter)
- **Blå** = IN_PROGRESS (pågående)
- **Rød** = FAILED (feilet)

### Knapper
- **Ekspander alle** - Åpne alle dato-grupper
- **Kollaps alle** - Lukk alle dato-grupper

## 🔧 Konfigurasjon

### Backend (application-local.yaml)
```yaml
azure:
  enabled: true  # MÅ være true
  storage:
    connection-string: "..."
    queue-name: lpg-transactions
  sync:
    interval-seconds: 30  # Synk-intervall
```

### Docker Compose
```bash
# Se alle volumes
docker volume ls | grep lpg

# Inspiser Azurite volume
docker volume inspect lpg-ehl_azurite-data

# Fjern volume (SLETTER DATA!)
docker volume rm lpg-ehl_azurite-data
```

## 🐛 Feilsøking

### Ingen meldinger vises
1. Sjekk at Azurite kjører: `docker-compose ps`
2. Sjekk at backend kjører med `azure.enabled=true`
3. Opprett en transaksjon via emulator
4. Vent 30 sekunder (sync-intervall)
5. Refresh Azure Storage-siden

### Data forsvant etter restart
- Sjekk at volume er konfigurert i docker-compose-local.yaml
- Verifiser: `docker volume ls | grep azurite`
- Hvis missing: restart med `docker-compose up`

### "Azure Storage er ikke tilgjengelig"
- Backend kjører ikke eller `azure.enabled=false`
- Sjekk backend-logger for "Initializing Azure Queue client"
- Verifiser at Azurite kjører på port 10001

## 📚 Relaterte dokumenter

- `AZURE-SYNC-VISUALIZATION.md` - Frontend synk-status visualisering
- `scripts/AZURITE-MONITORING.md` - CLI-verktøy for å inspisere køen
- `DEVELOPMENT.md` - Generell utviklerinformasjon
- `DOCKER-COMPOSE-README.md` - Docker Compose-oppsett

## 🎯 Bruksscenarier

### Scenario 1: Verifiser dobbel lagring
Se at transaksjoner lagres både i Postgres og Azure Storage.

### Scenario 2: Debug synk-problemer
Se hvilke transaksjoner som venter på synkronisering og hvorfor.

### Scenario 3: Historisk visning
Se alle transaksjoner som har vært synkronisert til skyen, gruppert på dato.

### Scenario 4: Testing av persistent data
Restart systemet og verifiser at data ikke forsvinner.

## ✨ Kommende forbedringer

Potensielle fremtidige utvidelser:
- 🔍 Søk og filtrering av meldinger
- 📥 Last ned meldinger som CSV/JSON
- 🗑️ Slett gamle meldinger (cleanup)
- 📈 Grafer og statistikk over tid
- 🔔 Varsler ved feilede synkroniseringer
