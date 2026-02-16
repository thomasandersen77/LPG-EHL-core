# Problem statement
Vi vil kunne teste (og utvikle) Kotlin-kode som integrerer mot PaymentTerminalMonoServer HTTP-API uten å måtte kjøre en ekte terminal eller Mono-server lokalt. Vi trenger derfor:
1) WireMock-baserte integrasjonstester som verifiserer request/response-mapping, feilhåndtering og casing.
2) En liten mock-server i Kotlin (evt. WireMock programmatisk) som eksponerer endepunktene definert i `openapi-payment-terminal.yaml`.
# Current state (relevant findings)
* OpenAPI-kontrakten ligger i `openapi-payment-terminal.yaml` og definerer bl.a. `/health` (lowercase keys) og `/v1/*`-endepunkter med PascalCase responser (f.eks. `Success`, `OperationId`, `ErrorCode`).
* Dokumentasjon fra Alejandro i `instructions/Terminal_API_Contract.md` og `instructions/Terminal_documentation_2026.md` beskriver samme API og viktige regler: single in-flight operation, idempotency via `ClientRequestId`, og standard feilsituasjoner (`terminal_busy`, `terminal_not_ready`, `operation_timeout`, `vendor_call_failure`).
* `lpg-ehl-webapp/pom.xml` har allerede WireMock som test dependency (`org.wiremock:wiremock-standalone:3.3.1`), men eksisterende integration tests i `lpg-ehl-webapp/src/test/...` bruker Testcontainers + RestAssured og ikke WireMock.
* Det finnes tidligere WireMock-testmønster i arkivert kode (`legacy/archived/rest-api-attempt/NetsCloudClientTest.kt`) som viser hvordan WireMockExtension + scenarier kan brukes.
* I nåværende aktive kodebase finnes det ikke en produksjonsklar klient mot PaymentTerminalMonoServer (dvs. en Kotlin-HTTP-klient som kaller `/v1/payments/purchase` osv.).
# Proposed approach (high level)
Vi innfører en eksplisitt Kotlin-klient/adapter for PaymentTerminalMonoServer ("payment-terminal-client") og dekker den med WireMock-tester. I tillegg lager vi en liten mock-server som kan kjøres manuelt i dev/CI for end-to-end testing av andre komponenter.
# 1) WireMock integrasjonstester
## 1.1 Avklare test-scope (hva skal testes)
Vi tester primært:
* URLer + HTTP-metoder + content-type
* JSON payload mapping (inkl. casing)
* Feilhåndtering pr. HTTP status + `ErrorCode`
* Idempotency via `ClientRequestId`
* Minst én "happy path" og flere "unhappy paths" for purchase + 1-2 admin endepunkter
## 1.2 Hvor koden bør ligge
Alternativer:
* A) Legg klienten i `lpg-ehl-service` (adapter/service-lag), og legg WireMock-testene der. Fordel: riktig lag. Ulempe: modulen har ikke spring-web i dag.
* B) Legg klienten i `lpg-ehl-webapp` (har allerede wiremock dependency). Fordel: raskt å komme i gang. Ulempe: klientlogikk havner i “app”-laget.
* C) Ny modul `lpg-payment-terminal-client` (ren bibliotekmodul). Fordel: ryddig og gjenbrukbar, lett å teste. Ulempe: litt mer Maven wiring.
Anbefaling: C hvis dere ønsker langsiktig ryddighet; A hvis dere ønsker minimal modul-endring og er komfortable med å legge til en liten HTTP-dependency.
## 1.3 Implementere klient-API (minimal, testbar)
Foreslått Kotlin-API (konseptuelt):
* `PaymentTerminalClient.health()`
* `PaymentTerminalClient.getTerminalStatus()`
* `PaymentTerminalClient.openTerminal()` / `closeTerminal()`
* `PaymentTerminalClient.purchase(request)`
* `PaymentTerminalClient.refund(request)`
* `PaymentTerminalClient.reversal(password)` (minst ett admin-kall)
Teknologi:
* Bruk Java `HttpClient` + Jackson (matcher mønsteret i repoet) eller Spring `RestClient` hvis dere vil standardisere på Spring.
* Konfigurer JSON parsing til å være robust på casing (serveren lover PascalCase på responses, men dokumentene i repoet har blandede eksempler).
## 1.4 WireMock-testdesign
Oppsett pr. testklasse:
* Start WireMock på `dynamicPort()`.
* Konfigurer klientens `baseUrl` til WireMock sin baseUrl.
* Stubbe relevante endepunkter.
* Verifiser request body med JSONPath (beløp, operatorId, clientRequestId).
* Returner response bodies som matcher OpenAPI:
    * Happy path: `200` med `OperationResponse` og `Success=true`.
    * Busy/not-ready/timeout/vendor failure: `409/503/408/500` med `ErrorResponse`.
Konkrete testcases (første batch):
* `GET /health` returns lowercase keys → klient parser.
* `GET /v1/terminal/status` → klient parser.
* `POST /v1/payments/purchase`:
    * success
    * 409 `terminal_busy`
    * 503 `terminal_not_ready`
    * 408 `operation_timeout`
* `POST /v1/admin/reversal`:
    * success
    * 409 busy
## 1.5 Ressursfiler for testdata
Legg JSON-eksempler i `src/test/resources/payment-terminal/` (f.eks. `purchase-approved.json`, `error-terminal-busy.json`) for å gjøre testene lesbare og gjenbrukbare.
## 1.6 CI/Runtime
* WireMock-testene kjører uten Docker.
* Testcontainers-testene (som dere allerede har) fortsetter å være Docker-avhengige; WireMock-testene kan kjøres separat for rask feedback.
# 2) Kotlin mock-server som eksponerer APIet
Her finnes to praktiske varianter.
## 2.1 Variant A: Kotlin-app som starter WireMockServer (anbefalt for fart)
Lag et lite Kotlin “runner”-program (f.eks. egen modul eller en `main` i en eksisterende modul) som:
* Starter `WireMockServer` på port (default 5000/8081) og laster mappings fra `wiremock/mappings/`.
* Eksponerer alle endepunktene i `openapi-payment-terminal.yaml` via mappings.
* Bruker WireMock “scenarios” for å simulere state (busy/not-ready, approval/decline) ved behov.
Fordel: nesten ingen server-kode, lett å vedlikeholde.
## 2.2 Variant B: Ekte Kotlin-server (Ktor eller Spring Boot)
Implementer en lett server som:
* Har routes for `/health` + `/v1/*`.
* Har en enkel in-memory “terminal state”: `terminalOpen`, `terminalReady`, “in-flight operation lock”.
* Implementerer idempotency på `ClientRequestId`.
* Returnerer `OperationResponse` og `ErrorResponse` i PascalCase.
* (Valgfritt) SSE på `/v1/events/stream`.
Fordel: mer realistisk oppførsel enn WireMock-mappings, spesielt for concurrency/idempotency og SSE.
Ulempe: mer kode.
## 2.3 “I henhold til OpenAPI”-nivå
Tre nivåer av “conformance” (velg én):
* Nivå 1: Endepunkter + felt finnes (best-effort).
* Nivå 2: Request/response valideres mot OpenAPI (krever ekstra validator-bibliotek).
* Nivå 3: Generer server-stubs fra OpenAPI og implementer kun businesslogikk.
Anbefaling: start med Nivå 1 (evt. Nivå 2 senere når kontrakten stabiliserer seg).
# Open questions (need your input)
1) Hvilken del skal vi teste først: kun klient/adapter mot PaymentTerminalMonoServer, eller også integrasjon inn i `PaymentGateway`-flyt i lpg-ehl?
2) Vil dere at mock-serveren skal støtte stateful flows (busy, idempotency, events), eller holder det med statiske responses for nå?
3) Hvilken JSON-casing er “fasit” i produksjon: PascalCase (som OpenAPI) eller camelCase (som noen eksempler i `Terminal_API_Contract.md`)?
# Next step
Når du svarer på spørsmålene over (spesielt 1 og 3), kan jeg konkretisere planen til eksakte moduler/filer og foreslå første testklasse + mapping-filer som gir maksimal nytte raskt.