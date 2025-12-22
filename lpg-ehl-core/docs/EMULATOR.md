# EHL Dispenser Emulator Guide

## Overview

The EHL Dispenser Emulator simulates a real LPG dispenser for testing and development without physical hardware. It implements the complete EHL protocol with realistic pump behavior including:

- **PAYMENT_PENDING state** - Totals frozen after STOP/BLOCK
- **Atomic stop mechanism** - No race conditions
- **Transaction lifecycle** - Complete flow from start to payment
- **Admin API** - Reset and clear operations

## Architecture

```
┌─────────────────┐
│  Windows App /  │
│  Test Client    │
└────────┬────────┘
         │ EHL Protocol
         │ (via TCP/Serial)
         ▼
┌─────────────────┐
│ InMemorySerialPort│
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────────┐
│ EhlDispenserEmulator│────│ DispenserSimulator│
│  (State Machine)  │      │  (Flow Simulation)│
└─────────────────┘      └──────────────────┘
```

## State Machine

```
IDLE ──┐
       │ PRODUCT_SELECT
       ▼
AUTHORIZED ──┐
             │ UNBLOCK
             ▼
DELIVERING ──┐
             │ STOP/BLOCK
             ▼
PAYMENT_PENDING ──┐
                  │ markPaid/clear
                  ▼
IDLE (cycle repeats)
```

### State Descriptions

- **IDLE**: No transaction, ready to start
- **AUTHORIZED**: Product selected, ready for UNBLOCK
- **DELIVERING**: Active delivery in progress, simulation running
- **PAYMENT_PENDING**: Transaction complete, totals frozen, awaiting reset

## Usage

### Basic Setup

```kotlin
import no.cloudberries.lpg.emulator.*
import no.cloudberries.lpg.communication.EhlCommunicator

// Create emulator
val emulator = EhlDispenserEmulator(
    address = 1,
    pricePerLitreCents = 1590,  // 15.90 kr/L
    litresPerSecond = 0.5        // Flow rate
)

// Create in-memory serial port
val port = InMemorySerialPort(emulator)
port.connect()

// Create communicator
val communicator = EhlCommunicator(port)
```

### Complete Transaction Flow

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.*

runBlocking {
    // 1. Select product
    communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
    communicator.receive() // OK
    communicator.receive() // STATE (AUTHORIZED)
    
    // 2. Start delivery
    communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
    communicator.receive() // OK
    communicator.receive() // STATE (DELIVERING)
    
    // 3. Let it pump for 2 seconds
    delay(2000)
    
    // 4. Query volume during delivery
    communicator.send(EhlPacket(1, EhlCommand.VOLUME))
    val volumeResponse = communicator.receive()
    // volumeResponse.data contains [volHigh, volLow, amountHigh, amountLow]
    
    // 5. Stop delivery
    communicator.send(EhlPacket(1, EhlCommand.STOP))
    communicator.receive() // OK
    val stateAfterStop = communicator.receive() // STATE (PAYMENT_PENDING = 0x08)
    val finalVolume = communicator.receive() // VOLUME (frozen)
    
    // Verify state is PAYMENT_PENDING
    println("State after STOP: 0x${stateAfterStop.data[0].toString(16)}")
    // Output: State after STOP: 0x8
    
    // 6. Try to start new delivery (will be denied)
    communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
    val response = communicator.receive() // OK (but no new transaction starts)
    val stillPending = communicator.receive() // STATE still shows PAYMENT_PENDING
    
    // 7. Mark as paid and reset
    emulator.markTransactionPaid()
    
    // 8. Verify reset
    communicator.send(EhlPacket(1, EhlCommand.STATE))
    val idleState = communicator.receive() // STATE (IDLE = 0x00)
    
    communicator.send(EhlPacket(1, EhlCommand.VOLUME))
    val zeroVolume = communicator.receive() // All zeros
}
```

## Admin API

The emulator provides admin methods for transaction management:

### Get Current Transaction

```kotlin
val completedTx = emulator.getCurrentTransaction()
if (completedTx != null) {
    println("Transaction: ${completedTx.id}")
    println("Volume: ${completedTx.volumeLitres} L")
    println("Amount: ${completedTx.amountCents / 100.0} kr")
    println("Price: ${completedTx.unitPriceCents / 100.0} kr/L")
}
```

### Mark Transaction as Paid

```kotlin
val success = emulator.markTransactionPaid()
if (success) {
    println("Transaction marked as paid, emulator reset to IDLE")
} else {
    println("No transaction to mark as paid")
}
```

### Clear Transaction (Testing)

```kotlin
val success = emulator.clearTransaction()
if (success) {
    println("Transaction cleared, emulator reset to IDLE")
}
```

### Get Current State

```kotlin
val state = emulator.getCurrentState()
println("Emulator state: $state")
// Output: Emulator state: PAYMENT_PENDING
```

## Key Features

### 1. PAYMENT_PENDING State

After STOP/BLOCK:
- Totals are frozen in `completedTx`
- State transitions to PAYMENT_PENDING (0x08)
- UNBLOCK is denied until reset
- VOLUME queries return frozen values

```kotlin
// After STOP
assertEquals(0x08.toByte(), stateResponse.data[0]) // PAYMENT_PENDING

// Totals frozen
val vol1 = queryVolume()
delay(1000)
val vol2 = queryVolume()
assertEquals(vol1, vol2) // Identical
```

### 2. Atomic Stop Mechanism

No race conditions when stopping:
- `AtomicBoolean` flag for thread-safe signaling
- Coroutine cancellation
- Double-check before each update
- Immediate stop, no lingering updates

```kotlin
// Simulation loop checks flag BEFORE and AFTER each delay
while (simRunning.get() && isActive) {
    delay(100)
    if (!simRunning.get()) break // Critical check
    // Update totals
}
```

### 3. Transaction Lifecycle

Complete transaction tracking:
- `ActiveTransaction` - during delivery
- `CompletedTransaction` - after STOP (frozen)
- Clean separation of active vs completed state

### 4. Deterministic Responses

UNBLOCK in PAYMENT_PENDING returns:
1. OK acknowledgement (Windows expects this)
2. STATE showing PAYMENT_PENDING (0x08)

This prevents Windows from thinking the pump is available.

## Testing

Run the comprehensive test suite:

```bash
mvn test -Dtest=EhlDispenserEmulatorTest
```

### Test Coverage

All tests pass:
1. ✅ STOP during pumping → PAYMENT_PENDING and freezes totals
2. ✅ UNBLOCK in PAYMENT_PENDING does not start new transaction
3. ✅ markPaid resets to IDLE and clears totals
4. ✅ clear resets to IDLE without payment record
5. ✅ No simulation updates after STOP command
6. ✅ New UNBLOCK after reset starts fresh transaction from zero
7. ✅ Atomic stop prevents race condition

## Integration with Windows Dispenserkontroll

The emulator is designed to work seamlessly with the existing Windows application:

1. **No Windows changes required** - Emulator speaks the same EHL protocol
2. **Realistic behavior** - Mimics physical pump state machine
3. **Frozen totals** - Windows can query VOLUME repeatedly after STOP
4. **Clear indication** - PAYMENT_PENDING state (0x08) signals "not available"

### Connection Setup

Configure Windows application to connect to:
- **TCP**: `localhost:9000` (if using TCP bridge)
- **Serial**: Virtual COM port (if using virtual serial)

### Expected Behavior

| Windows Action | Emulator Response | Result |
|----------------|-------------------|--------|
| Velg produkt | PRODUCT_SELECT → AUTHORIZED | Ready for start |
| Frigi dispenser | UNBLOCK → DELIVERING | Simulation starts |
| Stopp | STOP → PAYMENT_PENDING | Totals frozen |
| Frigi etter stopp | UNBLOCK → PAYMENT_PENDING | Denied, no new tx |
| (Call markPaid) | → IDLE | Ready for next customer |
| Frigi dispenser | UNBLOCK → DELIVERING | New transaction from 0 |

## Troubleshooting

### Simulation doesn't stop immediately

**Symptom**: Updates continue after STOP command
**Solution**: Check that `AtomicBoolean` is used and checked before/after each delay

### UNBLOCK starts new transaction in PAYMENT_PENDING

**Symptom**: New transaction starts without reset
**Solution**: Verify handleUnblock() checks for PAYMENT_PENDING state

### Totals not frozen

**Symptom**: Volume changes after STOP
**Solution**: Ensure completedTx is created and used for VOLUME responses

### Windows thinks pump is available after STOP

**Symptom**: Windows allows new transaction without payment
**Solution**: Verify STATE response returns 0x08 (PAYMENT_PENDING), not 0x00 (IDLE)

## Advanced Configuration

### Custom Flow Rate

```kotlin
val emulator = EhlDispenserEmulator(
    litresPerSecond = 1.0  // Faster flow for testing
)
```

### Custom Price

```kotlin
val emulator = EhlDispenserEmulator(
    pricePerLitreCents = 2150  // 21.50 kr/L
)
```

### Custom Address

```kotlin
val emulator = EhlDispenserEmulator(
    address = 2  // For multi-dispenser testing
)
```

## Logging

The emulator uses SLF4J logging. Configure in `logback.xml`:

```xml
<logger name="no.cloudberries.lpg.emulator" level="DEBUG"/>
```

### Key Log Messages

- `UNBLOCK: Starting new transaction` - Delivery starting
- `STOP/BLOCK: Stopping delivery` - Delivery stopping
- `Transaction completed: X.X L, X.XX kr` - Totals frozen
- `Totals FROZEN - requires reset before next transaction` - PAYMENT_PENDING active
- `UNBLOCK denied: Transaction awaiting payment` - Denied start attempt
- `Emulator reset to IDLE - ready for new transaction` - After markPaid/clear

## See Also

- [WARP.md](../WARP.md) - Project overview and development guide
- [README.md](../README.md) - Quick start and installation
- [EhlDispenserEmulator.kt](../src/main/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulator.kt) - Source code
- [EhlDispenserEmulatorTest.kt](../src/test/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulatorTest.kt) - Test examples
