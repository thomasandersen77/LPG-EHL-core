# WARP.md - LPG-EHL Edge System

**Project Location**: `/Users/tandersen/git/NorgesGass/lpg-ehl`

Moderne Kotlin-basert edge system for LPG-stasjoner. Erstatter legacy Windows Dispenserkontroll (VB6) med produksjonsklar, type-safe implementasjon.

## Project Overview

### Tre Generasjoner av EHL-protokoll

1. **Visual Basic 6** (Legacy) - Original Windows-basert Dispenserkontroll fra ARC-maskiner
2. **Python** (Eksperiment) - Proof-of-concept re-implementasjon av VB6-logikken
3. **Kotlin** (Production) - Moderne, type-safe, cloud-native implementasjon

### Hovedmoduler (Hexagonal/Modular Monolith)

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
│   ├── payment/            # PaymentGateway interface + Mock/Simulated
│   └── resources/db/       # Liquibase migrations (owned by service)
│
├── lpg-ehl-emulator/       # LAB mode dispenser simulator
│
├── lpg-ehl-webapp/         # Web API + React frontend (THIN WRAPPER)
│   ├── controller/         # REST Controllers only
│   ├── websocket/          # WebSocket handlers
│   └── config/             # Security, Web config
│
├── lpg-ehl-app-headless/   # Headless production (no web server)
│   └── For Raspberry Pi / embedded deployment
│
├── lpg-ehl-cli/            # Spring Shell CLI for testing
│   └── Interactive dispenser commands
│
├── lpg-web/                # React frontend source (builds to webapp)
│
├── legacy/archived/        # Archived implementations
│
├── legacy/norgesgass_legacy/ # VB6 legacy code (reference only)
│
└── legacy/more_legacy/     # Python re-implementation (reference)
```

### Module Dependencies
```
lpg-ehl-webapp ──┬──► lpg-ehl-service ──┬──► lpg-ehl-core (protocol)
                 │                      ├──► lpg-transport (serial/TCP)
 lpg-ehl-headless┘                      └──► lpg-ehl-emulator (LAB mode)

lpg-ehl-cli ──► lpg-ehl-core + lpg-transport + lpg-ehl-emulator
```

### JAR Sizes
| Module | Size | Purpose |
|--------|------|----------|
| lpg-ehl-core | 304K | Protocol (no Spring) |
| lpg-ehl-service | 240K | Business logic + DB |
| lpg-ehl-webapp | 116M | Full Web + React |
| lpg-ehl-headless | 66M | Headless (Raspberry Pi) |
| lpg-ehl-cli | 67M | Interactive Shell |

### Legacy Code Location

**Local**: `/Users/tandersen/git/NorgesGass/lpg-ehl/{norgesgass_legacy,more_legacy}`  
**Google Drive Backup**: `/Users/tandersen/Google Drive/My Drive/Norgesgass/LPG-EHL-Legacy/`

**Se full analyse**: [LEGACY_ANALYSIS.md](LEGACY_ANALYSIS.md)

## Multi-Station Architecture

Hver Edge-instans har unique identiteter:
- **stationId**: "S001", "S002" (station identifier)
- **edgeId**: "EDGE-S001-01" (edge device identifier)
- **dispenserId**: "D001", "D002" (dispenser within station)

### Environment Variables
```bash
STATION_ID=S001
EDGE_ID=EDGE-S001-01
DISPENSER_ID=D001
emulator.port=9000
lpg-api.base-url=http://localhost:8081
```

## Technology Stack

### Core Module (lpg-ehl-core)
- **Language**: Kotlin 1.9.23
- **Java**: 21 (Temurin 21.0.7-tem)
- **Build**: Maven
- **Testing**: JUnit 5 + Mockk (61+ tests)
- **Logging**: SLF4J

### Emulator Module (lpg-ehl-emulator)
- **Framework**: Spring Boot 3.x
- **REST API**: Multi-station support
- **WebSocket**: Real-time updates
- **Database**: PostgreSQL (cloud integration)

## Key Features

### Protocol Implementation
✅ Complete EHL protocol support (STATE, VOLUME, PRICE, TANK, etc.)  
✅ XOR checksum validation  
✅ Type-safe command enums  
✅ Sealed class result types  
✅ Immutable data structures

### Transaction Management
✅ State machine with 9 transaction states  
✅ Payment type support (cash, bank card, station card)  
✅ Multi-dispenser tracking  
✅ Validated state transitions

### Testing & Quality
✅ 61+ unit tests  
✅ In-memory serial port emulator  
✅ No hardware needed for testing  
✅ Integration tests for full delivery cycle

### Legacy Compatibility
✅ 100% protocol-compatible with VB6 implementation  
✅ Maps all VB6 commands to Kotlin enums  
✅ Preserves polling logic and timing

## Development Commands

Start single instance:
```bash
cd lpg-ehl-emulator
mvn spring-boot:run
```

Start with custom station:
```bash
STATION_ID=S001 EDGE_ID=EDGE-LOCAL DISPENSER_ID=D001 mvn spring-boot:run
```

Build all:
```bash
mvn clean install
```

## Payment Integration - Nets Cloud Connect

**SSL/TLS Encrypted Payment Architecture**

The system uses **Nets Cloud Connect** - a secure SSL/TLS tunnel for encrypted terminal communication using the standard Baxi protocol.

**IMPORTANT:** Cloud Connect is NOT a REST API. It's an SSL/TLS socket tunnel that carries Baxi protocol frames.

### Architecture
```
┌─────────────────┌         ┌────────────────────────────┌         ┌─────────────────┌
│                 │         │                            │         │                 │
│  LPG-EHL Core    ├─────────▶│  Nets Cloud Connect     ├─────────▶│  Payment        │
│  (Edge Device)   │ SSL/TLS │  3.33.230.243:6001      │   ECR   │  Terminal       │
│                 │◀─────────│  (Baxi Protocol Frames) │◀─────────│  (Ingenico)     │
└─────────────────┘         └────────────────────────────┘         └─────────────────┘
  CloudTerminalClient              Encrypted Tunnel                  Baxi Protocol
```

### What We Manage
✅ SSL/TLS socket connection to Nets Cloud
✅ Baxi protocol command creation and parsing
✅ Transaction recording and cloud sync
✅ Business logic and error handling

### What Nets Manages (NOT Our Responsibility)
❌ Terminal connectivity and routing
❌ SSL certificate management
❌ Terminal firmware and configuration
❌ Network failover and retry logic
❌ Card processing and security

**Documentation:** See `docs/NETS_CLOUD_CONNECT.md` for setup and implementation details.

### Implementation

**Core Components:**
- `NetsBaxProtocol.kt` - Baxi protocol implementation (559 lines)
- `CloudTerminalClient.kt` - SSL/TLS socket client (358 lines)
- `BaxResponse` - Protocol response types

**Protocol Framing:**
- TCP_ETHERNET mode: 2-byte length header + payload
- SERIAL mode: STX/ETX/LRC framing (legacy)

### Terminal Configuration
- **ECR**: Yes
- **ECR IP**: `3.33.230.243` (Nets Cloud SSL endpoint)
- **ECR Port**: `6001`
- **TLS/SSL**: Enabled
- **Communication**: Ethernet/WIFI
- Terminal connects TO Nets (outbound only, no firewall config needed)

### Testing Cloud Connect

Run demo:
```bash
cd lpg-ehl-core
mvn exec:java -Dexec.mainClass="no.cloudberries.lpg.MainKt" -Dexec.args="--cloud-connect"
```

### Architecture History

**2025-01-03:** SSL/TLS Socket (Current)
- CloudTerminalClient with SSLSocket
- Baxi protocol over TLS 1.2/1.3
- Direct connection to 3.33.230.243:6001

**2025-01-02:** REST API Attempt (ARCHIVED)
- Incorrectly assumed Cloud Connect was REST API
- Archived to `legacy/archived/rest-api-attempt/`

**Pre-2025:** Direct TCP/ECR (ARCHIVED)
- Direct TCP socket to terminal
- Archived to `legacy/archived/baxi-protocol/`

## ✅ Current Build Status (2025-01-03)

All tests passing:
```bash
mvn clean test  # 52 tests, 0 failures
```

## 🚧 Implementation Status

Ports & Adapters foundation complete:
- ✅ Clean Architecture interfaces (lpg-ehl-core)
- ✅ SSL/TLS socket client (NetsCloudSocketClient)
- ✅ FakeNetsCloudServer for testing
- ⏳ Docker Compose deployment
- ⏳ Simulated payment + PLS

See IMPLEMENTATION_PLAN_PORTS_ADAPTERS.md for full roadmap.

## Cloud Integration

Transactions synced to MinLPG cloud with full multi-tenant metadata.

See full documentation in README.md and CHANGELOG.md
