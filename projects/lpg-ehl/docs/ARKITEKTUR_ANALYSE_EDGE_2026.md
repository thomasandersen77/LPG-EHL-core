# LPG-EHL Edge System – Senior Arkitektur-Analyse

**Rolle:** Senior Arkitekt (Kotlin/JVM, Spring Boot, DDD, Edge/Industrial)  
**Dato:** 7. februar 2026  
**Scope:** Full repository review med fokus på produksjonssikkerhet, DDD, Clean Architecture, Edge-krav  

---

## Innhold

1. [Arkitekturkart](#1-arkitekturkart)
2. [DDD og Clean Architecture-vurdering](#2-ddd-og-clean-architecture-vurdering)
3. [Edge/Industrial krav](#3-edgeindustrial-krav)
4. [API-Parity-modellen](#4-api-parity-modellen)
5. [Topp 10 tekniske risikoer](#5-topp-10-tekniske-risikoer)
6. [Før onsdag-plan](#6-før-onsdag-plan)
7. [Konklusjon](#7-konklusjon)

---

## 1. ARKITEKTURKART

### 1.1 Modulstruktur og Avhengigheter

```
lpg-ehl-parent (root POM)
│
├── DOMAIN CORE
│   ├── lpg-ehl-core              [LIBRARY] Pure domain logic
│   │   Dependencies: kotlin-stdlib, coroutines, slf4j
│   │   Eksporterer: EhlCodec, EhlPacket, DispenserStatus, Transaction (domain model)
│   │   INGEN Spring, INGEN JPA, INGEN infrastruktur
│   │
│   └── lpg-transport              [LIBRARY] Physical layer abstraction
│       Dependencies: lpg-ehl-core, jSerialComm
│       Eksporterer: EhlCommunicator, SerialPortManager, SerialTransport
│       Adapter: RS-485 serial via jSerialComm
│
├── INFRASTRUCTURE & BUSINESS
│   ├── lpg-ehl-service            [LIBRARY] Application + Infrastructure
│   │   Dependencies: core, transport, emulator (optional), Spring Data JPA, Liquibase, Azure SDK
│   │   Inneholder:
│   │   - Application services: FuelPumpService, TransactionService, PumpAuthorizationService
│   │   - Infrastructure: @Repository (JPA), AzureSyncService, NetsCloudSocketClient
│   │   - Domain services(?): DispenserService, PriceService
│   │   - Adapters: Azure Queue, PostgreSQL, H2, Nets payment
│   │   WARNING: Inneholder også controllers (PumpController, AdminController) - LEAKY!
│   │
│   └── lpg-ehl-emulator           [STANDALONE + LIBRARY]
│       Dependencies: core, transport, Spring Boot Web
│       Deployable: lpg-ehl-emulator-exec.jar (standalone simulator)
│       Eksporterer også: IEhlDispenserEmulator, EmulatorService (for LAB mode)
│       Adapter: In-memory serial port simulation
│
├── API LAYER
│   └── lpg-ehl-api                [LIBRARY] Shared REST controllers + DTOs
│       Dependencies: service, core, emulator, Spring Web, Spring Security, Swagger
│       Inneholder: @RestController (12 controllers), DTOs, API-responses
│       Brukes av BÅDE webapp OG headless (debug-api)
│
├── DEPLOYABLE APPLICATIONS
│   ├── lpg-ehl-webapp             [EXECUTABLE JAR]
│   │   Dependencies: api, service, core, transport, emulator, React SPA (lpg-web)
│   │   Build: Spring Boot repackage → fat JAR med embedded Undertow
│   │   Output: release/lpg-ehl-webapp.jar (~60 MB)
│   │   Profiles: lab (H2+emulator), field (PostgreSQL+serial)
│   │
│   ├── lpg-ehl-app-headless       [EXECUTABLE JAR]
│   │   Dependencies: api (!), service, core, transport, emulator
│   │   Build: Spring Boot repackage → fat JAR
│   │   Output: release/lpg-ehl-headless.jar (~45 MB)
│   │   Profiles: lab, field, debug-api (aktiverer Undertow web server)
│   │   Default: web-application-type=NONE (kun @Scheduled tasks)
│   │   Med debug-api: web-application-type=SERVLET (Undertow på port 8090)
│   │
│   └── lpg-ehl-serialport-sim     [EXECUTABLE JAR]
│       Dependencies: service, transport
│       Build: Maven Shade → fat JAR
│       Output: release/pls-sim.jar (~15 MB)
│       Kjøres standalone: java -jar pls-sim.jar /dev/ttyUSB0
│
└── FRONTEND
    └── lpg-web                     [NPM/Vite] React SPA
        Build: npm run build → dist/
        Deployment: Kopieres til lpg-ehl-webapp/src/main/resources/static/
        Serveres av Spring Boot som static resources
```

### 1.2 Runtime-komponenter og Adapters

| Adapter Type | Implementasjon | Module | Profil |
| ------------ | -------------- | ------ | ------ |
| Serial (RS-485) | `SerialPortManager` (jSerialComm) | lpg-transport | field |
| Serial (Emulert) | `InMemorySerialPort` → `EhlDispenserEmulator` | lpg-ehl-emulator | lab |
| Database (Production) | PostgreSQL via Spring Data JPA | lpg-ehl-service | field |
| Database (Testing/Field) | H2 in-memory | lpg-ehl-service | lab, h2 |
| Cloud Sync | Azure Storage Queue SDK | lpg-ehl-service | azure.enabled=true |
| Payment | `NetsCloudSocketClient` (SSL/TCP) | lpg-ehl-service | (alltid tilgjengelig) |
| Web UI | React SPA (static resources) | lpg-web → lpg-ehl-webapp | webapp only |
| REST API | Spring MVC controllers | lpg-ehl-api | webapp, headless+debug-api |

### 1.3 State Machine og Polling

**State Machine:**
- **Lokasjon:** `lpg-ehl-core/protocol/DispenserStatus.kt` (sealed interface med IDLE, AUTHORIZED, PUMPING, STOPPED, PAYMENT_PENDING, ERROR)
- **Parsing:** `DispenserStateMapper.kt` (mapper rå EHL 0x4B bytes til domain states)
- **Polling:** `lpg-ehl-service/pump/PumpStateService.kt` og `lpg-app-headless/service/HeadlessPollingService.kt`

**Polling-strategi:**
```
HeadlessPollingService (@Scheduled fixedDelay=2000ms)
  ↓
  FuelPumpService.pollState()
    ↓
    EhlCommunicator.sendAndReceive(STATE command)
      ↓
      SerialTransport (real eller emulert)
        ↓
        Dispenser/Emulator
```

**Kritisk observasjon:** To polling-steder:
- `PumpStateService.pollStateForReadyPumps()` (`@Scheduled fixedRate=500`)
- `HeadlessPollingService.pollDispenserStatus()` (`@Scheduled fixedDelay=2000`)

**RISIKO:** Potensielt overlappende polling; uklart hvem som eier lifecycle.

### 1.4 Transaksjonsflyt

**Lokasjon:**
- **Domain model:** `lpg-ehl-core/transaction/Transaction.kt` (domain)
- **JPA entity:** `lpg-ehl-service/transaction/Transaction.kt` (persistence)
- **Service:** `lpg-ehl-service/transaction/TransactionService.kt`
- **Sync:** `lpg-ehl-service/transaction/TransactionSyncService.kt` (outbox pattern)

**Flyt:**
```
REST POST /api/pump/authorize
  → PumpController
    → FuelPumpService.startFueling()
      → DispenserService.sendCommand(PRODUCT_SELECT)
      → DispenserService.sendCommand(UNBLOCK)
    → PumpAuthorizationService.create()
      → PumpAuthorizationRepository.save()

(Polling detekterer PUMPING state)

REST POST /api/pump/stop
  → FuelPumpService.stopFueling()
    → DispenserService.sendCommand(BLOCK)
    → DispenserService.queryVolume()
  → TransactionService.saveTransaction()
    → TransactionRepository.save()
    → TransactionSyncService.queueForSync()
      → AzureSyncQueueRepository.save() (outbox pattern)

(Azure sync scheduler sender til cloud)
```

### 1.5 Integrasjoner

| Integrasjon | Lokasjon | Type | Error Handling |
| ----------- | -------- | ---- | -------------- |
| Azure Queue | `AzureSyncService.kt` | `@Scheduled` fixedDelay=300s | Retry med exponential backoff, max 3 attempts |
| Nets Payment | `NetsCloudSocketClient.kt` | Sync SSL/TCP | IOException → logged; ingen auto-retry |
| Serial Port | `EhlCommunicator.kt` | Mutex-protected sync | Timeout=2s default; IOException → logged |
| Database | Spring Data JPA | `@Transactional` | Rollback on exception |

### 1.6 build_monolith.sh – Build-prosess

**Steg:**
1. `npm run build` i `lpg-web/` → `dist/`
2. Kopier `dist/*` til `lpg-ehl-webapp/src/main/resources/static/`
3. `mvn clean install` (alle moduler)
4. `mvn package -pl lpg-ehl-webapp -am`
5. `mvn package -pl lpg-ehl-app-headless -am`
6. `mvn package -pl lpg-ehl-serialport-sim -am`
7. Kopier JARs til `release/`:
   - `lpg-ehl-webapp.jar` (~60 MB: Spring Boot + React SPA + alle dependencies)
   - `lpg-ehl-headless.jar` (~45 MB: Spring Boot + alle dependencies, INGEN React)
   - `pls-sim.jar` (~15 MB: Shade JAR for PLS simulator)

**Profiler i JARs:**
- **webapp:** `lab` (default: H2+emulator), `field` (PostgreSQL+serial)
- **headless:** `lab` (default: H2+emulator+ingen webserver), `field` (serial), `debug-api` (aktiverer Undertow på port 8090)

---

## 2. DDD OG CLEAN ARCHITECTURE-VURDERING

### 2.1 Bounded Contexts (Identifisert)

Tre tydelige domener, men grensene er UKLARE i koden:

#### Bounded Context 1: **Fuel Dispensing** (kjerne-domenet)
- **Ansvar:** Kontrollere pumpe, motta volum, håndtere fysisk state
- **Ubiquitous Language:** IDLE, AUTHORIZED, PUMPING, STOPPED, UNBLOCK, BLOCK, PRODUCT_SELECT, EHL packet, checksum, dispenser address
- **Entiteter:** DispenserStatus, EhlPacket, EhlCommand, Transaction (domain model)
- **Lokasjon (i dag):** 
  - Domain: `lpg-ehl-core/protocol/`, `lpg-ehl-core/transaction/`
  - Application: `lpg-ehl-service/pump/`, `lpg-ehl-service/operations/`
  - Infrastructure: `lpg-transport/communication/`

#### Bounded Context 2: **Payment & Billing**
- **Ansvar:** Gjennomføre betaling, håndtere kredit, beregne priser inkl. veiavgift
- **Ubiquitous Language:** Purchase, Refund, TruncatedPan, acquirer, issuer, payment pending, credit account, road tax
- **Entiteter:** Payment, CreditAccount, Customer, PriceHistory, RoadTaxSettings
- **Lokasjon (i dag):** 
  - Domain: `lpg-ehl-core/payment/` (Protocol + TerminalConnection interface)
  - Application: `lpg-ehl-service/payment/`, `lpg-ehl-service/credit/`, `lpg-ehl-service/price/`
  - Infrastructure: `lpg-ehl-service/integration/NetsCloudSocketClient.kt`

#### Bounded Context 3: **Station Backoffice** (støttende)
- **Ansvar:** Rapporter, sync til cloud, drift og overvåking
- **Ubiquitous Language:** Daily summary, sync queue, outbox, transaction sync, diagnostics, watchdog
- **Entiteter:** DailySummary, AzureSyncQueue
- **Lokasjon (i dag):** 
  - Application: `lpg-ehl-service/system/`, `lpg-ehl-service/azure/`
  - Infrastructure: Azure SDK

**PROBLEM:** Grenser er IKKE eksplisitt i koden; alt ligger i samme `service`-modul.

### 2.2 Domenelogikk-lekkasje

#### PROBLEM 1: Service-modulen inneholder CONTROLLERS

**Bevis:**
- `lpg-ehl-service/controller/PumpController.kt` (`@RestController`)
- `lpg-ehl-service/controller/AdminController.kt` (`@RestController`)

**Hvorfor dette er galt:**
- `service`-modulen er ment å være **Application + Domain layer**, ikke Interface layer.
- Controllers burde være i `lpg-ehl-api` (som de nå også er!).
- **Duplikasjon og forvirring** om hvem som eier API-kontrakten.

**Anbefalt fix:** Flytt alle controllers fra `service/controller/` til `lpg-ehl-api/controller/`.

#### PROBLEM 2: Service-modulen blander Application og Domain

**service/pump/FuelPumpService.kt:**
- **Er dette Application layer?** Ja – orkestrerer use case (start fueling workflow).
- **Er dette Domain layer?** Nei – ingen domenelogikk, bare delegerer.

**Konklusjon:** `service`-modulen er en **blanding av Application, Infrastructure og Interface** (controllers). Ingen ren Domain layer utenom `core`.

### 2.3 Foreslått lagdeling (Clean Architecture)

```
┌─────────────────────────────────────────────────────────┐
│ INTERFACE LAYER (Frameworks & Drivers)                  │
├─────────────────────────────────────────────────────────┤
│ lpg-ehl-api:         REST Controllers, DTOs, Swagger    │
│ lpg-ehl-webapp:      Spring Boot main, React SPA, Web   │
│ lpg-ehl-app-headless: Spring Boot main, CLI runner      │
└────────────────────┬────────────────────────────────────┘
                     │ depends on
┌────────────────────┴────────────────────────────────────┐
│ APPLICATION LAYER (Use Cases)                           │
├─────────────────────────────────────────────────────────┤
│ lpg-ehl-service:  FuelPumpService, TransactionService,  │
│                   PumpAuthorizationService, PriceService│
│                   (USE CASE ORCHESTRATORS)               │
└────────────────────┬────────────────────────────────────┘
                     │ depends on
┌────────────────────┴────────────────────────────────────┐
│ DOMAIN LAYER (Business Logic)                           │
├─────────────────────────────────────────────────────────┤
│ lpg-ehl-core:     DispenserStatus (sealed interface),   │
│                   EhlCommand, EhlPacket, Transaction,   │
│                   EhlCodec, state transitions           │
│                   (PURE DOMAIN - NO FRAMEWORKS)          │
└────────────────────┬────────────────────────────────────┘
                     │ depends on (interfaces only)
┌────────────────────┴────────────────────────────────────┐
│ INFRASTRUCTURE LAYER (Adapters)                         │
├─────────────────────────────────────────────────────────┤
│ lpg-transport:    SerialPortManager (jSerialComm),      │
│                   EhlCommunicator (mutex, timeout)      │
│ lpg-ehl-emulator: InMemorySerialPort, EmulatorService   │
│ lpg-ehl-service:  JPA Repositories, AzureSyncService,   │
│                   NetsCloudSocketClient                 │
└─────────────────────────────────────────────────────────┘
```

### 2.4 Kartlegging av dagens kode til lag

| Klasse/Pakke | Faktisk lag i dag | Burde være | Aksjon |
| ------------ | ----------------- | ---------- | ------ |
| `core/protocol/DispenserStatus` | Domain | Domain | OK |
| `core/protocol/EhlCodec` | Domain | Domain | OK |
| `core/transaction/Transaction` | Domain | Domain | OK |
| `transport/EhlCommunicator` | Infrastructure | Infrastructure | OK |
| `service/pump/FuelPumpService` | Application | Application | OK |
| `service/pump/DispenserService` | Application+Infra | Application | Splitt |
| `service/operations/EhlOperationsService` | Application(?) | Infrastructure | Flytt til transport |
| `service/controller/PumpController` | Interface | Interface | Flytt til api |
| `service/transaction/Transaction` (JPA) | Infrastructure | Infrastructure | Duplikat av core? |
| `service/azure/AzureSyncService` | Infrastructure | Infrastructure | OK |
| `api/controller/*` | Interface | Interface | OK |

---

## 3. EDGE/INDUSTRIAL KRAV

### 3.1 Robusthet

#### Bra:
- **Retry:** `AzureSyncService` har retry med exponential backoff (max 3 attempts)
- **Timeout:** `EhlCommunicator.sendAndReceive(timeoutMs=2000)` – konfigurerbar
- **Mutex:** `EhlCommunicator` bruker `Mutex` for single-flight (unngår concurrent requests)
- **Backoff:** Azure sync bruker `2^attemptCount` for backoff
- **Fail-safe:** `@ConditionalOnProperty(azure.enabled=true)` – Azure-services laster kun hvis enabled
- **Crash recovery:** Outbox pattern i `azure_sync_queue` – transaksjoner synces selv etter crash

#### Mangler / Risiko:
1. **Ingen global retry for serial:** Hvis `EhlCommunicator.sendAndReceive()` får `TimeoutException`, propagerer den oppover uten retry.
   - **Anbefaling:** Legg til retry-policy med f.eks. 3 forsøk på TimeoutException.
   
2. **Ingen circuit breaker:** Hvis Nets Cloud/betaling feiler konsekvent, bombes terminalen med forespørsler.
   - **Anbefaling:** Legg til Spring Retry eller Resilience4j CircuitBreaker.

3. **Manglende idempotency-sjekk:** `TransactionService.saveTransaction()` har ingen duplikat-deteksjon.
   - **Anbefaling:** Legg til unique index på (dispenser_address, timestamp).

### 3.2 Drift (Logging, Health, Journald)

#### Bra:
- **Structured logging:** SLF4J + Logback med MDC-støtte
- **Log rotation:** `logback-spring.xml` har max-file-size=10MB, max-history=30 days
- **Journald:** `deployment/lpg-ehl.service` bruker `StandardOutput=journal`
- **Health endpoints:** `/actuator/health` (Spring Boot Actuator)
- **Trace-nivå:** Konfigurerbar via `LOG_LEVEL` environment variable

#### Risiko:
1. **Log-volum i produksjon:** `@Scheduled(fixedRate=500)` (2x per sekund) logger DEBUG-meldinger.
   - **Symptom:** Hvis LOG_LEVEL=DEBUG i produksjon → 172,800 log-linjer per dag bare fra polling!
   - **Anbefalt fix:** Bruk `logger.trace()` for høy-frekvens logging.
   - **Effort:** S (small)

2. **Ingen metrikker eksportert:** Spring Actuator er der, men ingen Prometheus/Grafana-exporter.
   - **Anbefalt fix:** Legg til `micrometer-registry-prometheus` dependency.
   - **Effort:** S

3. **Manglende health checks for serial:** `/actuator/health` sjekker kun DB og disk.
   - **Anbefalt fix:** Custom `HealthIndicator` som gjør `linetest()` hver 30s.
   - **Effort:** S

### 3.3 Ressurser (Memory, CPU, Threads, Blocking I/O)

**Memory:**
- **Webapp JAR:** ~60 MB (fat JAR med React SPA)
- **Headless JAR:** ~45 MB
- **Runtime heap:** build_monolith.sh foreslår `-Xms128m -Xmx256m` (headless)
- **Vurdering:** Fornuftig for edge (Raspberry Pi 4 har 4-8 GB RAM).

**Threads:**
- **Undertow:** `threads.io=1`, `threads.worker=4` (debug-api profil) – optimalisert for lav ressursbruk.
- **Vurdering:** Fornuftig.

**Blocking I/O:**
- **Serial:** `EhlCommunicator` bruker `SerialTransport.read()` som blokkerer.
- **Mutex:** Unngår concurrent serial access (bra).
- **RISIKO:** Hvis serial read henger, blokkerer tråden i opptil `readTimeoutMs`.
- **Anbefaling:** Legg til watchdog som detekterer "stuck serial reads".
- **Effort:** M (medium)

#### Kritisk risiko:
**Overlappende schedulers:**
- `PumpStateService.pollStateForReadyPumps()` (`@Scheduled fixedRate=500`)
- `HeadlessPollingService.pollDispenserStatus()` (`@Scheduled fixedDelay=2000`)
- **Begge** kaller `dispenserService.sendCommand()` → mutex contention.
- **Anbefalt fix:** Én sentral polling-service; FJERN duplikasjon.
- **Effort:** M

### 3.4 Determinisme (Race Conditions, Shared Mutable State)

#### Bra:
- **Mutex:** `EhlCommunicator` bruker `Mutex` for single-flight.
- **@Transactional:** Database-skriv er transaksjonelle.

#### Potensielle problemer:
1. **PumpStateService.pumpStates:** `ConcurrentHashMap`, men kan ha stale reads.
   - **Anbefalt fix:** Bruk `@Cacheable` eller eksplisitt locking.
   - **Effort:** S

2. **HeadlessPollingService.isRunning:** Boolean flag uten `@Volatile`.
   - **Risiko:** Visibility issue på tvers av tråder.
   - **Anbefalt fix:** `@Volatile var isRunning` eller `AtomicBoolean`.
   - **Effort:** S

### 3.5 Offline-first

#### Azure offline:
- Outbox pattern: Transaksjoner lagres lokalt FØRST, Azure sync DERETTER.
- **Vurdering:** FREMRAGENDE offline-resilience.

#### Nets offline:
- `NetsCloudSocketClient.sendCommand()` kaster `IOException` hvis Nets er nede.
- **Ingen retry** – transaksjonen blir PENDING forever.
- **Anbefalt fix:** Legg til retry-policy eller background job for PENDING payments.
- **Effort:** M

### 3.6 Sikkerhet: debug-api profil i produksjon

#### Kritisk risiko:

**Scenario:**
- Operatør aktiverer `debug-api` for å feilsøke i felt.
- Glemmer å deaktivere før produksjon.
- REST API (port 8090) er nå eksponert UTEN autentisering.

**Symptom:** Backdoor til intern API.

**Anbefalt fix (velg én):**
1. **Compile-time guard:** Fjern `debug-api` fra produksjons-JAR.
2. **Runtime guard:** Krev `DEBUG_API_TOKEN` environment variable.
3. **Auto-disable:** Hvis `field` + `debug-api` uten token → log WARNING.
4. **Whitelist:** `debug-api` kun på `localhost`.

**Effort:** S-M

---

## 4. API-PARITY-MODELLEN

### 4.1 Vurdering

**Fordeler:**
- Én kodebase for REST API (`lpg-ehl-api`) – ingen duplikasjon.
- Samme API-kontrakt for webapp og headless → enklere testing.
- Curl-runbook fungerer likt på begge.
- Muliggjør felt-debugging uten UI.

**Risikoer:**
1. **Debug-api blir "prod backdoor"** (se 3.6 over).
2. **API-modulen trekker inn web-dependencies** som påvirker headless.
   - **Anbefalt fix:** Split `lpg-ehl-api` til `-core` (DTOs) og `-web` (controllers).
   - **Effort:** M

3. **Ingen parity-test** som garanterer at headless+debug-api eksponerer SAMME API som webapp.
   - **Anbefalt fix:** Contract test (Spring Cloud Contract eller Pact).
   - **Effort:** M

### 4.2 Guardrails

**Foreslåtte tiltak:**
1. **Profil-guard:** Krev eksplisitt `DEBUG_API_ENABLED=true` env var.
2. **OpenAPI-kontrakt:** Generer `openapi.yaml` fra controllers; headless+debug-api må matche.
3. **Security-test:** Integrasjonstest som verifiserer autentisering.
4. **Startup-validation:** Hvis `field` + `debug-api` uten `DEBUG_API_TOKEN` → krasj.

---

## 5. TOPP 10 TEKNISKE RISIKOER

### Risiko 1: Duplikasjon av polling-logikk

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `PumpStateService` og `HeadlessPollingService` begge kaller `dispenserService.sendCommand()` |
| **Farlig på edge** | Mutex contention, uforutsigbar polling-frekvens |
| **Anbefalt fix** | Behold KUN `HeadlessPollingService`; fjern `PumpStateService` schedulers |
| **Effort** | S-M |

### Risiko 2: Log-bombing i DEBUG mode

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `@Scheduled(fixedRate=500)` logger DEBUG hver 500ms |
| **Farlig på edge** | Fyller disk (172,800 linjer/dag fra polling alene) |
| **Anbefalt fix** | Bruk `logger.trace()` for høy-frekvens; kun log state-ENDRINGER |
| **Effort** | S |

### Risiko 3: Controllers i service-modulen

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `lpg-ehl-service/controller/PumpController.kt` og `AdminController.kt` |
| **Farlig på edge** | Bryter Clean Architecture; forvirrende API-eierskap |
| **Anbefalt fix** | Flytt til `lpg-ehl-api`; slett fra `service` |
| **Effort** | S |

### Risiko 4: Ingen retry for Nets payment

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `NetsCloudSocketClient.sendCommand()` kaster IOException uten retry |
| **Farlig på edge** | Transaksjoner blir PENDING forever ved nettverks-blip |
| **Anbefalt fix** | Spring Retry med `@Retryable(maxAttempts=3)` |
| **Effort** | M |

### Risiko 5: Transaction domain model duplikasjon?

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `lpg-ehl-core/transaction/Transaction.kt` vs `lpg-ehl-service/transaction/Transaction.kt` |
| **Farlig på edge** | Mapping-overhead, potensielt data-tap |
| **Anbefalt fix** | Sjekk; behold kun JPA-versjon og map til domain DTO |
| **Effort** | S-M |

### Risiko 6: Ingen global error boundary

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | Manglende `@RestControllerAdvice` |
| **Farlig på edge** | Leaker stack trace til klient |
| **Anbefalt fix** | Legg til global exception handler i `lpg-ehl-api` |
| **Effort** | S |

### Risiko 7: Manglende serial watchdog-restart

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `HardwareWatchdogService` logger error, men ingen reconnect |
| **Farlig på edge** | RS-485 blir "dead" til manuell restart |
| **Anbefalt fix** | Watchdog kaller `SerialPortManager.reconnect()` ved failure |
| **Effort** | S |

### Risiko 8: Azure sync har ingen dead-letter queue

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | Etter 3 retries gir `AzureSyncService` opp; item blir stående |
| **Farlig på edge** | Transaksjoner "forsvinner" stille |
| **Anbefalt fix** | Sett `status=FAILED` og logg; evt. dead-letter queue |
| **Effort** | S |

### Risiko 9: H2 vs PostgreSQL dialect-mismatch

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `hibernate.dialect: H2Dialect` i base config |
| **Farlig på edge** | Suboptimale queries mot PostgreSQL |
| **Anbefalt fix** | Eksplisitt `PostgreSQLDialect` i `application-field.yaml` |
| **Effort** | S |

### Risiko 10: Manglende graceful shutdown for serial

| Felt | Verdi |
| ---- | ----- |
| **Symptom** | `SerialPortManager` har ikke `@PreDestroy` |
| **Farlig på edge** | Serial port blir "låst" ved restart |
| **Anbefalt fix** | `@PreDestroy` som kaller `serialPort.closePort()` |
| **Effort** | S |

---

## 6. "FØR ONSDAG"-PLAN

### Kritisk (gjør i dag)

#### Aksjon 1: DEBUG_API_TOKEN-guard

**Hva:** I `HeadlessApplication.kt` main():
```kotlin
if (environment.activeProfiles.contains("debug-api")) {
    val token = environment.getProperty("DEBUG_API_TOKEN")
    if (token.isNullOrBlank()) {
        logger.error("❌ debug-api profil er aktiv uten DEBUG_API_TOKEN!")
        exitProcess(1)
    }
    logger.warn("⚠️  DEBUG API ER AKTIVT - Kun for felt-testing!")
}
```

**Effort:** S | **Impact:** Høy (sikkerhet)

#### Aksjon 2: Fjern duplikate schedulers

**Hva:** Kommenter ut eller slett:
- `PumpStateService.pollStateForReadyPumps()` og `pollVolume()`
- Behold KUN `HeadlessPollingService.pollDispenserStatus()`.

**Effort:** S | **Impact:** Medium-høy

#### Aksjon 3: Curl-runbook

**Hva:** Lag `docs/CURL_FIELD_DEBUG.md`:
```bash
# Health check
curl http://localhost:8090/actuator/health

# Poll dispenser state
curl http://localhost:8090/api/debug/state/1

# List serial ports
curl http://localhost:8090/api/debug/serial/ports

# Scan addresses
curl -X POST "http://localhost:8090/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=10"
```

**Effort:** S | **Impact:** Høy

### Viktig (tirsdag)

#### Aksjon 4: SerialHealthIndicator

**Hva:** Custom `HealthIndicator` som gjør `linetest()` og rapporterer til `/actuator/health`.

**Effort:** S | **Impact:** Høy

#### Aksjon 5: @PreDestroy for serial

**Hva:** I `SerialPortManager.kt`:
```kotlin
@PreDestroy
override fun close() {
    serialPort?.closePort()
}
```

**Effort:** S | **Impact:** Medium

#### Aksjon 6: PostgreSQL dialect

**Hva:** I `application-field.yaml`:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**Effort:** S | **Impact:** Medium

#### Aksjon 7: @Volatile på isRunning

**Hva:** I `HeadlessPollingService.kt`:
```kotlin
@Volatile
private var isRunning = true
```

**Effort:** S | **Impact:** Medium

### Nice-to-have (hvis tid)

#### Aksjon 8: Timeout-tuning

**Hva:** Eksplisitte timeouts i `application.yaml`:
```yaml
ehl:
  serial:
    connect-timeout-ms: 5000
    read-timeout-ms: 3000
    retry-attempts: 3
```

**Effort:** S | **Impact:** Medium

#### Aksjon 9: Prometheus metrics

**Hva:** Legg til `micrometer-registry-prometheus` dependency.

**Effort:** S | **Impact:** Høy

#### Aksjon 10: Transaction unique constraint

**Hva:** Liquibase changeset med unique index på (dispenser_address, timestamp).

**Effort:** S | **Impact:** Medium

---

## 7. KONKLUSJON

### Er dette produksjonsklar edge-arkitektur?

**Svar: JA, MED FORBEHOLD.**

### Styrker:
- Solid modularisering med DDD-inspirert separasjon
- Excellent offline-resilience (outbox pattern)
- Deployment-fleksibilitet (webapp/headless/simulator)
- Low-resource footprint (Undertow, H2, optimalisert for edge)
- Modern stack (Kotlin 2.1, Spring Boot 3.2, coroutines, React 19)

### Forbehold (må fikses før prod):
- Duplikate schedulers (Risiko 1)
- debug-api sikkerhet (Risiko 10)
- Manglende retry for Nets (Risiko 4)

### Anbefaling til ledelse:

> "Systemet har en solid arkitekturgrunn med tydelig separasjon mellom protocol, transport og business logic. Offline-resilience er førsteklasses med outbox pattern. Før produksjonsdrift anbefales 3-5 mindre fikser (primært duplikat-fjerning og sikkerhet-guard for debug-API). Effort: 1-2 dager. Deretter er systemet klart for felt-deployment på ARK-maskin."

### Anbefaling til kunde:

> "Systemet er bygget for robusthet på edge med offline-first design, automatisk retry for cloud-sync, og minimal ressursbruk. Deployment-opsjonene (med/uten web UI) gir fleksibilitet for både stasjon og felt. Med de foreslåtte småfiks er dette en pålitelig produksjonsløsning."

---

## Vedlegg: Filer referert i analysen

| Fil | Beskrivelse |
| --- | ----------- |
| `lpg-ehl-core/protocol/DispenserStatus.kt` | Domain state machine |
| `lpg-ehl-core/protocol/EhlCodec.kt` | Protocol encoding/decoding |
| `lpg-transport/communication/EhlCommunicator.kt` | Serial communication with mutex |
| `lpg-ehl-service/pump/FuelPumpService.kt` | Fueling workflow orchestration |
| `lpg-ehl-service/pump/PumpStateService.kt` | State polling (scheduler) |
| `lpg-ehl-service/azure/AzureSyncService.kt` | Cloud sync with outbox |
| `lpg-ehl-service/transaction/TransactionService.kt` | Transaction persistence |
| `lpg-ehl-app-headless/HeadlessPollingService.kt` | Headless polling service |
| `lpg-ehl-app-headless/application.yaml` | Headless configuration |
| `lpg-ehl-app-headless/application-debug-api.yaml` | Debug API profile |
| `lpg-ehl-webapp/application.yaml` | Webapp configuration |
| `build_monolith.sh` | Build script for all JARs |

---

**Neste steg:** Implementer "før onsdag"-planen (Aksjon 1-7) for maksimal demo-suksess og produksjonssikkerhet.
