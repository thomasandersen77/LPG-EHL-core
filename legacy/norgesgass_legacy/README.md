# Norges Gass Legacy Code

This directory contains the **original Visual Basic 6 source code** for the Pumpestyring (pump control) application that this project re-implements and modernizes.

## About the Original Code

The VB6 application was used to control LPG (Liquefied Petroleum Gas) dispensers using the EHL (European Hexadecimal Language) protocol over RS-485 serial communication.

### Key Components

**Main Application - Pumpestyring**
- `pumpcontrol.vbp` - Main VB6 project file
- `fra_dispenser.bas` - EHL protocol implementation
- `Transaction.cls` - Transaction management class
- Various `.frm` files - User interface forms

**EHL Protocol Versions**
- `EHL4x/` - EHL 4.x protocol implementation
- `EHL4x 2/` - Alternative/backup EHL implementation

**Dispenser Client**
- `Dispenserklient/` - Client application for dispenser communication

## Modern Kotlin Implementation

This legacy code has been completely re-implemented in Kotlin with:

- ✅ **Type safety** - Kotlin's type system prevents many runtime errors
- ✅ **Modern architecture** - Clean separation of concerns
- ✅ **Comprehensive tests** - 61+ unit tests ensuring correctness
- ✅ **Cloud-native** - REST API, WebSocket support, containerization
- ✅ **Maintainability** - Well-documented, idiomatic Kotlin code

See the parent directory for the modern implementation.

## VB6 to Kotlin Mapping

| VB6 Module | Kotlin Implementation |
|------------|----------------------|
| `fra_dispenser.bas` | `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/` |
| `Transaction.cls` | `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/transaction/` |
| `defs.bas` | `EhlCommands.kt` + `Transaction.kt` |

## Note on Executables

`.exe` files are excluded from git as per `.gitignore` since we have all the source code. The original executables are preserved locally for reference but not committed to version control.

## Reference Documentation

For implementation details and protocol reference, see:
- `../lpg-ehl-core/WARP.md` - Development guide
- `../lpg-ehl-core/README.md` - Core module documentation
- `../ANALYSIS_REPORT.md` - VB6 vs Kotlin analysis
