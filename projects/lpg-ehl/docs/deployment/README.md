# Deployment Documentation

Denne katalogen inneholder all dokumentasjon relatert til deployment av LPG-EHL systemet.

## Innhold

- **[DEPLOYMENT_NO.md](DEPLOYMENT_NO.md)** - Norsk guide for deployment til Linux ARK-maskin
  - Prosjektstruktur (Monolith SPA)
  - Systemd service oppsett
  - Produksjonskonfigurasjon

- **[MONOLITH_DEPLOYMENT.md](MONOLITH_DEPLOYMENT.md)** - Single JAR deployment guide
  - Build prosess med `build_monolith.sh`
  - SPA (React Router) support
  - Deployment til ARK-maskin (32-bit Linux)
  - CI/CD integration

- **[RENDER_DEPLOY.md](RENDER_DEPLOY.md)** - Cloud deployment til Render.com
  - Cloud hosting setup
  - Miljøvariabler konfigurasjon

## Deployment Strategier

### 1. Monolith JAR (Anbefalt for Edge)
Best for ARK-maskiner på bensinstasjoner:
- Én eksekverbar JAR-fil
- Backend + Frontend i samme prosess
- Minimal ressursbruk
- Se [MONOLITH_DEPLOYMENT.md](MONOLITH_DEPLOYMENT.md)

### 2. Cloud Deployment
For cloud-basert administrasjonssystem:
- Render.com / Azure / AWS
- Separat fra edge-enheter
- Se [RENDER_DEPLOY.md](RENDER_DEPLOY.md)

## Norsk / Engelsk

- Norske guider for interne brukere
- Engelske guider for ekstern dokumentasjon og open source
