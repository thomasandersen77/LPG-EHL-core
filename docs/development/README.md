# Development Documentation

Denne katalogen inneholder dokumentasjon for utviklere som jobber med LPG-EHL prosjektet.

## Innhold

- **[INTELLIJ_SETUP.md](INTELLIJ_SETUP.md)** - Quick start for IntelliJ IDEA
  - Prerequisites (Java 21, Docker, Maven)
  - Steg-for-steg setup
  - Database og Azurite konfigurasjon
  - Debugging tips

- **[INTELLIJ_FULL_STACK.md](INTELLIJ_FULL_STACK.md)** - Full stack development i IntelliJ
  - Start hele systemet med én knapp
  - Compound run configuration
  - Frontend + Backend + Emulator + Database
  - Full testflyt med Windows Dispenserkontroll

- **[MULTI-STATION-SETUP.md](MULTI-STATION-SETUP.md)** - Multi-station development setup
  - Kjør 3 Edge-stasjoner + Cloud samtidig
  - Docker Compose konfigurasjon
  - Testing scenarios
  - Monitoring og debugging

- **[DEMO_GUIDE.md](DEMO_GUIDE.md)** - Demo setup for presentasjoner
  - LPG-EHL (Pump System) + MinLPG (Cloud Admin)
  - Ngrok setup for ekstern tilgang
  - Demo-scenarios
  - Troubleshooting

## Getting Started

### For nye utviklere:
1. Start med [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md)
2. Kjør systemet med [INTELLIJ_FULL_STACK.md](INTELLIJ_FULL_STACK.md)
3. Test med Windows Dispenserkontroll

### For multi-station testing:
1. Les [MULTI-STATION-SETUP.md](MULTI-STATION-SETUP.md)
2. Start Docker Compose med 3 stasjoner
3. Koble Windows-klienter til forskjellige porter

### For demo/presentasjoner:
1. Følg [DEMO_GUIDE.md](DEMO_GUIDE.md)
2. Setup ngrok for remote access
3. Vis full Edge-to-Cloud integrasjon

## Development Workflow

```bash
# 1. Start database og services
docker-compose -f docker-compose.postgres.yaml up -d

# 2. Run i IntelliJ
# Bruk "Full Stack (API + Emulator)" run configuration

# 3. Test med Windows client
# Koble til localhost:9000

# 4. Verifiser i frontend
# http://localhost:8080
```

## Viktige Porter

- **8080** - LPG-EHL API + Frontend
- **9000** - Emulator TCP server (for Windows Dispenserkontroll)
- **5432** - PostgreSQL database
- **10001** - Azurite Azure Storage Emulator
- **8081** - MinLPG Cloud API (separat prosjekt)
