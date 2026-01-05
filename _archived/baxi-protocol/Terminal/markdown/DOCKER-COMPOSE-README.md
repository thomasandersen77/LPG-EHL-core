# Docker Compose Local Development

Komplett lokal utviklingsstack for LPG-EHL systemet.

## Stack-komponenter

| Service   | Port | Beskrivelse                                    |
|-----------|------|------------------------------------------------|
| Frontend  | 5173 | React web UI (Vite dev server)                |
| API       | 8080 | Spring Boot REST API                           |
| Emulator  | 9000 | LPG dispenser emulator (TCP socket)           |
| Postgres  | 5432 | PostgreSQL 16 database                         |
| Azurite   | 10001| Azure Storage Queue emulator                   |
| WireMock  | 8081 | Mock server for eksterne API-er               |

## Rask start

Start hele stacken:
```bash
docker-compose -f docker-compose-local.yaml up
```

Start i bakgrunnen:
```bash
docker-compose -f docker-compose-local.yaml up -d
```

Se logger:
```bash
docker-compose -f docker-compose-local.yaml logs -f
docker-compose -f docker-compose-local.yaml logs -f api
docker-compose -f docker-compose-local.yaml logs -f emulator
```

Stopp alt:
```bash
docker-compose -f docker-compose-local.yaml down
```

Rebuild og start:
```bash
docker-compose -f docker-compose-local.yaml up --build
```

## Access points

- **Frontend:** http://localhost:5173
- **API:** http://localhost:8080
  - Health: http://localhost:8080/actuator/health
  - API Docs: http://localhost:8080/swagger-ui.html
- **Database:** localhost:5432
  - User: `lpg_user`
  - Password: `lpg_dev_password`
  - Database: `lpg_ehl`
- **Azurite:** localhost:10001
- **WireMock:** http://localhost:8081

## Arkitektur

```
┌─────────────┐
│  Frontend   │ :5173
│  (React)    │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────┐
│     API     │ :8080
│(Spring Boot)│
└──┬────┬────┬┘
   │    │    │
   ▼    ▼    ▼
┌──────┐┌──────┐┌──────┐
│Emul- ││Post- ││Azur- │
│ator  ││gres  ││ite   │
└──────┘└──────┘└──────┘
:9000   :5432   :10001
```

## Komponenter i detalj

### Frontend (lpg-web)
- React 18 + TypeScript
- Vite dev server med HMR
- TanStack Query for data-håndtering
- Tailwind CSS styling
- Poller API hvert sekund for sanntidsdata

### API (lpg-ehl-api)
- Spring Boot 3.2.1 REST API
- 17 REST endpoints
- Azure Queue integration (mot Azurite lokalt)
- OpenAPI/Swagger dokumentasjon
- Bearer token auth (`dev-token-12345`)

### Emulator (lpg-ehl-emulator)
- Simulerer ekte LPG dispenser
- TCP socket på port 9000
- Støtter EHL protokoll
- Konfigurerbart via env vars

### Database (PostgreSQL 16)
- Persistent storage av transaksjoner
- Master for alle data
- Health checks før API starter

### Azurite
- Lokal Azure Storage Queue emulator
- Brukes for testing av sync-logikk
- Connection string pre-konfigurert

### WireMock
- Mock eksterne API-er
- Mappings i `wiremock/mappings/`
- Responses i `wiremock/__files/`

## Miljøvariabler

API konfigureres med disse environment variables:

```yaml
SPRING_PROFILES_ACTIVE: local
DB_HOST: postgres
DB_PORT: 5432
DB_NAME: lpg_ehl
DB_USER: lpg_user
DB_PASSWORD: lpg_dev_password
AZURE_ENABLED: true
AZURE_CONNECTION_STRING: "DefaultEndpointsProtocol=http;..."
AZURE_QUEUE_NAME: lpg-transactions
AZURE_SYNC_INTERVAL: 30
API_AUTH_TOKEN: dev-token-12345
CORS_ALLOWED_ORIGINS: http://localhost:5173,http://localhost:3000
EMULATOR_HOST: emulator
EMULATOR_PORT: 9000
```

## Testing av API

Med curl:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Get dispenser state (med auth)
curl -H "Authorization: Bearer dev-token-12345" \
  http://localhost:8080/api/v1/dispenser/state

# Frigi dispenser
curl -X POST \
  -H "Authorization: Bearer dev-token-12345" \
  http://localhost:8080/api/v1/dispenser/unblock

# Get transaksjoner
curl -H "Authorization: Bearer dev-token-12345" \
  http://localhost:8080/api/v1/transactions
```

## Feilsøking

### Container starter ikke
```bash
# Se detaljerte logger
docker-compose -f docker-compose-local.yaml logs <service-name>

# Restart én service
docker-compose -f docker-compose-local.yaml restart <service-name>
```

### Database connection issues
```bash
# Sjekk at postgres er healthy
docker-compose -f docker-compose-local.yaml ps postgres

# Test connection
docker exec lpg-postgres psql -U lpg_user -d lpg_ehl -c "SELECT 1"
```

### Frontend kan ikke nå API
- Sjekk at CORS er konfigurert riktig i API
- Verifiser at API er oppe: http://localhost:8080/actuator/health
- Se frontend console for errors

### Rebuild etter kodeendringer
```bash
# Rebuild én service
docker-compose -f docker-compose-local.yaml build api

# Rebuild og restart
docker-compose -f docker-compose-local.yaml up --build api
```

## Utvikling

For lokal utvikling uten Docker:
1. Start bare database og azurite: `docker-compose -f docker-compose-local.yaml up postgres azurite`
2. Kjør API lokalt: `mvn spring-boot:run -pl lpg-ehl-api`
3. Kjør frontend lokalt: `cd lpg-web && npm run dev`
4. Kjør emulator lokalt: `mvn spring-boot:run -pl lpg-ehl-emulator`

## Produksjon

For produksjon:
- Bygg production images med optimalisering
- Bruk ekte Azure Storage Queue i stedet for Azurite
- Bruk ekte RS-485 hardware i stedet for emulator
- Konfigurer SSL/TLS certificates
- Bruk secrets management (Azure Key Vault)
- Sett opp proper logging og monitoring
