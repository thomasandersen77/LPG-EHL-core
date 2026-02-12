---
name: kotlin-ddd-patterns
description: Follow Domain Driven Design, SOLID, and Clean Code principles when working with Kotlin projects. Implements Controller-Service-Repository pattern with clear module separation. Use when creating or modifying Kotlin code, especially in multi-module Spring Boot projects with shared business logic.
---

# Kotlin DDD Patterns & Architecture

## Module Structure

This project follows a **multi-module architecture** where business logic is separated from presentation:

```
lpg-ehl/
├── lpg-ehl-api/              # API module (Controllers only)
│   └── controller/           # REST endpoints - THIN WRAPPER
│
├── lpg-ehl-service/          # Business logic + Database (CORE)
│   ├── service/              # Business services (TransactionService, PumpStateService)
│   ├── model/                # Domain entities (Transaction, PriceHistory)
│   ├── repository/           # JPA repositories (TransactionRepository)
│   ├── pump/                 # Domain: Pump management
│   ├── transaction/          # Domain: Transaction management
│   ├── price/                # Domain: Price management
│   └── payment/              # Domain: Payment processing (interfaces)
│
├── lpg-ehl-app-headless/    # Headless app (shares service module)
└── lpg-ehl-core/             # Protocol layer (NO Spring dependencies)
```

**Key Principle**: The `lpg-ehl-service` module contains **ALL business logic** and database access. Both `lpg-ehl-api` (web app) and `lpg-ehl-app-headless` depend on the service module to share the same business logic.

---

## Controller-Service-Repository Pattern

### Layer Responsibilities

**Controller (API module)**
- **ONLY** handles HTTP requests/responses
- Maps DTOs to domain objects
- Delegates ALL business logic to services
- Returns appropriate HTTP status codes
- Example: `PumpController`, `TransactionController`

**Service (Service module)**
- Contains ALL business logic
- Orchestrates domain operations
- Uses repositories for data access
- Implements domain rules and validations
- Example: `PumpStateService`, `TransactionService`, `PriceService`

**Repository (Service module)**
- Data access layer (Spring Data JPA)
- Query methods for entities
- NO business logic
- Example: `TransactionRepository`, `PriceHistoryRepository`

### Example Structure

```kotlin
// Controller (lpg-ehl-api)
@RestController
@RequestMapping("/api/v1/pumps")
class PumpController(
    private val pumpStateService: PumpStateService  // From service module
) {
    @PostMapping("/{address}/unblock")
    fun unblockPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        val result = pumpStateService.unblock(address, withAuthorization = true)
        return result.fold(
            onSuccess = { status -> ResponseEntity.ok(mapOf("success" to true, "state" to status.state)) },
            onFailure = { error -> ResponseEntity.status(409).body(mapOf("error" to error.message)) }
        )
    }
}

// Service (lpg-ehl-service)
@Service
class PumpStateService(
    private val transactionService: TransactionService,
    private val ehlCommunicator: EhlCommunicator,
    private val transactionRepository: TransactionRepository
) {
    fun unblock(address: Int, withAuthorization: Boolean): Result<PumpStatus> {
        // Business logic here
        val transaction = transactionService.createStartedTransaction(address, currentPriceKr)
        ehlCommunicator.sendUnblock(address)
        // ... more business logic
        return Result.success(pumpStatus)
    }
}

// Repository (lpg-ehl-service)
@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByDispenserAddress(address: Int, pageable: Pageable): Page<Transaction>
    fun findWithFilters(...): Page<Transaction>
}
```

---

## Domain Driven Design Principles

### Domain Entities

Entities live in the **service module** and represent core business concepts:

```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/transaction/Transaction.kt
@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    val transactionId: UUID = UUID.randomUUID(),
    
    val dispenserAddress: Int,
    val volumeDeciliters: Int,
    val amountOre: Int,
    
    @Enumerated(EnumType.STRING)
    val paymentStatus: PaymentStatus,
    
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

### Domain Services

Services encapsulate business logic that doesn't naturally fit in entities:

```kotlin
@Service
class TransactionService(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Create a new transaction when pump is unblocked.
     * 
     * Business rules:
     * - Transaction starts with volume = 0
     * - Status is STARTED
     * - Price is locked at creation time
     */
    @Transactional
    fun createStartedTransaction(
        dispenserAddress: Int,
        pricePerLiterKr: Double
    ): Transaction {
        val transaction = Transaction(
            dispenserAddress = dispenserAddress,
            volumeDeciliters = 0,
            amountOre = 0,
            paymentStatus = PaymentStatus.STARTED,
            pricePerLiterKr = pricePerLiterKr
        )
        return transactionRepository.save(transaction)
    }
}
```

### Value Objects

Use data classes for value objects (immutable, no identity):

```kotlin
data class PumpStatus(
    val address: Int,
    val state: PumpState,
    val volumeLitres: Double,
    val amountKr: Double,
    val pricePerLitreKr: Double
)
```

---

## SOLID Principles

### Single Responsibility Principle (SRP)

Each class has ONE reason to change:

- `TransactionService` - Only transaction lifecycle
- `PriceService` - Only price management
- `PumpStateService` - Only pump state orchestration

### Open/Closed Principle (OCP)

Use interfaces for extensibility:

```kotlin
// Port (interface)
interface PaymentGateway {
    fun startPayment(request: PaymentRequest): Payment
}

// Adapter implementations
class MockPaymentGateway : PaymentGateway { ... }
class SimulatedPaymentGateway : PaymentGateway { ... }
class NetsCloudPaymentGateway : PaymentGateway { ... }
```

### Liskov Substitution Principle (LSP)

Implementations must be substitutable:

```kotlin
@Service
class PumpStateService(
    private val paymentGateway: PaymentGateway  // Can be any implementation
)
```

### Interface Segregation Principle (ISP)

Keep interfaces focused:

```kotlin
// Good: Focused interface
interface EventPublisher {
    fun publishPriceUpdate(priceKr: Double)
    fun publishPumpStatusUpdate(status: PumpStatus)
}

// Bad: Too broad
interface SystemEvents {
    fun publishPriceUpdate(...)
    fun publishPumpStatusUpdate(...)
    fun publishTransactionUpdate(...)
    fun publishSystemHealth(...)
    // ... too many responsibilities
}
```

### Dependency Inversion Principle (DIP)

Depend on abstractions, not concretions:

```kotlin
// Service depends on interface (port)
@Service
class TransactionService(
    private val repository: TransactionRepository,  // Interface
    private val syncService: TransactionSyncService?  // Interface
)

// Not: private val postgresRepository: PostgresTransactionRepository
```

---

## Clean Code Practices

### Naming Conventions

- **Services**: `*Service` suffix (e.g., `TransactionService`, `PumpStateService`)
- **Repositories**: `*Repository` suffix (e.g., `TransactionRepository`)
- **Controllers**: `*Controller` suffix (e.g., `PumpController`)
- **Entities**: Domain names (e.g., `Transaction`, `PriceHistory`, `Customer`)
- **DTOs**: `*Request`, `*Response` suffixes (e.g., `PriceUpdateRequest`, `TransactionResponse`)

### Function Design

- **Small functions**: One responsibility per function
- **Descriptive names**: Function name describes what it does
- **Avoid side effects**: Pure functions when possible
- **Error handling**: Use `Result<T>` for operations that can fail

```kotlin
// Good: Clear, single responsibility
fun unblock(address: Int, withAuthorization: Boolean): Result<PumpStatus> {
    validatePumpState(address)
    val transaction = createTransactionIfNeeded(address)
    sendUnblockCommand(address)
    return updatePumpState(address, PumpState.READY_TO_PUMP)
}

// Bad: Too many responsibilities, unclear name
fun doStuff(address: Int): PumpStatus {
    // 50 lines of mixed concerns
}
```

### Documentation

**Always document**:
- **Classes**: Purpose, responsibilities, usage examples
- **Public methods**: Parameters, return values, exceptions, business rules
- **Complex logic**: Why, not just what
- **Business rules**: Domain-specific constraints

```kotlin
/**
 * Pump state service som kommuniserer med dispenser via EhlCommunicator.
 * 
 * Sender ekte EHL protokoll-kommandoer med HEX-logging:
 * - UNBLOCK: Frigir pumpe for leveranse
 * - BLOCK: Stopper leveranse
 * - VOLUME: Henter volumet under og etter pumping
 * 
 * Alle kommandoer logges med TX/RX HEX til Protocol-fanen.
 */
@Service
class PumpStateService(...) {
    
    /**
     * Unblock pump and start pumping flow.
     * 
     * Business rules:
     * - Pump must be in IDLE state
     * - Creates transaction if authorization exists
     * - Sends EHL UNBLOCK command to hardware
     * - Starts 60-second timeout if pumping doesn't start
     * 
     * @param address Dispenser address (1-8)
     * @param withAuthorization If true, requires active authorization
     * @return Result containing pump status or error
     */
    fun unblock(address: Int, withAuthorization: Boolean): Result<PumpStatus> {
        // Implementation
    }
}
```

### Code Organization

**Package structure** (service module):
```
no.cloudberries.lpg.service/
├── pump/                    # Domain: Pump management
│   ├── PumpStateService.kt
│   ├── PumpAuthorizationService.kt
│   └── DispenserStatusRepository.kt
├── transaction/             # Domain: Transaction management
│   ├── TransactionService.kt
│   ├── Transaction.kt
│   └── TransactionRepository.kt
├── price/                   # Domain: Price management
│   ├── PriceService.kt
│   ├── PriceHistory.kt
│   └── PriceHistoryRepository.kt
└── payment/                 # Domain: Payment (interfaces)
    └── PaymentGateway.kt
```

---

## Hexagonal Architecture (Ports & Adapters)

### Input Ports (Primary Adapters)

Controllers, CLI commands, scheduled tasks invoke services:

```kotlin
// Controller (adapter)
@RestController
class PumpController(
    private val pumpStateService: PumpStateService  // Port
)

// Scheduled task (adapter)
@Scheduled(fixedRate = 500)
fun pollVolume() {
    pumpStateService.updateVolume()  // Port
}
```

### Output Ports (Secondary Adapters)

Services depend on interfaces for external systems:

```kotlin
// Port (interface)
interface PaymentGateway {
    fun startPayment(request: PaymentRequest): Payment
}

// Service uses port
@Service
class TransactionService(
    private val paymentGateway: PaymentGateway  // Port, not implementation
)

// Adapter implementation
@Service
class MockPaymentGateway : PaymentGateway { ... }
```

---

## Creating New Features

### Step 1: Analyze Domain

Identify the domain concept:
- Is it a new entity? → Create entity in appropriate domain package
- Is it business logic? → Create service in appropriate domain package
- Is it data access? → Create repository in same domain package

### Step 2: Create Domain Layer (Service Module)

```kotlin
// 1. Entity (if needed)
@Entity
@Table(name = "my_entities")
data class MyEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    val name: String
)

// 2. Repository
@Repository
interface MyEntityRepository : JpaRepository<MyEntity, UUID> {
    fun findByName(name: String): MyEntity?
}

// 3. Service
@Service
class MyEntityService(
    private val repository: MyEntityRepository
) {
    /**
     * Create a new entity.
     * 
     * Business rules:
     * - Name must be unique
     * - Name cannot be empty
     */
    @Transactional
    fun create(name: String): MyEntity {
        require(name.isNotBlank()) { "Name cannot be empty" }
        require(repository.findByName(name) == null) { "Name must be unique" }
        
        val entity = MyEntity(name = name)
        return repository.save(entity)
    }
}
```

### Step 3: Create API Layer (API Module)

```kotlin
// Controller
@RestController
@RequestMapping("/api/v1/my-entities")
class MyEntityController(
    private val myEntityService: MyEntityService  // From service module
) {
    /**
     * Create a new entity.
     */
    @PostMapping
    fun create(@RequestBody request: CreateMyEntityRequest): ResponseEntity<MyEntityResponse> {
        val entity = myEntityService.create(request.name)
        return ResponseEntity.ok(MyEntityResponse.from(entity))
    }
}

// DTOs
data class CreateMyEntityRequest(
    val name: String
)

data class MyEntityResponse(
    val id: UUID,
    val name: String
) {
    companion object {
        fun from(entity: MyEntity) = MyEntityResponse(
            id = entity.id,
            name = entity.name
        )
    }
}
```

---

## Code Review Checklist

When reviewing or creating code, verify:

- [ ] **Module separation**: Controllers in API module, services in service module
- [ ] **No business logic in controllers**: Controllers only map HTTP ↔ domain
- [ ] **Services contain all business logic**: No logic in repositories or controllers
- [ ] **Repositories are data access only**: No business rules in repositories
- [ ] **Documentation**: All public methods have KDoc comments
- [ ] **SOLID principles**: Single responsibility, dependency inversion
- [ ] **Domain-driven**: Code reflects domain concepts, not technical concerns
- [ ] **Error handling**: Uses `Result<T>` for operations that can fail
- [ ] **Naming**: Clear, descriptive names following conventions
- [ ] **Package organization**: Code in appropriate domain packages

---

## Common Patterns

### Result Type for Error Handling

```kotlin
fun unblock(address: Int): Result<PumpStatus> {
    return try {
        validatePumpState(address)
        val status = performUnblock(address)
        Result.success(status)
    } catch (e: IllegalStateException) {
        Result.failure(e)
    }
}

// Usage
val result = pumpStateService.unblock(1)
result.fold(
    onSuccess = { status -> logger.info("Pump unblocked: {}", status.state) },
    onFailure = { error -> logger.error("Failed: {}", error.message) }
)
```

### Transaction Management

```kotlin
@Service
@Transactional(readOnly = true)  // Default: read-only
class TransactionService(...) {
    
    @Transactional  // Override for write operations
    fun createTransaction(...): Transaction {
        // Write operation
    }
    
    fun getTransaction(id: UUID): Transaction? {
        // Read operation (read-only)
    }
}
```

### Domain Events

```kotlin
// Publish domain events
@Service
class PumpStateService(
    private val eventPublisher: EventPublisher
) {
    fun unblock(address: Int) {
        // ... business logic
        eventPublisher.publishPumpStatusUpdate(pumpStatus)
    }
}
```

---

## Summary

1. **Module separation**: API module = controllers, Service module = business logic + database
2. **Controller-Service-Repository**: Clear layer responsibilities
3. **Domain Driven Design**: Entities, services, repositories organized by domain
4. **SOLID principles**: Single responsibility, dependency inversion, interfaces
5. **Clean Code**: Small functions, descriptive names, thorough documentation
6. **Hexagonal Architecture**: Ports (interfaces) and adapters (implementations)
7. **Documentation**: Always document classes, public methods, and business rules

When creating new code, start with the domain (service module), then add the API layer (API module). Keep business logic in services, never in controllers or repositories.
