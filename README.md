# LPG-EHL Multi-Module Project

**Modern Kotlin implementation of the EHL (European Hexadecimal Language) protocol for LPG dispenser control**

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Tests](https://img.shields.io/badge/tests-61%20passed-brightgreen)](https://github.com/thomasandersen77/LPG-EHL-core)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-blue)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/java-21-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 📖 About

This is a multi-module Maven project that implements the EHL protocol for controlling LPG (Liquefied Petroleum Gas) dispensers via RS-485 communication. The project is organized into:

- **lpg-ehl-core**: Core protocol implementation with real serial port communication
- **lpg-ehl-emulator**: Emulator for testing without physical hardware
- **lpg-ehl-api**: Spring Boot REST API with Azure sync and full observability
- **lpg-web**: React frontend with modern UI and real-time updates
- **Docker + PostgreSQL**: Production-ready containerized deployment with local database
- **Azure Sync**: Automatic cloud backup and reporting integration with retry logic

## 📚 Documentation

### 🚀 Getting Started
- **[QUICK START GUIDE](QUICK_START.md)** ⭐ **START HER!** - Raskeste vei til å få systemet opp og kjøre
- **[IntelliJ IDEA Setup](docs/development/INTELLIJ_SETUP.md)** - Quick start for debugging in IntelliJ
- **[IntelliJ Full Stack](docs/development/INTELLIJ_FULL_STACK.md)** - Run entire system with one button
- **[Demo Guide](docs/development/DEMO_GUIDE.md)** - Complete demo setup for presentations

### 📋 Project Overview
- **[WARP.md](docs/project-overview/WARP.md)** - Complete technical documentation for development
- **[Changelog](docs/project-overview/CHANGELOG.md)** - All notable changes to the project
- **[Status](docs/project-overview/STATUS.md)** - Current build and test status
- **[Executive Summary](docs/EXECUTIVE_SUMMARY.md)** - Quick overview for decision makers

### 🏗️ Architecture & Implementation
- **[Architecture Analysis](docs/ARCHITECTURE_ANALYSIS.md)** - Detailed technical comparison
- **[Ports & Adapters Plan](docs/implementation/IMPLEMENTATION_PLAN_PORTS_ADAPTERS.md)** - Clean architecture implementation
- **[Comprehensive Report](docs/implementation/COMPREHENSIVE_IMPLEMENTATION_REPORT.md)** - Multi-station implementation
- **[VB6 Protocol Complete](docs/implementation/VB6_PROTOCOL_IMPLEMENTATION_COMPLETE.md)** - VB6 compatibility details
- **[Payment Pending Testing](docs/implementation/TESTING_PAYMENT_PENDING.md)** - Payment flow testing

### 🚀 Deployment
- **[Deployment Guide (Norwegian)](docs/deployment/DEPLOYMENT_NO.md)** - Deployment til Linux ARK-maskin
- **[Docker Deploy](docs/deployment/DOCKER_DEPLOY.md)** - Docker Compose deployment guide
- **[Monolith Deployment](docs/deployment/MONOLITH_DEPLOYMENT.md)** - Single JAR deployment
- **[Render Deploy](docs/deployment/RENDER_DEPLOY.md)** - Cloud deployment to Render.com

### 🔧 Development
- **[Multi-Station Setup](docs/development/MULTI-STATION-SETUP.md)** - Run 3 Edge stations + Cloud simultaneously
- **[API Documentation](lpg-ehl-api/README.md)** - REST API endpoints and configuration
- **[Frontend Documentation](lpg-web/README.md)** - React frontend setup and usage

### 📜 Legacy & History
- **[Legacy Analysis](docs/legacy/LEGACY_ANALYSIS.md)** - VB6/Python legacy code analysis
- **[ZIP Contents Manifest](docs/legacy/ZIP_CONTENTS_MANIFEST.md)** - Archive contents documentation

## 🏗️ Project Structure

```
LPG-EHL-core/
├── pom.xml                          # Parent POM (multi-module Maven)
├── build_monolith.sh                # Builds a single runnable JAR (API + Web)
├── docker-compose.postgres.yaml     # Local Postgres + Azurite (queue emulator)
├── init-db.sql                      # Database schema (used by Liquibase)
├── release/
│   └── lpg-ehl-monolith.jar         # Monolith output from build_monolith.sh
├── lpg-ehl-core/                    # Core protocol implementation
├── lpg-ehl-emulator/                # Emulator for testing without hardware
├── lpg-ehl-api/                     # Spring Boot REST API + Azure queue sync
└── lpg-web/                         # React frontend (bundled into the monolith)
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** (Temurin recommended)
- **Node.js 18+** (needed to build the bundled React frontend)
- **Docker + Docker Compose** (local PostgreSQL + Azurite)
- **Maven 3.9+** (or just use `./mvnw`)
- **SDKMAN** (optional, recommended)

### Installation

1. **Clone the repository**
   ```bash
   git clone git@github.com:thomasandersen77/LPG-EHL-core.git
   cd LPG-EHL-core
   ```

2. **Install SDKMAN** (if not already installed)
   ```bash
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/sdkman-init.sh"
   ```

3. **Install project dependencies**
   ```bash
   sdk env install
   ```

4. **Build all modules**
   ```bash
   mvn clean install
   ```
   
   Expected output:
   ```
   Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
   BUILD SUCCESS
   ```

## 💳 Payment Integration - Nets Cloud Connect

The system uses **Nets Cloud Connect** for modern, cloud-based payment terminal integration.

### What This Means
- ✅ **No TCP socket management** - We communicate via REST API only
- ✅ **No terminal state tracking** - Nets handles all terminal connectivity
- ✅ **No binary protocols** - Clean REST JSON instead of hex/ECR protocols
- ✅ **Simplified testing** - Easy to mock REST endpoints

### Architecture
```
LPG-EHL API → REST → Nets Cloud → ECR Protocol → Payment Terminal
    (Us)              (Nets Managed)                 (Ingenico)
```

**What We Manage:**
- REST API calls to Nets Cloud
- Payment status polling
- Transaction recording

**What Nets Manages (NOT our responsibility):**
- Terminal connectivity and state
- TCP/ECR protocol handling
- Terminal firmware updates
- Network failover
- Card processing security

**Setup Guide:** See [`docs/NETS_CLOUD_CONNECT.md`](docs/NETS_CLOUD_CONNECT.md)

**Terminal Configuration:**
- ECR IP: `3.33.230.243` (Nets Cloud)
- ECR Port: `6001`
- Terminal connects TO Nets (not to our server)

---

## 🌩️ Cloud Integration & Simulation

This project implements a complete **Edge-to-Cloud** architecture for LPG stations.

### Architecture
- **Edge (Pump Station)**: This project (`lpg-ehl`). Runs locally on the station hardware.
  - Controls pumps via RS-485.
  - Stores transactions in local PostgreSQL.
  - Pushes transactions to **Azure Storage Queue** (via `azure_sync_queue` outbox table).
- **Cloud (Admin System)**: The `MinLPG` project (separate repo).
  - Consumes messages from Azure Storage Queue.
  - Provides admin dashboard for station owners.

### Running the Full Simulation (Local)

You can run the **Edge system (this repo)** locally with PostgreSQL + Azurite (queue emulator).
The **Cloud Admin System (MinLPG)** lives in a separate repo and can be run separately if you need end-to-end messaging.

1. **Start local dependencies (PostgreSQL + Azurite)**
   ```bash
   # Remove -d if you want logs in the foreground
   docker-compose -f docker-compose.postgres.yaml up -d
   ```

2. **Build the monolith JAR (API + Web UI)**
   ```bash
   ./build_monolith.sh
   ```

3. **Run the monolith**

   This uses the `local` profile by default (see `lpg-ehl-api/src/main/resources/application.yaml`).

   ```bash
   java -jar release/lpg-ehl-monolith.jar
   ```

### Simulation Tools

We provide scripts to simulate real-world usage:

- **Generate Traffic**: Creates random fuel transactions and pays them.
  ```bash
  ./scripts/simulate-traffic.sh      # 1 customer per hour
  ./scripts/simulate-traffic.sh 10   # 1 customer every 10 seconds (fast mode)
  ```

- **Monitor Azure Queue**: View raw messages being sent to the cloud.
  ```bash
  ./scripts/view-azurite-messages.sh --watch
  ```

### Access Points
- **Monolith UI (Web)**: http://localhost:8080
- **Monolith API**: http://localhost:8080/api/v1/
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Azurite Queue service**: http://localhost:10001

### Production Deployment (Pump Linux Machine)

1. **Prepare environment**
   ```bash
   # Create directories
   sudo mkdir -p /opt/lpg-ehl/{data,backups,logs,config}
   
   # Copy configuration
   cp .env.example .env
   nano .env  # Edit with production settings
   ```

2. **Configure environment**
   ```bash
   # .env file
   POSTGRES_PASSWORD=<strong-password>
   SERIAL_PORT=/dev/ttyUSB0
   DISPENSER_ADDRESS=1
   PRICE_PER_LITRE_CENTS=1590
   AZURE_CONNECTION_STRING=<your-azure-connection>
   API_AUTH_TOKEN=<random-token>
   ```

3. **Start production services**
   ```bash
   docker-compose up -d
   
   # Monitor logs
   docker-compose logs -f lpg-ehl-app
   
   # Check status
   docker-compose ps
   ```

## 🤖 AI Analysis - Zipping Modules

For AI analysis (Claude, ChatGPT, Gemini), you can create focused ZIP archives of specific modules or legacy code.

### Zip Individual Modules

**Core module (EHL protocol):**
```bash
zip -r lpg-ehl-core-for-ai.zip lpg-ehl-core/src \
    lpg-ehl-core/pom.xml \
    lpg-ehl-core/README.md \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"
```

**API module (Spring Boot REST API + Nets Cloud Connect):**
```bash
zip -r lpg-ehl-api-for-ai.zip lpg-ehl-api/src \
    lpg-ehl-api/pom.xml \
    lpg-ehl-api/README.md \
    lpg-ehl-api/.env.local.example \
    -x "*/target/*" "*/.idea/*" "*/node_modules/*"
```

**Emulator module:**
```bash
zip -r lpg-ehl-emulator-for-ai.zip lpg-ehl-emulator/src \
    lpg-ehl-emulator/pom.xml \
    lpg-ehl-emulator/README.md \
    -x "*/target/*" "*/.idea/*"
```

### Zip Legacy Code for Analysis

**VB6 Legacy Code (Original Dispenserkontroll):**
```bash
zip -r norgesgass-legacy-for-ai.zip norgesgass_legacy/ \
    -x "*/bin/*" "*/obj/*" "*/.vs/*"
```

**Python PoC (Proof-of-Concept re-implementation):**
```bash
zip -r python-legacy-for-ai.zip more_legacy/ehl_pumpekontroll_clone/ \
    -x "*/__pycache__/*" "*/.pytest_cache/*" "*/venv/*"
```

### Zip Documentation Only

**Project documentation:**
```bash
zip -r docs-for-ai.zip docs/ WARP.md README.md CHANGELOG.md \
    LEGACY_ANALYSIS.md IMPLEMENTATION_ROADMAP.md
```

**Archived Baxi protocol (for historical analysis):**
```bash
zip -r archived-baxi-for-ai.zip _archived/baxi-protocol/ \
    -x "*/Terminal/images/*"
```

### Quick Script - Zip Everything

Create all archives at once:
```bash
#!/bin/bash
# Save as scripts/zip-all-for-ai.sh

echo "Creating AI analysis archives..."

zip -r lpg-ehl-core-for-ai.zip lpg-ehl-core/src lpg-ehl-core/pom.xml lpg-ehl-core/README.md -x "*/target/*" "*/.idea/*"
echo "✓ Core module zipped"

zip -r lpg-ehl-api-for-ai.zip lpg-ehl-api/src lpg-ehl-api/pom.xml lpg-ehl-api/README.md lpg-ehl-api/.env.local.example -x "*/target/*" "*/.idea/*"
echo "✓ API module zipped"

zip -r lpg-ehl-emulator-for-ai.zip lpg-ehl-emulator/src lpg-ehl-emulator/pom.xml lpg-ehl-emulator/README.md -x "*/target/*" "*/.idea/*"
echo "✓ Emulator module zipped"

zip -r norgesgass-legacy-for-ai.zip norgesgass_legacy/ -x "*/bin/*" "*/obj/*" "*/.vs/*"
echo "✓ VB6 legacy zipped"

zip -r python-legacy-for-ai.zip more_legacy/ehl_pumpekontroll_clone/ -x "*/__pycache__/*" "*/.pytest_cache/*" "*/venv/*"
echo "✓ Python PoC zipped"

zip -r docs-for-ai.zip docs/ WARP.md README.md CHANGELOG.md LEGACY_ANALYSIS.md IMPLEMENTATION_ROADMAP.md
echo "✓ Documentation zipped"

zip -r archived-baxi-for-ai.zip _archived/baxi-protocol/ -x "*/Terminal/images/*"
echo "✓ Archived Baxi protocol zipped"

echo ""
echo "All archives created! Upload to AI for analysis:"
ls -lh *-for-ai.zip
```

**Make executable and run:**
```bash
chmod +x scripts/zip-all-for-ai.sh
./scripts/zip-all-for-ai.sh
   ```

4. **Verify deployment**
   ```bash
   # Health check
   curl http://localhost:8080/health
   
   # Check database
   docker exec lpg-ehl-postgres psql -U lpg_user -d lpg_ehl -c "SELECT COUNT(*) FROM transactions;"
   
   # View backups
   ls -lh /opt/lpg-ehl/backups/
   ```

### Architecture Overview

```
┌─────────────────────────────────────────────────┐
│         Pump Linux Machine                      │
│                                                 │
│  ┌──────────────┐    ┌──────────────┐          │
│  │ lpg-ehl-app  │───>│  PostgreSQL  │          │
│  │  (Docker)    │    │   (Docker)   │          │
│  │              │    │              │          │
│  │ - Protocol   │    │ - Transactions│         │
│  │ - REST API   │    │ - Events     │         │
│  │ - Serial I/O │    │ - Outbox     │         │
│  └──────┬───────┘    └──────┬───────┘          │
│         │                   │                  │
│         │              /opt/lpg-ehl/           │
│         │              ├─ data/     (DB)       │
│         │              ├─ backups/  (Hourly)   │
│         │              └─ logs/     (App)      │
│         │                                      │
│    /dev/ttyUSB0 (RS-485)                       │
│         │                                      │
│         ↓                                      │
│   ┌─────────────┐                              │
│   │  Dispenser  │                              │
│   │   Hardware  │                              │
│   └─────────────┘                              │
│                                                 │
│  ┌──────────────────────────────┐              │
│  │  Azure Sync (Background)     │              │
│  │  - Outbox pattern            │              │
│  │  - Retry queue               │              │
│  │  - 5min interval             │              │
│  └────────────┬─────────────────┘              │
│               │                                │
└───────────────┼────────────────────────────────┘
                ↓ (when network available)
       ┌────────────────┐
       │  Azure Cloud   │
       │  - PostgreSQL  │  (Replica/Backup)
       │  - Reports     │  (Norges Gass)
       │  - Invoicing   │
       └────────────────┘
```

### Database Schema

The PostgreSQL database includes:

- **transactions** - Master record of all fuel deliveries
- **protocol_events** - Detailed EHL protocol communication log
- **system_events** - Application health and errors
- **dispenser_status** - Current state of each dispenser
- **azure_sync_queue** - Outbox pattern for resilient cloud sync
- **daily_summary** (view) - Quick reporting
- **unsynced_transactions** (view) - Azure sync monitoring

See `init-db.sql` for complete schema.

### Backup Strategy

**Automatic backups:**
- Hourly backups (kept for 24 hours)
- Daily backups (kept for 7 days)
- Stored in `/opt/lpg-ehl/backups/`

**Manual backup:**
```bash
# Create manual backup
docker exec lpg-ehl-postgres pg_dump -U lpg_user lpg_ehl | gzip > manual_backup_$(date +%Y%m%d).sql.gz

# Restore from backup
gunzip < backup.sql.gz | docker exec -i lpg-ehl-postgres psql -U lpg_user -d lpg_ehl
```

### API Endpoints

The application exposes a REST API for external access:

```bash
# Health check (no auth)
GET /health

# Get transactions (requires API_AUTH_TOKEN)
GET /api/transactions?from=2024-01-01&to=2024-01-31
Authorization: Bearer <API_AUTH_TOKEN>

# Get dispenser status
GET /api/dispensers
Authorization: Bearer <API_AUTH_TOKEN>

# Daily summary
GET /api/reports/daily?date=2024-01-15
Authorization: Bearer <API_AUTH_TOKEN>
```

### Monitoring

```bash
# View all logs
docker-compose logs

# Follow specific service
docker-compose logs -f lpg-ehl-app
docker-compose logs -f postgres
docker-compose logs -f azure-sync

# Check disk usage
du -sh /opt/lpg-ehl/*

# Monitor database size
docker exec lpg-ehl-postgres psql -U lpg_user -d lpg_ehl -c "\l+"

# Check unsynced transactions
docker exec lpg-ehl-postgres psql -U lpg_user -d lpg_ehl -c "SELECT * FROM unsynced_transactions;"
```

## 📦 Modules

### lpg-ehl-core

Core implementation of the EHL protocol with:
- Complete packet encoding/decoding with XOR checksum validation
- RS-485 serial port communication (via jSerialComm)
- Transaction state machine for fuel delivery management
- 38 unit tests

See [lpg-ehl-core/README.md](lpg-ehl-core/README.md) for detailed documentation.

### lpg-ehl-emulator

Testing emulator that simulates an EHL dispenser without physical hardware:
- **EhlDispenserEmulator**: State machine with IDLE → DELIVERING → FINISHED flow
- **InMemorySerialPort**: In-memory serial port for testing
- Supports STATE, UNBLOCK, STOP, VOLUME commands
- Simulates fuel delivery with configurable flow rate
- 6 integration tests

## 🧪 Testing with the Emulator

The emulator allows you to test the protocol implementation without physical hardware:

```kotlin
import no.cloudberries.lpg.emulator.*
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand

// Create emulator with configuration
val emulator = EhlDispenserEmulator(
    address = 1,
    pricePerLitreCents = 1000,  // 10.00 kr/l
    litresPerSecond = 1.0        // 1 liter per second
)

// Connect via in-memory serial port
val port = InMemorySerialPort(emulator)
val comm = EhlCommunicator(port)
port.connect()

// Query initial state
comm.send(EhlPacket(1, EhlCommand.STATE))
val stateResponse = comm.receive()
println("State: ${stateResponse.data[0]}")  // 0 = IDLE

// Start delivery
comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
comm.receive()  // OK
val deliveryState = comm.receive()  // STATE = DELIVERING

// Wait for fuel to flow
delay(1500)

// Stop delivery
comm.send(EhlPacket(1, EhlCommand.STOP))
comm.receive()  // OK
comm.receive()  // STATE = FINISHED
val volume = comm.receive()  // VOLUME response

println("Delivered: ${volume.data} deciliters")
```

### Running Emulator Tests

```bash
cd lpg-ehl-emulator
mvn test
```

The integration tests demonstrate:
- Initial state query
- UNBLOCK and delivery start
- Complete delivery cycle
- Volume query during delivery
- Multiple delivery cycles
- Wrong address handling

## 🏗️ Architecture

### Dependency Inversion

The project uses dependency inversion for testability:

```
EhlCommunicator
    ↓ depends on
SerialPortIO (interface)
    ↑              ↑
    implements     implements
SerialPortManager  InMemorySerialPort
    ↑                   ↑
    (production)    (testing)
```

- **SerialPortIO**: Interface for serial port operations
- **SerialPortManager**: Production implementation using real serial ports (jSerialComm)
- **InMemorySerialPort**: Test implementation that communicates with emulator

### Emulator State Machine

```
IDLE → READY → DELIVERING → FINISHED
                     ↓
                  ERROR
```

**Commands:**
- `STATE`: Query current state and delivery volume
- `UNBLOCK`: Start delivery (IDLE/FINISHED → DELIVERING)
- `STOP`: Stop delivery (DELIVERING → FINISHED)
- `VOLUME`: Query volume and amount

**Fuel Delivery Simulation:**
- Volume = seconds × litresPerSecond
- Amount = volume × pricePerLitreCents

## 📚 Usage Examples

### Testing Protocol Implementation

```kotlin
// Use emulator for testing
val emulator = EhlDispenserEmulator(address = 1)
val port = InMemorySerialPort(emulator)
val comm = EhlCommunicator(port)

// Test STATE query
comm.send(EhlPacket(1, EhlCommand.STATE))
val response = comm.receive()
assertEquals(EhlCommand.STATE, response.command)
```

### Production Usage

```kotlin
// Use real serial port for production
val port = SerialPortManager(portName = "/dev/ttyUSB0")
val comm = EhlCommunicator(port)
port.connect()

// Same API as emulator
comm.send(EhlPacket(1, EhlCommand.STATE))
val response = comm.receive()
```

## 🛠️ Development

### Building

```bash
mvn clean install
```

### Running All Tests

```bash
mvn test
```

### Running Core Tests Only

```bash
cd lpg-ehl-core
mvn test
```

### Running Emulator Tests Only

```bash
cd lpg-ehl-emulator
mvn test
```

## 📋 EHL Protocol Reference

### Packet Structure

```
STX (0x20) | Length | Address | Command | Data (0-n) | Checksum (XOR) | ETX (0x36)
```

### Supported Commands

| Command | Code | Description |
|---------|------|-------------|
| OK | 30 | Command acknowledgement |
| ERROR | 37 | Error code data |
| STOP | 47 | Stop the dispenser |
| VOLUME | 69 | Give/take fuel amount |
| STATE | 75 | Give/take calculator state |
| UNBLOCK | 119 | Start delivery mode |

See [lpg-ehl-core/README.md](lpg-ehl-core/README.md) for complete protocol reference.

## 🗺️ Roadmap

### Current Status
- ✅ Multi-module Maven structure
- ✅ Core protocol implementation
- ✅ Serial port communication
- ✅ Transaction state machine
- ✅ EHL dispenser emulator
- ✅ Integration tests

### Future Enhancements
- [ ] Async message handling
- [ ] Database persistence layer
- [ ] REST API service layer
- [ ] WebSocket real-time updates
- [ ] Payment system integration
- [ ] Admin web interface
- [ ] Docker containerization

## 📄 Documentation

- [Core Module Documentation](lpg-ehl-core/README.md)
- [Implementation Guide](lpg-ehl-core/IMPLEMENTATION_GUIDE.md)
- [Emulator Instructions](emulator-instructions.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Re-implements and modernizes the original VB6 codebase (see `norgesgass_legacy/` for original source)
- EHL protocol specification from fuel dispenser manufacturers
- Built with Kotlin and modern JVM best practices

## 📧 Contact

Thomas Andersen - [@thomasandersen77](https://github.com/thomasandersen77)

Project Link: [https://github.com/thomasandersen77/LPG-EHL-core](https://github.com/thomasandersen77/LPG-EHL-core)
