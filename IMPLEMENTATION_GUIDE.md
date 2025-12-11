# LPG-EHL Implementasjonsguide

## Status: Fase 0 Fullført ✅

### Hva er implementert

- ✅ **EHL Protokoll Core** (100%)
  - Alle EHL-kommandoer (STATE, UNBLOCK, BLOCK, PRICE, etc.)
  - Packet encoding/decoding med checksum-validering
  - Builder patterns for vanlige operasjoner
  - Robust feilhåndtering

- ✅ **Transaction Management** (100%)
  - State machine med 9 tilstander
  - Payment type support (cash, bank card, station card)
  - TransactionManager for multi-dispenser
  - 29 unit tester (alle grønne)

- ✅ **Docker Infrastructure** (100%)
  - Production Dockerfile (multi-stage build)
  - Development Dockerfile (med hot reload)
  - Docker Compose (app + PostgreSQL + Adminer)
  - Environment configuration (.env)
  - Complete deployment documentation

## Neste Steg: Start Fase 1

### Fase 1: Serial Communication (2-3 dager)

**Prioritet: KRITISK** ⭐⭐⭐

Dette er fundamentet for alt annet. Uten serial kommunikasjon kan vi ikke snakke med dispensere.

#### Oppgaver

1. **Add jSerialComm dependency** (10 min)
   ```xml
   <dependency>
       <groupId>com.fazecast</groupId>
       <artifactId>jSerialComm</artifactId>
       <version>2.10.4</version>
   </dependency>
   ```

2. **Implement SerialPortManager** (3 timer)
   - Open/close serial ports
   - Configure baud rate, parity, etc.
   - Handle connection errors
   - Port discovery/listing

3. **Implement EhlCommunicator** (4 timer)
   - Send EHL packets over serial
   - Receive and parse responses
   - Buffer management for incomplete packets
   - Timeout handling

4. **Implement DispenserConnection** (2 timer)
   - Per-dispenser connection state
   - Command queue
   - Response correlation

5. **Add Kotlin Coroutines** (1 time)
   - Async communication
   - Non-blocking I/O

6. **Write tests** (3 timer)
   - Mock serial port
   - Integration tests
   - Timeout scenarios

#### Files to Create

```
src/main/kotlin/no/cloudberries/lpg/communication/
├── SerialPortManager.kt
├── EhlCommunicator.kt
├── DispenserConnection.kt
└── SerialPortConfig.kt

src/test/kotlin/no/cloudberries/lpg/communication/
├── SerialPortManagerTest.kt
├── EhlCommunicatorTest.kt
└── MockSerialPort.kt
```

#### Testing

```bash
# Run tests
mvn test -Dtest=*Communication*

# Manual testing med loopback
# (Connect TX to RX on serial port)
mvn -q exec:java -Dexec.mainClass="no.cloudberries.lpg.communication.SerialDemo"
```

## Deployment

### Development (Lokal)

```bash
# Start bare database
docker-compose up -d postgres adminer

# Run application lokalt
mvn clean install
mvn spring-boot:run
```

### Production (Docker)

```bash
# Setup environment
cp .env.example .env
nano .env  # Update SERIAL_PORT, DISPENSER_ADDRESSES, etc.

# Start everything
docker-compose up -d

# View logs
docker-compose logs -f lpg-ehl-app
```

## Testing Strategy

### Unit Tests
- Test hver komponent isolert
- Mock dependencies
- Edge cases og feilscenarier

### Integration Tests
- Test kommunikasjon mellom komponenter
- Mock serial port (eller loopback)
- Test full transaction flow

### Manual Testing
- Med ekte dispenser (eller simulator)
- Verifiser EHL protokoll med oscilloscope
- Test error recovery

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Application                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │         REST API / Web Service                   │   │
│  │         (Fase 5 - Optional)                      │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Transaction Service                      │   │
│  │         (Fase 3)                                 │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Dispenser Service                        │   │
│  │         (Fase 2)                                 │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Serial Communication                     │   │
│  │         (Fase 1 - NEXT!)                         │   │
│  └─────────────────────────────────────────────────┘   │
│                          │                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │         EHL Protocol Core                        │   │
│  │         (✅ DONE!)                                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ↓
                  ┌───────────────┐
                  │  RS-485 Bus   │
                  └───────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ↓                 ↓                  ↓
   Dispenser 1      Dispenser 2       Dispenser 3
```

## Resources

### Documentation
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Detailed implementation docs
- [DOCKER.md](DOCKER.md) - Docker deployment guide
- [WARP.md](WARP.md) - AI assistant guidance
- [README.md](README.md) - Project overview

### Key Files
- [pom.xml](pom.xml) - Maven configuration
- [docker-compose.yml](docker-compose.yml) - Docker services
- [.env.example](.env.example) - Environment template

### EHL Protocol Reference
- Packet structure: `STX | Length | Address | Command | Data | Checksum | ETX`
- Commands: STATE(75), UNBLOCK(119), BLOCK(105), PRICE(92), etc.
- Checksum: XOR of all bytes between STX and checksum

## Kontakt

**Developer:** Thomas Andersen (Cloudberries)  
**Customer:** Tobias (NorgesGass)  
**GitHub:** https://github.com/thomasandersen77/LPG-EHL-core

---

**Oppdatert:** 2024-12-11  
**Neste milestone:** Fase 1 - Serial Communication
