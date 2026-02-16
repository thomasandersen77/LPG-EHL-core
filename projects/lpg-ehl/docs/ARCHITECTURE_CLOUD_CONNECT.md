# LPG-EHL Cloud Connect Architecture

## Complete System Architecture

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         LPG STATION (Edge System)                          │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────┐                    ┌─────────────────────┐
│                     │      RS-485        │                     │
│   LPG Dispenser     │◀──────────────────▶│   lpg-ehl-core      │
│   (Physical Pump)   │    Serial Port     │   (EHL Protocol)    │
│                     │    /dev/ttyUSB0    │                     │
└─────────────────────┘                    └──────────┬──────────┘
                                                      │
                                                      │ Transaction
                                                      │ State Machine
                                                      │
                                                      ▼
                                           ┌─────────────────────┐
                                           │                     │
                                           │   lpg-ehl-api       │
                                           │   (Spring Boot)     │
                                           │                     │
                                           └──────────┬──────────┘
                                                      │
                                        ┌─────────────┼─────────────┐
                                        │             │             │
                                        ▼             ▼             ▼
                              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
                              │              │  │              │  │              │
                              │ PostgreSQL   │  │ Nets Cloud   │  │ Azure Queue  │
                              │ (Local DB)   │  │ REST API     │  │ (Cloud Sync) │
                              │              │  │              │  │              │
                              └──────────────┘  └──────┬───────┘  └──────────────┘
                                                       │
                                                       │ HTTPS
                                                       │ REST API
                                                       │
┌───────────────────────────────────────────────────────────────────────────────┐
│                         NETS CLOUD (Managed Service)                          │
└───────────────────────────────────────────────────────────────────────────────┘
                                                       │
                                                       │ ECR Protocol
                                                       │ TCP Port 6001
                                                       │
                                                       ▼
                                           ┌─────────────────────┐
                                           │                     │
                                           │ Payment Terminal    │
                                           │ (Ingenico)          │
                                           │ IP: 3.33.230.243    │
                                           │                     │
                                           └─────────────────────┘
```

## Payment Flow - Cloud Connect

### 1. Customer Starts Fueling

```
┌────────┐                 ┌────────┐                 ┌────────┐
│Customer│                 │Dispenser                 │EHL Core│
└───┬────┘                 └───┬────┘                 └───┬────┘
    │                          │                          │
    │ 1. Insert nozzle         │                          │
    ├─────────────────────────▶│                          │
    │                          │                          │
    │                          │ 2. STATE query           │
    │                          ├─────────────────────────▶│
    │                          │                          │
    │                          │ 3. IDLE → ACTIVE         │
    │                          │◀─────────────────────────┤
    │                          │                          │
    │ 4. Dispense fuel         │                          │
    │◀─────────────────────────┤                          │
    │                          │                          │
```

### 2. Transaction Completion & Payment

```
┌────────┐    ┌─────┐    ┌──────────┐    ┌──────┐    ┌─────────┐
│Dispenser    │Core│    │API       │    │Nets  │    │Terminal │
└───┬────┘    └──┬──┘    └────┬─────┘    └───┬──┘    └────┬────┘
    │            │              │              │            │
    │ 1. FINISHED│              │              │            │
    ├───────────▶│              │              │            │
    │            │              │              │            │
    │            │ 2. Create TX │              │            │
    │            ├─────────────▶│              │            │
    │            │              │              │            │
    │            │              │ 3. POST /sale│            │
    │            │              ├─────────────▶│            │
    │            │              │              │            │
    │            │              │              │ 4. Push    │
    │            │              │              │  payment   │
    │            │              │              ├───────────▶│
    │            │              │              │            │
    │            │              │ 5. paymentId │            │
    │            │              │◀─────────────┤            │
    │            │              │              │            │
    │            │              │ 6. Poll status (every 500ms)
    │            │              ├─────────────▶│            │
    │            │              │  GET /status │            │
    │            │              │              │            │
    │            │              │   PENDING    │            │
    │            │              │◀─────────────┤            │
    │            │              │              │            │
    │            │              │              │  [Customer │
    │            │              │              │   inserts  │
    │            │              │              │   card]    │
    │            │              │              │            │
    │            │              │              │  Response  │
    │            │              │              │◀───────────┤
    │            │              │              │            │
    │            │              │ Poll status  │            │
    │            │              ├─────────────▶│            │
    │            │              │              │            │
    │            │              │  APPROVED ✓  │            │
    │            │              │◀─────────────┤            │
    │            │              │              │            │
    │            │ 7. Payment OK│              │            │
    │            │◀─────────────┤              │            │
    │            │              │              │            │
    │ 8. Release │              │              │            │
    │   nozzle   │              │              │            │
    │◀───────────┤              │              │            │
```

## Component Responsibilities

### LPG-EHL Core (lpg-ehl-core)
**What it manages:**
- ✅ RS-485 serial communication with dispenser
- ✅ EHL protocol encoding/decoding
- ✅ Transaction state machine (IDLE → ACTIVE → FINISHED)
- ✅ Volume and amount calculation
- ✅ XOR checksum validation

**What it does NOT manage:**
- ❌ Payment processing (delegated to API)
- ❌ Network communication
- ❌ Database persistence

### LPG-EHL API (lpg-ehl-api)
**What it manages:**
- ✅ REST API endpoints
- ✅ Payment gateway abstraction (`PaymentGateway` interface)
- ✅ Transaction persistence (PostgreSQL)
- ✅ Cloud sync (Azure Queue)
- ✅ Nets Cloud Connect integration

**What it does NOT manage:**
- ❌ Terminal connectivity (Nets Cloud)
- ❌ ECR protocol (Nets Cloud)
- ❌ Card processing (Nets Cloud)

### Nets Cloud Connect
**What Nets manages:**
- ✅ Terminal connectivity and health monitoring
- ✅ ECR protocol (TCP/hex/binary)
- ✅ Terminal firmware updates
- ✅ Network failover and retry logic
- ✅ PCI DSS compliance
- ✅ Card processing security

**What we do:**
- ✅ Call REST API to initiate payment
- ✅ Poll for payment status
- ✅ Handle approved/declined responses

## Technology Stack

### Communication Layers

| Layer | Protocol | Responsibility |
|-------|----------|----------------|
| **Dispenser ↔ Core** | RS-485 / EHL | lpg-ehl-core |
| **Core ↔ API** | In-process (JVM) | Transaction passing |
| **API ↔ Database** | JDBC / JPA | Spring Data |
| **API ↔ Nets Cloud** | HTTPS / REST | NetsCloudClient |
| **Nets Cloud ↔ Terminal** | TCP / ECR | Nets (managed) |
| **API ↔ Azure** | HTTPS / Queue | Azure SDK |

## Data Flow

### Transaction Lifecycle

```
1. Customer inserts nozzle
   └─▶ Dispenser: IDLE → ACTIVE
       └─▶ Core: StartTransaction()
           └─▶ API: POST /transactions {status: PENDING}
               └─▶ DB: INSERT transaction
   
2. Customer fuels
   └─▶ Dispenser: Dispensing (polling every 500ms)
       └─▶ Core: Update volume/amount
           └─▶ API: PATCH /transactions/:id
               └─▶ DB: UPDATE transaction
   
3. Customer returns nozzle
   └─▶ Dispenser: FINISHED
       └─▶ Core: CompleteTransaction()
           └─▶ API: POST /transactions/:id/complete
               └─▶ Payment Gateway: startPayment()
                   ├─▶ NetsCloudClient.initiateSale()
                   │   └─▶ Nets Cloud API
                   │       └─▶ Terminal: Show payment request
                   │
                   ├─▶ Poll status (500ms intervals, max 60s)
                   │   └─▶ NetsCloudClient.checkPaymentStatus()
                   │       └─▶ Nets Cloud API
                   │
                   └─▶ Payment result: APPROVED/DECLINED
                       └─▶ DB: UPDATE transaction (status, payment_id)
                           └─▶ Azure Queue: Enqueue sync message
   
4. Cloud sync (background)
   └─▶ API: Scheduled task (every 5 min)
       └─▶ DB: SELECT from azure_sync_queue (status=PENDING)
           └─▶ Azure Queue: SEND messages
               └─▶ MinLPG Cloud: Consume messages
```

## Configuration

### Environment Variables

```bash
# EHL Core
SERIAL_PORT=/dev/ttyUSB0
DISPENSER_ADDRESS=1
PRICE_PER_LITRE_CENTS=1590

# API
PORT=8080
DB_HOST=localhost
DB_PORT=5432

# Nets Cloud Connect
NETS_CLOUD_ENABLED=true
NETS_CLOUD_URL=https://api.nets.eu/terminal/v1
NETS_CLOUD_USERNAME=<from_nets>
NETS_CLOUD_PASSWORD=<from_nets>
NETS_TERMINAL_ID=42696609
NETS_MERCHANT_ID=<from_nets>

# Azure Sync
AZURE_ENABLED=true
AZURE_CONNECTION_STRING=<connection_string>
AZURE_QUEUE_NAME=lpg-transactions
```

## Security Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│ SECURE ZONE: LPG Station (Edge)                            │
│                                                             │
│  ┌────────────┐         ┌────────────┐                     │
│  │ Dispenser  │◀──────▶│ EHL Core   │                     │
│  │            │  RS-485 │            │                     │
│  └────────────┘         └────────────┘                     │
│                                                             │
│                         ┌────────────┐                     │
│                         │ API        │                     │
│                         │ PostgreSQL │                     │
│                         └──────┬─────┘                     │
│                                │ HTTPS                     │
└────────────────────────────────┼─────────────────────────┘
                                 │
                         ┌───────▼────────┐
                         │                │
                         │ INTERNET       │
                         │                │
                         └───────┬────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
┌───────────────┐      ┌─────────────────┐      ┌────────────────┐
│               │      │                 │      │                │
│ Nets Cloud    │      │ Azure Queue     │      │ MinLPG Cloud   │
│ (PCI DSS)     │      │ (Sync)          │      │ (Admin)        │
│               │      │                 │      │                │
└───────────────┘      └─────────────────┘      └────────────────┘
        │
        │ ECR (TCP)
        │
        ▼
┌───────────────┐
│               │
│ Terminal      │
│ (Secured by   │
│  Nets)        │
│               │
└───────────────┘
```

### Security Responsibilities

| Component | Security Responsibility |
|-----------|------------------------|
| **Our System** | - API authentication (tokens)<br>- Database encryption at rest<br>- HTTPS for all external calls<br>- Secrets management (.env) |
| **Nets Cloud** | - PCI DSS Level 1 compliance<br>- Terminal security<br>- Card data encryption<br>- Fraud detection |
| **Azure** | - Queue encryption<br>- Network security<br>- Identity management |

## Migration from Legacy TCP/ECR

### What Changed

**Before (Baxi Protocol):**
```
API ─TCP:8009─▶ [Our ECR Server] ─Hex Protocol─▶ Terminal
```

**After (Cloud Connect):**
```
API ─HTTPS─▶ [Nets Cloud] ─ECR Protocol─▶ Terminal
```

### Code Removed (Archived)
- `NetsBaxProtocol.kt` - 559 lines of TCP/hex protocol
- `EcrServer.kt` - TCP socket server (port 8009)
- `PaymentTerminal.kt` - Socket client
- All hex encoding/decoding utilities
- Terminal state management

### Code Added
- `NetsCloudConfig.kt` - Configuration properties
- `NetsCloudClient.kt` - REST API client (~220 lines)
- `NetsCloudPaymentGateway.kt` - PaymentGateway implementation (~235 lines)

**Net Result:**
- **Removed:** ~1200 lines of complex TCP/protocol code
- **Added:** ~500 lines of simple REST code
- **Complexity reduction:** ~60%

## Testing Strategy

### Local Development (No Terminal)
```kotlin
// Use SimulatedPaymentGateway
nets:
  cloud-connect:
    enabled: false
```

### Integration Testing (With Terminal)
```kotlin
// Use NetsCloudPaymentGateway
nets:
  cloud-connect:
    enabled: true
    base-url: https://test.api.nets.eu/terminal/v1
```

### Production
```kotlin
// Production Nets Cloud
nets:
  cloud-connect:
    enabled: true
    base-url: https://api.nets.eu/terminal/v1
```

## Monitoring & Observability

### Metrics to Track
- Payment initiation success rate
- Average payment polling time
- Terminal response time
- Payment approval/decline ratio
- API response times

### Logs to Monitor
```
# Payment flow
[INFO] Initiating sale: amount=10000 øre, reference=TXN-001
[INFO] Sale initiated successfully: paymentId=xyz123, status=PENDING
[DEBUG] Poll attempt 1/120: status=PENDING
[DEBUG] Poll attempt 2/120: status=PROCESSING
[INFO] Payment terminal: status=APPROVED
[INFO] Payment completed: amount=10000, status=APPROVED
```

## References

- **Nets Cloud Connect Setup:** [docs/NETS_CLOUD_CONNECT.md](NETS_CLOUD_CONNECT.md)
- **Terminal Configuration:** https://support.nets.eu/nb-NO/article/how-to-setup-your-terminal-for-connectcloud
- **API Documentation:** [../lpg-ehl-api/README.md](../lpg-ehl-api/README.md)
- **Migration Log:** [../CHANGELOG.md](../CHANGELOG.md)
