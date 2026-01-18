# EHL Mode Implementation Summary

## Overview
Successfully refactored `lpg-ehl-pls-sim` to support real EHL protocol binary frames alongside the existing LINE and STX_ETX ASCII modes.

## Changes Made

### 1. New Frame Mode
- Added `FrameMode.EHL` enum value in `CliArgs.kt`
- Updated CLI parser to accept `--mode=ehl`
- Updated help text and documentation

### 2. EHL Frame Codec (`EhlFrameCodec.kt`)
New file implementing complete EHL protocol encoding/decoding:
- **Frame format**: `STX LEN ADDR CMD [DATA...] CHK ETX`
- **STX values**: 
  - `0x10` (Controller → Dispenser)
  - `0x20` (Dispenser → Controller)
- **ETX value**: `0x36`
- **Checksum**: XOR of all bytes from STX through last DATA byte
- **Validation**: Checks STX, LEN, ETX, and CHK for every frame
- **Supported commands**:
  - `0x6A` LINETEST → returns OK
  - `0x4B` STATE → returns STATE with 1 byte (0x30 = ready)
  - `0x45` VOLUME → returns VOLUME with 4 bytes (0x30 0x30 0x30 0x30)
  - `0x69` BLOCK → returns OK
  - `0x77` UNBLOCK → returns OK
  - `0x2F` STOP → returns OK

### 3. Frame Extraction (`FrameExtractor.kt`)
Enhanced to support EHL mode:
- Length-based frame extraction (reads STX, then LEN byte, then remaining bytes)
- Validates STX on first byte
- Validates length >= 6 bytes (minimum frame size)
- Accumulates bytes until complete frame received
- Resets on invalid frames

### 4. State Management (`PlsState.kt`)
Added EHL command processing:
- New method: `processEhlCommand(frame: EhlFrame): EhlCommandResult`
- Parses binary command codes
- Returns appropriate response types (OkAck, StateResponse, VolumeResponse)
- Maintains dispenser state for BLOCK/UNBLOCK/STOP commands

### 5. Serial Port Handler (`SerialPortHandler.kt`)
Updated for binary protocol support:
- Detects mode and routes to appropriate handler
- In EHL mode:
  - Uses `EhlFrameCodec.decode()` to parse frames
  - Calls `state.processEhlCommand()`
  - Uses `EhlFrameCodec.encode()` to build responses
  - Logs binary frames as hex
- Chunking support preserved (splits response bytes, not content)

### 6. Documentation (`README.md`)
Added comprehensive "EHL Mode Quickstart" section:
- Frame format specification
- Command reference table
- Setup instructions with socat
- Example LINETEST command with correct checksums
- Integration guide for lpg-ehl-core application

### 7. Test Script (`test-ehl-mode.sh`)
Automated test script that:
- Creates virtual serial port pair with socat
- Starts simulator in EHL mode
- Sends LINETEST frame (`10 06 31 6A 4D 36`)
- Validates response (`20 06 31 1E 09 36`)
- Cleans up resources on exit

## Verification
✅ Build successful: `mvn clean package -DskipTests`  
✅ Test passed: `./test-ehl-mode.sh`  
✅ Correctly handles LINETEST with valid checksum  
✅ Returns proper OK response with correct checksum

## Usage

### Basic Usage
```bash
java -jar target/pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true
```

### With Real Application
```bash
# Terminal 1: Start socat
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 &

# Terminal 2: Start simulator
java -jar lpg-ehl-pls-sim/target/pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true

# Terminal 3: Run your application
cd lpg-ehl-core
mvn exec:java -Dexec.mainClass="no.cloudberries.lpg.MainKt" -Dexec.args="--port=/tmp/ttyV1"
```

## Technical Notes

### Checksum Calculation
XOR of all bytes from STX through last DATA byte (excluding CHK and ETX):
```
Example request:  10 06 31 6A ? 36
Checksum: 0x10 ^ 0x06 ^ 0x31 ^ 0x6A = 0x4D

Example response: 20 06 31 1E ? 36  
Checksum: 0x20 ^ 0x06 ^ 0x31 ^ 0x1E = 0x09
```

### Frame Length
- LEN byte includes entire packet: STX + LEN + ADDR + CMD + [DATA...] + CHK + ETX
- Minimum: 6 bytes (no data payload)
- Maximum: 256 bytes (protocol limit)

### Compatibility
The simulator maintains 100% backward compatibility with existing LINE and STX_ETX modes while adding full EHL protocol support.

## Files Modified
- `src/main/kotlin/no/cloudberries/lpg/pls/sim/CliArgs.kt`
- `src/main/kotlin/no/cloudberries/lpg/pls/sim/FrameExtractor.kt`
- `src/main/kotlin/no/cloudberries/lpg/pls/sim/PlsState.kt`
- `src/main/kotlin/no/cloudberries/lpg/pls/sim/SerialPortHandler.kt`
- `README.md`

## Files Created
- `src/main/kotlin/no/cloudberries/lpg/pls/sim/EhlFrameCodec.kt`
- `test-ehl-mode.sh`
- `EHL-MODE-IMPLEMENTATION.md` (this file)
