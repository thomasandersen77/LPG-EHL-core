# Azure Transaction Sync Guide

## Oversikt

Dette systemet synkroniserer transaksjoner fra LPG-pumpene (emulator/core) til Azure Storage Queue, som deretter kan leses av MinLPG admin-systemet.

## Arkitektur

```
┌─────────────┐
│  Emulator   │──┐
│   (Port     │  │
│   9000)     │  │  POST /api/v1/transactions
└─────────────┘  │
                 │
┌─────────────┐  │
│  Core API   │  │
│   (Kotlin)  │  │
└─────────────┘  │
                 ↓
┌──────────────────────────────────┐
│  LPG-EHL API (Port 8080)         │
│  ┌────────────────────────────┐  │
│  │ TransactionService         │  │
│  │  - saveTransaction()       │  │
│  │  - updatePaymentStatus()   │  │
│  └────────────┬───────────────┘  │
│               │                  │
│               ↓                  │
│  ┌────────────────────────────┐  │
│  │ TransactionSyncService     │  │
│  │  - queueTransactionForSync │  │
│  └────────────┬───────────────┘  │
│               │                  │
│               ↓                  │
│  ┌────────────────────────────┐  │
│  │ PostgreSQL Database        │  │
│  │  - transactions (tabell)   │  │
│  │  - azure_sync_queue        │  │
│  └────────────┬───────────────┘  │
│               │                  │
│               ↓                  │
│  ┌────────────────────────────┐  │
│  │ AzureSyncService           │  │
│  │  - Scheduled task (30s)    │  │
│  │  - syncPendingItems()      │  │
│  └────────────┬───────────────┘  │
└───────────────┼──────────────────┘
                │
                ↓
       ┌────────────────┐
       │  Azurite Queue │  (localhost:10001)
       │  lpg-          │
       │  transactions  │
       └────────┬───────┘
                │
                │  Azure Storage Queue Protocol
                │
                ↓
       ┌────────────────┐
       │  MinLPG Admin  │  (Fremtidig system)
       │  System        │
       │  - Static Web  │
       │  - Backend API │
       │  - Database    │
       └────────────────┘
```

## Hvordan det fungerer

### 1. Transaksjon opprettes (PENDING)

Når en pump (emulator eller fysisk) leverer drivstoff og stopper:

1. **Emulator** sender transaksjon til API:
   ```http
   POST /api/v1/transactions
   {
     "dispenserAddress": 1,
     "volumeDeciliters": 500,
     "amountOre": 7950,
     "pricePerLiter": 1590,
     "paymentStatus": "PENDING"
   }
   ```

2. **TransactionService** lagrer i database og kaller `TransactionSyncService`:
   ```kotlin
   val saved = transactionRepository.save(transaction)
   transactionSyncService?.queueTransactionForSync(saved, "CREATED")
   ```

3. **TransactionSyncService** legger transaksjonen i `azure_sync_queue`:
   ```sql
   INSERT INTO azure_sync_queue (
     entity_type, entity_id, payload, status
   ) VALUES (
     'TRANSACTION', 'uuid-123', { ... }, 'PENDING'
   )
   ```

4. **AzureSyncService** kjører scheduled task (hver 30. sekund) og sender til Azurite:
   ```kotlin
   queueClient.sendMessage(BinaryData.fromString(messageBody))
   ```

### 2. Betaling gjøres (PAID)

Når kunden betaler for transaksjonen:

1. **Frontend/Betalingssystem** kaller API:
   ```http
   PATCH /api/v1/transactions/{id}/payment?paymentMethod=CARD&paymentStatus=PAID
   ```

2. **TransactionService** oppdaterer transaksjonen og synker på nytt:
   ```kotlin
   transaction.paymentStatus = "PAID"
   transaction.paymentType = "CARD"
   val saved = transactionRepository.save(transaction)
   transactionSyncService?.queueTransactionForSync(saved, "PAYMENT_UPDATED")
   ```

3. **Ny melding** legges i `azure_sync_queue` med `eventType: "PAYMENT_UPDATED"`

4. **AzureSyncService** sender oppdatert transaksjon til Azurite

## Konfigurasjon

### IntelliJ (lokal utvikling)

Default profil er nå `local`, som bruker `localhost`:

```yaml
# application.yaml (default for IntelliJ)
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

azure:
  enabled: true
  storage:
    connection-string: UseDevelopmentStorage=true;DevelopmentStorageProxyUri=http://localhost:10001
    queue-name: lpg-transactions
  sync:
    interval-seconds: 30  # Synker hver 30. sekund
```

### Docker Compose

Når du kjører med Docker Compose, brukes `azurite` hostname:

```yaml
# docker-compose-local.yaml
environment:
  AZURE_CONNECTION_STRING: "UseDevelopmentStorage=true;DevelopmentStorageProxyUri=http://azurite:10001"
```

## Kjøre lokalt

### 1. Start Azurite

```bash
docker run -p 10001:10001 mcr.microsoft.com/azure-storage/azurite \
  azurite-queue --queueHost 0.0.0.0 --queuePort 10001
```

Eller bruk Docker Compose:
```bash
docker-compose -f docker-compose-local.yaml up azurite
```

### 2. Start PostgreSQL

```bash
docker-compose -f docker-compose-local.yaml up postgres
```

### 3. Start API fra IntelliJ

- Ingen profil behøver å settes (bruker automatisk `local`)
- Azure sync starter automatisk hver 30. sekund

### 4. Start Emulator fra IntelliJ

Emulatoren vil automatisk sende transaksjoner til API-et.

### 5. Overvåk Azurite-køen

```bash
./scripts/view-azurite-messages.sh
```

Du skal nå se meldinger i køen!

## Database-tabeller

### `transactions`
Hovedtabellen for transaksjoner:
- `transaction_id` (UUID, PK)
- `dispenser_address`, `nozzle_number`
- `volume_deciliters`, `amount_ore`
- `payment_type`, `payment_status`
- `timestamp`, `created_at`

### `azure_sync_queue`
Kø for Azure-synkronisering:
- `queue_id` (UUID, PK)
- `entity_type` (f.eks. "TRANSACTION")
- `entity_id` (UUID til transaksjon)
- `payload` (JSONB med transaksjonsdata)
- `status` (PENDING, IN_PROGRESS, SYNCED, FAILED)
- `retry_count`, `last_error`
- `created_at`, `synced_at`

## Payload-struktur

Meldinger sendt til Azure Queue har følgende format:

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "dispenserAddress": 1,
  "nozzleNumber": 1,
  "volumeLiters": "50.0",
  "amountKr": "79.50",
  "pricePerLiter": "15.90",
  "paymentType": "CARD",
  "paymentStatus": "PAID",
  "customerId": "",
  "customerName": "",
  "productCode": "LPG",
  "includesRoadTax": true,
  "timestamp": "2025-12-24T23:00:00",
  "eventType": "PAYMENT_UPDATED"
}
```

## MinLPG Admin-systemet

MinLPG er et separat repository (`/Users/tandersen/git/NorgesGass/MinLPG`) som skal:

1. **Lese fra Azure Storage Queue** (`lpg-transactions`)
2. **Vise transaksjoner** i admin-grensesnittet
3. **Håndtere rapportering** og analyse

### Fremtidig arkitektur (MinLPG)

```
┌──────────────────────────┐
│  Azure Storage Queue     │
│  (prod: ekte Azure)      │
│  (local: Azurite)        │
└────────────┬─────────────┘
             │
             ↓
┌──────────────────────────┐
│  MinLPG Backend          │
│  - Kotlin/Spring Boot    │
│  - Azure Queue Consumer  │
│  - PostgreSQL/MySQL      │
└────────────┬─────────────┘
             │
             ↓
┌──────────────────────────┐
│  MinLPG Frontend         │
│  - Static Web App        │
│  - React/Vue             │
└──────────────────────────┘
```

### Må du starte MinLPG nå?

**NEI, ikke nødvendig ennå!** 

Du kan teste Azure-synkroniseringen uten MinLPG:

1. Transaksjoner lagres i `transactions` tabell
2. De legges i `azure_sync_queue` tabell
3. De sendes til Azurite Queue
4. Du kan se dem med `./scripts/view-azurite-messages.sh`

Når du er klar til å bygge admin-systemet, kan du starte MinLPG-prosjektet for å konsumere meldingene.

## Feilsøking

### "UnknownHostException: azurite"

**Problem:** Du kjører fra IntelliJ uten profil, og den prøver å koble til `azurite` hostname.

**Løsning:** 
- ✅ Fikset! Default profil er nå `local` som bruker `localhost:10001`
- Restart API fra IntelliJ

### Ingen meldinger i Azurite

**Problem:** Du ser ingen meldinger i køen.

**Sjekkliste:**
1. ✅ Er Azurite kjørende? `docker ps`
2. ✅ Er API kjørende med `azure.enabled=true`?
3. ✅ Har du opprettet en transaksjon via emulatoren?
4. ✅ Sjekk `azure_sync_queue` tabell i PostgreSQL:
   ```sql
   SELECT * FROM azure_sync_queue ORDER BY created_at DESC;
   ```
5. ✅ Sjekk API-logger for sync-aktivitet:
   ```
   📤 Queued transaction ... for Azure sync
   ✅ Successfully synced TRANSACTION ... to Azure
   ```

### Transaksjoner synkes ikke

**Problem:** Transaksjoner ligger i `azure_sync_queue` men kommer ikke til Azurite.

**Sjekk:**
1. Status i `azure_sync_queue`:
   - `PENDING`: Venter på sync
   - `IN_PROGRESS`: Synker nå
   - `SYNCED`: Ferdig synket
   - `FAILED`: Feil oppstod

2. Logger fra `AzureSyncService`:
   ```
   Processing X pending items for Azure sync
   ```

3. Connection string:
   ```bash
   echo $AZURE_CONNECTION_STRING
   # Skal være: UseDevelopmentStorage=true;DevelopmentStorageProxyUri=http://localhost:10001
   ```

## API Endpoints

### Transaksjoner
- `POST /api/v1/transactions` - Opprett transaksjon (fra emulator)
- `GET /api/v1/transactions` - List transaksjoner
- `GET /api/v1/transactions/{id}` - Hent enkelt transaksjon
- `PATCH /api/v1/transactions/{id}/payment` - Oppdater betalingsstatus

### Azure Sync (fremtidig)
- `GET /api/v1/sync/status` - Vis sync-status
- `POST /api/v1/sync/retry/{id}` - Prøv sync på nytt

## Testing

### Test hele flyten

Du kan enten teste manuelt som beskrevet under, eller bruke det automatiske trafikksimulerings-skriptet:

**Alternativ A: Automatisk simulering (Anbefalt)**
```bash
# Simulerer en ny kunde hver time (default)
./scripts/simulate-traffic.sh

# Eller raskere: en ny kunde hvert 10. sekund
./scripts/simulate-traffic.sh 10
```

**Alternativ B: Manuell testing**

1. Start systemet:
   ```bash
   # Terminal 1: Azurite
   docker-compose -f docker-compose-local.yaml up azurite postgres
   
   # Terminal 2: API (IntelliJ)
   
   # Terminal 3: Emulator (IntelliJ)
   ```

2. Simuler drivstofflevering i emulatoren

3. Sjekk at transaksjon er lagret:
   ```bash
   curl http://localhost:8080/api/v1/transactions
   ```

4. Sjekk sync-køen:
   ```sql
   SELECT * FROM azure_sync_queue WHERE entity_type = 'TRANSACTION';
   ```

5. Vent 30 sekunder (sync interval)

6. Sjekk Azurite:
   ```bash
   ./scripts/view-azurite-messages.sh
   ```

7. Simuler betaling:
   ```bash
   curl -X PATCH "http://localhost:8080/api/v1/transactions/{id}/payment?paymentMethod=CARD&paymentStatus=PAID"
   ```

8. Vent 30 sekunder og sjekk Azurite igjen - du skal nå se en ny melding med `eventType: "PAYMENT_UPDATED"`

## Neste steg

1. ✅ **Fikset Azure connection** (localhost i stedet for azurite hostname)
2. ✅ **Implementert TransactionSyncService** (legger transaksjoner i sync-køen)
3. ✅ **Integrert med TransactionService** (automatisk synkronisering)
4. 🔜 **Test hele flyten** (opprett transaksjon → se i Azurite)
5. 🔜 **Start MinLPG-prosjektet** når du er klar til å bygge admin-grensesnittet
6. 🔜 **Deploy til Azure** (ekte Azure Storage Queue i stedet for Azurite)
