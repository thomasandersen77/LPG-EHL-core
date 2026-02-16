# Fault Injection Testing Guide

The PLS Simulator supports three fault injection modes to test error handling in the EHL protocol implementation.

## Fault Injection Modes

### 1. Disconnect After N Seconds
Simulates a physical disconnect of the serial port after a configured time.

**CLI Parameter:** `--disconnectAfterSeconds=<seconds>`

**Behavior:**
- Waits for N seconds after startup
- Closes the serial port abruptly
- Logs: `🔌 FAULT INJECTION: Force disconnect triggered`

### 2. Bad Checksum Rate
Randomly corrupts EHL frame checksums at a configured rate (0.0-1.0).

**CLI Parameter:** `--badChecksumRate=<rate>`

**Behavior:**
- Each response has a probability of `rate` to have a corrupted checksum
- Checksum byte (second-to-last byte) is XORed with random bits
- Logs: `⚠️  FAULT INJECTION: Corrupting checksum`

### 3. Power Fault After N Seconds
Simulates a power loss: resets state to IDLE and disconnects.

**CLI Parameter:** `--powerfaultAfterSeconds=<seconds>`

**Behavior:**
- Waits for N seconds after startup
- Resets dispenser state to IDLE (volume=0, nozzle holstered, no frozen transaction)
- Closes the serial port
- Logs: `⚡ FAULT INJECTION: Power fault triggered` followed by disconnect

## Testing Instructions

### Manual Testing with start-socat-sim.sh

Edit the script to add fault injection parameters to the simulator command:

```bash
# Test disconnect after 10 seconds
java -Dsim.log.level=DEBUG -jar "$SIM_JAR" \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --disconnectAfterSeconds=10

# Test 30% bad checksum rate
java -Dsim.log.level=DEBUG -jar "$SIM_JAR" \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --badChecksumRate=0.3

# Test power fault after 15 seconds
java -Dsim.log.level=DEBUG -jar "$SIM_JAR" \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --powerfaultAfterSeconds=15
```

### Automated Testing with Python

Use the `06_test_fault_injection.py` script:

#### Test Disconnect
```bash
# Terminal 1: Start simulator with disconnect
java -jar release/pls-sim.jar \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --disconnectAfterSeconds=5

# Terminal 2: Run test
cd projects/python-test
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test disconnect
```

#### Test Checksum Corruption
```bash
# Terminal 1: Start simulator with 50% bad checksum rate
java -jar release/pls-sim.jar \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --badChecksumRate=0.5

# Terminal 2: Run test
cd projects/python-test
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test checksum
```

#### Test Power Fault
```bash
# Terminal 1: Start simulator with power fault
java -jar release/pls-sim.jar \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --powerfaultAfterSeconds=5

# Terminal 2: Run test
cd projects/python-test
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test powerfault
```

## Expected Results

### Disconnect Test
- Simulator runs normally for N seconds
- At N seconds: `🔌 FAULT INJECTION: Force disconnect triggered`
- Serial port closes
- Client receives I/O error or timeout on next read/write

### Checksum Test
- Some responses have valid checksums (✓)
- Some responses fail checksum validation (✗)
- Error rate should match configured rate (±20%)

### Power Fault Test
- Simulator runs normally for N seconds
- At N seconds:
  1. `⚡ FAULT INJECTION: Power fault triggered`
  2. State resets to IDLE (status=0x00)
  3. `🔌 FAULT INJECTION: Force disconnect triggered`
- Client loses connection

## Use Cases

### 1. Test Reconnection Logic
Use disconnect and power fault modes to verify that:
- Client detects connection loss
- Client attempts to reconnect
- Client resumes normal operation after reconnect

### 2. Test Checksum Validation
Use bad checksum mode to verify that:
- Client properly validates EHL frame checksums
- Client rejects frames with bad checksums
- Client can recover from transient checksum errors

### 3. Test Transaction Recovery
Use power fault mode to verify that:
- Client handles mid-transaction power loss
- Client properly clears frozen transactions after reconnect
- No data corruption or state inconsistency

## Combining Fault Modes

All three fault modes can be combined:

```bash
java -jar release/pls-sim.jar \
    --port=/tmp/vserial0 \
    --address=1 \
    --mode=ehl \
    --badChecksumRate=0.1 \
    --powerfaultAfterSeconds=30
```

This creates a realistic scenario:
- 10% of responses have corrupted checksums (communication noise)
- Power loss occurs after 30 seconds

## Debugging Tips

1. **Enable hex logging:** Add `--logHex=true` to see raw TX/RX bytes
2. **Check status byte:** Monitor dispenser state (0x00=IDLE, 0x01=AUTH, 0x07=PUMPING, 0x08=STOPPED)
3. **Watch for frozen transactions:** Heartbeat shows pending transactions in square brackets
4. **Monitor disconnects:** Look for forced disconnects in logs vs. normal closes

## Related Files

- Simulator CLI: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/CliArgs.kt`
- State Machine: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/PlsState.kt`
- Serial Handler: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/SerialPortHandler.kt`
- Test Script: `projects/python-test/06_test_fault_injection.py`
