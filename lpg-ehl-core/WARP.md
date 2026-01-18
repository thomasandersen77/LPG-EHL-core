# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

LPG-EHL Core is a Kotlin implementation of the EHL (European Hexadecimal Language) protocol for controlling LPG dispensers via RS-485 communication. This project modernizes legacy Visual Basic 6 code into a type-safe, testable architecture.

### Legacy Code Location

The original Visual Basic 6 code that this project emulates and re-implements is located at:
```
./norgesgass_legacy
```

This directory contains the original VB6 Pumpestyring (pump control) application that communicated with LPG dispensers using the EHL protocol over RS-485. The Kotlin implementation is a complete rewrite that preserves the EHL protocol logic while modernizing the architecture with type safety, testability, and cloud-native capabilities.

## Development Environment Setup

### Java and Maven
This project requires Java 21.0.7-tem and Maven 3.9.11. Use SDKMAN for version management:

```bash
# Install SDKMAN if not already installed
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/sdkman-init.sh"

# Install project dependencies automatically
sdk env install
```

SDKMAN will automatically switch to the correct Java and Maven versions when entering the project directory.

## Common Development Commands

### Build and Compile
```bash
# Clean and compile
mvn clean compile

# Full build with tests
mvn clean install
```

### Testing
```bash
# Run all tests (29 unit tests)
mvn test

# Run specific test class
mvn test -Dtest=EhlCodecTest

# Run specific test method
mvn test -Dtest=TransactionTest#testValidStateTransitions
```

### Running the Demo
```bash
# Run main demo (quiet mode)
mvn -q exec:java

# Run with full Maven output
mvn exec:java
```

## Architecture

### Core Modules

**`no.cloudberries.lpg.protocol`** - EHL Protocol Implementation
- `EhlCommands.kt`: All EHL command definitions as enums (STATE, UNBLOCK, BLOCK, PRICE, etc.)
- `EhlPacket.kt`: Packet data structure with checksum validation
- `EhlCodec.kt`: Encoding/decoding logic with sealed result types (`EhlPacketParseResult`)

**`no.cloudberries.lpg.transaction`** - Transaction Management
- `Transaction.kt`: Complete transaction state machine with 9 states (NOT_STARTED → READY → ACTIVE → FINISHED → ACCOUNTED)
- Payment type support (cash, bank card, station card)
- `TransactionManager`: Multi-dispenser transaction tracking

### EHL Packet Structure
All EHL packets follow this binary format:
```
STX (0x20) | Length | Address | Command | Data (0-n) | Checksum (XOR) | ETX (0x36)
```

Example: `0x20 0x06 0x01 0x4B 0x6C 0x36` represents a STATE query for dispenser address 1.

### Transaction State Flow
Valid state transitions:
- NOT_STARTED → READY
- READY → ACTIVE or ANNULATED
- ACTIVE → FINISHED or UNACCOUNTED
- FINISHED → ACCOUNTED or FINANCIAL_RETURN
- UNACCOUNTED → ACCOUNTED
- FINANCIAL_RETURN → ACCOUNTED

Invalid transitions return `false` from `transitionTo()` without changing state.

### Code Patterns

**Immutability**: Data structures use `val` for immutable fields where possible. Transaction uses `var` for mutable state (volume, amount, etc.) but state transitions are validated.

**Sealed Classes**: `EhlPacketParseResult` uses sealed class pattern for type-safe error handling (Success, ChecksumError, InvalidFormat, Incomplete).

**Builder Pattern**: `EhlPacketBuilder` provides factory methods for common operations:
```kotlin
EhlPacketBuilder.createStateQuery(address = 1)
EhlPacketBuilder.createPriceProgram(address = 1, price = "15.90")
EhlPacketBuilder.createValuePreset(address = 1, amount = 50000)
```

**Result Types**: Decoding returns sealed result types instead of throwing exceptions. Always pattern match on result:
```kotlin
when (val result = EhlCodec.decode(bytes)) {
    is EhlPacketParseResult.Success -> // handle packet
    is EhlPacketParseResult.ChecksumError -> // handle error
    is EhlPacketParseResult.InvalidFormat -> // handle error
    is EhlPacketParseResult.Incomplete -> // need more data
}
```

## Legacy VB6 Mapping

This Kotlin implementation replaces these VB6 modules:
- `defs.bas` → `EhlCommands.kt` + `Transaction.kt`
- `fra_dispenser.bas` → `EhlCodec.kt` + `EhlPacket.kt`
- `Transaction.cls` → `Transaction.kt`

Key improvements over VB6: type safety, immutability, testability, functional error handling, separation of concerns.

## Testing Strategy

All components have unit tests. The test suite validates:
- Packet encoding/decoding with round-trip verification
- Checksum calculation and validation
- Transaction state machine transitions (valid and invalid)
- Edge cases (incomplete packets, invalid checksums, boundary conditions)
- Builder utilities

When adding features, maintain test coverage following existing patterns in `EhlCodecTest.kt` and `TransactionTest.kt`.

## Coding Conventions

- Use KDoc comments for public APIs
- Validate input in `init` blocks (e.g., address range 1-255)
- Log protocol operations using SLF4J logger
- Format byte arrays as hex strings: `"%02X".format(byte)`
- Use descriptive enum names with code and description properties
- Monetary amounts in øre/cents (Int), volumes in liters (Float)

## Emulator

### EHL Dispenser Emulator
A complete LPG dispenser simulator for testing without hardware:
- **Location**: `no.cloudberries.lpg.emulator`
- **Main class**: `EhlDispenserEmulator.kt`
- **Test coverage**: `EhlDispenserEmulatorTest.kt` (7 comprehensive tests)

**Key features:**
- PAYMENT_PENDING state with frozen totals after STOP/BLOCK
- Atomic stop mechanism (no race conditions)
- Complete transaction lifecycle (IDLE → AUTHORIZED → DELIVERING → PAYMENT_PENDING → IDLE)
- Admin API for reset/clear operations (`markTransactionPaid()`, `clearTransaction()`)
- InMemorySerialPort for communication without physical RS-485

**Usage:**
```kotlin
val emulator = EhlDispenserEmulator(address = 1, pricePerLitreCents = 1590)
val port = InMemorySerialPort(emulator)
val communicator = EhlCommunicator(port)
```

See [docs/EMULATOR.md](docs/EMULATOR.md) for complete documentation.

## Multi-Module Architecture (Hexagonal/Modular Monolith)

### Module Structure
```
lpg-ehl/
├── lpg-ehl-core/           # Protocol layer (NO Spring dependencies)
│   ├── protocol/           # EHL packet encoding/decoding
│   ├── transaction/        # Transaction state machine
│   └── communication/      # Serial port abstraction
│
├── lpg-transport/          # Serial/TCP transport (NO business logic)
│
├── lpg-ehl-service/        # Business logic + Database (Spring Data JPA)
│   ├── model/              # JPA Entities (Transaction, DispenserStatus...)
│   ├── repository/         # Spring Data repositories
│   ├── service/            # TransactionService, PriceService, AzureSyncService
│   ├── credit/             # Customer, CreditAccount entities + repos
│   ├── payment/            # PaymentGateway interface + implementations
│   └── resources/db/       # Liquibase migrations (owned by service module)
│
├── lpg-ehl-emulator/       # LAB mode dispenser simulator
│
├── lpg-ehl-webapp/         # Web API + React frontend (THIN WRAPPER)
│   ├── controller/         # REST Controllers only
│   ├── websocket/          # WebSocket handlers
│   └── config/             # Security, Web config
│
├── lpg-ehl-app-headless/   # Headless production (no web server)
│   └── Depends on service, runs without Tomcat
│
├── lpg-ehl-cli/            # Spring Shell CLI for testing
│   └── Interactive dispenser commands
│
└── lpg-web/                # React frontend source (builds to webapp)
```

### Module Dependencies (Layered)
```
                    ┌─────────────────┐
                    │   lpg-ehl-cli   │  (Spring Shell)
                    └────────┬────────┘
                             │
┌─────────────────┐  ┌───────┴───────┐  ┌─────────────────┐
│ lpg-ehl-webapp  │  │lpg-ehl-headless│  │                 │
│ (Web + React)   │  │   (No Web)     │  │                 │
└────────┬────────┘  └───────┬────────┘  │                 │
         │                   │           │                 │
         └─────────┬─────────┘           │                 │
                   │                     │                 │
           ┌───────▼───────┐             │                 │
           │lpg-ehl-service│◄────────────┘                 │
           │(Business Logic)│                              │
           │ + Liquibase DB │                              │
           └───────┬────────┘                              │
                   │                                       │
    ┌──────────────┼──────────────┐                        │
    │              │              │                        │
┌───▼───┐   ┌──────▼──────┐  ┌────▼─────┐                  │
│  core │   │lpg-transport│  │lpg-ehl-  │                  │
│(proto)│   │ (Serial/TCP)│  │ emulator │                  │
└───────┘   └─────────────┘  └──────────┘                  │
```

### Port Configuration
- **lpg-ehl-webapp**: Port 8080 (production Web API + React)
- **lpg-ehl-emulator**: Port 9001 (LAB mode test GUI)
- **EHL Protocol TCP**: Port 9000 (serial-over-TCP)

### Database
- **PostgreSQL**: Port 5432
- **Liquibase migrations**: `lpg-ehl-service/src/main/resources/db/changelog/`
- **Key tables**: `transactions`, `price_history`, `dispenser_status`, `customers`, `credit_accounts`

### JAR Sizes (Production)
| Module | Size | Purpose |
|--------|------|----------|
| lpg-ehl-core | 304K | Protocol (no Spring) |
| lpg-transport | 8K | Serial/TCP transport |
| lpg-ehl-service | 240K | Business logic + DB |
| lpg-ehl-webapp | 116M | Full Web API + React |
| lpg-ehl-headless | 66M | Headless (Raspberry Pi) |
| lpg-ehl-cli | 67M | Interactive Shell |

## WebSocket Real-Time Logging

### Architecture
Log streaming via WebSocket to `/control` GUI:

**Backend (lpg-ehl-emulator)**:
- `LogBufferAppender.kt`: Custom Logback appender
- `LogWebSocketHandler.kt`: WebSocket handler with 3 channels
- `LoggingConfiguration.kt`: Wires Spring beans to Logback at startup

**Frontend (lpg-web)**:
- `ControlPanel.tsx`: WebSocket client subscribing to log channels
- Connects to `ws://localhost:9001/ws/logs`

**Log Channels**:
1. **api**: REST API calls (PumpController, PriceController)
2. **emulator**: State machine logs (EhlDispenserEmulator)
3. **protocol**: TX/RX HEX packets (EhlCommunicator)

**Subscription**:
```json
{"action": "subscribe", "channels": ["api", "emulator", "protocol"]}
```

**Log Entry Format**:
```json
{
  "channel": "protocol",
  "timestamp": "2026-01-12T16:00:00Z",
  "level": "INFO",
  "logger": "EhlCommunicator",
  "message": "TX: 20 06 01 4B 6C 36"
}
```

## Price Management

### PriceService (Centralized Price Updates)
**Location**: `lpg-ehl-api/src/main/kotlin/.../service/PriceService.kt`

**Responsibilities**:
1. Save price changes to `price_history` table
2. Update emulator price (if running in LAB MODE)
3. Broadcast to WebSocket clients (real-time GUI update)
4. Update PumpStateService.currentPriceKr (for new transactions)

**Flow**:
```
PriceController.updatePrice() 
  → PriceService.updatePrice()
    → Database: price_history.save()
    → Emulator: setPrice()
    → WebSocket: broadcastPriceUpdate()
    → PumpStateService: currentPriceKr updated
```

**Database Schema**:
```sql
CREATE TABLE price_history (
  id UUID PRIMARY KEY,
  product_code VARCHAR(50),
  product_name VARCHAR(255),
  price_per_liter DECIMAL(10,2),
  vat_rate DECIMAL(5,4),
  effective_from TIMESTAMP,
  effective_until TIMESTAMP,
  created_by VARCHAR(100)
);
```

**Startup Behavior**:
- `PumpStateService.initializePriceFromDatabase()` restores last price on boot
- Falls back to emulator default (15.90 kr/L) if no history exists
- Broadcasts initial price to GUI via WebSocket

### Price Endpoints
- `GET /api/v1/prices` - Get current prices
- `POST /api/v1/prices/update` - Update price (saves to DB + broadcasts)

## IntelliJ Compound Run Configuration

**Compound Configuration**: "Full Stack (API + Emulator)"
- Starts both Spring Boot applications simultaneously
- API on 8080, Emulator on 9001
- Allows testing with both prod endpoints and mock dispenser

## Future Extensions

The architecture is designed to support:
- ✅ RS-485 serial communication layer (implemented)
- ✅ Async message handling with coroutines (implemented)
- ✅ Database persistence for transactions (PostgreSQL + Liquibase)
- ✅ REST API service layer (Spring Boot)
- ✅ WebSocket real-time updates (LogWebSocketHandler)
- ✅ Payment system integration (Nets Cloud Connect - SSL/TLS)

When adding features, maintain separation between protocol (low-level), transaction (business logic), and infrastructure layers.
