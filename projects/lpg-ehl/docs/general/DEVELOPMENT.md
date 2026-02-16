# LPG-EHL Lokal Utvikling

Guide for å komme i gang med lokal utvikling av LPG-EHL systemet.

## Forutsetninger

- **Docker Desktop** installert og kjørende
- **Git** for versjonskontroll
- **Node.js 18+** (kun hvis du vil kjøre frontend lokalt utenfor Docker)
- **Java 21** + **Maven** (kun hvis du vil kjøre backend lokalt utenfor Docker)

## Quick Start - Alt i Docker

Den enkleste måten å komme i gang på:

```bash
# Start alt med ett kommando
./start-local.sh
```

Dette starter:
- ✅ PostgreSQL database (med alle migrasjoner)
- ✅ Azurite (Azure Queue emulator)
- ✅ LPG-EHL Emulator (TCP dispenser simulator)
- ✅ LPG-EHL API (Spring Boot)
- ✅ Frontend (React/Vite)
- ✅ WireMock (for testing)

## Access Points

Når alt kjører:

| Service | URL | Beskrivelse |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | React web UI |
| **API** | http://localhost:8080 | REST API |
| **Swagger** | http://localhost:8080/swagger-ui.html | API dokumentasjon |
| **Health** | http://localhost:8080/actuator/health | API health check |
| **Database** | localhost:5432 | PostgreSQL |
| **Emulator** | tcp://localhost:9000 | EHL protocol emulator |
| **Azurite** | http://localhost:10001 | Azure Queue emulator |
| **WireMock** | http://localhost:8081 | Mock server |

## Database Access

```bash
# Koble til database med psql
psql -h localhost -U lpg_user -d lpg_ehl

# Credentials:
# Host: localhost
# Port: 5432
# Database: lpg_ehl
# User: lpg_user
# Password: lpg_dev_password
```

## Docker Compose Kommandoer

```bash
# Start alle services i bakgrunnen
docker-compose -f docker-compose-local.yaml up -d

# Start med rebuild
docker-compose -f docker-compose-local.yaml up -d --build

# Se logger for alle services
docker-compose -f docker-compose-local.yaml logs -f

# Se logger kun for API
docker-compose -f docker-compose-local.yaml logs -f api

# Se logger kun for frontend
docker-compose -f docker-compose-local.yaml logs -f frontend

# Stopp alle services
docker-compose -f docker-compose-local.yaml down

# Stopp og fjern volumes (database data slettes!)
docker-compose -f docker-compose-local.yaml down -v

# Restart en spesifikk service
docker-compose -f docker-compose-local.yaml restart api

# Rebuild en spesifikk service
docker-compose -f docker-compose-local.yaml up -d --build api
```

## Hybrid Development

Hvis du vil kjøre noen komponenter lokalt (for raskere utvikling):

### Kjør kun infrastruktur i Docker

```bash
# Start bare database, azurite og emulator
docker-compose -f docker-compose-local.yaml up postgres azurite emulator -d
```

### Kjør API lokalt

```bash
# Fra lpg-ehl-api mappen
cd lpg-ehl-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Kjør Frontend lokalt

```bash
# Fra lpg-web mappen
cd lpg-web
npm install  # Første gang
npm run dev  # Starter på http://localhost:5173
```

## Testing

### Test API direkte

```bash
# Health check
curl http://localhost:8080/actuator/health

# Get dispenser state
curl http://localhost:8080/api/v1/dispenser/state

# List transactions
curl http://localhost:8080/api/v1/transactions

# Create credit account
curl -X POST http://localhost:8080/api/v1/credit/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Test Bedrift AS",
    "customerNumber": "CUST001",
    "creditLimitNok": 10000
  }'

# List credit accounts
curl http://localhost:8080/api/v1/credit/accounts
```

### Test Payment API

```bash
# Start payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amountCents": 10000,
    "method": "CARD",
    "reference": "TEST-001"
  }'

# Check payment status (bruk ID fra response)
curl http://localhost:8080/api/v1/payments/<payment-id>
```

### Test Emulator API

```bash
# Set scenario
curl -X POST http://localhost:8080/api/v1/emulator/scenario \
  -H "Content-Type: application/json" \
  -d '{
    "dispenserAddress": 1,
    "scenario": "TIMEOUT"
  }'

# Get status
curl http://localhost:8080/api/v1/emulator/status/1

# Reset
curl -X POST http://localhost:8080/api/v1/emulator/reset/1
```

## Database Migrations

Migrasjoner kjører automatisk ved oppstart av database-containeren.

Hvis du trenger å kjøre migrasjoner manuelt:

```bash
# Koble til container
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl

# Eller fra host
psql -h localhost -U lpg_user -d lpg_ehl -f migrations/002_add_credit_accounts.sql
```

## Troubleshooting

### Port allerede i bruk

```bash
# Finn prosess som bruker port 8080
lsof -i :8080

# Eller port 5432
lsof -i :5432

# Drep prosess
kill -9 <PID>
```

### Database connection refused

```bash
# Sjekk at database kjører
docker ps | grep lpg-postgres

# Se database logger
docker logs lpg-postgres

# Restart database
docker-compose -f docker-compose-local.yaml restart postgres
```

### API starter ikke

```bash
# Se API logger
docker logs lpg-api

# Eller live
docker-compose -f docker-compose-local.yaml logs -f api

# Sjekk at database er klar
docker-compose -f docker-compose-local.yaml ps

# Restart API
docker-compose -f docker-compose-local.yaml restart api
```

### Frontend viser CORS feil

Sjekk at API CORS er konfigurert riktig:
```yaml
CORS_ALLOWED_ORIGINS: http://localhost:5173,http://localhost:3000
```

### Nullstill alt

```bash
# Stopp og fjern alt (inkludert volumes)
docker-compose -f docker-compose-local.yaml down -v

# Start på nytt
./start-local.sh
```

## Nyttige Docker Kommandoer

```bash
# Se alle kjørende containere
docker ps

# Se alle containere (også stoppede)
docker ps -a

# Fjern alle stoppede containere
docker container prune

# Fjern ubrukte images
docker image prune

# Koble til en container
docker exec -it lpg-api bash

# Se container logs
docker logs lpg-api

# Se live logs
docker logs -f lpg-api
```

## IDE Setup

### IntelliJ IDEA

1. Import project som Maven project
2. Set SDK til Java 21
3. Enable annotation processing (for Lombok hvis brukt)
4. Add run configuration:
   - Main class: `no.cloudberries.lpg.api.LpgEhlApiApplicationKt`
   - VM options: `-Dspring.profiles.active=local`
   - Environment variables: (se docker-compose-local.yaml for verdier)

### VS Code

1. Install extensions:
   - Kotlin
   - Spring Boot Extension Pack
   - Docker
   - React/TypeScript extensions

2. For frontend debugging, se `.vscode/launch.json`

## Ytterligere Dokumentasjon

- [README.md](README.md) - Prosjekt overview
- [QUICKSTART.md](QUICKSTART.md) - 2-minutters quick start
- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) - Implementeringsstatus
- [lpg-ehl-core/WARP.md](lpg-ehl-core/WARP.md) - Core protocol detaljer
- [OpenAPI Spec](lpg-ehl-api/src/main/resources/openapi.yaml) - API spesifikasjon

## Support

For spørsmål eller problemer, se:
- GitHub Issues
- API dokumentasjon på http://localhost:8080/swagger-ui.html
- Health endpoint på http://localhost:8080/actuator/health
