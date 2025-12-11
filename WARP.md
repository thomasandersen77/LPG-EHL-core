# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

LPG-EHL Core is a Kotlin implementation of the EHL (European Hexadecimal Language) protocol for controlling LPG dispensers via RS-485 communication. This project modernizes legacy Visual Basic 6 code into a type-safe, testable architecture.

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

## Future Extensions

The architecture is designed to support:
- RS-485 serial communication layer
- Async message handling with coroutines
- Database persistence for transactions
- REST API service layer
- WebSocket real-time updates
- Payment system integration

When adding these, maintain separation between protocol (low-level), transaction (business logic), and infrastructure layers.
