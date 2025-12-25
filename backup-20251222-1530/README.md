# LPG-EHL Project Backup
**Created:** 2025-12-22 15:30 (CET)
**Branch:** feature/payment-pending-lifecycle-atomic-stop

## Contents

This backup contains the complete source code and configuration for the LPG-EHL project and the legacy Windows Dispenserkontroll application.

### LPG-EHL Modules

#### 1. lpg-ehl-core-20251222-1530.zip (73 KB)
**Core protocol library**
- EHL protocol implementation (codec, packet builder, commands)
- Dispenser state machine and status handling
- Domain models (DispenserStatus, DispenserState)
- Comprehensive test suite (151 tests)
- **Key Files:**
  - `src/main/kotlin/no/cloudberries/lpg/protocol/` - Protocol implementation
  - `src/test/kotlin/` - Unit and integration tests
  - `pom.xml` - Maven configuration

#### 2. lpg-ehl-emulator-20251222-1530.zip (27 KB)
**Dispenser hardware emulator**
- TCP server emulating LPG dispenser hardware (port 9000)
- Dual protocol support (EHL binary + legacy text commands)
- Transaction persistence to database via REST API
- Real-time fuel delivery simulation
- **Key Files:**
  - `src/main/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulator.kt` - Core emulation
  - `src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt` - TCP server
  - `src/main/kotlin/no/cloudberries/lpg/emulator/api/LpgApiClient.kt` - API client
  - `src/main/kotlin/no/cloudberries/lpg/emulator/service/TransactionPersistenceService.kt` - Transaction saving
  - `src/main/resources/application.yaml` - Configuration

#### 3. lpg-ehl-api-20251222-1530.zip (90 KB)
**REST API backend**
- Spring Boot REST API (port 8080)
- PostgreSQL database integration with Liquibase migrations
- Transaction management and reporting
- Payment gateway integration (simulated for dev)
- Azure Queue sync support
- **Key Files:**
  - `src/main/kotlin/no/cloudberries/lpg/api/controller/` - REST controllers
  - `src/main/kotlin/no/cloudberries/lpg/api/service/` - Business logic
  - `src/main/kotlin/no/cloudberries/lpg/api/model/` - JPA entities
  - `src/main/resources/db/changelog/` - Database migrations
  - `src/main/resources/application.yaml` - Configuration

#### 4. lpg-web-20251222-1530.zip (60 KB)
**React frontend**
- React + TypeScript + Vite
- Real-time transaction monitoring
- Dispenser simulator interface
- Protocol testing tools
- Reports and analytics
- **Key Files:**
  - `src/pages/` - Page components
  - `src/components/` - Reusable components
  - `src/api/` - API client modules
  - `package.json` - Dependencies
  - `vite.config.ts` - Build configuration

#### 5. lpg-ehl-config-docs-20251222-1530.zip (4.0 MB)
**Project configuration and documentation**
- Docker Compose configurations
- Dockerfiles for all services
- Comprehensive documentation
- Architecture diagrams
- Testing guides
- **Key Files:**
  - `docker-compose-local.yaml` - Local development stack
  - `README.md` - Project overview
  - `WARP.md` - AI agent context document
  - `docs/DATABASE_INTEGRATION_TESTING.md` - Testing guide
  - `docs/ARCHITECTURE_ANALYSIS.md` - Architecture documentation
  - `pom.xml` - Parent Maven POM

### Legacy Application

#### 6. CSharpConverted-V2-20251222-1530.zip (56 KB)
**Windows Dispenserkontroll application**
- Original VB6-based Windows application ported to C#
- Legacy protocol client connecting to emulator
- Customer management, reporting, and payment processing
- **Key Files:**
  - `DispenserkontrollForm.cs` - Main form
  - `TcpClientWrapper.cs` - TCP communication
  - `DbHelper.cs` - Database helper
  - `Dispenserkontroll.csproj` - Project file
  - `appsettings.json` - Configuration

## System Architecture

```
Windows Dispenserkontroll (Legacy Client - C#)
    ↓ TCP port 9000 (Legacy text commands)
lpg-ehl-emulator (Spring Boot - Kotlin)
    ├─ EhlDispenserEmulator (Protocol handler)
    ├─ EmulatorService (TCP server)
    └─ TransactionPersistenceService
        ↓ HTTP POST /api/v1/transactions
lpg-ehl-api (Spring Boot - Kotlin)
    ├─ TransactionController (REST endpoints)
    ├─ TransactionService (Business logic)
    └─ TransactionRepository (JPA)
        ↓ JDBC
PostgreSQL Database (Docker)
    └─ Liquibase migrations

lpg-web (React + Vite)
    └─ REST API client → lpg-ehl-api
```

## Technology Stack

### Backend
- **Language:** Kotlin 2.1.10
- **Framework:** Spring Boot 3.2.1
- **JVM:** Java 21 (via SDKMAN)
- **Build:** Maven 3.9+
- **Database:** PostgreSQL 16
- **Migrations:** Liquibase

### Frontend
- **Framework:** React 18
- **Language:** TypeScript
- **Build Tool:** Vite
- **Package Manager:** npm

### Infrastructure
- **Containerization:** Docker + Docker Compose
- **CI/CD:** Render.com (test deployment)
- **Storage:** Azure Queue (optional)

## Key Features Implemented

### PAYMENT_PENDING Lifecycle
- ✅ Atomic stop mechanism with race-free totals freezing
- ✅ State code 0x08 mapped to PAYMENT_PENDING
- ✅ UNBLOCK denial in PAYMENT_PENDING state
- ✅ Reset to IDLE functionality
- ✅ All 151 tests passing

### Database Persistence
- ✅ Transaction auto-save on STOP command
- ✅ Multi-dispenser support (addresses 1, 2, 3)
- ✅ Volume in deciliters, amount in øre (correct EHL units)
- ✅ REST API integration with proper error handling
- ✅ CORS configuration for frontend

### Security
- ✅ Spring Security with token auth (disabled for local dev)
- ✅ CORS whitelisting (localhost:3000, localhost:5173)
- ✅ SQL injection protection via JPA

## Running the System

### Prerequisites
- Docker + Docker Compose
- Java 21 (SDKMAN: `sdk use java 21.0.7-tem`)
- Node.js + npm (for frontend)

### Quick Start
```bash
# 1. Start database
docker-compose -f docker-compose-local.yaml up postgres -d

# 2. Start API (in IntelliJ or terminal)
cd lpg-ehl-api && ./mvnw spring-boot:run

# 3. Start Emulator (in IntelliJ or terminal)
cd lpg-ehl-emulator && ./mvnw spring-boot:run

# 4. Start Frontend
cd lpg-web && npm install && npm run dev
```

### Verify
- API: http://localhost:8080/actuator/health
- Emulator: TCP port 9000 (accepts Windows client)
- Frontend: http://localhost:5173

## Testing Guide

See `lpg-ehl-config-docs-20251222-1530.zip/docs/DATABASE_INTEGRATION_TESTING.md` for comprehensive testing instructions.

## Database Connection

**Development:**
- Host: localhost:5432
- Database: lpg_ehl
- User: lpg_user
- Password: lpg_dev_password

## Git Information

**Repository:** https://github.com/thomasandersen77/LPG-EHL-core.git
**Branch:** feature/payment-pending-lifecycle-atomic-stop
**Latest Commit:** da94a86 (Fix negative repeat count in log formatting)

## Notes

- All builds exclude `target/`, `node_modules/`, and build artifacts
- Configuration files use environment variables with sensible defaults
- The emulator can run standalone or integrate with full stack
- Frontend is optional - API and emulator work independently

## Contact

For questions or support, refer to the WARP.md file in the config-docs archive.

---

**Archive Created:** 2025-12-22 15:30:00 CET
**Total Size:** ~4.3 MB (all archives combined)
