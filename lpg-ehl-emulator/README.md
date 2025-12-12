# LPG-EHL Emulator

**Testing emulator for the EHL protocol - test your protocol implementation without physical hardware**

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Tests](https://img.shields.io/badge/tests-6%20passed-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)

---

## 📖 About

The LPG-EHL Emulator module provides a software emulator that simulates an EHL-protocol LPG dispenser. This allows you to test the protocol implementation (`lpg-ehl-core`) without requiring physical hardware.

## ✨ Features

- **State Machine**: IDLE → DELIVERING → FINISHED flow with proper state transitions
- **Protocol Commands**: Supports STATE, UNBLOCK, STOP, VOLUME commands
- **Fuel Simulation**: Simulates fuel delivery with configurable flow rate
- **Checksum Validation**: Full protocol validation including checksums
- **In-Memory Communication**: No serial ports required
- **Integration Tests**: 6 comprehensive tests covering all scenarios

## 🏗️ Architecture

### Components

1. **EhlDispenserEmulator**: Simulates the dispenser hardware
   - State machine with 5 states (IDLE, READY, DELIVERING, FINISHED, ERROR)
   - Handles EHL commands (STATE, UNBLOCK, STOP, VOLUME)
   - Simulates fuel flow based on time

2. **InMemorySerialPort**: In-memory serial port implementation
   - Implements `SerialPortIO` interface from core module
   - Uses concurrent queues for byte buffering
   - Communicates with emulator

### Dependency Inversion

```
EhlCommunicator (from core)
    ↓ depends on
SerialPortIO (interface)
    ↑
    implements
InMemorySerialPort
    ↓ communicates with
EhlDispenserEmulator
```

## 🚀 Quick Start

### Running Tests

```bash
cd lpg-ehl-emulator
mvn test
```

Expected output:
```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

### Using in Your Tests

```kotlin
import no.cloudberries.lpg.emulator.*
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.*
import kotlinx.coroutines.runBlocking

@Test
fun testDelivery() = runBlocking {
    // Create emulator
    val emulator = EhlDispenserEmulator(
        address = 1,
        pricePerLitreCents = 1000,  // 10.00 kr/l
        litresPerSecond = 1.0        // 1 liter per second
    )
    
    // Connect via in-memory port
    val port = InMemorySerialPort(emulator)
    val comm = EhlCommunicator(port)
    port.connect()
    
    // Start delivery
    comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
    val ack = comm.receive()
    assertEquals(EhlCommand.OK, ack.command)
    
    val state = comm.receive()
    assertEquals(EhlCommand.STATE, state.command)
    assertEquals(2, state.data[0].toInt())  // DELIVERING
    
    // Wait for fuel to flow
    delay(1500)  // 1.5 seconds
    
    // Stop delivery
    comm.send(EhlPacket(1, EhlCommand.STOP))
    comm.receive()  // OK
    comm.receive()  // STATE = FINISHED
    val volume = comm.receive()  // VOLUME
    
    // Verify delivered amount (should be ~1.5L)
    val volDeci = ((volume.data[0].toInt() and 0xFF) shl 8) or 
                  (volume.data[1].toInt() and 0xFF)
    val litres = volDeci / 10.0
    assertTrue(litres > 1.0 && litres < 2.0)
}
```

## 🧪 Integration Tests

The module includes 6 integration tests:

1. **should query initial state**: Tests STATE command on idle dispenser
2. **should handle UNBLOCK and start delivery**: Tests UNBLOCK command
3. **should complete full delivery cycle**: Tests complete UNBLOCK → STOP flow
4. **should query volume during delivery**: Tests VOLUME query while delivering
5. **should handle multiple delivery cycles**: Tests resetting after STOP
6. **should handle wrong address**: Tests address filtering

## 📋 Emulator Configuration

### Constructor Parameters

```kotlin
EhlDispenserEmulator(
    address: Int = 1,              // Dispenser address (1-255)
    pricePerLitreCents: Int = 1126, // Price per litre in cents (11.26 kr/l)
    litresPerSecond: Double = 0.5   // Flow rate for simulation
)
```

### State Machine

```
IDLE (0) ─── UNBLOCK ──→ DELIVERING (2)
  ↑                            │
  │                         STOP
  │                            ↓
  └────── UNBLOCK ───── FINISHED (3)
```

### Commands

| Command | Action | Response |
|---------|--------|----------|
| STATE | Query state | STATE + state code + volume |
| UNBLOCK | Start delivery | OK + STATE(DELIVERING) |
| STOP | Stop delivery | OK + STATE(FINISHED) + VOLUME |
| VOLUME | Query volume | VOLUME + volume + amount |

### Fuel Delivery Calculation

```kotlin
val seconds = (currentTime - startTime) / 1000.0
volumeLitres = seconds * litresPerSecond
amountCents = volumeLitres * pricePerLitreCents
```

## 🔄 State Transitions

### IDLE → DELIVERING
Triggered by: `UNBLOCK` command
Response: `OK` + `STATE(DELIVERING)`
Side effects:
- Starts timer
- Resets volume to 0
- Resets amount to 0

### DELIVERING → FINISHED
Triggered by: `STOP` command
Response: `OK` + `STATE(FINISHED)` + `VOLUME`
Side effects:
- Calculates final volume
- Calculates final amount

### Live Volume Updates
During DELIVERING state:
- `STATE` command returns live volume
- `VOLUME` command returns live volume and amount
- Volume increases linearly with time

## 📊 Example Output

```
Emulator: UNBLOCK - starting delivery
Delivered: 1.5 L for 1500 øre
Mid-delivery volume: 0.5 L
```

## 🛠️ Development

### Building

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Running with Debug Logging

The emulator uses SLF4J for logging. To see debug output, add a logback configuration:

```xml
<!-- src/test/resources/logback-test.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} -- %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="no.cloudberries.lpg.emulator" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

## 🔍 Troubleshooting

### Volume always returns 0.0
Make sure you're calling `delay()` between UNBLOCK and STOP to allow the emulated time to pass.

### Checksum errors
The emulator validates checksums using the same logic as the real protocol. Make sure you're using `EhlCodec.encode()` to create packets.

### Wrong address responses
The emulator ignores packets sent to a different address. Check that you're using the correct address (default is 1).

## 📚 Related Documentation

- [Parent Project README](../README.md)
- [Core Module README](../lpg-ehl-core/README.md)
- [Emulator Instructions](../emulator-instructions.md)

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.
