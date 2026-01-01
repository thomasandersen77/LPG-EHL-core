# WARP.md - LPG-EHL Edge System

**Project Location**: `/Users/tandersen/git/NorgesGass/lpg-ehl`

Moderne Kotlin-basert edge system for LPG-stasjoner. Erstatter legacy Windows Dispenserkontroll (VB6) med produksjonsklar, type-safe implementasjon.

## Project Overview

### Tre Generasjoner av EHL-protokoll

1. **Visual Basic 6** (Legacy) - Original Windows-basert Dispenserkontroll fra ARC-maskiner
2. **Python** (Eksperiment) - Proof-of-concept re-implementasjon av VB6-logikken
3. **Kotlin** (Production) - Moderne, type-safe, cloud-native implementasjon

### Hovedmoduler

```
lpg-ehl/
├── lpg-ehl-core/           # Core protocol implementation (Kotlin)
│   ├── protocol/           # EHL packet encoding/decoding
│   ├── transaction/        # Transaction state machine
│   ├── payment/            # Nets Bax payment terminal
│   └── communication/      # Serial port abstraction
│
├── lpg-ehl-emulator/       # Testing emulator (Spring Boot)
│   └── Simulates dispenser hardware
│
├── norgesgass_legacy/      # VB6 legacy code (reference only)
│   ├── pumpekontroll.frm   # Original UI + logic
│   └── fra_dispenser.bas   # EHL protocol implementation
│
└── more_legacy/            # Python re-implementation (reference)
    └── ehl_pumpekontroll_clone/
        ├── protocol.py     # EHL framing
        ├── model.py        # State management
        └── poller.py       # Polling loop
```

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

## Cloud Integration

Transactions synced to MinLPG cloud with full multi-tenant metadata.

See full documentation in README.md and IMPLEMENTATION_PLAN_PAYMENT_RESET.md
