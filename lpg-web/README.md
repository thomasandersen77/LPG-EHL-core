# LPG Pumpestyring - Web Frontend

Modern React-basert web frontend for testing og demonstrasjon av LPG dispenser-systemet.

## Funksjonalitet

- **Sanntidsvisning** av dispenser-status (poller API hvert sekund)
- **Tre store metrikk-kort**: Beløp, Liter, Pris kr/L
- **To kontrollknapper**: Frigi dispenser og Stopp dispenser
- **Statusbar** som viser tilkoblingsstatus til emulator/API
- **Modus-indikatorer**: Kort aktiv, Dag modus, Stasjonskreditt aktiv

## Teknologi

- **React 18** med TypeScript
- **Vite** for rask utvikling og bygging
- **TanStack Query** (React Query) for data-håndtering
- **Axios** for HTTP-requests
- **Tailwind CSS** for styling

## Kom i gang

### Forutsetninger

- Node.js 18+ installert
- Backend API kjører på `http://localhost:8080` (eller sett `VITE_API_URL` i `.env.local`)

### Installasjon

```bash
npm install
```

### Kjør i utviklingsmodus

```bash
npm run dev
```

Åpner på `http://localhost:5173`

### Bygg for produksjon

```bash
npm run build
```

Output havner i `dist/`

## Konfigurasjon

Opprett `.env.local` for å overstyre API URL:

```bash
VITE_API_URL=http://localhost:8080/api/v1
```

## Integrasjon med backend

Frontenden kommuniserer med disse endepunktene:

- `GET /api/v1/dispenser/state` - Henter nåværende tilstand
- `POST /api/v1/dispenser/unblock` - Frigir dispenser
- `POST /api/v1/dispenser/stop` - Stopper dispenser

## Utvikling

Frontenden er designet for å kunne kjøre både:
- **Lokalt** mot emulator (via API)
- **I test** mot test-miljø
- **I produksjon** mot ekte RS-485 hardware

Endepunktene er de samme, bare backend-konfigurasjonen endres.
