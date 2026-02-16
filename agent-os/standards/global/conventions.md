## General development conventions

### Repository scope and “source of truth”

- **Main product lives in `projects/lpg-ehl/`**: Treat this as the authoritative codebase (edge application + bundled UI + simulator tooling).
- **Everything else is satellite/supporting**: Do not “spread” core business logic into other top-level folders. If a satellite needs shared logic, the shared logic should live in `projects/lpg-ehl/` (usually `lpg-ehl-core` or `lpg-ehl-service`) and be consumed from there.

### Monorepo structure (high-level)

- **Core protocol**: `projects/lpg-ehl/lpg-ehl-core/`
  - Pure Kotlin protocol framing/codec + protocol models.
  - **Rule**: No Spring dependencies here.
- **Transport layer**: `projects/lpg-ehl/lpg-transport/`
  - RS-485 serial IO (`jSerialComm`), implements transport abstractions.
- **Business/service layer**: `projects/lpg-ehl/lpg-ehl-service/`
  - Domain services + orchestration + persistence + integrations (Azure queue, payment HTTP client).
  - **Rule**: Business rules live here (or in core if truly protocol-level).
- **Applications**:
  - `projects/lpg-ehl/lpg-ehl-webapp/`: Spring Boot entrypoint serving React SPA + REST + WebSockets.
  - `projects/lpg-ehl/lpg-ehl-app-headless/`: Spring Boot headless daemon/service.
- **Emulation/simulation**:
  - `projects/lpg-ehl/lpg-ehl-emulator/`: in-memory emulator (“lab mode”).
  - `projects/lpg-ehl/lpg-ehl-serialport-sim/`: serialport simulator.
  - `projects/lpg-ehl/lpg-ehl-payment-terminal-*`: payment terminal simulator + GUI.
- **Frontend source**: `projects/lpg-ehl/lpg-web/` (built and bundled into the webapp static resources).

### Code organization rules (enforced by architecture)

- **Keep layers thin**:
  - Web layer (controllers/websocket adapters) should be a thin adapter.
  - Business logic should be in services, not controllers or repositories.
- **Respect module boundaries**:
  - `lpg-ehl-core` should not depend on Spring or database concerns.
  - `lpg-ehl-service` is the “brain” reused by both webapp and headless.
- **Prefer ports/adapters**:
  - Service code should depend on interfaces for external systems when it simplifies testing and swaps (hardware transport, payment, cloud).

### Build and artifact hygiene

- **Build from `projects/lpg-ehl/`** using `./mvnw` (and project scripts like `build_monolith.sh`).
- **Do not commit build outputs**: `target/`, `dist/`, generated bundles under `src/main/resources/static/assets/`, etc. are treated as build artifacts unless a module explicitly vendors static assets for runtime.

### General engineering hygiene

- **Clear Documentation**: Keep `projects/lpg-ehl/README.md` and `projects/lpg-ehl/docs/*` accurate (setup + architecture + operations).
- **Environment Configuration**: Use env vars and config files; never commit secrets.
- **Version Control**: Prefer small, focused commits with meaningful messages.
- **Testing**: Match the repo’s test strategy (JUnit/Kotlin + Spring integration where it matters).
