# Plan: WireMock integrasjonstester + Kotlin mock-server for PaymentTerminalMonoServer API

Denne planen beskriver hvordan vi kan teste Kotlin-kode som integrerer mot PaymentTerminalMonoServer sitt HTTP-API (Baxi/Nets) uten å måtte kjøre ekte terminal eller Mono-server lokalt.

Kilder (kontrakt / spesifikasjon):
- `/Users/tandersen/git/NorgesGass/lpg-ehl/openapi-payment-terminal.yaml`
- `/Users/tandersen/git/NorgesGass/lpg-ehl/instructions/Terminal_API_Contract.md`
- `/Users/tandersen/git/NorgesGass/lpg-ehl/instructions/Terminal_documentation_2026.md`

## 1. Mål
1. **WireMock-baserte integrasjonstester** som verifiserer at en Kotlin-klient/adapter:
   - kaller riktige URLer og HTTP-metoder
   - sender riktig JSON request body (inkl. idempotency-felt)
   - tolker responses korrekt (inkl. casing)
   - håndterer typiske feil: busy / not ready / timeout / vendor call failure
2. En **liten mock-server i Kotlin** som kan kjøres manuelt eller i CI for å simulere PaymentTerminalMonoServer.

## 2. Viktige kontraktpunkter vi må støtte
Basert på OpenAPI + docs:
- `/health` returnerer **lowercase keys** (unntak fra resten).
- `/v1/*` endpoints returnerer `OperationResponse` (PascalCase felter, f.eks. `Success`, `OperationId`).
- Standard feilkoder:
  - `terminal_busy` (HTTP 409)
  - `terminal_not_ready` (HTTP 503)
  - `operation_timeout` (HTTP 408)
  - `vendor_call_failure` (HTTP 500)
  - `diagnostics_disabled` (HTTP 403)
- **Idempotency** via `ClientRequestId` (typisk for purchase/refund/cashback).
- **Single in-flight operation** (praktisk konsekvens: vi må kunne simulere 409).

## 3. Avklaring om JSON casing (beslutning)
Det er en inkonsistens mellom:
- `openapi-payment-terminal.yaml`: sier PascalCase på responses (f.eks. `Success`).
- `Terminal_API_Contract.md`: har flere eksempler i camelCase/lowercase (f.eks. `success`, `operationId`).

Planen anbefaler:
- **Bruk OpenAPI som fasit** i mock-responser og i klient-parsing.
- Gjør klient-parsing robust der det er billig (f.eks. tolerer begge casinger i en overgang), men test primært PascalCase slik at testene matcher kontrakten.

## 4. Leveranse A: PaymentTerminal-klient (for å ha noe å WireMock-teste)
### 4.1 Hvor skal klienten ligge?
Vi har tre fornuftige alternativer i `/Users/tandersen/git/NorgesGass/lpg-ehl`:

A) Legg klienten i `lpg-ehl-service`
- Fordel: dette er “service/adapter”-lag og gjenbrukbart.
- Ulempe: må ta et bevisst valg av HTTP-klient avhengigheter.

B) Legg klienten i `lpg-ehl-webapp`
- Fordel: WireMock dependency finnes allerede i test-scope.
- Ulempe: klientlogikk havner nær app/web-laget.

C) Ny modul (anbefalt hvis dere vil ha ryddig struktur): `lpg-payment-terminal-client`
- Fordel: ren bibliotekmodul, lett å gjenbruke (webapp, headless, service).
- Ulempe: litt ekstra Maven wiring.

Anbefaling: **C** hvis dette blir “en ekte integrasjon”; **A** hvis dere vil raskest mulig i gang uten ny modul.

### 4.2 Foreslått API (minimal overflate først)
Implementer et smalt klient-API som er lett å teste:
- `health()` → GET `/health`
- `terminalStatus()` → GET `/v1/terminal/status`
- `openTerminal()` → POST `/v1/terminal/open`
- `closeTerminal()` → POST `/v1/terminal/close`
- `purchase(request)` → POST `/v1/payments/purchase`
- `refund(request)` → POST `/v1/payments/refund`
- `reversal(password)` → POST `/v1/admin/reversal`

Teknologi-anbefaling for Kotlin-klienten:
- Bruk Java `HttpClient` + Jackson (matcher mønster som allerede finnes i repoet i flere små klienter), eller Spring `RestClient` hvis dere ønsker å standardisere på Spring.

### 4.3 DTOs (bevisst enkel start)
For testbarhet anbefales:
- Lag egne DTOer som matcher OpenAPI (minst feltene vi trenger i testene).
- Alternativt: parse responses “løst” til `Map<String, Any>` for første iterasjon, men det gir svakere compile-time garanti.

## 5. Leveranse B: WireMock integrasjonstester
### 5.1 Testmål
WireMock-testene skal verifisere:
- riktig path/method
- request body inneholder forventede felter
- klient tolker suksess/feil korrekt

### 5.2 Test-struktur
Bruk JUnit 5 + WireMock (mønster finnes allerede i arkivert testkode i repoet):
- Start WireMock på `dynamicPort()`
- Konfigurer klienten med `baseUrl = wireMock.baseUrl()`
- Stubbe endpoints per test
- Verifiser request med JSONPath (WireMock verify)

### 5.3 Første testpakke (minimum)
1) `GET /health`:
- Returner lowercase keys (f.eks. `{"status":"ok","timestamp":"...","configLoaded":true}`)
- Assert at klient kan parse.

2) `GET /v1/terminal/status`:
- Returner `TerminalStatusResponse` iht. OpenAPI.

3) `POST /v1/payments/purchase`:
- Happy path (HTTP 200 + `OperationResponse` med `Success=true`)
- Busy (HTTP 409 + `ErrorResponse` med `ErrorCode=terminal_busy`)
- Not ready (HTTP 503 + `ErrorResponse` med `ErrorCode=terminal_not_ready`)
- Timeout (HTTP 408 + `ErrorResponse` med `ErrorCode=operation_timeout`)

4) `POST /v1/admin/reversal`:
- Happy path + busy.

### 5.4 Testdata som filer
Legg eksempelfiler i `src/test/resources/payment-terminal/`:
- `health-ok.json`
- `terminal-status-ready.json`
- `purchase-approved.json`
- `error-terminal-busy.json`
- `error-terminal-not-ready.json`
- `error-operation-timeout.json`

Fordel: enklere vedlikehold og lesbarhet enn store multiline strings i testene.

## 6. Leveranse C: Kotlin mock-server
Dette er et dev/CI-verktøy som eksponerer APIet fra `openapi-payment-terminal.yaml`.

### 6.1 Variant 1 (anbefalt først): Kotlin-app som starter WireMockServer
Lag en liten Kotlin `main()` som:
- Starter `WireMockServer(port=5000)` (eller leser port fra env/arg)
- Laster mappings fra en mappe (f.eks. `wiremock/payment-terminal/mappings/`)
- (Valgfritt) bruker WireMock scenario for state (busy/not-ready osv.)

Fordeler:
- nesten ingen server-kode
- enkelt å legge til nye endpoints

### 6.2 Variant 2: Ekte server (Ktor eller Spring Boot)
Implementer en liten stateful server som:
- Holder in-memory state: `terminalOpen`, `terminalReady`, “operation lock”
- Implementerer idempotency: map fra `ClientRequestId` → tidligere `OperationResponse`
- Returnerer PascalCase felter
- (Valgfritt) SSE `/v1/events/stream`

Når bør vi velge denne?
- når vi trenger realistisk concurrency/idempotency og vil teste dette uten å “fake” det i WireMock.

### 6.3 Kortleser-ledet mock (styrbar kortflyt / "kortdragning")
Dette punktet er for å kunne teste “kunde-interaksjon” deterministisk (godkjent/avslått/feil PIN/avbrutt) uten ekte terminal.

Mål:
- Kunne kjøre samme API-kall (f.eks. `POST /v1/payments/purchase`) men få ulike terminalutfall.
- Kunne styre utfallet per test eller per manuell kjøring (uten å endre mapping-filer hver gang).

#### 6.3.1 Styringsflate (to enkle alternativer)
A) **Header-/query-styrt scenario (raskest)**
- Klienten/testen sender f.eks. `X-Terminal-Scenario: APPROVED|WRONG_PIN|USER_CANCEL|DECLINED|TIMEOUT|BUSY|NOT_READY`.
- Mocken velger respons basert på headeren.
- Fordel: ingen global state, enkelt i CI.

B) **Kontroll-endepunkt som setter global state (mer “lab-følelse”)**
- Mocken tilbyr et eget kontroll-endepunkt (f.eks. `POST /__control/scenario` og `GET /__control/scenario`).
- Dette endepunktet er *ikke* del av OpenAPI-kontrakten, men gjør manuell testing veldig effektiv.
- Fordel: du kan bytte scenario i UI/terminal uten å endre klient.

Begge alternativene kan implementeres enten:
- med WireMock (programmatisk dispatch via `ResponseTransformer` / `ServeEventListener`), eller
- i en stateful Kotlin-server (Ktor/Spring) med enkel in-memory variabel.

#### 6.3.2 Scenarier som bør støttes (første versjon)
For `purchase/refund/cashback`:
- **APPROVED**
  - HTTP 200
  - `OperationResponse.Success=true`
  - `CallResult=1`, `LocalModeResult=0`, `ResponseCode="00"`
- **DECLINED**
  - HTTP 200 (eller 200 med `Success=false`, avhengig av hvordan dere vil modellere avslått i klienten)
  - `Success=false`, `CallResult=1`, `LocalModeResult=2`, `ResponseCode="05"` (typisk decline)
- **WRONG_PIN**
  - HTTP 200
  - `Success=false`, `LocalModeResult=2`, `ResponseCode="Z1"`, `RejectionSource="3"`, `RejectionReason` inneholder `3:2:Z1`
- **USER_CANCEL**
  - HTTP 200
  - `Success=false`, `LocalModeResult=2`, blank `ResponseCode`, `RejectionReason` f.eks. `2:1`
- **BUSY**
  - HTTP 409 med `ErrorResponse.ErrorCode=terminal_busy`
- **NOT_READY**
  - HTTP 503 med `ErrorResponse.ErrorCode=terminal_not_ready`
- **TIMEOUT**
  - HTTP 408 med `ErrorResponse.ErrorCode=operation_timeout`

For admin-endepunkter (f.eks. `/v1/admin/reversal`):
- APPROVED (HTTP 200, `Success=true`)
- BUSY (HTTP 409)
- NOT_READY (HTTP 503)

#### 6.3.3 Idempotency (ClientRequestId)
Hvis vi skal bruke mocken til realistisk flyt-test (retries), bør den støtte:
- Hvis request har `ClientRequestId` og samme id kommer igjen, returneres *samme* `OperationResponse` (inkl. `OperationId`).
- Minimal implementasjon: `ConcurrentHashMap<String, OperationResponse>`.

#### 6.3.4 Single in-flight operation (terminal_busy)
Hvis vi velger stateful mock (Variant 2), bør vi kunne simulere “busy window”:
- En enkel lock rundt operasjoner.
- Alternativ: scenario `BUSY` som alltid returnerer 409.

## 7. Milestones (foreslått rekkefølge)
1) Avklare casing-policy (vi følger OpenAPI; evt. toleranse i parser)
2) Implementere minimal PaymentTerminalClient (kun 2–3 endpoints først)
3) Skrive WireMock-tester for `health`, `terminal/status`, `purchase` (happy + 2 feilsituasjoner)
4) Utvide til admin endpoints + resten av feilsituasjonene
5) Lage mock-server (Variant 1) som kan startes lokalt og brukes av andre tester/komponenter
6) Utvide mock-server med **kortleser-ledet mock** (6.3) slik at man kan styre “kortutfall” deterministisk
7) (Valgfritt) senere: Variant 2 stateful server hvis behov

## 8. Ferdigkriterier (acceptance)
- WireMock-testene kjører uten Docker og er stabile.
- Vi har minst 1 happy path og minst 3 feilsituasjoner testet for purchase.
- Mock-serveren kan starte og svare på minst `/health`, `/v1/terminal/status`, `/v1/payments/purchase` iht. kontrakten.
- Mock-serveren støtter minst ett “kortutfall”-scenario (f.eks. APPROVED og WRONG_PIN) styrt via header eller kontroll-endepunkt.

## 9. Neste steg (hva jeg trenger fra deg for å implementere)
1) Hvilken plassering velger du for klienten: A (`lpg-ehl-service`), B (`lpg-ehl-webapp`) eller C (ny modul)?
2) Skal mock-serveren i første iterasjon være statisk (WireMock mappings), eller må den være stateful (idempotency/busy)?
3) Ønsker du styring av kortutfall via (A) header/query eller (B) eget kontroll-endepunkt (eller begge)?
