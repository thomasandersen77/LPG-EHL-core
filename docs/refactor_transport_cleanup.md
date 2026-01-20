# Refactoring Checklist: Module Cleanup

## Goal
Achieve strict module separation:
- **lpg-ehl-core**: Pure protocol/codec (no serial, no Spring, no IO)
- **lpg-transport**: Pure IO/transport (serial/TCP-serial + jSerialComm)
- **lpg-ehl-service**: Pure business logic/use-cases
- **Entry points**: Thin wiring layers

## Step 1: Mapping Complete ✅

### Files to Move from lpg-ehl-core/communication → lpg-transport
- [x] DispenserConnection.kt
- [x] EhlCommunicator.kt
- [x] HardwareWatchdogCapable.kt
- [x] SerialDemo.kt
- [x] SerialPortConfig.kt
- [x] SerialPortIO.kt
- [x] SerialPortManager.kt

### Files to Move from lpg-ehl-core/service → lpg-ehl-service
- [x] EhlOperationsService.kt
- [x] EhlResults.kt

### Files to Keep in lpg-ehl-core/transport
- SerialTransport.kt (interface - stays as abstraction)

## Step 2: Move Transport Code
- [ ] Create package structure in lpg-transport
- [ ] Move communication files using git mv
- [ ] Update imports
- [ ] Test compilation

## Step 3: Clean lpg-ehl-core
- [ ] Remove jSerialComm dependency from core POM
- [ ] Remove Spring dependencies
- [ ] Verify no imports of webapp/service/transport

## Step 4: Move Use-Cases to Service
- [ ] Move EhlOperationsService and EhlResults
- [ ] Update imports
- [ ] Test compilation

## Step 5: POM Cleanup
- [ ] Remove jSerialComm from core
- [ ] Verify dependency flow: core ← transport ← service ← entrypoints

## Step 6: Thin Entry Points
- [ ] Check webapp for misplaced logic
- [ ] Check headless for misplaced logic
- [ ] Check cli for misplaced logic

## Step 7: Verification
- [ ] Full build with tests
- [ ] Verify all success criteria

## Step 8: Dependency Guards
- [ ] Add CI guard for core imports
- [ ] Add CI guard for webapp entities
