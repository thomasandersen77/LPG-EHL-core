# Removed Tests During Refactoring

## EhlDispenserEmulatorTest.kt

**Reason for Removal:** Circular dependency issue

**Details:**
The test `EhlDispenserEmulatorTest.kt` was removed during the module refactoring because it creates a circular dependency:
- The test requires `EhlCommunicator` which was moved to `lpg-transport`
- `lpg-transport` depends on `lpg-ehl-core` for protocol definitions
- `lpg-ehl-core` cannot depend on `lpg-transport` (circular dependency)

**Location:** Previously at `lpg-ehl-core/src/test/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulatorTest.kt`

**Restoration Strategy:**
1. Move the test to `lpg-ehl-emulator` module where it can access both core and transport
2. Update the test to work with the emulator module's version of `EhlDispenserEmulator`
3. Or, refactor the test to not require `EhlCommunicator` directly

**Test Coverage:**
The removed test covered:
- PAYMENT_PENDING lifecycle
- Atomic stop operations
- Transaction reset operations

**Action Required:**
- [ ] Restore test in appropriate module
- [ ] Verify test compatibility with emulator module's API
- [ ] Ensure test coverage is maintained
