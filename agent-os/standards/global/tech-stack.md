## Tech stack (LPG-EHL)

This repo is a **monorepo**. The **main product** lives in `projects/lpg-ehl/` (Kotlin/Spring Boot edge application + bundled React SPA). Everything outside that directory should be treated as **satellite/supporting** unless explicitly stated otherwise.

### Backend (edge application)
- **Language**: Kotlin (property in `projects/lpg-ehl/pom.xml`: `kotlin.version=2.1.0`)
- **Runtime target**: Java 17 (ARK-3360 target runtime; `maven.compiler.target=17`)
- **Framework**: Spring Boot 3.2.x (property: `spring-boot.version=3.2.1`)
- **Build**: Maven multi-module (`projects/lpg-ehl/pom.xml`) using the Maven Wrapper (`projects/lpg-ehl/mvnw`)
- **Web server**: Spring Boot embedded server (Undertow used in production webapp per docs)
- **Persistence**: Spring Data JPA + Hibernate
- **Migrations**: Liquibase changelogs live under `projects/lpg-ehl/lpg-ehl-service/src/main/resources/db/changelog/`
- **Serial comms**: RS-485 via `jSerialComm` (module `lpg-transport`)
- **Cloud integration**: Azure Storage Queue (edge-to-cloud sync; retryable/outbox-style)
- **Payment integration (production)**: HTTP REST to `PaymentTerminalMonoServer` (C#/.NET/Mono process) using `openapi-payment-terminal.yaml` contract

### Frontend (bundled SPA)
- **Framework**: React (repo docs mention React 19)
- **Language**: TypeScript
- **Build tooling**: Vite
- **Styling**: Tailwind CSS (repo docs mention Tailwind)
- **Packaging**: SPA is built and shipped as static assets served by `lpg-ehl-webapp` (see `projects/lpg-ehl/lpg-ehl-webapp/src/main/resources/static/` and `projects/lpg-ehl/lpg-web/`)

### Database
- **Production**: PostgreSQL (optional depending on deployment; supported)
- **Field / local**: H2 file DB (default in field deployments per architecture docs)

### Testing
- **Unit & integration**: JUnit 5 + Kotlin test
- **HTTP stubbing**: WireMock (property: `wiremock.version=3.3.1`)
- **Profiles**: `lab` (emulator/in-memory), `field` (real serial), plus debug/diagnostics profiles where applicable

### Deployment / operations (primary)
- **Target**: ARK-3360 industrial edge PC (Linux)
- **Runtime shape**:
  - `lpg-ehl-webapp` (UI + REST + WebSockets)
  - or `lpg-ehl-app-headless` (background service, no UI)
  - plus `PaymentTerminalMonoServer` as a separate local process (port 18080 default)
- **Scripts**: production/runtime scripts live under `projects/lpg-ehl/scripts/`
