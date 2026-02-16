# API Parity Refactoring Report

**Date:** 2026-02-08  
**Branch:** `refactor/api-parity-maven`  
**Status:** ✅ Complete

## Executive Summary

Successfully refactored the Maven multi-module LPG-EHL project to achieve API parity between the WebApp and Headless applications. This enables field engineers to debug and test the REST API in production environments using the headless application with a debug-api profile, without needing the full webapp deployment.

## Table of Contents

1. [Motivation](#motivation)
2. [Architecture Changes](#architecture-changes)
3. [Implementation Details](#implementation-details)
4. [Module Structure](#module-structure)
5. [Usage Guide](#usage-guide)
6. [Testing & Verification](#testing--verification)
7. [Breaking Changes](#breaking-changes)
8. [Migration Guide](#migration-guide)

---

## Motivation

### Problem Statement

**Before refactoring:**
- REST controllers duplicated across webapp and emulator modules
- Headless application had no API (runs as background daemon only)
- No way to test/debug API in field without full webapp
- DTOs duplicated in multiple modules
- Circular dependency risks between modules

**Goals:**
- Single source of truth for REST API controllers
- Enable headless to expose same API as webapp (on-demand via profile)
- Clean module dependencies (no circular dependencies)
- Maintain existing functionality and tests

---

## Architecture Changes

### Before

```
lpg-ehl-parent/
├── lpg-ehl-core/          # Protocol (no Spring)
├── lpg-ehl-service/       # Business logic + DTOs (mixed)
├── lpg-ehl-webapp/        # REST controllers + React + Config
└── lpg-ehl-app-headless/  # Headless daemon (no web)
```

**Issues:**
- Controllers in webapp only
- DTOs scattered in service and webapp
- Headless cannot expose API

### After

```
lpg-ehl-parent/
├── lpg-ehl-core/          # Protocol (no Spring)
├── lpg-ehl-service/       # Business logic + DTOs
├── lpg-ehl-api/           # ✨ NEW: REST controllers (shared)
├── lpg-ehl-webapp/        # Config + React (thin wrapper)
└── lpg-ehl-app-headless/  # Headless + optional API
```

**Benefits:**
- Controllers centralized in API module
- DTOs remain in service (avoids circular deps)
- Both webapp and headless import API module
- Headless can enable API via `debug-api` profile

---

## Implementation Details

### 1. New Module: lpg-ehl-api

**Purpose:** Library module containing REST controllers only

**Location:** `lpg-ehl-api/`

**Dependencies:**
- `lpg-ehl-service` - Business logic and DTOs
- `lpg-ehl-core` - Protocol types
- `lpg-ehl-emulator` - Emulator interface (LAB mode)
- `spring-boot-starter-web` - REST support
- `springdoc-openapi` - OpenAPI/Swagger

**Packaging:** JAR (library, not executable)

**POM:**
```xml
<artifactId>lpg-ehl-api</artifactId>
<packaging>jar</packaging>

<dependencies>
    <dependency>
        <groupId>no.cloudberries.lpg</groupId>
        <artifactId>lpg-ehl-service</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

### 2. Controllers Moved (15 total)

All REST controllers moved from `lpg-ehl-webapp` to `lpg-ehl-api`:

| Controller | Endpoint | Purpose |
|------------|----------|---------|
| `PaymentController` | `/api/v1/payments` | Payment processing |
| `TransactionController` | `/api/v1/transactions` | Transaction CRUD |
| `PriceController` | `/api/v1/prices` | Price management |
| `RoadTaxController` | `/api/v1/road-tax` | Road tax settings |
| `DemoDispenserController` | `/api/v1/dispenser` | Demo dispenser control |
| `DispenserController` | `/api/v1/dispensers` | Real dispenser control |
| `SyncController` | `/api/v1/sync` | Azure sync |
| `ProtocolTestController` | `/api/v1/protocol-test` | Protocol testing |
| `DiagnosticsController` | `/api/v1/diagnostics` | System diagnostics |
| `SerialDebugController` | `/api/debug/serial` | Serial port debugging |
| `ConfigController` | `/api/v1/config` | Configuration |
| `ReportsController` | `/api/v1/reports` | Reporting |
| `CreditController` | `/credit` | Credit management |
| `EmulatorController` | `/api/v1/emulator` | Emulator control |
| `DemoTransactionController` | `/api/v1/demo-transactions` | Demo transactions |

**Supporting Service:**
- `MockPlsService` - Mock price list system (LAB mode)

### 3. DTO Architecture

**Decision:** DTOs remain in `lpg-ehl-service`

**Rationale:**
- Avoids circular dependency (API ← Service, Service ← API ❌)
- Service defines the contract (DTOs)
- API implements REST layer using service DTOs
- Clean dependency flow: Core ← Service ← API

**Package:** `no.cloudberries.lpg.service.dto`

**DTOs:**
- `TransactionResponse`
- `PageResponse<T>`
- `CreateTransactionRequest`
- `DispenserStatusResponse`
- `DailySummaryResponse`
- `PeriodSummaryResponse`
- `SyncStatusResponse`
- `AzureQueueMessageDto`
- `ErrorResponse`

### 4. WebApp Changes

**lpg-ehl-webapp/pom.xml:**
```xml
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-ehl-api</artifactId>
</dependency>
```

**Removed:**
- All `*Controller.kt` files (moved to API)
- `api/dto/ApiResponses.kt` (DTOs in service)
- `api/pls/MockPlsService.kt` (moved to API)

**Kept:**
- Configuration classes (`SecurityConfig`, `WebSocketConfig`, etc.)
- React frontend (static resources)
- Application entry point

**Result:** WebApp is now a thin wrapper that:
1. Imports API controllers
2. Serves React frontend
3. Configures security/CORS
4. Manages WebSocket for real-time logs

### 5. Headless Changes

**lpg-ehl-app-headless/pom.xml:**
```xml
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-ehl-api</artifactId>
</dependency>
```

**HeadlessApplication.kt:**
```kotlin
@ComponentScan(
    basePackages = [
        "no.cloudberries.lpg.headless",
        "no.cloudberries.lpg.api",        // ← Added
        "no.cloudberries.lpg.service",
        "no.cloudberries.lpg.communication",
        "no.cloudberries.lpg.transport",
        "no.cloudberries.lpg.pls",
        "no.cloudberries.lpg.credit",     // ← Added
        "no.cloudberries.lpg.emulator"    // ← Added
    ]
)
```

**application.yaml (default):**
```yaml
spring:
  main:
    web-application-type: none  # No web server by default
```

**application-debug-api.yaml (new):**
```yaml
spring:
  main:
    web-application-type: servlet  # Enable web server

server:
  port: 8090  # Avoid conflict with webapp (8080)
  shutdown: graceful

logging:
  level:
    no.cloudberries.lpg: DEBUG
```

**Result:** Headless can now:
- Run without web server (default)
- Enable API via `--spring.profiles.active=debug-api`
- Expose identical API as webapp on port 8090

---

## Module Structure

### Dependency Graph

```
┌─────────────────────────────────────────────┐
│           lpg-ehl-webapp (8080)             │
│         (Config + React frontend)           │
└──────────────────┬──────────────────────────┘
                   │
                   ├──────────────────────────┐
                   │                          │
┌──────────────────▼──────────────────────┐   │
│         lpg-ehl-app-headless             │   │
│      (Headless + optional debug-api)     │   │
└──────────────────┬──────────────────────┘   │
                   │                          │
                   │                          │
                   │    ┌─────────────────────▼───┐
                   │    │     lpg-ehl-api          │
                   │    │  (REST Controllers)      │
                   │    └─────────────┬────────────┘
                   │                  │
                   │    ┌─────────────▼────────────┐
                   └────►    lpg-ehl-service       │
                        │ (Business Logic + DTOs)  │
                        └─────────────┬────────────┘
                                      │
                        ┌─────────────┼────────────┐
                        │             │            │
                ┌───────▼──────┐  ┌──▼────────┐  ┌▼───────────┐
                │ lpg-ehl-core │  │lpg-       │  │lpg-ehl-    │
                │  (Protocol)  │  │transport  │  │emulator    │
                └──────────────┘  └───────────┘  └────────────┘
```

### Module Responsibilities

| Module | Responsibility | Spring? | Web? |
|--------|---------------|---------|------|
| `lpg-ehl-core` | EHL protocol, packets, codecs | ❌ No | ❌ No |
| `lpg-transport` | Serial/TCP transport | ✅ Yes | ❌ No |
| `lpg-ehl-service` | Business logic, JPA, DTOs | ✅ Yes | ❌ No |
| `lpg-ehl-emulator` | Dispenser simulator | ✅ Yes | ❌ No |
| **`lpg-ehl-api`** | **REST controllers** | **✅ Yes** | **✅ Yes** |
| `lpg-ehl-webapp` | Config, security, React | ✅ Yes | ✅ Yes |
| `lpg-ehl-app-headless` | Headless daemon | ✅ Yes | ⚠️ Optional |

---

## Usage Guide

### 1. WebApp (Normal Mode)

**Start:**
```bash
cd lpg-ehl-webapp
mvn spring-boot:run
```

**Access:**
- Frontend: http://localhost:8080
- API: http://localhost:8080/api/v1/*
- H2 Console: http://localhost:8080/h2-console
- Swagger: http://localhost:8080/swagger-ui.html

**Configuration:** `lpg-ehl-webapp/src/main/resources/application.yaml`

### 2. Headless (Daemon Mode - Default)

**Start:**
```bash
cd lpg-ehl-app-headless
mvn spring-boot:run
```

**Behavior:**
- Runs without web server
- Polls dispenser via serial port
- Saves transactions to database
- No HTTP endpoints exposed

**Configuration:** `lpg-ehl-app-headless/src/main/resources/application.yaml`

### 3. Headless (Debug API Mode) ✨ NEW

**Start:**
```bash
cd lpg-ehl-app-headless
mvn spring-boot:run -Dspring-boot.run.profiles=lab,debug-api
```

**Or with JAR:**
```bash
java -jar lpg-ehl-app-headless-*.jar \
  --spring.profiles.active=lab,debug-api
```

**Access:**
- API: http://localhost:8090/api/v1/*
- H2 Console: http://localhost:8090/h2-console
- Swagger: http://localhost:8090/swagger-ui.html

**Configuration:** `lpg-ehl-app-headless/src/main/resources/application-debug-api.yaml`

**Use Cases:**
- Field debugging with curl
- Test API without frontend
- Remote API access in production
- CI/CD integration testing

### 4. Profile Combinations

| Profiles | WebApp | Headless | Use Case |
|----------|--------|----------|----------|
| `local` | Web + LAB | Daemon | Local development |
| `lab` | Web + LAB | Daemon | Lab testing |
| `lab,debug-api` | - | API + LAB | Headless API testing |
| `field` | Web + Serial | Daemon | Production with web |
| `field,debug-api` | - | API + Serial | Production headless with API |

---

## Testing & Verification

### 1. Build & Test

```bash
# Full build
mvn clean install

# All tests
mvn test

# Specific module
mvn test -pl lpg-ehl-api
```

**Results:**
- ✅ All 29+ tests passing
- ✅ No compilation errors
- ✅ No circular dependencies

### 2. API Endpoint Verification

#### Common Endpoints (Both Apps)

**Health Check:**
```bash
# WebApp
curl http://localhost:8080/actuator/health

# Headless (debug-api)
curl http://localhost:8090/actuator/health
```

**Transactions:**
```bash
# List transactions
curl http://localhost:8080/api/v1/transactions
curl http://localhost:8090/api/v1/transactions

# Get by ID
curl http://localhost:8080/api/v1/transactions/{id}
curl http://localhost:8090/api/v1/transactions/{id}

# Create transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "dispenserAddress": 1,
    "nozzleNumber": 1,
    "volumeDeciliters": 250,
    "amountOre": 3975,
    "pricePerLiter": 1590,
    "paymentType": "CARD",
    "productCode": "LPG"
  }'
```

**Prices:**
```bash
# Get current prices
curl http://localhost:8080/api/v1/prices
curl http://localhost:8090/api/v1/prices

# Update price (admin)
curl -X POST http://localhost:8080/api/v1/prices/update \
  -H "Content-Type: application/json" \
  -d '{"pricePerLiter": 16.50}'
```

**Dispenser Control:**
```bash
# Get dispenser state
curl http://localhost:8080/api/v1/dispenser/state
curl http://localhost:8090/api/v1/dispenser/state

# Unblock dispenser (start pumping)
curl -X POST http://localhost:8080/api/v1/dispenser/unblock?paymentType=CARD
curl -X POST http://localhost:8090/api/v1/dispenser/unblock?paymentType=CARD

# Stop delivery
curl -X POST http://localhost:8080/api/v1/dispenser/stop
curl -X POST http://localhost:8090/api/v1/dispenser/stop

# Settle payment
curl -X POST http://localhost:8080/api/v1/dispenser/settle?paymentMethod=CARD
curl -X POST http://localhost:8090/api/v1/dispenser/settle?paymentMethod=CARD
```

### 3. Database Verification

#### H2 Console Access

**WebApp:**
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:lpgdb
Username: sa
Password: (empty)
```

**Headless (debug-api):**
```
URL: http://localhost:8090/h2-console
JDBC URL: jdbc:h2:mem:lpgdb
Username: sa
Password: (empty)
```

#### SQL Queries

```sql
-- View all transactions
SELECT * FROM transactions ORDER BY timestamp DESC;

-- View price history
SELECT * FROM price_history ORDER BY effective_from DESC;

-- View dispenser status
SELECT * FROM dispenser_status;

-- View pending transactions
SELECT * FROM transactions WHERE payment_status = 'PENDING';
```

### 4. Clean Database Before Testing

```bash
# Stop all apps
# Delete H2 database files (if file-based)
rm -rf data/lpgdb.*

# Or use H2 console to run:
DROP ALL OBJECTS;
```

### 5. API Parity Test

**Location:** `lpg-ehl-api/src/test/kotlin/no/cloudberries/lpg/api/ApiParityTest.kt`

**Purpose:**
- Verifies all API endpoints are registered
- Checks controller packages are scanned
- Validates HTTP methods
- Ensures core endpoints exist

**Run:**
```bash
mvn test -pl lpg-ehl-api -Dtest=ApiParityTest
```

---

## Breaking Changes

### ❌ None!

This refactoring is **fully backward compatible**:

- ✅ All existing API endpoints unchanged
- ✅ All URL paths identical
- ✅ All HTTP methods unchanged
- ✅ All request/response DTOs unchanged
- ✅ All existing tests pass
- ✅ WebApp behavior identical
- ✅ Headless behavior unchanged (when not using debug-api)

### New Features

- ✅ Headless can now expose API (opt-in via profile)
- ✅ Centralized controller management
- ✅ Cleaner module structure

---

## Migration Guide

### For Developers

**No code changes required!**

**New workflow:**
1. Pull latest from `refactor/api-parity-maven` branch
2. Run `mvn clean install`
3. Controllers are now in `lpg-ehl-api` module
4. Everything else works as before

### For DevOps

**WebApp deployment - unchanged:**
```bash
java -jar lpg-ehl-webapp-*.jar --spring.profiles.active=field
```

**Headless deployment - unchanged:**
```bash
java -jar lpg-ehl-app-headless-*.jar --spring.profiles.active=field
```

**New: Headless with API (for debugging):**
```bash
java -jar lpg-ehl-app-headless-*.jar \
  --spring.profiles.active=field,debug-api \
  --server.port=8090
```

### For Frontend Developers

**No changes required!**

- Frontend still connects to `http://localhost:8080/api/v1/*`
- All endpoints remain the same
- WebSocket still works

### For API Clients

**No changes required!**

- All URLs unchanged
- All request/response formats unchanged
- Authentication unchanged

---

## Architecture Decision Records

### ADR-001: DTOs in Service Layer

**Decision:** Keep DTOs in `lpg-ehl-service`, not `lpg-ehl-api`

**Rationale:**
- Avoids circular dependency (API depends on Service)
- Service defines the contract
- API implements the transport layer
- Clean dependency flow

**Alternatives Considered:**
1. ❌ DTOs in API - Creates circular dependency
2. ❌ Separate DTO module - Adds complexity
3. ✅ DTOs in Service - Clean and simple

### ADR-002: Debug API Profile for Headless

**Decision:** Use Spring profile to enable/disable web server in headless

**Rationale:**
- Single JAR for both modes
- No code duplication
- Easy to toggle in production
- Minimal configuration

**Alternatives Considered:**
1. ❌ Separate headless-api JAR - Duplication
2. ❌ Always enable web - Wastes resources
3. ✅ Profile-based - Flexible and efficient

### ADR-003: Port 8090 for Headless Debug API

**Decision:** Use port 8090 for headless debug-api mode

**Rationale:**
- Avoids conflict with webapp (8080)
- Easy to remember (8080 + 10)
- Standard development port range

---

## Troubleshooting

### Issue: Controllers not found

**Symptom:** 404 errors for API endpoints

**Solution:** Verify ComponentScan includes API package:
```kotlin
@ComponentScan(basePackages = ["no.cloudberries.lpg.api", ...])
```

### Issue: Circular dependency

**Symptom:** Build fails with circular dependency error

**Solution:** Verify:
- Service does NOT depend on API
- API depends on Service (one-way)
- DTOs are in Service, not API

### Issue: Headless web server not starting

**Symptom:** Headless runs without API despite debug-api profile

**Solution:**
```bash
# Verify profile is active
java -jar lpg-ehl-app-headless-*.jar \
  --spring.profiles.active=lab,debug-api \
  --debug

# Check application-debug-api.yaml exists
# Verify web-application-type: servlet
```

### Issue: Port 8090 already in use

**Symptom:** Headless fails to start with "port already in use"

**Solution:**
```bash
# Use different port
java -jar lpg-ehl-app-headless-*.jar \
  --spring.profiles.active=lab,debug-api \
  --server.port=8091

# Or kill process on 8090
lsof -ti:8090 | xargs kill -9
```

---

## Future Enhancements

### Potential Improvements

1. **OpenAPI Contract Testing**
   - Generate OpenAPI spec from controllers
   - Compare webapp vs headless specs
   - Automated contract validation

2. **API Versioning**
   - Implement versioning strategy
   - Support multiple API versions
   - Deprecation policy

3. **Performance Monitoring**
   - Add metrics for API calls
   - Monitor response times
   - Track error rates

4. **Security Enhancements**
   - API key authentication
   - Rate limiting
   - CORS configuration per environment

---

## Summary

✅ **Successfully completed** Maven API parity refactoring

**Benefits:**
- Centralized REST API in shared module
- WebApp and Headless expose identical API
- Field engineers can debug with headless + debug-api
- Clean module architecture
- Zero breaking changes

**Modules:**
- ✨ **New:** `lpg-ehl-api` - REST controllers
- ✅ **Updated:** `lpg-ehl-webapp` - Thin wrapper
- ✅ **Updated:** `lpg-ehl-app-headless` - Optional API

**Next Steps:**
1. Merge to `main` branch
2. Update CI/CD pipelines
3. Document for team
4. Deploy to staging
5. Verify in production

---

## References

- [Spring Boot Multi-Module](https://spring.io/guides/gs/multi-module/)
- [Maven Multi-Module](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [API Design Best Practices](https://swagger.io/resources/articles/best-practices-in-api-design/)

---

**Report Generated:** 2026-02-08  
**Author:** WARP AI Agent  
**Co-Authored-By:** Warp <agent@warp.dev>
