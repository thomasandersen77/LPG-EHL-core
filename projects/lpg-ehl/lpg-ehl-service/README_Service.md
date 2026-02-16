# LPG-EHL Service Module

**Version:** 0.0.1-SNAPSHOT  
**Language:** Kotlin  
**Framework:** Spring Boot 3.x  
**Architecture:** Hexagonal Architecture (Ports & Adapters)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Module Structure](#module-structure)
- [Core Components](#core-components)
- [Business Logic Domains](#business-logic-domains)
- [Integration & External Systems](#integration--external-systems)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Testing](#testing)
- [Usage Examples](#usage-examples)

---

## Overview

**`lpg-ehl-service`** is the **central business logic and service layer** of the LPG-EHL system. This module contains all domain logic, business rules, and orchestration needed to manage LPG (Liquefied Petroleum Gas) dispensing operations at filling stations.

### Key Responsibilities

- **Business Logic**: All domain rules for fuel dispensing, transactions, pricing, and payments
- **Transaction Management**: Create, track, and persist fuel transactions
- **Pump State Management**: Monitor and control dispenser state machines
- **Price Management**: Handle dynamic pricing with historical tracking
- **Payment Processing**: Abstract payment gateway integrations
- **Azure Cloud Sync**: Queue and sync transactions to Azure Storage Queue
- **Hardware Monitoring**: Watchdog service for RS-485 connection health
- **Protocol Operations**: High-level EHL protocol operations (UNBLOCK, BLOCK, VOLUME, etc.)
- **Reporting & Diagnostics**: System health checks and operational reports

### Architectural Role

This module sits **at the heart** of the system, implementing the **hexagonal architecture core**:

```
┌─────────────────────────────────────────────────────────┐
│                   Adapters (Input)                      │
│   REST API │ CLI │ Scheduled Tasks │ WebSocket Events   │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────▼────────────┐
        │   lpg-ehl-service       │  ◄─── YOU ARE HERE
        │  (Business Logic Core)  │
        │                         │
        │ • TransactionService    │
        │ • PumpStateService      │
        │ • PriceService          │
        │ • PaymentGateway        │
        │ • AzureSyncService      │
        │ • HardwareWatchdog      │
        └────────────┬────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Adapters (Output)                          │
│  lpg-ehl-core │ lpg-transport │ PostgreSQL │ Azure      │
└─────────────────────────────────────────────────────────┘
```

**Design Principles:**

- **No framework coupling in domain logic**: Core services can run independently of Spring
- **Port interfaces**: Define contracts (`PaymentGateway`, `EventPublisher`, etc.) for dependency inversion
- **Adapter implementations**: Injected via Spring (e.g., `MockPaymentGateway`, `NoOpEventPublisher`)
- **Testability**: Services are easily unit-testable with mocked dependencies

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The service module implements **hexagonal architecture** to ensure clean separation of concerns:

#### Input Ports (Primary Adapters)
Services expose public methods that are invoked by:
- **REST Controllers** (webapp module)
- **CLI Commands** (cli module)  
- **Scheduled Tasks** (`@Scheduled` polling, watchdog checks)
- **Event Handlers** (WebSocket events, external triggers)

#### Output Ports (Secondary Adapters)
Services depend on **interfaces** (ports) for external interactions:
- `PaymentGateway` → Card payment processing
- `EventPublisher` → WebSocket broadcasts, message queue
- `HardwareWatchdogCapable` → Serial connection monitoring
- `EhlCommunicator` → Low-level protocol communication (lpg-ehl-core)
- JPA Repositories → Database persistence

#### Core Domain Logic
Pure business logic with no infrastructure dependencies:
- Transaction lifecycle management
- Pump state machine (IDLE → READY_TO_PUMP → PUMPING → PAYMENT_PENDING → IDLE)
- Price calculation and validation
- Authorization and payment flows

---

## Module Structure

```
lpg-ehl-service/
├── src/main/kotlin/no/cloudberries/lpg/service/
│   ├── azure/                    # Azure cloud integration
│   │   ├── AzureSyncService.kt           # Scheduled sync to Azure Storage Queue
│   │   ├── AzureSyncQueue.kt             # Entity for sync queue
│   │   ├── AzureSyncQueueRepository.kt   # JPA repository
│   │   └── AzureQueueReaderService.kt    # Read messages from Azure (optional)
│   │
│   ├── config/                   # Spring configuration
│   │   └── ServiceConfiguration.kt       # Bean definitions, conditional configs
│   │
│   ├── credit/                   # Credit account management
│   │   ├── Customer.kt                   # Customer entity
│   │   ├── CreditAccount.kt              # Credit account entity
│   │   └── CreditRepositories.kt         # JPA repositories
│   │
│   ├── dto/                      # Data Transfer Objects
│   │   └── ApiResponses.kt               # Response DTOs for REST API
│   │
│   ├── event/                    # Event publishing (Output Port)
│   │   └── EventPublisher.kt             # Interface + NoOpEventPublisher
│   │
│   ├── integration/              # External integrations
│   │   └── NetsCloudSocketClient.kt      # Nets payment terminal client
│   │
│   ├── model/                    # Shared domain models
│   │   └── [Domain entities]
│   │
│   ├── operations/               # EHL protocol operations
│   │   ├── EhlOperationsService.kt       # High-level EHL commands (UNBLOCK, VOLUME, etc.)
│   │   └── EhlResults.kt                 # Result types (VolumeResult, PriceResult, etc.)
│   │
│   ├── payment/                  # Payment processing (Output Port)
│   │   ├── PaymentGateway.kt             # Interface for payment providers
│   │   ├── MockPaymentGateway.kt         # Mock implementation for testing
│   │   └── SimulatedPaymentGateway.kt    # Simulated payment flow
│   │
│   ├── price/                    # Price management
│   │   ├── PriceService.kt               # Central price update service
│   │   ├── PriceHistory.kt               # Price history entity
│   │   └── PriceHistoryRepository.kt     # JPA repository
│   │
│   ├── pump/                     # Pump state management
│   │   ├── PumpStateService.kt           # Main pump orchestration service
│   │   ├── PumpAuthorizationService.kt   # Authorization flow for card payments
│   │   └── [Pump state models]
│   │
│   ├── repository/               # Database repositories
│   │   ├── DailySummaryRepository.kt     # Daily sales summaries
│   │   └── RoadTaxSettingsRepository.kt  # Road tax configuration
│   │
│   ├── service/                  # Utility services
│   │   ├── EhlPacketProcessor.kt         # Process incoming EHL packets
│   │   ├── MockProtocolService.kt        # Mock protocol for testing
│   │   └── WireTraceService.kt           # Wire-level protocol tracing
│   │
│   ├── system/                   # System services
│   │   ├── HardwareWatchdogService.kt    # RS-485 connection monitoring
│   │   ├── DiagnosticsService.kt         # System health checks
│   │   └── ReportService.kt              # Operational reporting
│   │
│   └── transaction/              # Transaction management
│       ├── TransactionService.kt         # Main transaction CRUD service
│       ├── TransactionSyncService.kt     # Queue transactions for Azure sync
│       ├── Transaction.kt                # Transaction entity
│       └── TransactionRepository.kt      # JPA repository
│
├── src/test/kotlin/               # Unit tests
│   └── no/cloudberries/lpg/service/
│       └── service/
│           ├── DispenserServiceTest.kt
│           ├── HardwareWatchdogServiceTest.kt
│           └── DiagnosticsServiceTest.kt
│
├── pom.xml                        # Maven configuration
└── README_Service.md              # This file
```

---

## Core Components

### 1. TransactionService

**Location:** `transaction/TransactionService.kt`

**Purpose:** Manage the complete lifecycle of fuel transactions from creation to payment.

#### Key Methods

```kotlin
// Create a new transaction when pump is unblocked
fun createStartedTransaction(
    dispenserAddress: Int,
    pricePerLiterKr: Double
): Transaction

// Update transaction volume during/after pumping
fun updateTransactionVolume(
    transactionId: UUID,
    volumeLiters: Double,
    amountKr: Double,
    newStatus: String? = null
): Transaction?

// Mark transaction as paid
fun markTransactionPaid(
    transactionId: UUID,
    paymentMethod: String = "CARD"
): Transaction?

// Query transactions with filters
fun getTransactions(
    from: LocalDateTime?,
    to: LocalDateTime?,
    dispenserAddress: Int?,
    paymentType: String?,
    paymentStatus: String?,
    customerId: UUID?,
    page: Int = 0,
    size: Int = 50
): PageResponse<TransactionResponse>
```

#### Transaction Lifecycle

```
┌─────────────┐
│   STARTED   │  ◄─── Pump unblocked (FRI PUMPE pressed)
└──────┬──────┘
       │
       │ Pumping begins
       ▼
┌─────────────┐
│   PENDING   │  ◄─── Pump stopped, awaiting payment
└──────┬──────┘
       │
       │ Payment confirmed
       ▼
┌─────────────┐
│    PAID     │  ◄─── Transaction completed
└─────────────┘
```

#### Database Integration

- Persists to `transactions` table via `TransactionRepository`
- Queues for Azure sync via `TransactionSyncService`
- Supports filtering by date range, pump, payment type, and status

---

### 2. PumpStateService

**Location:** `pump/PumpStateService.kt`

**Purpose:** Orchestrate dispenser state machine and manage real-time pumping operations.

#### Pump State Machine

```
        ┌─────────┐
        │  IDLE   │ ◄────────────────────┐
        └────┬────┘                      │
             │ unblock()                 │
             ▼                           │
    ┌────────────────┐                  │
    │ READY_TO_PUMP  │                  │
    │ (60s timeout)  │                  │
    └────┬───────────┘                  │
         │ startPumping()               │
         ▼                               │
    ┌──────────┐                        │
    │ PUMPING  │                        │
    └────┬─────┘                        │
         │ block()                      │
         ▼                               │
 ┌────────────────────┐                 │
 │ PAYMENT_PENDING    │                 │
 └────┬───────────────┘                 │
      │ settle() / confirmPayment()     │
      └─────────────────────────────────┘
```

#### Key Features

- **Real EHL Protocol**: Sends actual UNBLOCK, BLOCK, VOLUME commands via `EhlCommunicator`
- **HEX Logging**: All TX/RX bytes logged for debugging
- **60-Second Timeout**: Auto-BLOCK if pumping doesn't start after UNBLOCK
- **Volume Polling**: Queries volume every 500ms during pumping
- **Transaction Integration**: Creates and updates transactions in real-time
- **Price Synchronization**: Reads price from database on startup
- **Authorization Support**: Links to `PumpAuthorizationService` for card payment flows

#### Key Methods

```kotlin
// Unblock pump (send EHL UNBLOCK command)
fun unblock(address: Int = 1): Result<PumpStatus>

// Start pumping (cancel 60s timeout)
fun startPumping(address: Int = 1): Result<PumpStatus>

// Block pump (send EHL BLOCK command, read final volume)
fun block(address: Int = 1): Result<PumpStatus>

// Settle transaction (mark as paid, reset pump)
fun settle(address: Int = 1, paymentMethod: String = "CARD"): SettledTransaction?

// Confirm payment (for external payment flows)
fun confirmPayment(address: Int = 1, paymentMethod: String = "SIMULATION"): Result<Transaction>

// Update price for all pumps
fun updatePrice(priceKr: Double, roadTaxEnabled: Boolean = true)
```

#### Scheduled Tasks

```kotlin
@Scheduled(fixedRate = 500)  // Every 500ms
fun pollVolume() {
    // Query volume from dispenser during pumping
    // Log every 0.5L milestone
    // Update transaction in database
}
```

---

### 3. PriceService

**Location:** `price/PriceService.kt`

**Purpose:** Centralized price management with database persistence and real-time propagation.

#### Price Update Flow

```
updatePrice(priceKr)
   │
   ├─► 1. Save to database (price_history table)
   │
   ├─► 2. Update emulator (if LAB mode)
   │
   └─► 3. Publish event (WebSocket broadcast to UI)
```

#### Key Methods

```kotlin
// Update price and broadcast to all systems
fun updatePrice(
    productCode: String = "LPG",
    productName: String = "LPG (Flytende petroleumsgass)",
    pricePerLiter: BigDecimal,
    createdBy: String = "admin"
): PriceHistory

// Get current active price
fun getCurrentPrice(productCode: String = "LPG"): PriceHistory?

// Get price history
fun getPriceHistory(productCode: String = "LPG"): List<PriceHistory>
```

#### Database Schema

```sql
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    price_per_liter DECIMAL(10, 2) NOT NULL,
    vat_rate DECIMAL(5, 4) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### Integration Points

- **PumpStateService**: Reads current price on startup via `getCurrentPrice()`
- **Emulator**: Syncs price to `EhlDispenserEmulator` if available
- **EventPublisher**: Broadcasts `publishPriceUpdate()` for real-time UI updates

---

### 4. EhlOperationsService

**Location:** `operations/EhlOperationsService.kt`

**Purpose:** High-level wrapper around EHL protocol commands with domain-level result types.

#### Key Features

- **Coroutine-based**: All operations are `suspend` functions
- **Framework-agnostic**: No Spring dependencies, can be used in CLI, tests, or other contexts
- **Rich result types**: Returns domain objects instead of raw packets
- **Comprehensive error handling**: Uses Kotlin `Result<T>` for safe error propagation

#### Available Operations

```kotlin
// Test connectivity
suspend fun linetest(address: Int): EhlPacket

// Query dispenser state
suspend fun getState(address: Int): EhlPacket

// Query and parse volume
suspend fun getVolume(address: Int): VolumeResult

// Query and parse price
suspend fun getPrice(address: Int): PriceResult

// Unblock dispenser
suspend fun unblock(address: Int): Result<Unit>

// Block dispenser
suspend fun block(address: Int): Result<Unit>

// Query error status
suspend fun getError(address: Int): ErrorResult

// Query tank level
suspend fun getTank(address: Int): TankResult

// Run VB6 compatibility test sequence
suspend fun runVb6Sequence(address: Int): SequenceResult
```

#### Result Types

```kotlin
data class VolumeResult(
    val volumeLitres: Double,
    val pumpNumber: Int,
    val raw: EhlPacket
)

data class PriceResult(
    val pricePerLitreCents: Int,
    val raw: EhlPacket
)

data class ErrorResult(
    val hasError: Boolean,
    val errorCode: Int?,
    val errorDescription: String?,
    val raw: EhlPacket
)
```

---

### 5. HardwareWatchdogService

**Location:** `system/HardwareWatchdogService.kt`

**Purpose:** Monitor RS-485 serial connection health and automatically attempt reconnection when connection fails.

#### Monitoring Strategy

- **Health checks every 30 seconds** (after 1-minute startup delay)
- **Attempt-based failure threshold**: Reconnect after repeated failed command attempts
- **Silence is OK**: No reconnect on inactivity unless recent attempts failed
- **Exponential backoff**: 3 retry attempts with 5-minute cooldown
- **Automatic reconnection**: Calls `reconnect()` on `HardwareWatchdogCapable` implementation

#### Configuration

```yaml
# Health check runs every 30s (configurable via @Scheduled)
spring:
  task:
    scheduling:
      pool:
        size: 2  # Separate thread pool for watchdog
```

#### Key Methods

```kotlin
// Enable watchdog monitoring
fun initialize()

// Periodic health check (runs automatically)
@Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
fun performHealthCheck()

// Get current statistics
fun getStatistics(): WatchdogStatistics

// Manual reconnect trigger
fun forceReconnect(): Boolean
```

#### Statistics

```kotlin
data class WatchdogStatistics(
    val isEnabled: Boolean,
    val consecutiveFailures: Int,
    val reconnectAttempts: Int,
    val lastSuccessfulCheckTime: Long,
    val timeSinceLastData: Long  // telemetry only
)
```

---

### 6. AzureSyncService

**Location:** `azure/AzureSyncService.kt`

**Purpose:** Queue and sync transactions to Azure Storage Queue with automatic retry logic.

#### Sync Strategy

- **Scheduled sync**: Runs at configurable intervals (default: every 60 seconds)
- **Batch processing**: Processes up to N items per batch (default: 50)
- **Retry logic**: Exponential backoff with max retries (default: 3)
- **Status tracking**: PENDING → IN_PROGRESS → SYNCED/FAILED

#### Configuration

```yaml
azure:
  enabled: true  # Enable/disable Azure sync
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    queue-name: lpg-transactions
  sync:
    interval-seconds: 60
    batch-size: 50
    max-retries: 3
```

#### Sync Queue Entity

```kotlin
@Entity
@Table(name = "azure_sync_queue")
data class AzureSyncQueue(
    @Id
    val queueId: UUID = UUID.randomUUID(),
    
    val entityType: String,      // "TRANSACTION", "PRICE_UPDATE", etc.
    val entityId: UUID,           // ID of the entity to sync
    
    @Column(columnDefinition = "jsonb")
    val payload: Map<String, Any>,  // JSON payload to send to Azure
    
    var status: SyncStatus = SyncStatus.PENDING,
    var retryCount: Int = 0,
    var lastError: String? = null,
    var syncedAt: LocalDateTime? = null,
    
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class SyncStatus {
    PENDING, IN_PROGRESS, SYNCED, FAILED
}
```

#### Key Methods

```kotlin
// Scheduled sync job
@Scheduled(fixedDelayString = "\${azure.sync.interval-seconds}000")
fun syncPendingItems()

// Sync a single item
fun syncItem(item: AzureSyncQueue)

// Get sync statistics
fun getSyncStatus(): SyncStatusResponse

// Manual retry for a specific item
fun retrySyncItem(queueId: UUID): Boolean

// Cleanup old synced items (runs daily at 2 AM)
@Scheduled(cron = "0 0 2 * * *")
fun cleanupOldSyncedItems()
```

---

## Business Logic Domains

### Transaction Domain

**Files:**
- `transaction/Transaction.kt` - Entity
- `transaction/TransactionService.kt` - Business logic
- `transaction/TransactionRepository.kt` - Data access
- `transaction/TransactionSyncService.kt` - Azure sync

**Responsibilities:**
- Create transactions when pump is unblocked
- Update transaction volume during pumping
- Track payment status (STARTED → PENDING → PAID)
- Query transaction history with filters
- Queue transactions for Azure sync

**Database Schema:**

```sql
CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    dispenser_address INT NOT NULL,
    nozzle_number INT NOT NULL,
    volume_deciliters INT NOT NULL,
    amount_ore INT NOT NULL,
    price_per_liter DECIMAL(10, 2) NOT NULL,
    payment_type VARCHAR(50),
    payment_status VARCHAR(50) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    includes_road_tax BOOLEAN NOT NULL,
    customer_id UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

### Pump Management Domain

**Files:**
- `pump/PumpStateService.kt` - State machine orchestration
- `pump/PumpAuthorizationService.kt` - Authorization flow

**Responsibilities:**
- Manage pump state machine (IDLE → READY_TO_PUMP → PUMPING → PAYMENT_PENDING → IDLE)
- Send EHL protocol commands (UNBLOCK, BLOCK, VOLUME)
- Poll volume during pumping
- Handle 60-second timeout after UNBLOCK
- Link transactions to authorizations
- Reset pump after payment

**State Persistence:**

Pump state is kept **in-memory** (`ConcurrentHashMap`) for performance. Transactions are persisted to database.

---

### Price Management Domain

**Files:**
- `price/PriceService.kt` - Business logic
- `price/PriceHistory.kt` - Entity
- `price/PriceHistoryRepository.kt` - Data access

**Responsibilities:**
- Store price history with timestamps
- Query current active price
- Update price across all systems (database, emulator, UI)
- Calculate price with VAT

**Database Schema:**

```sql
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    price_per_liter DECIMAL(10, 2) NOT NULL,
    vat_rate DECIMAL(5, 4) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_product_effective (product_code, effective_from DESC)
);
```

---

### Payment Domain

**Files:**
- `payment/PaymentGateway.kt` - Interface (Port)
- `payment/MockPaymentGateway.kt` - Mock implementation
- `payment/SimulatedPaymentGateway.kt` - Simulated flow

**Responsibilities:**
- Abstract payment provider integrations
- Support multiple payment methods (CARD, CREDIT, VIPPS)
- Handle payment lifecycle (PENDING → APPROVED/DECLINED)

**Interface:**

```kotlin
interface PaymentGateway {
    fun startPayment(request: PaymentRequest): Payment
    fun getPayment(id: UUID): Payment?
}

data class PaymentRequest(
    val amountCents: Long,
    val method: PaymentMethod,
    val reference: String,
    val metadata: Map<String, String> = emptyMap()
)

data class Payment(
    val id: UUID,
    val requestedAt: Instant,
    val completedAt: Instant?,
    val amountCents: Long,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val reference: String,
    val metadata: Map<String, String>
)
```

---

### System Monitoring Domain

**Files:**
- `system/HardwareWatchdogService.kt` - Connection monitoring
- `system/DiagnosticsService.kt` - Health checks
- `system/ReportService.kt` - Operational reports

**Responsibilities:**
- Monitor RS-485 serial connection health
- Automatic reconnection on failure
- System diagnostics and health checks
- Generate operational reports

---

## Integration & External Systems

### lpg-ehl-core (Protocol Layer)

**Dependency:** `lpg-ehl-core` module

**Usage:**
- `EhlCommunicator`: Send/receive EHL protocol packets
- `EhlPacket`, `EhlCommand`: Protocol data structures
- `EhlDataParser`: Parse volume, price, error data

**Example:**

```kotlin
val packet = EhlPacket(address = 1, command = EhlCommand.VOLUME)
val response = ehlCommunicator.sendAndReceive(packet, timeout = 3000)
val volumeLitres = EhlDataParser.parseVolumeDataVb6(response.data)
```

---

### lpg-transport (Serial Communication)

**Dependency:** `lpg-transport` module

**Usage:**
- `SerialPortManager`: Open/close serial port
- `SerialPortIO`: Read/write bytes to RS-485

**Note:** Service layer uses `EhlCommunicator` from `lpg-ehl-core`, which internally uses `lpg-transport`.

---

### PostgreSQL (Database)

**Dependency:** `spring-boot-starter-data-jpa`, `postgresql` driver

**Tables:**
- `transactions` - Fuel transactions
- `price_history` - Price changes over time
- `azure_sync_queue` - Pending Azure sync items
- `customers` - Customer accounts
- `credit_accounts` - Credit account balances
- `daily_summaries` - Daily sales reports
- `road_tax_settings` - Road tax configuration

**Migrations:** Managed by Liquibase (see `lpg-ehl-service/src/main/resources/db/changelog/`)

---

### Azure Storage Queue

**Dependency:** `azure-storage-queue`

**Usage:**
- Queue transactions for cloud sync
- Batch processing with retry logic
- Exponential backoff on failure

**Configuration:**

```yaml
azure:
  enabled: true
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    queue-name: lpg-transactions
```

---

### lpg-ehl-emulator (Optional)

**Dependency:** `lpg-ehl-emulator` (optional)

**Usage:**
- LAB mode: Use emulator instead of real hardware
- FIELD mode: Emulator is `null`, use real RS-485 connection

**Example:**

```kotlin
@Service
class PumpStateService(
    private val dispenserEmulator: EhlDispenserEmulator?  // Null in FIELD mode
) {
    fun updatePrice(priceKr: Double) {
        dispenserEmulator?.setPrice((priceKr * 100).toInt())
    }
}
```

---

## Configuration

### Application Properties

**File:** `application.yaml` (in webapp/headless/cli modules)

```yaml
# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lpgehl
    username: lpguser
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Use Liquibase for schema management

# Liquibase
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

# Azure Sync
azure:
  enabled: ${AZURE_ENABLED:false}
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING:}
    queue-name: lpg-transactions
  sync:
    interval-seconds: 60
    batch-size: 50
    max-retries: 3

# EHL Protocol
ehl:
  emulator:
    enabled: ${EHL_EMULATOR_ENABLED:true}  # LAB mode by default
  serial:
    port: ${EHL_SERIAL_PORT:/dev/ttyUSB0}
    baud-rate: 9600
    data-bits: 8
    stop-bits: 1
    parity: none
```

### Environment Variables

**Production (FIELD mode):**

```bash
EHL_EMULATOR_ENABLED=false
EHL_SERIAL_PORT=/dev/ttyUSB0
DB_HOST=localhost
DB_PASSWORD=secret123
AZURE_ENABLED=true
AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;..."
```

**Development (LAB mode):**

```bash
EHL_EMULATOR_ENABLED=true
DB_HOST=localhost
DB_PASSWORD=dev123
AZURE_ENABLED=false
```

---

## Dependencies

### Maven Dependencies

**Core Dependencies:**

```xml
<!-- Internal modules -->
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-ehl-core</artifactId>
</dependency>
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-transport</artifactId>
</dependency>
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-ehl-emulator</artifactId>
    <optional>true</optional>  <!-- Only in LAB mode -->
</dependency>

<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-integration</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>

<!-- Azure -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-queue</artifactId>
</dependency>

<!-- JSONB support -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
</dependency>
```

---

## Testing

### Unit Tests

**Location:** `src/test/kotlin/no/cloudberries/lpg/service/`

**Test Strategy:**
- Mock external dependencies (`EhlCommunicator`, `TransactionRepository`, etc.)
- Focus on business logic correctness
- Test state transitions in `PumpStateService`
- Verify transaction lifecycle in `TransactionService`

**Example Test:**

```kotlin
@Test
fun `should create transaction when pump is unblocked`() {
    // Given
    val dispenserAddress = 1
    val pricePerLiterKr = 16.50
    
    // When
    val transaction = transactionService.createStartedTransaction(
        dispenserAddress, 
        pricePerLiterKr
    )
    
    // Then
    assertNotNull(transaction.transactionId)
    assertEquals("STARTED", transaction.paymentStatus)
    assertEquals(dispenserAddress, transaction.dispenserAddress)
    assertEquals(0, transaction.volumeDeciliters)
}
```

### Integration Tests

**Strategy:**
- Use `@SpringBootTest` for full context
- Use Testcontainers for PostgreSQL
- Test full transaction flow from UNBLOCK to PAID

---

## Usage Examples

### Example 1: Update Price

```kotlin
@RestController
class PriceController(
    private val priceService: PriceService
) {
    @PostMapping("/api/v1/prices/update")
    fun updatePrice(@RequestBody request: PriceUpdateRequest): PriceHistory {
        return priceService.updatePrice(
            productCode = "LPG",
            productName = "LPG (Flytende petroleumsgass)",
            pricePerLiter = request.pricePerLiter,
            createdBy = "admin"
        )
    }
}
```

### Example 2: Unblock Pump

```kotlin
@RestController
class PumpController(
    private val pumpStateService: PumpStateService
) {
    @PostMapping("/api/v1/pumps/{address}/unblock")
    fun unblockPump(@PathVariable address: Int): ResponseEntity<PumpStatus> {
        return pumpStateService.unblock(address)
            .fold(
                onSuccess = { ResponseEntity.ok(it) },
                onFailure = { ResponseEntity.badRequest().build() }
            )
    }
}
```

### Example 3: Query Transactions

```kotlin
@RestController
class TransactionController(
    private val transactionService: TransactionService
) {
    @GetMapping("/api/v1/transactions")
    fun getTransactions(
        @RequestParam(required = false) from: LocalDateTime?,
        @RequestParam(required = false) to: LocalDateTime?,
        @RequestParam(required = false) dispenserAddress: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): PageResponse<TransactionResponse> {
        return transactionService.getTransactions(
            from, to, dispenserAddress, 
            paymentType = null, paymentStatus = null, customerId = null,
            page, size
        )
    }
}
```

### Example 4: Manual Azure Sync Retry

```kotlin
@RestController
class SyncController(
    private val azureSyncService: AzureSyncService
) {
    @PostMapping("/api/v1/sync/{queueId}/retry")
    fun retrySync(@PathVariable queueId: UUID): ResponseEntity<String> {
        val success = azureSyncService.retrySyncItem(queueId)
        return if (success) {
            ResponseEntity.ok("Sync retry initiated")
        } else {
            ResponseEntity.badRequest().body("Failed to retry sync")
        }
    }
}
```

---

## Summary

The **`lpg-ehl-service`** module is the **heart of the LPG-EHL system**, containing all business logic and orchestration:

- **Hexagonal Architecture**: Clean separation between domain logic and infrastructure
- **Transaction Management**: Full lifecycle from STARTED to PAID
- **Pump Orchestration**: Real-time state machine with EHL protocol integration
- **Price Management**: Centralized pricing with historical tracking
- **Azure Sync**: Automatic cloud sync with retry logic
- **Hardware Monitoring**: Watchdog service for connection health
- **Framework-agnostic Core**: Services can run in webapp, headless, or CLI contexts

**Next Steps:**
- Read `lpg-ehl-core/README.md` for EHL protocol details
- Read `lpg-ehl-webapp/README.md` for REST API documentation
- Read `ARCHITECTURE.md` for system-wide architecture overview

---

**Questions or Issues?**  
Contact the development team or file an issue in the repository.
