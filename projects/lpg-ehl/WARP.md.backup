# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

LPG-EHL is a multi-module Maven project that implements the EHL (European Hexadecimal Language) protocol for controlling LPG dispensers via RS-485 communication. This modernizes legacy Visual Basic 6 code into a cloud-native architecture with REST API, database persistence, and Azure integration.

**Legacy VB6 Source**: Original Visual Basic 6 codebase is preserved in `./norgesgass_legacy/` for reference.

## Module Structure

```
lpg-ehl/
├── lpg-ehl-core/          # Protocol implementation library (Kotlin)
├── lpg-ehl-emulator/      # TCP-based dispenser emulator for testing
├── lpg-ehl-api/           # Spring Boot REST API with database & Azure sync
└── lpg-web/               # React frontend with Vite + TypeScript
```

### Module Dependencies
- **lpg-ehl-api** depends on **lpg-ehl-core** (protocol library)
- **lpg-ehl-emulator** depends on **lpg-ehl-core** (protocol library)
- **lpg-web** communicates with **lpg-ehl-api** via HTTP/REST
- All modules share parent POM for dependency management

## Development Environment Setup

### Required Tools
- **Java 21.0.7-tem** (Temurin)
- **Maven 3.9.11**
- **SDKMAN** (for automatic version switching)
- **Node.js 18+** (for frontend)
- **Docker & Docker Compose** (recommended)

### Initial Setup

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/sdkman-init.sh"

# Auto-install correct Java/Maven versions
cd lpg-ehl-core
sdk env install

# Install Node.js (if not present)
brew install node  # macOS
# or use nvm
```

SDKMAN will automatically switch to Java 21.0.7-tem and Maven 3.9.11 when entering the `lpg-ehl-core/` directory.

## Common Development Commands

### Build All Modules
```bash
# Full build with tests (61 tests)
mvn clean install

# Build without tests (faster)
mvn clean package -DskipTests

# Build specific module
mvn clean install -pl lpg-ehl-core
mvn clean install -pl lpg-ehl-api
```

### Running Tests
```bash
# Run all tests
mvn test

# Test specific module
mvn test -pl lpg-ehl-core
mvn test -pl lpg-ehl-api
mvn test -pl lpg-ehl-emulator

# Run specific test class
mvn test -Dtest=EhlCodecTest
mvn test -Dtest=TransactionTest#testValidStateTransitions

# Integration tests (uses Testcontainers)
mvn verify -pl lpg-ehl-api
```

### Local Development with Docker Compose

**Full Stack (Recommended)**:
```bash
# Start all services: postgres + azurite + emulator + api + frontend + wiremock
docker-compose -f docker-compose-local.yaml up
```

**Cloud Simulation (MinLPG)**:
This project (`lpg-ehl`) acts as the Edge/Pump system. To simulate the full Edge-to-Cloud flow, you can run the `MinLPG` project (Admin System) in parallel.

1. Start `lpg-ehl` (provides Azurite on port 10001).
2. Start `MinLPG` in a separate terminal:
   ```bash
   cd ../MinLPG
   docker-compose up
   ```
   *MinLPG will connect to the Azurite instance running in this project.*

**Access Points:**
- Pump System: http://localhost:3000 (Frontend), http://localhost:8080 (API)
- Admin System: http://localhost:3001 (Frontend), http://localhost:8081 (API)
# Start in background
docker-compose -f docker-compose-local.yaml up -d

# View logs
docker-compose -f docker-compose-local.yaml logs -f
docker-compose -f docker-compose-local.yaml logs -f api

# Stop all
docker-compose -f docker-compose-local.yaml down

# Rebuild after code changes
docker-compose -f docker-compose-local.yaml up --build
```

**Hybrid Development** (Infrastructure in Docker, API/Frontend local):
```bash
# Start only database, Azure emulator, and dispenser emulator
docker-compose -f docker-compose-local.yaml up postgres azurite emulator -d

# Then run API locally
cd lpg-ehl-api
mvn spring-boot:run -Dspring-boot.run.profiles=local

# And frontend locally
cd lpg-web
npm install  # first time only
npm run dev
```

### Makefile Commands

The root `Makefile` provides convenient shortcuts:

```bash
make help              # Show all commands
make build             # Build all modules
make test              # Run all tests
make run-local         # Start docker-compose-local.yaml
make run-api           # Run API locally (requires DB)
make stop              # Stop all containers
make clean             # Clean build artifacts
make docker-up         # Start in detached mode
make docker-down       # Stop and remove containers
make logs              # Tail all logs
```

### Frontend Development

```bash
cd lpg-web

# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Lint
npm run lint

# Configure API URL (optional)
echo "VITE_API_URL=http://localhost:8080/api/v1" > .env.local
```

## Service Ports Reference

| Port | Service | URL/Connection |
|------|---------|----------------|
| 3000 | Frontend | http://localhost:3000 |
| 5173 | Frontend (dev) | http://localhost:5173 |
| 5432 | PostgreSQL | jdbc:postgresql://localhost:5432/lpg_ehl |
| 8080 | API | http://localhost:8080 |
| 8081 | WireMock | http://localhost:8081 |
| 9000 | Emulator | tcp://localhost:9000 (EHL protocol) |
| 10001 | Azurite | http://localhost:10001 (Azure Queue emulator) |

**Default credentials**:
- Database: `lpg_user` / `lpg_dev_password`
- API Token: `dev-token-12345`

## Architecture Overview

### Communication Flow

```
React Frontend (lpg-web)
    ↓ HTTP/REST
Spring Boot API (lpg-ehl-api)
    ↓ EHL Protocol over TCP
TCP Emulator (lpg-ehl-emulator) or Real RS-485 Dispenser
    ↑
Uses EHL Protocol Library (lpg-ehl-core)
```

### Data Flow

```
Frontend ──HTTP──> API ──JDBC──> PostgreSQL (master database)
                   │                   │
                   │                   │ DB trigger
                   │                   ↓
                   │            azure_sync_queue (outbox pattern)
                   │                   │
                   └─ Scheduled Job ───┘
                            │
                            ↓
                   Azure Storage Queue ──> Azure Function (reporting)
```

### Module Responsibilities

**lpg-ehl-core**: Pure protocol library
- Packet encoding/decoding (EHL binary format)
- Transaction state machine (9 states: NOT_STARTED → READY → ACTIVE → FINISHED → ACCOUNTED)
- Sealed result types for error handling
- No I/O dependencies (testable)
- See `lpg-ehl-core/WARP.md` for detailed protocol documentation

**lpg-ehl-emulator**: Testing simulator
- TCP server listening on port 9000
- Simulates dispenser state machine (IDLE → DELIVERING → FINISHED)
- Configurable fuel flow rate (default 0.5 L/s)
- Responds to STATE, UNBLOCK, STOP, VOLUME commands

**lpg-ehl-api**: Spring Boot service
- REST API with OpenAPI/Swagger documentation
- Bearer token authentication (except health endpoint)
- JPA/PostgreSQL persistence with JSONB support
- Azure Storage Queue integration with exponential backoff retry
- Scheduled sync job (default 30s interval)
- Testcontainers-based integration tests
- See `lpg-ehl-api/README.md` for API endpoint details

**lpg-web**: React frontend
- Vite + TypeScript + Tailwind CSS
- TanStack Query for data caching
- Real-time dispenser status updates (500ms polling during delivery)
- Interactive pump simulator UI

## Key Patterns and Conventions

### Protocol Layer (lpg-ehl-core)

**EHL Packet Structure**:
```
STX (0x20) | Length | Address | Command | Data (0-n) | Checksum (XOR) | ETX (0x36)
```

**Sealed Result Types** (never throw exceptions for protocol errors):
```kotlin
when (val result = EhlCodec.decode(bytes)) {
    is EhlPacketParseResult.Success -> // handle packet
    is EhlPacketParseResult.ChecksumError -> // handle error
    is EhlPacketParseResult.InvalidFormat -> // handle error
    is EhlPacketParseResult.Incomplete -> // need more data
}
```

**Builder Pattern** for common operations:
```kotlin
EhlPacketBuilder.createStateQuery(address = 1)
EhlPacketBuilder.createPriceProgram(address = 1, price = "15.90")
EhlPacketBuilder.createValuePreset(address = 1, amount = 50000)
```

### Transaction State Machine

Valid state transitions (others return `false`):
- NOT_STARTED → READY
- READY → ACTIVE or ANNULATED
- ACTIVE → FINISHED or UNACCOUNTED
- FINISHED → ACCOUNTED or FINANCIAL_RETURN
- UNACCOUNTED → ACCOUNTED
- FINANCIAL_RETURN → ACCOUNTED

### API Layer (lpg-ehl-api)

**Outbox Pattern** for Azure Sync:
1. Transaction saved to `transactions` table
2. Database trigger creates entry in `azure_sync_queue`
3. Scheduled job processes pending items
4. On failure: exponential backoff (30s, 60s, 120s, ...)
5. After max retries: marked as FAILED

**Authentication**:
- All endpoints except `/actuator/health` require `Authorization: Bearer <token>`
- Use `API_AUTH_TOKEN` environment variable (default: `dev-token-12345`)

**Profiles**:
- `default`: Production (real Azure, real serial port)
- `local`: Local development (Azurite, TCP emulator)
- `test`: Integration tests (Testcontainers)

### Database Schema

Key tables:
- `transactions`: Master fuel delivery records
- `protocol_events`: Detailed EHL communication log
- `system_events`: Application health/errors
- `dispenser_status`: Current state per dispenser
- `azure_sync_queue`: Outbox for cloud sync
- `daily_summary` (view): Reporting
- `unsynced_transactions` (view): Sync monitoring

See `init-db.sql` for complete schema.

## Testing Strategy

### Unit Tests (lpg-ehl-core)
- Packet encoding/decoding round-trip validation
- Checksum calculation
- Transaction state transitions (valid and invalid)
- No external dependencies

### Integration Tests (lpg-ehl-emulator)
- Complete delivery cycle with emulator
- Multi-command sequences
- Volume queries during delivery

### API Integration Tests (lpg-ehl-api)
- Uses Testcontainers for real PostgreSQL
- WireMock for external API mocks
- Full request/response cycle testing

### Frontend Testing
- Manual testing via pump simulator UI
- API integration via Axios client

## Debugging Tips

### Run API in Debug Mode (IntelliJ)

**Run → Edit Configurations → Spring Boot**:
```
Name: API
Module: lpg-ehl-api
Main class: no.cloudberries.lpg.api.LpgEhlApiApplicationKt
Active profiles: local
Environment variables:
  DB_HOST=localhost
  DB_PORT=5432
  DB_NAME=lpg_ehl
  DB_USER=lpg_user
  DB_PASSWORD=lpg_dev_password
  EMULATOR_HOST=localhost
  EMULATOR_PORT=9000
  AZURE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://localhost:10001/devstoreaccount1;
```

### Test EHL Protocol Directly

Send raw EHL commands to emulator via netcat:
```bash
# STATE query for dispenser address 1
# STX(20) LEN(04) ADDR(01) CMD(75) CHK(50) ETX(36)
echo -ne '\x20\x04\x01\x75\x50\x36' | nc localhost 9000 | xxd
```

### Database Access

```bash
# Connect to database
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl

# Useful queries
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10;
SELECT * FROM dispenser_status;
SELECT * FROM unsynced_transactions;
SELECT * FROM daily_summary;
```

### View Azure Queue (Azurite)

```bash
# Use Azure Storage Explorer GUI (connect to localhost:10001)
# Or Azure CLI
az storage queue list --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://localhost:10001/devstoreaccount1;"
```

## Common Issues

### "Port already in use"
```bash
# Find process using port
lsof -i :8080  # or :3000, :5432, etc.
kill -9 <PID>
```

### Database connection refused
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Start if needed
docker-compose -f docker-compose-local.yaml up postgres -d
```

### API cannot connect to emulator
```bash
# Test emulator is listening
nc -zv localhost 9000

# View emulator logs
docker-compose -f docker-compose-local.yaml logs emulator
```

### Frontend CORS errors
Verify API configuration includes frontend origins:
```yaml
# application-local.yaml
cors.allowed-origins: http://localhost:5173,http://localhost:3000
```

## Production Deployment

See `README.md` and `DOCKER-COMPOSE-README.md` for full production deployment guide on pump Linux machines with real RS-485 hardware.

**Key differences from local development**:
- Real Azure Storage Queue connection string
- Serial port: `/dev/ttyUSB0` (RS-485 adapter)
- Real dispenser address (configured per installation)
- Production price per liter
- Automatic backups to `/opt/lpg-ehl/backups/`

## Documentation Links

- `README.md`: Full project documentation
- `QUICKSTART.md`: 2-minute Docker Compose quick start
- `DEVELOPER_GUIDE.md`: Detailed local development guide (Norwegian)
- `DOCKER-COMPOSE-README.md`: Docker deployment guide
- `lpg-ehl-core/WARP.md`: Core protocol implementation details
- `lpg-ehl-core/README.md`: Protocol reference and examples
- `lpg-ehl-api/README.md`: API endpoints and configuration
- `lpg-web/README.md`: Frontend architecture
- `openapi.yaml`: REST API specification

## Important Rules

- Always use Java 21.0.7-tem via SDKMAN (automatic in lpg-ehl-core/)
- Never commit `.env.local` files (use `.env.example` as template)
- Use sealed result types instead of exceptions for protocol errors
- Maintain transaction state machine integrity (invalid transitions return false)
- Format byte arrays as hex: `"%02X".format(byte)`
- Monetary amounts in øre/cents (Int), volumes in liters (Float)
- Use KDoc comments for public APIs
- Log protocol operations with SLF4J
- All API endpoints (except health) require Bearer token authentication
