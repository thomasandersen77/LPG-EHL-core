# WARP.md - LPG-EHL Edge System

**Project Location**: `/Users/tandersen/git/NorgesGass/lpg-ehl`

Edge system for LPG-stasjoner - håndterer legacy Windows Dispenserkontroll og synkroniserer til cloud.

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

See full documentation in README.md and IMPLEMENTATION_PLAN_PAYMENT_RESET.md
