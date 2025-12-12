# LPG-EHL Multi-Module Project

**Modern Kotlin implementation of the EHL (European Hexadecimal Language) protocol for LPG dispenser control**

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Tests](https://img.shields.io/badge/tests-44%20passed-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-blue)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/java-21-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📖 About

This is a multi-module Maven project that implements the EHL protocol for controlling LPG (Liquefied Petroleum Gas) dispensers via RS-485 communication. The project is organized into:

- **lpg-ehl-core**: Core protocol implementation with real serial port communication
- **lpg-ehl-emulator**: Emulator for testing without physical hardware

## 🏗️ Project Structure

```
lpg-ehl/
├── pom.xml                    # Parent POM with dependency management
├── lpg-ehl-core/              # Core protocol implementation
│   ├── pom.xml
│   └── src/
│       ├── main/kotlin/no/cloudberries/lpg/
│       │   ├── protocol/          # EHL packet encoding/decoding
│       │   ├── communication/     # Serial port communication
│       │   └── transaction/       # Transaction state machine
│       └── test/kotlin/           # Unit tests (38 tests)
└── lpg-ehl-emulator/          # Testing emulator
    ├── pom.xml
    └── src/
        ├── main/kotlin/no/cloudberries/lpg/emulator/
        │   ├── EhlDispenserEmulator.kt    # Dispenser state machine
        │   └── InMemorySerialPort.kt      # In-memory serial port
        └── test/kotlin/                   # Integration tests (6 tests)
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** (Temurin 21.0.7)
- **Maven 3.9+**
- **SDKMAN** (recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone git@github.com:thomasandersen77/LPG-EHL-core.git
   cd lpg-ehl
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

4. **Build all modules**
   ```bash
   mvn clean install
   ```
   
   Expected output:
   ```
   Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
   BUILD SUCCESS
   ```

## 📦 Modules

### lpg-ehl-core

Core implementation of the EHL protocol with:
- Complete packet encoding/decoding with XOR checksum validation
- RS-485 serial port communication (via jSerialComm)
- Transaction state machine for fuel delivery management
- 38 unit tests

See [lpg-ehl-core/README.md](lpg-ehl-core/README.md) for detailed documentation.

### lpg-ehl-emulator

Testing emulator that simulates an EHL dispenser without physical hardware:
- **EhlDispenserEmulator**: State machine with IDLE → DELIVERING → FINISHED flow
- **InMemorySerialPort**: In-memory serial port for testing
- Supports STATE, UNBLOCK, STOP, VOLUME commands
- Simulates fuel delivery with configurable flow rate
- 6 integration tests

## 🧪 Testing with the Emulator

The emulator allows you to test the protocol implementation without physical hardware:

```kotlin
import no.cloudberries.lpg.emulator.*
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand

// Create emulator with configuration
val emulator = EhlDispenserEmulator(
    address = 1,
    pricePerLitreCents = 1000,  // 10.00 kr/l
    litresPerSecond = 1.0        // 1 liter per second
)

// Connect via in-memory serial port
val port = InMemorySerialPort(emulator)
val comm = EhlCommunicator(port)
port.connect()

// Query initial state
comm.send(EhlPacket(1, EhlCommand.STATE))
val stateResponse = comm.receive()
println("State: ${stateResponse.data[0]}")  // 0 = IDLE

// Start delivery
comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
comm.receive()  // OK
val deliveryState = comm.receive()  // STATE = DELIVERING

// Wait for fuel to flow
delay(1500)

// Stop delivery
comm.send(EhlPacket(1, EhlCommand.STOP))
comm.receive()  // OK
comm.receive()  // STATE = FINISHED
val volume = comm.receive()  // VOLUME response

println("Delivered: ${volume.data} deciliters")
```

### Running Emulator Tests

```bash
cd lpg-ehl-emulator
mvn test
```

The integration tests demonstrate:
- Initial state query
- UNBLOCK and delivery start
- Complete delivery cycle
- Volume query during delivery
- Multiple delivery cycles
- Wrong address handling

## 🏗️ Architecture

### Dependency Inversion

The project uses dependency inversion for testability:

```
EhlCommunicator
    ↓ depends on
SerialPortIO (interface)
    ↑              ↑
    implements     implements
SerialPortManager  InMemorySerialPort
    ↑                   ↑
    (production)    (testing)
```

- **SerialPortIO**: Interface for serial port operations
- **SerialPortManager**: Production implementation using real serial ports (jSerialComm)
- **InMemorySerialPort**: Test implementation that communicates with emulator

### Emulator State Machine

```
IDLE → READY → DELIVERING → FINISHED
                     ↓
                  ERROR
```

**Commands:**
- `STATE`: Query current state and delivery volume
- `UNBLOCK`: Start delivery (IDLE/FINISHED → DELIVERING)
- `STOP`: Stop delivery (DELIVERING → FINISHED)
- `VOLUME`: Query volume and amount

**Fuel Delivery Simulation:**
- Volume = seconds × litresPerSecond
- Amount = volume × pricePerLitreCents

## 📚 Usage Examples

### Testing Protocol Implementation

```kotlin
// Use emulator for testing
val emulator = EhlDispenserEmulator(address = 1)
val port = InMemorySerialPort(emulator)
val comm = EhlCommunicator(port)

// Test STATE query
comm.send(EhlPacket(1, EhlCommand.STATE))
val response = comm.receive()
assertEquals(EhlCommand.STATE, response.command)
```

### Production Usage

```kotlin
// Use real serial port for production
val port = SerialPortManager(portName = "/dev/ttyUSB0")
val comm = EhlCommunicator(port)
port.connect()

// Same API as emulator
comm.send(EhlPacket(1, EhlCommand.STATE))
val response = comm.receive()
```

## 🛠️ Development

### Building

```bash
mvn clean install
```

### Running All Tests

```bash
mvn test
```

### Running Core Tests Only

```bash
cd lpg-ehl-core
mvn test
```

### Running Emulator Tests Only

```bash
cd lpg-ehl-emulator
mvn test
```

## 📋 EHL Protocol Reference

### Packet Structure

```
STX (0x20) | Length | Address | Command | Data (0-n) | Checksum (XOR) | ETX (0x36)
```

### Supported Commands

| Command | Code | Description |
|---------|------|-------------|
| OK | 30 | Command acknowledgement |
| ERROR | 37 | Error code data |
| STOP | 47 | Stop the dispenser |
| VOLUME | 69 | Give/take fuel amount |
| STATE | 75 | Give/take calculator state |
| UNBLOCK | 119 | Start delivery mode |

See [lpg-ehl-core/README.md](lpg-ehl-core/README.md) for complete protocol reference.

## 🗺️ Roadmap

### Current Status
- ✅ Multi-module Maven structure
- ✅ Core protocol implementation
- ✅ Serial port communication
- ✅ Transaction state machine
- ✅ EHL dispenser emulator
- ✅ Integration tests

### Future Enhancements
- [ ] Async message handling
- [ ] Database persistence layer
- [ ] REST API service layer
- [ ] WebSocket real-time updates
- [ ] Payment system integration
- [ ] Admin web interface
- [ ] Docker containerization

## 📄 Documentation

- [Core Module Documentation](lpg-ehl-core/README.md)
- [Implementation Guide](lpg-ehl-core/IMPLEMENTATION_GUIDE.md)
- [Emulator Instructions](emulator-instructions.md)

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
