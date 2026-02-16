# Results

## Summary

Successfully completed the emulator refactoring! Here's what was accomplished:

### ✅ Refactoring Complete

**Core Module (lpg-ehl-core)** - Spring-agnostic protocol layer:
- Created interfaces: `IDispenserSimulator`, `IEhlDispenserEmulator`, `ISerialTransportFactory`
- Extracted data classes: `ActiveTransaction`, `CompletedTransaction`, `EmulatorState` to `EmulatorTypes.kt`
- Removed concrete implementations (no Spring dependencies)
- All 163 protocol tests pass

**Emulator Module (lpg-ehl-emulator)** - Spring-based LAB mode implementation:
- Created implementations with Spring annotations (`@Component`, `@Profile("LAB")`):
  - `DispenserSimulatorImpl`
  - `EhlDispenserEmulatorImpl`
  - `InMemorySerialPort` (moved to impl package)
- All 30 emulator tests pass
- Configured Spring Boot plugin to preserve original JAR for compile dependencies

**Service & Webapp Modules** - Updated to use interfaces:
- `PriceService` and `PumpStateService` now use `IEhlDispenserEmulator`
- Webapp configuration beans instantiate concrete implementations
- Dependency injection works correctly with LAB profile

**Headless Module** - Updated transport configuration to use new implementations

### Architecture Benefits

1. **Separation of Concerns**: Core contains only protocol logic, emulator contains Spring/LAB implementations
2. **No Circular Dependencies**: Core → Emulator (one-way dependency)
3. **Inversion of Control**: Service layer depends on interfaces, not concrete classes
4. **Testability**: Core can be tested without Spring, emulator can provide different implementations

All 8 modules build successfully with `mvn clean install`! 🎉

---

# Refactor: Extract Emulator Classes from Core Module

## Objective
Move emulator implementation classes from `lpg-ehl-core` to `lpg-ehl-emulator` module, using interface-based inversion of control to maintain separation of concerns. The core module should remain Spring-agnostic and focused solely on the EHL protocol.

## Architecture Principle

- **Core module**: Pure protocol implementation (NO Spring, NO emulator implementation)
- **Emulator module**: Spring-based LAB mode implementation that depends on core

## Current State

### Classes to Move (from lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/emulator/)
1. `DispenserSimulator.kt` - Flow simulation with atomic stop capability
2. `EhlDispenserEmulator.kt` - State machine emulator for LPG dispensers  
3. `InMemorySerialPort.kt` - SerialTransport implementation for testing

### Dependencies
- `DispenserSimulator`: Uses `kotlinx.coroutines`, `slf4j`, no protocol dependencies
- `EhlDispenserEmulator`: Depends on `no.cloudberries.lpg.protocol.*` (EhlCodec, EhlPacket, etc.)
- `InMemorySerialPort`: Implements `SerialTransport` interface, wraps `EhlDispenserEmulator`

### Data Classes (in DispenserSimulator.kt)
- `ActiveTransaction` - Mutable transaction during delivery
- `CompletedTransaction` - Frozen transaction after STOP/BLOCK

## Refactoring Steps

### Step 1: Create Interfaces in Core Module
**Location**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/emulator/`

Create three new interface files:

#### 1.1 `IDispenserSimulator.kt`
```kotlin
package no.cloudberries.lpg.emulator

interface IDispenserSimulator {
    fun updatePrice(pricePerLitreCents: Int)
    fun start(
        activeTx: ActiveTransaction,
        onUpdate: (volumeLitres: Double, amountCents: Int) -> Unit = { _, _ -> }
    )
    fun stopImmediately()
    fun isRunning(): Boolean
}
```

#### 1.2 `IEhlDispenserEmulator.kt`
```kotlin
package no.cloudberries.lpg.emulator

interface IEhlDispenserEmulator {
    fun setPrice(pricePerLitreCents: Int)
    fun getPricePerLitreKr(): Double
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray>
    fun getState(): EmulatorState
    fun markTransactionPaid()
    fun clearTransaction()
}
```

#### 1.3 Keep `SerialTransport` Interface
`InMemorySerialPort` already implements `SerialTransport` interface from `lpg-transport` module. No new interface needed, but create a factory interface:

```kotlin
package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.transport.SerialTransport

interface ISerialTransportFactory {
    fun createInMemoryPort(emulator: IEhlDispenserEmulator): SerialTransport
}
```

### Step 2: Keep Data Classes in Core
**Action**: Leave `ActiveTransaction` and `CompletedTransaction` in `lpg-ehl-core` as they are protocol-level data structures, not implementation details.

**Location**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorTypes.kt` (new file)

### Step 3: Move Implementation Classes to Emulator Module
**Target**: `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/impl/`

#### 3.1 Move and Adapt Classes
1. Copy `DispenserSimulator.kt` → `DispenserSimulatorImpl.kt`
   - Implement `IDispenserSimulator`
   - Add `@Component` and `@Profile("LAB")`
   
2. Copy `EhlDispenserEmulator.kt` → `EhlDispenserEmulatorImpl.kt`
   - Implement `IEhlDispenserEmulator`
   - Add `@Component` and `@Profile("LAB")`
   - Inject `IDispenserSimulator` via constructor
   
3. Copy `InMemorySerialPort.kt` → `InMemorySerialPortFactory.kt`
   - Implement `ISerialTransportFactory`
   - Add `@Component` and `@Profile("LAB")`
   - Factory method creates `InMemorySerialPort` instances

#### 3.2 Spring Configuration Example
```kotlin
@Component
@Profile("LAB")
class EhlDispenserEmulatorImpl(
    private val simulator: IDispenserSimulator,
    private val address: Int = 1,
    pricePerLitreCents: Int = 1590
) : IEhlDispenserEmulator {
    // Implementation from original EhlDispenserEmulator
}
```

### Step 4: Remove Original Classes from Core
**Action**: Delete these files from `lpg-ehl-core`:
- `src/main/kotlin/no/cloudberries/lpg/emulator/DispenserSimulator.kt`
- `src/main/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulator.kt`
- `src/main/kotlin/no/cloudberries/lpg/emulator/InMemorySerialPort.kt`

### Step 5: Update Dependencies

#### 5.1 Core Module POM (`lpg-ehl-core/pom.xml`)
**Verify NO Spring dependencies exist**. Should only have:
- `kotlinx-coroutines-core`
- `slf4j-api`
- Test dependencies (JUnit, Mockito)

#### 5.2 Emulator Module POM (`lpg-ehl-emulator/pom.xml`)
**Add dependency on core**:
```xml
<dependency>
    <groupId>no.cloudberries</groupId>
    <artifactId>lpg-ehl-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 6: Update Tests

#### 6.1 Core Module Tests
- **Keep protocol tests** in `lpg-ehl-core/src/test/kotlin/`
- **Remove emulator tests** (move to emulator module)
- Verify all remaining tests pass without emulator classes

#### 6.2 Emulator Module Tests
- Move `EhlDispenserEmulatorTest.kt` to `lpg-ehl-emulator/src/test/kotlin/`
- Update imports to reference implementation classes
- Add Spring test context if needed

## Acceptance Criteria

### ✅ Core Module (lpg-ehl-core)
- [ ] Contains only interfaces: `IDispenserSimulator`, `IEhlDispenserEmulator`, `ISerialTransportFactory`
- [ ] Contains data classes: `ActiveTransaction`, `CompletedTransaction`, `EmulatorState`
- [ ] NO concrete emulator implementations
- [ ] NO Spring dependencies in `pom.xml`
- [ ] All protocol tests pass (`mvn test` in lpg-ehl-core)
- [ ] Builds successfully (`mvn clean install`)

### ✅ Emulator Module (lpg-ehl-emulator)  
- [ ] Contains implementation classes in `impl/` package
- [ ] All implementations annotated with `@Component @Profile("LAB")`
- [ ] Depends on `lpg-ehl-core` in `pom.xml`
- [ ] All emulator tests pass
- [ ] Can instantiate beans in LAB profile

### ✅ Integration
- [ ] No circular dependencies between modules
- [ ] Core module builds independently without emulator
- [ ] Emulator module successfully uses core interfaces
- [ ] Spring Boot application starts in LAB mode with emulator beans
- [ ] Full Maven build succeeds (`mvn clean install` from parent)

## Testing Commands

```bash
# Test core module (no Spring, protocol only)
cd lpg-ehl-core
mvn clean test

# Test emulator module (with Spring LAB profile)
cd ../lpg-ehl-emulator
mvn clean test

# Full build from parent
cd ..
mvn clean install
```

## Rollback Plan
If issues arise:
1. Git stash changes
2. Create feature branch for refactoring
3. Incremental commits per step (Step 1, Step 2, etc.)
4. Each commit should compile and pass tests

## Notes
- **EnumTypes** (EmulatorState, etc.): Keep in core if used by interfaces
- **Protocol classes**: ALL stay in core (EhlCodec, EhlPacket, EhlCommand, etc.)
- **Spring annotations**: ONLY in emulator module
- **Constructor injection**: Prefer interface types over implementations
