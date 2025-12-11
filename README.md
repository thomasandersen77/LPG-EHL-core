# LPG-EHL-core
Re-engineering av gammel VBS-kode. 
=======
# LPG-EHL Core 🚀

**Modern Kotlin implementation of the EHL (European Hexadecimal Language) protocol for LPG dispenser control**

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Tests](https://img.shields.io/badge/tests-29%20passed-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-blue)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/java-21-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📖 About

LPG-EHL Core is a clean, modern reimplementation of the EHL protocol used for controlling LPG (Liquefied Petroleum Gas) dispensers via RS-485 communication. This project ports legacy Visual Basic 6 code to Kotlin, providing:

- 🔒 **Type-safe** protocol implementation
- 🧪 **Fully tested** with 29 unit tests
- 📝 **Well-documented** with KDoc and examples
- 🏗️ **Production-ready** architecture
- 🔄 **Transaction state machine** for fuel delivery management

## ✨ Features

### EHL Protocol Implementation
- ✅ Complete packet encoding/decoding with XOR checksum validation
- ✅ Support for all EHL commands (STATE, UNBLOCK, BLOCK, PRICE, VOLUME, etc.)
- ✅ Packet builder utilities for common operations
- ✅ Robust error handling (checksum errors, incomplete packets, invalid formats)

### Transaction Management
- ✅ State machine with 9 transaction states
- ✅ Payment type support (cash, bank card, station card)
- ✅ Multi-dispenser transaction tracking
- ✅ Validated state transitions
- ✅ Comprehensive transaction data model

### Code Quality
- ✅ 29 passing unit tests
- ✅ Immutable data structures
- ✅ Sealed classes for result types
- ✅ SLF4J logging integration
- ✅ Builder patterns for complex packets

## 🚀 Quick Start

### Prerequisites

- **Java 21** (Temurin 21.0.7)
- **Maven 3.9+**
- **SDKMAN** (recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone git@github.com:thomasandersen77/LPG-EHL-core.git
   cd LPG-EHL-core
   ```

2. **Install SDKMAN** (if not already installed)
   ```bash
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/sdkman-init.sh"
   ```

3. **Install project dependencies**
   ```bash
   sdk env install
   ```

4. **Build the project**
   ```bash
   mvn clean compile
   ```

5. **Run tests**
   ```bash
   mvn test
   ```
   
   Expected output:
   ```
   Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
   ```

6. **Run demo**
   ```bash
   mvn -q exec:java
   ```

## 📚 Usage Examples

### Creating and Encoding EHL Packets

```kotlin
import no.cloudberries.lpg.protocol.*

// Create a STATE query packet
val packet = EhlPacketBuilder.createStateQuery(address = 1)

// Encode to bytes for RS-485 transmission
val bytes = EhlCodec.encode(packet)
// Result: [0x20, 0x06, 0x01, 0x4B, 0x6C, 0x36]
```

### Decoding EHL Packets

```kotlin
val receivedBytes = byteArrayOf(0x20, 0x07, 0x01, 0x4B, 0x05, 0x68, 0x36)

when (val result = EhlCodec.decode(receivedBytes)) {
    is EhlPacketParseResult.Success -> {
        println("Decoded: ${result.packet}")
    }
    is EhlPacketParseResult.ChecksumError -> {
        println("Checksum mismatch")
    }
    is EhlPacketParseResult.InvalidFormat -> {
        println("Invalid packet format: ${result.reason}")
    }
    is EhlPacketParseResult.Incomplete -> {
        println("Need more data")
    }
}
```

### Transaction Management

```kotlin
import no.cloudberries.lpg.transaction.*

val manager = TransactionManager()

// Start a new transaction
val transaction = manager.startTransaction(dispenserAddress = 1)

// Transition through states
transaction.transitionTo(TransactionState.ACTIVE)

// Record delivery data
transaction.deliveredVolume = 45.5f
transaction.deliveredAmount = 72950  // 729.50 kr
transaction.unitPrice = 16.04f
transaction.paymentType = PaymentType.BANK_CARD

// Finalize
transaction.transitionTo(TransactionState.FINISHED)
manager.finalizeTransaction(1)
```

### Price Programming

```kotlin
// Program fuel price (15.90 kr/liter)
val pricePacket = EhlPacketBuilder.createPriceProgram(
    address = 1,
    price = "15.90"
)
val encoded = EhlCodec.encode(pricePacket)
```

## 🏗️ Architecture

```
lpg-ehl-core/
├── src/main/kotlin/no/cloudberries/lpg/
│   ├── protocol/
│   │   ├── EhlCommands.kt      # Command definitions
│   │   ├── EhlPacket.kt        # Packet data structure
│   │   └── EhlCodec.kt         # Encoding/decoding logic
│   ├── transaction/
│   │   └── Transaction.kt      # Transaction state machine
│   └── Main.kt                 # Demo application
└── src/test/kotlin/no/cloudberries/lpg/
    ├── protocol/
    │   └── EhlCodecTest.kt     # Protocol tests
    └── transaction/
        └── TransactionTest.kt  # Transaction tests
```

## 🧪 Testing

The project includes comprehensive unit tests:

```bash
mvn test
```

**Test Coverage:**
- ✅ Packet encoding/decoding
- ✅ Checksum validation
- ✅ Transaction state transitions
- ✅ Payment type handling
- ✅ Error scenarios
- ✅ Round-trip encode/decode
- ✅ Packet builders

## 📋 EHL Protocol Reference

### Supported Commands

| Command | Code | Description |
|---------|------|-------------|
| OK | 30 | Command acknowledgement |
| ERROR | 37 | Error code data |
| STOP | 47 | Stop the dispenser |
| VOLUME | 69 | Give/take fuel amount |
| STATE | 75 | Give/take calculator state |
| PRICE | 92 | Give/take fuel price |
| BLOCK | 105 | Block/stop dispenser |
| LINETEST | 106 | Transmission test |
| PROG_I | 112 | Program fuel amount |
| PROG_W | 117 | Program fuel value |
| UNBLOCK | 119 | Start delivery mode |
| ZER | 129 | Reset calculator |
| PROG_PRC | 169 | Program fuel price |
| TANK | 197 | Tank status/control |

### Packet Structure

```
STX (0x20) | Length | Address | Command | Data (0-n) | Checksum (XOR) | ETX (0x36)
```

**Example:** STATE query for dispenser address 1
```
0x20 0x06 0x01 0x4B 0x6C 0x36
  │    │    │    │    │    └─ ETX
  │    │    │    │    └─ Checksum
  │    │    │    └─ Command (STATE=75)
  │    │    └─ Address
  │    └─ Length
  └─ STX
```

## 🔄 Transaction State Machine

```
NOT_STARTED → READY → ACTIVE → FINISHED → ACCOUNTED
                 ↓                  ↓
              ANNULATED       UNACCOUNTED
```

**States:**
- `NOT_STARTED`: Initial state
- `READY`: Dispenser ready for delivery
- `ACTIVE`: Delivery in progress
- `FINISHED`: Delivery complete
- `ACCOUNTED`: Transaction recorded
- `ANNULATED`: Transaction cancelled
- `UNACCOUNTED`: Power failure during transaction
- `FINANCIAL_RETURN`: Financial return in progress
- `FINANCIAL_TECH_RETURN`: Technical return

## 🛠️ Development

### Building

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Running Demo

```bash
mvn -q exec:java
```

### SDKMAN Auto-Switching

Add to your shell profile (`~/.zshrc` or `~/.bashrc`):

```bash
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/sdkman-init.sh" ]] && source "$HOME/.sdkman/sdkman-init.sh"

# Auto-switch SDK versions
cd() { builtin cd "$@" && [[ -f .sdkmanrc ]] && sdk env; }
```

## 🗺️ Roadmap

### Future Enhancements

- [ ] RS-485 serial communication implementation
- [ ] Async message handling
- [ ] Database persistence layer
- [ ] REST API service layer
- [ ] WebSocket real-time updates
- [ ] Payment system integration
- [ ] Admin web interface
- [ ] Docker containerization

## 📄 Documentation

For detailed implementation documentation, see [IMPLEMENTATION.md](IMPLEMENTATION.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Ported from legacy Visual Basic 6 codebase
- EHL protocol specification from fuel dispenser manufacturers
- Built with Kotlin and modern JVM best practices

## 📧 Contact

Thomas Andersen - [@thomasandersen77](https://github.com/thomasandersen77)

Project Link: [https://github.com/thomasandersen77/LPG-EHL-core](https://github.com/thomasandersen77/LPG-EHL-core)




