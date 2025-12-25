# Azurite Queue Monitoring

Dette verktøyet lar deg overvåke og inspisere meldinger som sendes til Azurite (lokal Azure Storage emulator).

## 🚀 Quick Start

```bash
# Se hvor mange meldinger som er i køen
./scripts/view-azurite-messages.sh --count

# Se de 5 siste meldingene (uten å fjerne dem)
./scripts/view-azurite-messages.sh --peek 5

# Overvåk køen i sanntid (oppdateres hvert 2. sekund)
./scripts/view-azurite-messages.sh --watch

# Hent og fjern 1 melding fra køen
./scripts/view-azurite-messages.sh --receive 1

# Tøm hele køen
./scripts/view-azurite-messages.sh --clear
```

## 📋 Kommandoer

| Kommando | Beskrivelse |
|----------|-------------|
| `--count` | Vis antall meldinger i køen |
| `--peek N` | Se N meldinger uten å fjerne dem (default: 10) |
| `--receive N` | Hent og fjern N meldinger (default: 1) |
| `--watch` | Overvåk køen kontinuerlig (Ctrl+C for å stoppe) |
| `--clear` | Tøm hele køen (krever bekreftelse) |
| `--help` | Vis hjelp |

## 📊 Hva ser du i meldingene?

Hver melding inneholder informasjon om:
- **entityType**: Type entitet (f.eks. `TRANSACTION`)
- **entityId**: UUID for entiteten
- **payload**: Selve dataene (JSON med transaksjonsinfo)
- **status**: Synkroniseringsstatus (PENDING, IN_PROGRESS, SYNCED, FAILED)
- **createdAt**: Når meldingen ble opprettet
- **insertionTime**: Når meldingen ble lagt i køen

Eksempel på en transaksjon-melding:
```json
{
  "queueId": "123e4567-e89b-12d3-a456-426614174000",
  "entityType": "TRANSACTION",
  "entityId": "987fcdeb-51a2-43f1-b9e0-123456789abc",
  "payload": {
    "dispenserId": 1,
    "litres": 25.5,
    "pricePerLitre": 18.50,
    "totalAmount": 471.75,
    "paymentType": "CARD",
    "paymentStatus": "PAID"
  },
  "status": "PENDING",
  "retryCount": 0
}
```

## 🔧 Feilsøking

### Azure CLI ikke installert
```bash
brew install azure-cli
```

### jq ikke installert (for pene JSON-visning)
```bash
brew install jq
```

### Azurite kjører ikke
Kjør først:
```bash
docker-compose -f docker-compose-local.yaml up -d
```

### Queue finnes ikke
Køen opprettes automatisk første gang API'et starter. Sørg for at:
1. Azurite kjører (`docker-compose ps`)
2. API'et er startet (`./gradlew :lpg-ehl-api:bootRun`)
3. `azure.enabled=true` i `application-local.yaml`

## 🎯 Typiske bruksscenarier

### Scenario 1: Debug hvorfor transaksjoner ikke synkes
```bash
# 1. Sjekk om det er meldinger i køen
./scripts/view-azurite-messages.sh --count

# 2. Se hva som er i køen
./scripts/view-azurite-messages.sh --peek 10

# 3. Sjekk backend-logger for sync-feil
# Se etter ❌ eller ⚠️ emojis i loggene
```

### Scenario 2: Overvåk sanntidssync
```bash
# Start overvåking i ett terminal-vindu
./scripts/view-azurite-messages.sh --watch

# I et annet vindu, opprett en transaksjon via API eller Emulator
# Du vil se meldingen dukke opp i køen og forsvinne når den synkes
```

### Scenario 3: Rydd opp etter testing
```bash
# Tøm alle meldinger
./scripts/view-azurite-messages.sh --clear
```

## 🌐 Alternativ: Azure Storage Explorer GUI

For en mer visuell opplevelse, bruk **Azure Storage Explorer**:

1. Last ned fra: https://azure.microsoft.com/products/storage/storage-explorer/
2. Installer og åpne
3. Velg **"Attach to a local emulator"**
4. Koble til:
   - Display name: `Azurite Local`
   - Queue endpoint: `http://127.0.0.1:10001`
5. Naviger til **Queues → lpg-transactions**

## 📝 Se også

- `DEVELOPMENT.md` - Generell utviklerinformasjon
- `DOCKER-COMPOSE-README.md` - Docker Compose oppsett
- Backend loggfil for sync-status (se etter `AzureSyncService` logger)
