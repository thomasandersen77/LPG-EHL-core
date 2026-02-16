## serial-port-tests (RS-485 / EHL smoke-tests)

This folder contains a **self-contained serial port test kit** that exercises the RS‑485/EHL communication layer **without starting Spring / the main application**.

They intentionally depend only on the protocol + transport modules:

- `lpg-ehl-core` (EHL protocol)
- `lpg-transport` (jSerialComm-backed RS‑485 serial transport + `EhlCommunicator`)

### Prerequisites

- **Kotlin CLI** available as `kotlin` on PATH (scripts use `#!/usr/bin/env kotlin`).
- Local Maven artifacts installed for the modules the scripts depend on.

If you don’t have the Kotlin CLI installed:

```bash
# macOS (Homebrew)
brew install kotlin
```

Install/update the required artifacts to your local `~/.m2`:

```bash
./mvnw -pl lpg-ehl-core,lpg-transport -am -DskipTests install
```

### Build a release kit (for SFTP)

Creates `release/serial-port-tests-kit/` containing:
- `app.jar`
- `lib/*.jar`
- `run.sh`
- `00r-*.sh` wrappers
- `README.md`

```bash
./build-release.sh
```

### Environment variables (common)

- **`EHL_SERIAL_PORT`**: device path (examples: `/dev/ttyUSB0`, `/dev/ttyS0`, `/tmp/vserial1`)
- **`EHL_BAUD`**: default `9600`
- **`EHL_PARITY`**: `NONE` (default), `EVEN`, `ODD`, `MARK`, `SPACE`
- **`EHL_STOP_BITS`**: `1` (default) or `2`
- **`EHL_RAW_LOG`**: `true|false` (default `false`) – enables raw TX/RX logging inside `EhlCommunicator`
- **`EHL_TIMEOUT_MS`**: per-command timeout, default varies per script
- **`EHL_RETRY_MAX`**: retries inside `EhlCommunicator` (default `0` for “single shot” behavior)

Optional RS‑485 driver direction (RTS toggle), if your adapter/driver supports it:

- **`EHL_RS485_ENABLED`**: `true|false` (default `false`)
- **`EHL_RS485_RTS_BEFORE_MS`**: default `0`
- **`EHL_RS485_RTS_AFTER_MS`**: default `0`

### Safety (scripts that can change hardware state)

Scripts that can **UNBLOCK/BLOCK/RESET/program** require:

- **`I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true`**

### Scripts

- `00_list_serial_ports.main.kts`: list serial ports detected by jSerialComm
- `01_scan_addresses.main.kts`: scan an address range using `STATE`
- `02_baseline_snapshot.main.kts`: poll STATE/ERROR_QUERY/VOLUME/TANK for a single address
- `03_monitor_state_volume.main.kts`: monitor STATE+VOLUME for N seconds (log changes only)
- `04_unblock_hold_block.main.kts`: VB6-like UNBLOCK → hold/poll → BLOCK + verify
- `05_program_price.main.kts`: PRODUCT_SELECT + PROG_PRC (price programming)
- `06_program_preset_amount.main.kts`: PROG_AMOUNT (amount preset)
- `07_program_preset_volume.main.kts`: PROG_VOLUME (volume preset)

### Running

Fast path (recommended): run the wrappers:

```bash
chmod +x ./*.sh
./00r-list-ports.sh
```

Run via the Kotlin runner:

```bash
EHL_SERIAL_PORT=/tmp/vserial1 kotlin kts/01_scan_addresses.main.kts
```

If you **do not** have the Kotlin CLI, you can run via Maven (recommended):

```bash
# list ports
./mvnw -pl kotlin-scripts/serial-port-tests -Dexec.mainClass=no.cloudberries.lpg.scripts.SerialPortTestsMainKt exec:java -Dexec.args="list-ports"

# scan addresses
EHL_SERIAL_PORT=/tmp/vserial1 ./mvnw -pl kotlin-scripts/serial-port-tests -Dexec.mainClass=no.cloudberries.lpg.scripts.SerialPortTestsMainKt exec:java -Dexec.args="scan-addresses"
```

If you want to run them as executables:

```bash
chmod +x kts/*.main.kts
./kts/01_scan_addresses.main.kts
```

### Examples (copy/paste)

#### 0) List serial ports

```bash
./00r-list-ports.sh
```

#### 1) Scan for responding dispenser addresses (STATE)

Scan default `1-32` on `/tmp/vserial1`:

```bash
./01r-scan-addresses.sh
```

Scan `1-64` on a real adapter (example port):

```bash
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR_RANGE=1-64 ./01r-scan-addresses.sh
```

#### 2) Baseline snapshot (STATE / ERROR_QUERY / VOLUME / TANK)

```bash
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_TIMEOUT_MS=1200 ./02r-baseline-snapshot.sh
```

#### 3) Monitor (STATE + VOLUME) for changes

```bash
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_MONITOR_SECONDS=30 EHL_MONITOR_INTERVAL_MS=500 ./03r-monitor-state-volume.sh
```

#### 4) UNBLOCK → hold → BLOCK (+ verify open_for_delivery clears)

```bash
I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_HOLD_SECONDS=30 \
./04r-unblock-hold-block.sh
```

#### 5) Program price (PRODUCT_SELECT + PROG_PRC + PRICE readback)

```bash
I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_PRICE=15.90 \
./05r-program-price.sh
```

#### 6) Program presets

Amount preset (5 digits, VB6 semantics: `12345` → 123.45 kr):

```bash
I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_AMOUNT_5DIGITS=00100 \
./06r-program-preset-amount.sh
```

Volume preset (6 digits, VB6 semantics: `000500` → 5.00 L):

```bash
I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_VOLUME_6DIGITS=000500 \
./07r-program-preset-volume.sh
```

