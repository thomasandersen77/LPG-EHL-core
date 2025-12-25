# Azure Synkronisering - Frontend Visualisering

Nå kan du se Azure-synkroniseringsstatus direkte i frontend! 🎉

## 📊 Hva ser du?

På **Transaksjoner**-siden vises nå en statusboks øverst som viser:

### 1. **Status-ikon**
- ☁️ **Grønn** = Alt OK, ingen ventende meldinger
- ⏳ **Gul** = Det er meldinger som venter på å synkroniseres
- ⛔ **Rød** = Det er feilede synkroniseringer

### 2. **Statistikk**
- **X venter** - Antall transaksjoner som venter på å sendes til Azure
- **✓ X synket** - Totalt antall vellykket synkede transaksjoner
- **❌ X feilet** - Antall feilede forsøk (maks 3 retries)

### 3. **Siste synk-tidspunkt**
- Viser når siste vellykket synkronisering skjedde (HH:mm:ss)

### 4. **Manuell synk-knapp**
- **🔄 Synk nå** - Trigger manuell synkronisering
- Nyttig for testing eller hvis du vil tvinge synk umiddelbart

## 🚀 Hvordan teste det

### 1. Start systemet
```bash
# Terminal 1: Start database og Azurite
docker-compose -f docker-compose-local.yaml up

# Terminal 2: Start backend
./gradlew :lpg-ehl-api:bootRun

# Terminal 3: Start frontend
cd lpg-web && npm run dev
```

### 2. Opprett en transaksjon
Bruk en av disse metodene:
- **Windows Dispenserkontroll**: Kjør en pumping og stopp
- **Demo-emulator**: Bruk frontend til å starte/stoppe en pumping
- **API direkte**: POST til `/api/v1/transactions`

### 3. Se synkroniseringen live
1. Gå til **Transaksjoner**-siden i frontend
2. Du vil se statusboksen oppdateres automatisk hvert 5. sekund
3. Statusflyten:
   ```
   ⏳ 1 venter → (etter ~30 sek) → ☁️ 1 synket
   ```

### 4. Test manuell synk
- Klikk på **🔄 Synk nå**-knappen
- Systemet vil umiddelbart synkronisere alle ventende meldinger
- Nyttig hvis du ikke vil vente på automatisk synk (hver 30. sekund)

## 🧪 Overvåk Azurite Queue

Du kan også se meldingene i Azurite-køen:

```bash
# Se antall meldinger
./scripts/view-azurite-messages.sh --count

# Se innholdet i køen (uten å fjerne)
./scripts/view-azurite-messages.sh --peek 5

# Overvåk live (oppdateres hvert 2. sekund)
./scripts/view-azurite-messages.sh --watch
```

Se `scripts/AZURITE-MONITORING.md` for flere detaljer.

## 🎯 Hva skjer bak kulissene?

```
1. Transaksjon opprettet (via Windows/Emulator/API)
          ↓
2. Lagret i database med paymentStatus=PENDING
          ↓
3. AzureSyncQueue-entry opprettet (status=PENDING)
          ↓
4. [Hvert 30. sek] AzureSyncService kjører
          ↓
5. Sender melding til Azurite Queue (lpg-transactions)
          ↓
6. Status oppdateres til SYNCED
          ↓
7. Frontend viser ☁️ X synket
```

## 📱 Frontend Teknisk Oversikt

### Nye filer
- **`lpg-web/src/api/sync.ts`** - API-klient for sync-endepunkter
- **`lpg-web/src/components/AzureSyncStatus.tsx`** - Status-komponent

### API-endepunkter
- `GET /api/v1/sync/status` - Henter synk-status
- `POST /api/v1/sync/trigger` - Trigger manuell synk
- `POST /api/v1/sync/retry/{queueId}` - Prøv på nytt for en spesifikk melding

### Oppdateringsfrekvens
- **Auto-refresh**: Hvert 5. sekund
- Bruker React Query med `refetchInterval: 5000`

## 🛠️ Konfigurering

### Backend (application-local.yaml)
```yaml
azure:
  enabled: true  # Må være true for at sync skal fungere
  sync:
    interval-seconds: 30  # Hvor ofte auto-sync kjører
    batch-size: 5         # Antall meldinger per batch
    max-retries: 3        # Maks antall forsøk før FAILED
```

### Frontend
Ingen konfigurasjon nødvendig - komponenten vises automatisk hvis Azure sync er aktivert.

Hvis `azure.enabled=false` i backend, vil komponenten vise:
> ⚠️ Azure-synkronisering er ikke aktivert

## 🐛 Feilsøking

### Statusboks vises ikke
1. Sjekk at backend kjører med `azure.enabled=true`
2. Sjekk backend-logger for:
   ```
   Initializing Azure Queue client for queue: lpg-transactions
   Azure Queue 'lpg-transactions' is ready
   ```

### "X feilet" vises
1. Sjekk backend-logger for `❌` eller `⚠️` emojis
2. Se detaljerte feilmeldinger i `azure_sync_queue`-tabellen:
   ```sql
   SELECT * FROM azure_sync_queue WHERE status = 'FAILED';
   ```
3. Sjekk at Azurite kjører: `docker-compose ps`

### Transaksjoner synker ikke
1. Verifiser at `AzureSyncService` logger kjører hver 30. sekund:
   ```
   DEBUG no.cloudberries.lpg: Starting Azure sync job...
   ```
2. Sjekk at køen er tom: `./scripts/view-azurite-messages.sh --count`
3. Prøv manuell synk via frontend-knappen

## 📚 Relaterte dokumenter

- `scripts/AZURITE-MONITORING.md` - CLI-verktøy for å se køen
- `DEVELOPMENT.md` - Generell utviklerinfo
- `DOCKER-COMPOSE-README.md` - Docker-oppsett
