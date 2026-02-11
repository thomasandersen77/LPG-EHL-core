# Plan: Logback og MDC Actor

## 1. Logback – hvilke moduler trenger det?

| Modul | Type | Logback? | Begrunnelse |
|-------|------|----------|--------------|
| **lpg-ehl-webapp** | Executable | ✅ logback-spring.xml | Inkluderer core + WebSocket |
| **lpg-ehl-app-headless** | Executable | ✅ logback-spring.xml | Inkluderer core |
| **lpg-ehl-serialport-sim** | Executable | ✅ logback.xml | Standalone pls-sim.jar |
| **lpg-ehl-core** | Library | ✅ logback-include.xml | **Felles fragment** – inkluderes av webapp/headless |
| **lpg-ehl-service** | Library | ❌ IKKE NØDVENDIG | Logger konfigureres av root i executable |
| **lpg-transport** | Library | ❌ IKKE NØDVENDIG | `no.cloudberries.lpg.communication` allerede i core-include |

**Konklusjon:** Kun executable-moduler (webapp, headless, serialport-sim) trenger logback.
Core har `logback-include.xml` fordi den er en **delbar fragment**.
Service og transport trenger IKKE egne logback-filer – de er kun dependencies.

---

## 2. MDC Actor – implementeringsplan

### Aktører
- **OPERATOR** – Stasjonseier (manuell unblock, config, admin)
- **DEBUG** – Felt-debugging via curl
- **CUSTOMER** – Kunde ved pumpe (kortdragning → UNBLOCK)
- **SYSTEM** – Automatisk polling, Azure sync

### Komponenter
1. **logback-include.xml** – Legg til `%X{actor}` i pattern
2. **MdcActorFilter** (lpg-ehl-api) – Setter actor for HTTP requests basert på path
3. **MdcActor** (lpg-ehl-core) – Utility for å kjøre kode med actor satt
4. **HeadlessPollingService** – Bruk MdcActor for SYSTEM og CUSTOMER
5. **AzureSyncService** – Bruk MdcActor for SYSTEM

### Mapping
| Kilde | Actor |
|-------|-------|
| HTTP `/api/debug/*` | DEBUG |
| HTTP `/api/v1/*` | OPERATOR |
| HTTP `/actuator/*` | SYSTEM |
| HeadlessPollingService (polling) | SYSTEM |
| HeadlessPollingService (processPendingAuthorizations) | CUSTOMER |
| AzureSyncService | SYSTEM |

---

## 3. Effort
- Logback: 0 (ingen endringer nødvendig)
- MDC Actor: **S** (1–2 timer) – **IMPLEMENTERT**

## 4. Implementert (2026-02-07)

### Endrede filer
- `lpg-ehl-core/src/main/resources/logback-include.xml` – Lagt til `[%X{actor:--}]` i CONSOLE, FILE og PROTOCOL patterns
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/logging/MdcActor.kt` – Ny utility
- `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/logging/MdcActorFilter.kt` – Ny filter
- `lpg-ehl-app-headless/.../HeadlessPollingService.kt` – SYSTEM for polling, CUSTOMER for auth
- `lpg-ehl-service/.../AzureSyncService.kt` – SYSTEM for sync

### Eksempel på loggoutput
```
12:34:56 INFO  [SYSTEM]   n.c.lpg.headless.service - 🚀 Starting dispenser polling loop...
12:35:12 INFO  [CUSTOMER] n.c.lpg.headless.service - 💳 PENDING autorisasjon funnet: ...
12:35:13 INFO  [OPERATOR] n.c.lpg.api.controller - 🔓 FRI PUMPE: Unblock request for address 1
12:35:45 DEBUG [DEBUG]    n.c.lpg.headless.debug - Serial scan: addr 1 → RESPONSE
```
