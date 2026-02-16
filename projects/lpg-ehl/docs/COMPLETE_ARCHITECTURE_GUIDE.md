# LPG-EHL Complete Architecture Guide

**Last Updated:** 2026-01-30  
**Author:** Thomas Andersen  
**AI Assistant:** Warp Agent Mode (Claude 4.5 Sonnet)

---

## 📚 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Maven Module Structure](#maven-module-structure)
3. [Core Architectural Layers](#core-architectural-layers)
4. [Module Deep Dive](#module-deep-dive)
5. [Application Variants](#application-variants)
6. [Testing & Development Tools](#testing--development-tools)
7. [Deployment Modes](#deployment-modes)
8. [Protocol & Transport Layer](#protocol--transport-layer)
9. [Python Test Scripts](#python-test-scripts)
10. [Critical Configuration Issues](#critical-configuration-issues)
11. [Development Workflow](#development-workflow)

---

## 📋 Executive Summary

The LPG-EHL project is a **modern Kotlin/Spring Boot implementation** of the EHL (European Hexadecimal Language) protocol for controlling LPG fuel dispensers via RS-485 serial communication.

### Key Architectural Principles

1. **Layered Architecture**: Core → Transport → Service → Application
2. **Dependency Inversion**: Core defines interfaces, modules provide implementations
3. **Multiple Deployment Targets**: Web UI, headless daemon, CLI tools
4. **Hardware Abstraction**: Real hardware, Kotlin simulator, in-memory emulator
5. **Clean Separation**: Protocol logic independent of Spring, database, and UI

---

## 🏗️ Maven Module Structure

The project consists of **8 Maven modules** organized as a multi-module parent POM:

```
lpg-ehl-parent (pom.xml)
├── lpg-ehl-core             ← Protocol implementation (no Spring)
├── lpg-transport            ← Physical RS-485 serial port layer
├── lpg-ehl-service          ← Business logic (Spring/JPA)
├── lpg-ehl-emulator         ← In-memory hardware emulator
├── lpg-ehl-serialport-sim   ← Kotlin serial port simulator (PLS)
├── lpg-ehl-webapp           ← Web application (Spring Boot + React)
├── lpg-ehl-app-headless     ← Headless daemon (no UI)
└── lpg-ehl-cli              ← Command-line tools
```

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                           │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │ lpg-ehl-webapp   │         │ lpg-ehl-headless │             │
│  │ (Spring + React) │         │ (Spring daemon)  │             │
│  └────────┬─────────┘         └────────┬─────────┘             │
└───────────┼──────────────────────────────┼─────────────────────┘
            │                              │
            └──────────────┬───────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Service Layer (Brain)                       │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ lpg-ehl-service                                           │  │
│  │                                                           │  │
│  │ • PumpStateService      (state machine)                  │  │
│  │ • TransactionService    (CRUD, business logic)           │  │
│  │ • EhlOperationsService  (high-level protocol wrapper)    │  │
│  │ • PriceService          (pricing logic)                  │  │
│  │ • AzureSyncService      (cloud integration)              │  │
│  │ • DiagnosticsService    (health checks)                  │  │
│  │                                                           │  │
│  │ Dependencies: lpg-ehl-core, lpg-transport, Spring, JPA   │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ↓                 ↓                 ↓
┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
│ lpg-transport    │ │ lpg-ehl-core │ │ lpg-ehl-     │
│                  │ │              │ │ emulator     │
│ SerialPortMgr    │ │ EhlCodec     │ │              │
│ RealSerial       │ │ EhlCommands  │ │ InMemory     │
│ Hardware         │ │ Packet logic │ │ Simulator    │
│ Watchdog         │ │              │ │              │
│                  │ │ (No Spring)  │ │ (Optional)   │
└──────────────────┘ └──────────────┘ └──────────────┘
         │                    │
         │                    │
         ↓                    ↓
┌─────────────────────────────────────────────────┐
│        Physical Hardware Layer                  │
│                                                 │
│  /dev/ttyUSB0 (Real RS-485)                    │
│  /tmp/vserial0 (SOCAT virtual serial)          │
│  InMemorySerialPort (Testing)                  │
└─────────────────────────────────────────────────┘
```

---

## 🧱 Core Architectural Layers

### Layer 1: Protocol & Transport (Foundation)

**`lpg-ehl-core`** - Pure protocol implementation
- **Purpose**: EHL protocol encoding/decoding, packet framing
- **Key Classes**:
  - `EhlCodec` - Binary packet encoding/decoding
  - `EhlCommands` - Command constants (UNBLOCK, BLOCK, STATE, VOLUME, etc.)
  - `EhlPacket` - Data class for protocol frames
  - `SerialTransport` - **Interface** for transport abstraction
- **Dependencies**: None (pure Kotlin, SLF4J logging only)
- **Why separate?**: Protocol logic can be reused in CLI tools, embedded systems, or non-Spring environments

**`lpg-transport`** - Physical layer implementation
- **Purpose**: Real RS-485 serial port communication
- **Key Classes**:
  - `SerialPortManager` - Production implementation using jSerialComm
  - `RealSerialTransport` - Implements `SerialTransport` interface
  - `HardwareWatchdog` - Monitors connection health
- **Dependencies**: `lpg-ehl-core`, jSerialComm
- **Why separate?**: Swappable transport (real hardware vs emulator vs mock)

### Layer 2: Service (The Brain)

**`lpg-ehl-service`** - Business logic and state management
- **Purpose**: High-level operations, transaction management, state machines
- **Key Services**:
  - `EhlOperationsService` - Wraps low-level EHL protocol with domain logic
  - `PumpStateService` - Pump state machine (IDLE → AUTHORIZED → PUMPING → PAYMENT_PENDING)
  - `TransactionService` - Transaction CRUD, persistence
  - `PriceService` - Price management with history
  - `AzureSyncService` - Cloud synchronization
  - `DiagnosticsService` - System health monitoring
- **Dependencies**: `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-emulator` (optional), Spring Boot, JPA, PostgreSQL
- **Why separate?**: Reusable business logic for both webapp and headless daemon

### Layer 3: Applications (Delivery Mechanisms)

**`lpg-ehl-webapp`** - Web application
- **Purpose**: Full-featured web UI with REST API
- **Stack**: Spring Boot + Undertow + React (bundled)
- **Features**:
  - Dashboard for pump status
  - Control panel for manual operations
  - Transaction history
  - Real-time WebSocket logging
  - Swagger/OpenAPI docs
- **Dependencies**: `lpg-ehl-service`, React frontend

**`lpg-ehl-app-headless`** - Headless daemon
- **Purpose**: Production-ready background service (no UI)
- **Stack**: Spring Boot (minimal web for health endpoints)
- **Features**:
  - Runs as systemd service
  - Lightweight (no React bundle)
  - Optional REST API for monitoring
  - Same business logic as webapp
- **Dependencies**: `lpg-ehl-service`

---

## 📦 Module Deep Dive

### 1. `lpg-ehl-core` - The Foundation

**Role**: Pure protocol implementation with zero business logic.

#### Key Components

```kotlin
// Protocol encoding/decoding
EhlCodec.encode(addr: Byte, cmd: Byte, data: ByteArray): ByteArray
EhlCodec.decode(frame: ByteArray): EhlPacket?

// Packet representation
data class EhlPacket(
    val addr: Byte,
    val cmd: Byte,
    val data: ByteArray,
    val chksum: Byte
)

// Transport abstraction (interface only)
interface SerialTransport {
    val isConnected: Boolean
    fun connect(): Boolean
    fun disconnect()
    fun write(data: ByteArray): Int
    fun readAvailable(maxBytes: Int): ByteArray
    fun flush()
}
```

#### EHL Protocol Commands

| Command | Hex | Purpose |
|---------|-----|---------|
| OK | 0x1E | Acknowledgement |
| LINETEST | 0x4C | Connectivity test |
| STATE | 0x4B | Query dispenser state |
| VOLUME | 0x45 | Query/set volume |
| PRICE | 0x50 | Query/set price |
| UNBLOCK | 0x77 | Start fuel delivery |
| BLOCK | 0x69 | Stop fuel delivery |
| STOP | 0x47 | Emergency stop |

#### Packet Framing

```
STX | LEN | ADDR | CMD | DATA[0..n] | CHKSUM | ETX
0x10  0x06   0x01  0x4B                0x5C    0x36

CHKSUM = XOR(STX, LEN, ADDR, CMD, DATA[0], ..., DATA[n])
```

#### Why No Spring?

This module is **deliberately Spring-free** so it can be used in:
- CLI tools (`lpg-ehl-cli`)
- Embedded systems
- Unit tests without Spring context
- Python/Java interop (via JNI or REST)

---

### 2. `lpg-transport` - Hardware Abstraction

**Role**: Implements `SerialTransport` interface for real RS-485 hardware.

#### Key Components

```kotlin
class SerialPortManager(
    val portName: String,        // e.g., "/dev/ttyUSB0"
    val baud: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Parity = Parity.NONE  // ⚠️ CRITICAL: Must match hardware
) : SerialTransport {
    // Uses jSerialComm for real hardware communication
}

class HardwareWatchdog(
    private val serialPortManager: SerialPortManager,
    private val timeoutMs: Long = 60000
) {
    // Monitors connection health
    // Reconnects automatically on failures
}
```

#### Serial Configuration

**⚠️ CRITICAL PARITY ISSUE:**

- **Real hardware**: Typically uses **8E1** (8 data bits, EVEN parity, 1 stop bit)
- **Kotlin simulator**: Uses **8N1** (8 data bits, NO parity, 1 stop bit)
- **Mismatch = communication failure!**

**Solution**: Configure via Spring Boot properties:

```yaml
ehl:
  serial:
    port: /dev/ttyUSB0
    baud-rate: 9600
    data-bits: 8
    stop-bits: 1
    parity: EVEN  # or NONE for simulator
```

---

### 3. `lpg-ehl-service` - The Brain

**Role**: Business logic, state machines, and transaction management.

#### Architecture Pattern: Hexagonal (Ports & Adapters)

```
┌───────────────────────────────────────────────────────┐
│                  Service Layer (Domain)                │
│                                                        │
│  ┌─────────────────────────────────────────────────┐  │
│  │ Domain Services (Pure Business Logic)          │  │
│  │                                                 │  │
│  │ • PumpStateService       (state machine)       │  │
│  │ • TransactionService     (transaction logic)   │  │
│  │ • PriceService           (pricing rules)       │  │
│  └─────────────────────────────────────────────────┘  │
│                         │                             │
│                         │ depends on                  │
│                         ↓                             │
│  ┌─────────────────────────────────────────────────┐  │
│  │ Ports (Interfaces)                              │  │
│  │                                                 │  │
│  │ • EhlOperationsService  (protocol wrapper)     │  │
│  │ • TransactionRepository (data access)          │  │
│  │ • AzureSyncService      (cloud integration)    │  │
│  └─────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          ↓                             ↓
┌──────────────────┐           ┌─────────────────┐
│ Adapters (Impl)  │           │ Adapters (Impl) │
│                  │           │                 │
│ EhlCommunicator  │           │ JPA Repos       │
│ SerialPortMgr    │           │ Azure Queue     │
└──────────────────┘           └─────────────────┘
```

#### Key Services

##### `EhlOperationsService` - Protocol Wrapper

**Purpose**: High-level protocol operations with domain types (not raw bytes).

```kotlin
class EhlOperationsService(
    private val communicator: EhlCommunicator
) {
    suspend fun linetest(address: Int): EhlPacket
    suspend fun getState(address: Int): EhlPacket
    suspend fun getVolume(address: Int): VolumeResult  // Parsed domain object
    suspend fun getPrice(address: Int): PriceResult
    suspend fun unblock(address: Int): Result<Unit>
    suspend fun block(address: Int): Result<Unit>
}
```

**Why Useful?**
- Controllers/CLI don't deal with raw bytes
- Centralizes parsing logic
- Type-safe domain objects
- Easier testing

##### `PumpStateService` - State Machine

**Purpose**: Manages pump lifecycle and state transitions.

**State Diagram:**

```
IDLE
  │
  ↓ (card swipe → authorization)
AUTHORIZED_WAITING
  │
  ↓ (UNBLOCK → hardware confirmed)
READY_TO_PUMP
  │
  ↓ (customer lifts nozzle)
PUMPING
  │
  ↓ (BLOCK or auto-stop)
PAYMENT_PENDING
  │
  ↓ (payment complete)
COMPLETED
```

**Key Methods:**

```kotlin
class PumpStateService {
    fun handleCardSwipe(pumpId: Int, authId: UUID, maxAmount: Double)
    fun handleUnblockConfirmed(pumpId: Int)
    fun handlePumpingStarted(pumpId: Int)
    fun handlePumpingStopped(pumpId: Int, volume: Double, amount: Double)
    fun handlePaymentComplete(pumpId: Int, transactionId: UUID)
}
```

##### `TransactionService` - CRUD & Logic

**Purpose**: Transaction lifecycle management.

```kotlin
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val azureSyncService: AzureSyncService
) {
    fun createTransaction(pumpId: Int, pricePerLitre: Double): Transaction
    fun updateVolume(transactionId: UUID, volume: Double)
    fun completeTransaction(transactionId: UUID, paymentStatus: PaymentStatus)
    fun syncToCloud(transactionId: UUID)
}
```

#### Database Schema (Liquibase Migrations)

**Core Tables:**

- `transactions` - Fuel delivery records
- `pump_authorization` - Card swipe tracking
- `price_history` - Price changes over time
- `dispenser_status` - Current hardware state
- `azure_sync_queue` - Outbox pattern for cloud sync
- `customers` - Customer accounts
- `credit_accounts` - Credit balances

---

### 4. `lpg-ehl-emulator` - In-Memory Simulator

**Role**: Software-based hardware emulator for testing WITHOUT physical hardware.

#### Usage

```kotlin
// Create emulator
val emulator = EhlDispenserEmulator(
    address = 1,
    pricePerLitreCents = 1590,  // 15.90 kr/L
    litresPerSecond = 1.0
)

// Use in-memory transport
val transport = InMemorySerialPort(emulator)
val comm = EhlCommunicator(transport)
transport.connect()

// Now you can test protocol logic
comm.send(EhlPacket(1, EhlCommand.STATE))
val response = comm.receive()
```

#### When to Use?

- ✅ Unit tests
- ✅ CI/CD pipelines (no hardware required)
- ✅ Frontend development (mock backend)
- ❌ Real deployment (use `lpg-ehl-serialport-sim` instead)

---

### 5. `lpg-ehl-serialport-sim` - Kotlin PLS Simulator

**Role**: Kotlin simulator that communicates via **real serial port** (for SOCAT integration).

#### Architecture

```
┌─────────────────────────────────────────────────────┐
│ lpg-ehl-serialport-sim                              │
│                                                     │
│  PlsSimMain.kt  (standalone JAR)                   │
│  ├─ SerialPortHandler  (jSerialComm)               │
│  ├─ PlsState           (state machine)             │
│  ├─ EhlFrameCodec      (protocol logic)            │
│  └─ Heartbeat logging  (every 10s)                 │
└─────────────────────────────────────────────────────┘
                    │
                    │ Opens serial port
                    ↓
┌─────────────────────────────────────────────────────┐
│ /tmp/vserial0  (SOCAT virtual serial)              │
└─────────────────────────────────────────────────────┘
                    ↔
┌─────────────────────────────────────────────────────┐
│ /tmp/vserial1  (SOCAT virtual serial)              │
└─────────────────────────────────────────────────────┘
                    │
                    │ lpg-ehl-webapp connects here
                    ↓
┌─────────────────────────────────────────────────────┐
│ lpg-ehl-webapp (FIELD mode)                        │
│  SerialPortManager → /tmp/vserial1                 │
└─────────────────────────────────────────────────────┘
```

#### Starting the Simulator

```bash
./scripts/start-socat-sim.sh
```

**What It Does:**

1. Builds `pls-sim.jar` (if not exists)
2. Creates SOCAT virtual serial pair:
   - `/tmp/vserial0` ← Simulator connects here
   - `/tmp/vserial1` ← Webapp connects here
3. Starts simulator with:
   ```bash
   java -jar pls-sim.jar \
       --port=/tmp/vserial0 \
       --address=1 \
       --price=1590 \
       --baud=9600 \
       --mode=ehl \
       --logHex=true
   ```

#### Serial Configuration

**⚠️ Line 47 in `SerialPortHandler.kt`:**

```kotlin
port.parity = SerialPort.NO_PARITY  // 8N1
```

**This means:**
- Simulator uses **NO parity** (8N1)
- Webapp MUST also use **NO parity** when connecting via SOCAT
- Real hardware typically uses **EVEN parity** (8E1)

---

### 6. `lpg-ehl-webapp` - Web Application

**Role**: Full-featured web UI with REST API.

#### Stack

- **Backend**: Spring Boot 3.2.1, Kotlin, Undertow
- **Frontend**: React 18, TypeScript, Vite
- **WebSocket**: STOMP over SockJS for real-time logs
- **Database**: PostgreSQL (or H2 for testing)
- **Authentication**: Token-based (optional)

#### Project Structure

```
lpg-ehl-webapp/
├── src/main/kotlin/
│   └── no/cloudberries/lpg/api/
│       ├── controller/         # REST endpoints
│       ├── config/             # Spring configuration
│       ├── websocket/          # WebSocket handlers
│       └── LpgEhlApiApplication.kt
├── src/main/resources/
│   ├── application.yaml        # Spring config
│   ├── static/                 # React build output (bundled here)
│   └── db/changelog/           # Liquibase migrations
└── pom.xml
```

#### Running Modes

**LAB Mode** - Uses in-memory emulator (no hardware)

```bash
java -jar lpg-ehl-webapp.jar --lpg.mode=LAB
```

**FIELD Mode** - Uses real serial port

```bash
java -jar lpg-ehl-webapp.jar \
    --lpg.mode=FIELD \
    --ehl.serial.port=/tmp/vserial1 \
    --ehl.serial.parity=NONE
```

#### REST API Endpoints

```
GET  /api/v1/pump/{id}/status          # Pump state
POST /api/v1/pump/{id}/unblock         # Start delivery
POST /api/v1/pump/{id}/block           # Stop delivery
GET  /api/v1/transactions              # Transaction history
POST /api/v1/transactions/{id}/pay     # Mark as paid
GET  /api/v1/price                     # Current price
POST /api/v1/price                     # Update price
```

#### WebSocket Logging

**Endpoint**: `ws://localhost:8080/ws/logs`

**Log Categories** (filterable in UI):
- `API` - REST API requests
- `SERVICE` - Business logic events
- `EMULATOR` - Fault injection API (testing only)
- `PROTOCOL` - Raw EHL protocol messages (TX/RX hex)

---

### 7. `lpg-ehl-app-headless` - Headless Daemon

**Role**: Production-ready background service (no UI).

#### Why Use Headless?

- **Smaller footprint**: No React bundle (saves ~5 MB)
- **Faster startup**: No frontend asset compilation
- **Systemd integration**: Designed to run as a service
- **Monitoring-friendly**: Optional minimal web server for health checks

#### Configuration

```yaml
# application.yaml
spring:
  main:
    web-application-type: servlet  # or 'none' for zero web
server:
  port: 8081  # Only for health endpoint

lpg:
  mode: FIELD
ehl:
  serial:
    port: /dev/ttyUSB0
    parity: EVEN
```

#### Running as Systemd Service

```bash
# /etc/systemd/system/lpg-ehl.service
[Unit]
Description=LPG-EHL Fuel Dispenser Control
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-headless.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Start service:**

```bash
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl
sudo systemctl status lpg-ehl
```

---

## 🧪 Testing & Development Tools

### Python Test Scripts

**Location**: `projects/python-test/`

**Purpose**: Simple test scripts for **manual protocol testing** without running the full application.

#### Key Scripts

```
projects/python-test/
├── 01_probe_readonly.py      # Read-only state queries
├── 02_unblock_block.py        # Test pump control
├── 03_full_transaction.py    # Simulate complete transaction
├── ehl_protocol.py            # Protocol implementation
└── requirements.txt
```

#### Example Usage

```bash
cd projects/python-test
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1
```

**What It Does:**
1. Opens serial port
2. Sends LINETEST command
3. Sends STATE query
4. Sends VOLUME query
5. Prints responses

#### When to Use?

- ✅ Quick hardware verification
- ✅ Debugging protocol issues
- ✅ Testing without building Java/Kotlin code
- ❌ Production use (use Kotlin apps instead)

#### Protocol Compatibility

**Python vs Kotlin:**

- **Frame format**: 100% identical (STX, LEN, ADDR, CMD, DATA, CHKSUM, ETX)
- **Checksum algorithm**: Identical (XOR of all bytes)
- **Serial settings**: Python uses `termios` (8N1), Kotlin uses `jSerialComm` (8N1 or 8E1)

**Critical Difference:**

```python
# Python (ehl_protocol.py)
ser = serial.Serial(
    port='/tmp/vserial1',
    baudrate=9600,
    bytesize=8,
    parity=serial.PARITY_NONE,  # 8N1
    stopbits=1
)
```

vs.

```kotlin
// Kotlin (SerialPortHandler.kt)
port.parity = SerialPort.NO_PARITY  // 8N1
```

**Both use 8N1!** So Python ↔ Kotlin simulator works perfectly.

---

### SOCAT - Virtual Serial Bridge

**Purpose**: Creates a virtual serial port pair for testing.

```bash
socat -d -d \
    pty,rawer,echo=0,link=/tmp/vserial0 \
    pty,rawer,echo=0,link=/tmp/vserial1
```

**Result:**
- `/tmp/vserial0` ↔ `/tmp/vserial1` are bidirectionally connected
- Data written to one appears on the other
- Simulates real serial cable connection

**Use Cases:**

1. **Simulator ↔ Webapp**:
   - Simulator opens `/tmp/vserial0`
   - Webapp opens `/tmp/vserial1`
   - No physical hardware needed

2. **Python ↔ Kotlin Testing**:
   - Python script opens `/tmp/vserial1`
   - Kotlin simulator opens `/tmp/vserial0`
   - Test protocol compatibility

---

## 🚀 Deployment Modes

### Mode 1: LAB (Development/Testing)

**Configuration:**

```yaml
lpg:
  mode: LAB
```

**What Happens:**
- Uses `InMemorySerialPort` + `EhlDispenserEmulator`
- No physical hardware required
- No SOCAT required
- Perfect for:
  - Frontend development
  - Integration tests
  - CI/CD pipelines

**Start:**

```bash
java -jar lpg-ehl-webapp.jar --lpg.mode=LAB
```

### Mode 2: FIELD (Production/Hardware)

**Configuration:**

```yaml
lpg:
  mode: FIELD
ehl:
  serial:
    port: /dev/ttyUSB0
    parity: EVEN
```

**What Happens:**
- Uses `SerialPortManager` + real RS-485 hardware
- Requires physical connection to dispenser
- For production deployment

**Start:**

```bash
java -jar lpg-ehl-webapp.jar \
    --lpg.mode=FIELD \
    --ehl.serial.port=/dev/ttyUSB0 \
    --ehl.serial.parity=EVEN
```

### Mode 3: FIELD with SOCAT (Simulator Testing)

**Configuration:**

```yaml
lpg:
  mode: FIELD
ehl:
  serial:
    port: /tmp/vserial1
    parity: NONE  # Matches Kotlin simulator
```

**What Happens:**
- Uses `SerialPortManager` + SOCAT virtual serial
- Connects to Kotlin simulator via `/tmp/vserial1`
- For testing FIELD mode logic without hardware

**Start:**

```bash
# Terminal 1: Start SOCAT + Simulator
./scripts/start-socat-sim.sh

# Terminal 2: Start Webapp
./scripts/start-webapp.sh
```

---

## ⚙️ Protocol & Transport Layer

### SerialTransport Interface (Core Abstraction)

```kotlin
package no.cloudberries.lpg.transport

interface SerialTransport {
    val isConnected: Boolean
    fun connect(): Boolean
    fun disconnect()
    fun write(data: ByteArray): Int
    fun readAvailable(maxBytes: Int = 256): ByteArray
    fun flush()
    fun clearBuffer()
}
```

### Implementations

| Implementation | Module | Purpose | Hardware |
|----------------|--------|---------|----------|
| `RealSerialTransport` | `lpg-transport` | Production | Real RS-485 |
| `InMemorySerialPort` | `lpg-ehl-emulator` | Testing | None |
| `MockSerialTransport` | `lpg-ehl-service` | Unit tests | None |

### EHL Communicator (Protocol Engine)

```kotlin
class EhlCommunicator(
    private val transport: SerialTransport
) {
    suspend fun sendAndReceive(packet: EhlPacket): EhlPacket {
        val encoded = EhlCodec.encode(packet)
        transport.write(encoded)
        
        val response = transport.readAvailable()
        return EhlCodec.decode(response)
            ?: throw ProtocolException("Invalid response")
    }
}
```

**Why Suspend?**
- Uses Kotlin coroutines for non-blocking I/O
- Plays nicely with Spring WebFlux (if used)
- Better resource utilization

---

## ⚠️ Critical Configuration Issues

### Issue 1: Parity Mismatch (MOST COMMON BUG)

**Problem:**

```yaml
# Webapp configuration
ehl:
  serial:
    port: /tmp/vserial1
    parity: EVEN  # ⚠️ WRONG for simulator
```

```kotlin
// Kotlin simulator (SerialPortHandler.kt:47)
port.parity = SerialPort.NO_PARITY  // 8N1
```

**Result**: Communication fails, simulator stops responding.

**Solution:**

```yaml
# Webapp configuration (for SOCAT/simulator)
ehl:
  serial:
    port: /tmp/vserial1
    parity: NONE  # ✅ Matches simulator
```

### Issue 2: Legacy Property Confusion

**Problem:**

```bash
java -jar lpg-ehl-webapp.jar \
    --ehl.transport.mode=SOCAT  # ⚠️ DEPRECATED, IGNORED
```

**What Happens:**
- `ehl.transport.mode` is a **legacy property** from old code
- It is **ignored** by the current implementation
- Webapp defaults to **LAB mode** (emulator)

**Solution:**

```bash
java -jar lpg-ehl-webapp.jar \
    --lpg.mode=FIELD  # ✅ Correct property
```

### Issue 3: Spring Config Precedence

**Problem:**

```bash
java -jar lpg-ehl-webapp.jar \
    --spring.config.location=file:./application-h2.yaml \
    --ehl.serial.parity=NONE  # ⚠️ Ignored!
```

**Why?**
- `--spring.config.location` **replaces** default configs
- CLI arguments don't always override file-based configs

**Solution:**

```bash
java -jar lpg-ehl-webapp.jar \
    --spring.config.additional-location=file:./application-h2.yaml \
    --ehl.serial.parity=NONE  # ✅ Now takes effect
```

---

## 🛠️ Development Workflow

### 1. Build All Modules

```bash
./mvnw clean install -DskipTests
```

### 2. Start Local Database

```bash
docker-compose -f docker-compose-local.yaml up -d
```

### 3. Option A: Run in IntelliJ (Recommended)

**Run Configuration: Webapp (LAB mode)**

```
Main class: no.cloudberries.lpg.api.LpgEhlApiApplicationKt
VM options: -Dlpg.mode=LAB
Program arguments: --spring.profiles.active=h2
Working directory: /Users/tandersen/git/NorgesGass/lpg-ehl
Module: lpg-ehl-webapp
```

**Run Configuration: Webapp (FIELD mode with simulator)**

```
Main class: no.cloudberries.lpg.api.LpgEhlApiApplicationKt
VM options: -Dlpg.mode=FIELD
Program arguments: 
  --ehl.serial.port=/tmp/vserial1
  --ehl.serial.parity=NONE
  --spring.profiles.active=h2
```

### 3. Option B: Run via Scripts

**Start simulator + webapp:**

```bash
# Terminal 1
./scripts/start-socat-sim.sh

# Terminal 2
./scripts/start-webapp.sh
```

### 4. Access UI

- **Web UI**: http://localhost:8080
- **Swagger API**: http://localhost:8080/swagger-ui.html
- **Control Panel**: http://localhost:8080/control

### 5. Run Tests

```bash
# Unit tests only
./mvnw test

# Integration tests
./mvnw verify

# Specific module
./mvnw test -pl lpg-ehl-service
```

---

## 📖 Summary of Key Concepts

### Module Responsibilities

| Module | Role | Dependencies | Spring? |
|--------|------|--------------|---------|
| `lpg-ehl-core` | Protocol logic | None | ❌ No |
| `lpg-transport` | RS-485 hardware | Core | ❌ No |
| `lpg-ehl-service` | Business logic (brain) | Core, Transport | ✅ Yes |
| `lpg-ehl-emulator` | In-memory simulator | Core | ✅ Yes |
| `lpg-ehl-serialport-sim` | Serial port simulator | Core, Transport | ❌ No |
| `lpg-ehl-webapp` | Web UI + API | Service | ✅ Yes |
| `lpg-ehl-app-headless` | Daemon | Service | ✅ Yes |

### Application Variants

| Variant | Use Case | Hardware | UI |
|---------|----------|----------|-----|
| Webapp (LAB) | Development | None | ✅ React |
| Webapp (FIELD) | Production | Real RS-485 | ✅ React |
| Headless (FIELD) | Production daemon | Real RS-485 | ❌ None |
| CLI tools | Debugging | Any | ❌ Terminal |

### Testing Tools

| Tool | Purpose | Language | Hardware |
|------|---------|----------|----------|
| Python scripts | Quick protocol tests | Python | Real/SOCAT |
| Kotlin simulator | Realistic testing | Kotlin | SOCAT |
| In-memory emulator | Unit tests | Kotlin | None |

### Critical Config Properties

```yaml
# Mode selection
lpg:
  mode: LAB | FIELD

# Serial port (FIELD mode only)
ehl:
  serial:
    port: /dev/ttyUSB0  # or /tmp/vserial1 for SOCAT
    baud-rate: 9600
    data-bits: 8
    stop-bits: 1
    parity: NONE | EVEN | ODD  # ⚠️ Must match hardware/simulator
```

---

## 📚 Related Documentation

- **[README.md](../README.md)** - Quick start guide
- **[WARP.md](project-overview/WARP.md)** - Technical deep dive
- **[PYTHON_INTEGRATION_ANALYSIS.md](PYTHON_INTEGRATION_ANALYSIS.md)** - Python compatibility
- **[IMPLEMENTATION_MODE_PARITY_FIX.md](IMPLEMENTATION_MODE_PARITY_FIX.md)** - Parity fix implementation

---

## ✅ Conclusion

The LPG-EHL system is a **clean, layered architecture** with:

1. **Core** - Pure protocol implementation (reusable, no Spring)
2. **Transport** - Hardware abstraction (real serial, SOCAT, or in-memory)
3. **Service** - Business logic (the brain, reusable across apps)
4. **Applications** - Webapp or headless daemon (delivery mechanisms)
5. **Testing Tools** - Python scripts, Kotlin simulator, in-memory emulator

The key to successful development is understanding:
- **Which module does what**
- **Which implementation to use** (real hardware vs simulator vs emulator)
- **How to configure parity correctly** (8N1 vs 8E1)

This architecture allows maximum flexibility:
- Develop with LAB mode (no hardware)
- Test with SOCAT + Kotlin simulator (realistic serial communication)
- Deploy with FIELD mode (real RS-485 hardware)

**All using the same business logic!** 🚀
