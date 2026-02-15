# Arkitekturskåring – LPG-EHL

**Dato:** Oppdatert skåring  
**Formål:** Én oversikt over moduler, lag, bygg og kjøring på edge.

---

## 1. Systemets rolle

LPG-EHL er edge-software for **LPG-dispensere** (propan/autogass) på bensinstasjoner. Den styrer pumper via EHL-protokoll over RS-485, integrerer betalingsterminaler, håndterer transaksjoner og priser, og kan synkronisere data til sky (Azure).

---

## 2. Moduloversikt (10 moduler)

```
lpg-ehl-parent (POM)
├── lpg-ehl-core           ← Protokoll (EHL), ren Kotlin, ingen Spring
├── lpg-transport          ← RS-485 seriellport (implementerer core-interfaces)
├── lpg-ehl-serialport-sim ← Simulert seriellport (testing)
├── lpg-ehl-emulator       ← Dispenser-simulator (LAB-modus), Spring Boot
├── lpg-ehl-service        ← Forretningslogikk, JPA, Azure, avhengig av core + transport/emulator
├── lpg-ehl-api            ← REST-controllere og DTOer (avhenger av service + emulator)
├── lpg-ehl-webapp         ← Fat JAR: API + statisk frontend, Undertow (produksjon med UI)
├── lpg-ehl-app-headless   ← Fat JAR: samme logikk, uten/med minimal web (edge uten skjerm)
├── lpg-ehl-payment-terminal-sim  ← Simulator for betalingsterminal (REST)
└── lpg-ehl-payment-terminal-gui ← JavaFX-GUI for betalingsterminal-sim
```

| Modul | Ansvarsområde | Spring | Deployes som |
|-------|----------------|--------|--------------|
| **lpg-ehl-core** | EHL-pakker, tilstandsmaskin, protokoll | Nei | Bibliotek |
| **lpg-transport** | Fysisk RS-485 (jSerialComm) | Nei | Bibliotek |
| **lpg-ehl-serialport-sim** | Simulert seriellport | – | Test |
| **lpg-ehl-emulator** | Simulert dispenser (LAB) | Ja (Web) | Bibliotek / egen app |
| **lpg-ehl-service** | Transaksjoner, pumpe, pris, betaling, Azure, JPA | Ja | Bibliotek |
| **lpg-ehl-api** | REST, OpenAPI, DTO | Ja | Bibliotek |
| **lpg-ehl-webapp** | Én JAR: API + statisk frontend | Ja (Undertow) | **Produksjon (med UI)** |
| **lpg-ehl-app-headless** | Én JAR: logikk, valgfri debug-API | Ja | **Produksjon (ark uten skjerm)** |
| **lpg-ehl-payment-terminal-sim** | REST-simulator for terminal | Ja | Test / egen prosess |
| **lpg-ehl-payment-terminal-gui** | GUI for terminal-sim | Ja (JavaFX) | Test |

---

## 3. Lag og avhengigheter (skår vertikalt)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  INNGANGSPUNKTER (driving)                                                  │
│  • REST (webapp / headless debug-api)                                       │
│  • Planlagte jobber, WebSocket, hardware-events                             │
└─────────────────────────────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  lpg-ehl-api                                                                │
│  Controllers, DTOer, OpenAPI                                                │
└─────────────────────────────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  lpg-ehl-service                                                            │
│  TransactionService, PumpStateService, PriceService, PaymentGateway,       │
│  AzureSyncService, JPA-entities, Liquibase                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────────────────────────────────┐
│ lpg-ehl-core │ │ lpg-transport│ │ lpg-ehl-emulator (LAB)                    │
│ EHL-protokoll│ │ RS-485 /     │ │ Simulert dispenser (optional)             │
│ (ren Kotlin) │ │ SerialTransport│                                          │
└──────────────┘ └──────────────┘ └──────────────────────────────────────────┘
```

- **core:** Ingen Spring, ingen I/O – kun protokoll og tilstand.
- **transport:** Implementerer core sin transport-port; brukes i FIELD-modus.
- **emulator:** Byttes inn når man kjører i LAB-modus (uten ekte hardware).
- **service:** Orchestrerer domene, database og (valgfritt) sky; snakker med core/transport eller emulator.

---

## 4. Bygg og frontend (Build Mona Lisa)

Frontenden (React/Vite) ligger i eget modul/repo (**lpg-web**). Den **bygges ikke** på edge.

```
  BYGG-MILJØ (Node tilgjengelig)
        │
        │  Build Mona Lisa
        │  • npm install && npm run build i lpg-web
        │  • Kopier dist/ → lpg-ehl-webapp/src/main/resources/static
        ▼
  lpg-ehl-webapp
        │  src/main/resources/static/  (statisk innhold)
        │
        │  mvn package
        ▼
  lpg-ehl-webapp-*.jar   (fat JAR med API + statisk frontend)
```

På edge kjører **ingen Node**; kun JVM. Webapp-JAR-en serverer både REST API og ferdigbygget frontend fra `classpath` (statisk).

Se **docs/BUILD_OG_DEPLOY_SKAR.md** for detaljert bygg-/deploy-skår.

---

## 5. Hva som kjører på edge (ark)

| Alternativ | JAR | Bruk |
|------------|-----|------|
| **Med UI** | lpg-ehl-webapp | Skjerm på stasjon; operatør bruker nettleser mot lokal server. |
| **Uten UI** | lpg-ehl-app-headless | Kun logikk og (valgfritt) debug-API; ingen statisk frontend. |

Begge er **én Java-prosess** (Kotlin, Spring Boot).  
Runtime: **Java 17** (ARK).  
Database: **PostgreSQL** (eller H2 i dev/test).  
LAB vs FIELD styres av konfigurasjon (emulator vs. ekte transport).

---

## 6. Teknologier (kort)

| Område | Valg |
|--------|------|
| Språk | Kotlin 2.1, JVM 17 |
| Rammeverk | Spring Boot 3.2 (Web, Data JPA, Security, Integration) |
| Server | Undertow (ikke Tomcat) |
| Database | PostgreSQL; Liquibase; H2 i test |
| Serial | jSerialComm (RS-485) |
| Sky | Azure Storage Queue (valgfritt) |
| API-dokumentasjon | SpringDoc OpenAPI (Swagger) |
| Frontend (kilde) | React, Vite (lpg-web); bygges utenfor, leveres som statisk i webapp-JAR |

---

## 7. Kort oppsummering

- **10 Maven-moduler:** core (protokoll), transport (RS-485), emulator (LAB), service (domene + infra), api (REST), webapp (JAR med UI), headless (JAR uten UI), pluss simulators for serial og betalingsterminal.
- **Lag:** API → service → core/transport eller emulator. Tydelig separasjon mellom protokoll (core) og forretningslogikk (service).
- **Bygg:** Build Mona Lisa bygger React i lpg-web og kopierer til webapp `src/main/resources/static`; Maven pakker alt i webapp-JAR.
- **Edge:** Én JVM-prosess (webapp eller headless), ingen Node; frontend serveres statisk fra JAR når webapp brukes.

Denne skåringen beskriver **nåværende** moduler, lag og bygg/deploy; den kan oppdateres ved endringer i modulliste eller byggpipeline.

---

## 8. Vurdering for edge med 2 GB RAM

**Forutsetning:** Edge-enhet med **2 GB RAM**, 64-bit Debian Linux headless (uten GUI), 64-bit Java.

### Score: **6,5 / 10**

| Kriterium | Vurdering |
|----------|-----------|
| Én prosess, ingen Node på device | ✅ Sterkt pluss |
| Headless-variant uten bundlet frontend | ✅ Sparer minne på ark |
| 64-bit Java på 64-bit Linux | ✅ Fornuftig; ingen unødvendig overhead |
| Spring Boot + JPA + Undertow (ved debug-API) | ⚠️ Moderat minnebruk; typisk 300–500 MB+ heap i drift |
| Ingen eksplisitt heap-/GC-tuning i repo | ⚠️ Anbefales for 2 GB |

**Hvorfor ikke høyere:**  
Spring Boot-stacken (JPA, Liquibase, Undertow ved behov, koroutiner, valgfri Azure-klient) er ikke «lettvekts» for 2 GB. Det er **mulig** å kjøre komfortabelt med riktig heap og kanskje G1GC-tuning, men arkitekturen er ikke optimalisert for minimal fotavtrykk – den er optimalisert for lesbarhet, testbarhet og vedlikehold.

**Hvorfor ikke lavere:**  
Valgene er tilpasset edge: én JVM, headless-deploy, ingen GUI-stack på device, ingen Node. Med begrenset heap (f.eks. 768–1024 MB) og PostgreSQL på annen maskin (eller ekstern DB) bør 2 GB holde.

### Anbefalt tuning for 2 GB RAM

- **Heap:** Begrens JVM-heap slik at OS og eventuelt andre tjenester får plass, f.eks.  
  `-Xmx768m` eller `-Xmx1024m` (la ~512–800 MB til Debian og off-heap).
- **GC:** G1GC er standard fra Java 9; for små heap kan det være aktuelt å vurdere  
  `-XX:+UseSerialGC` (lavest overhead) eller beholde G1 med korte pauser.
- **Database:** Kjør PostgreSQL på annen maskin eller delt tjeneste hvis mulig, så ark ikke deler 2 GB med DB-prosess.
- **Debug-API:** Bruk headless uten `debug-api`-profil i produksjon der det ikke trengs; det sparer Undertow og noe tråd/minne.
- **Overvåk:** Mål faktisk heap- og RSS-bruk på reelle ark under normal last og juster `-Xmx` deretter.
