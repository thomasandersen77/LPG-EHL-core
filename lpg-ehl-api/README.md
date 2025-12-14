# LPG EHL API

Spring Boot REST API for LPG dispenser transaction management and Azure sync.

## Features

- 🔐 Bearer token authentication
- 📊 REST API for transactions, dispensers, and reports
- ☁️ Azure Storage Queue integration with retry logic
- 🐳 Docker support with Testcontainers
- 📝 OpenAPI/Swagger documentation
- 🧪 Full integration test suite
- 💾 PostgreSQL database with JSONB support

## Tech Stack

- **Kotlin 2.1.10**
- **Spring Boot 3.2.1**
- **PostgreSQL 16**
- **Azure Storage Queue 12.24.0**
- **OpenAPI 3.0**
- **Testcontainers**
- **WireMock**

## Getting Started

### Prerequisites

- JDK 21
- Docker & Docker Compose
- Maven 3.9+

### Local Development

1. **Start services (PostgreSQL + Azurite + WireMock)**
   ```bash
   docker-compose -f docker-compose-local.yaml up
   ```

2. **Run application**
   ```bash
   mvn spring-boot:run -pl lpg-ehl-api -Dspring-boot.run.profiles=local
   ```

3. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Running Tests

```bash
# Run all tests (uses Testcontainers)
mvn test -pl lpg-ehl-api

# Run specific test class
mvn test -pl lpg-ehl-api -Dtest=ApiIntegrationTest

# Run integration tests only
mvn verify -pl lpg-ehl-api
```

## API Endpoints

### Authentication

All endpoints (except `/actuator/health`) require Bearer token authentication:

```bash
Authorization: Bearer <your-token>
```

Default dev token: `dev-token-12345`

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/transactions` | List transactions (paginated) |
| GET | `/api/v1/transactions/{id}` | Get transaction by ID |
| GET | `/api/v1/transactions/unsynced` | List unsynced transactions |
| GET | `/api/v1/transactions/count` | Get transaction count |

**Example:**
```bash
curl -H "Authorization: Bearer dev-token-12345" \
  "http://localhost:8080/api/v1/transactions?page=0&size=10"
```

### Dispensers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dispensers` | List all dispensers |
| GET | `/api/v1/dispensers/{address}` | Get dispenser status |
| GET | `/api/v1/dispensers/active` | List active dispensers |

### Reports

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/reports/daily` | Daily summary |
| GET | `/api/v1/reports/period` | Period summary |
| GET | `/api/v1/reports/month/{year}/{month}` | Monthly summary |
| GET | `/api/v1/reports/year/{year}` | Yearly summary |

### Sync (Azure)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sync/status` | Get sync queue status |
| POST | `/api/v1/sync/retry/{queueId}` | Retry failed sync item |
| POST | `/api/v1/sync/trigger` | Manually trigger sync job |

### Health & Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check (public) |
| GET | `/actuator/metrics` | Prometheus metrics |
| GET | `/actuator/info` | Application info |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `lpg_ehl` |
| `DB_USER` | Database user | `lpg_user` |
| `DB_PASSWORD` | Database password | `lpg_password` |
| `API_AUTH_TOKEN` | Bearer token | `dev-token-12345` |
| `AZURE_ENABLED` | Enable Azure sync | `true` |
| `AZURE_CONNECTION_STRING` | Azure Storage connection | - |
| `AZURE_QUEUE_NAME` | Queue name | `lpg-transactions` |
| `AZURE_SYNC_INTERVAL` | Sync interval (seconds) | `300` |
| `AZURE_SYNC_BATCH_SIZE` | Batch size | `10` |
| `AZURE_SYNC_MAX_RETRIES` | Max retries | `3` |

### Profiles

- **default** - Production configuration
- **local** - Local development with Azurite
- **test** - Integration tests with Testcontainers

## Azure Sync

### How It Works

1. Transactions are saved to local PostgreSQL database
2. Database trigger adds entry to `azure_sync_queue` table
3. Scheduled job (`AzureSyncService`) processes pending items every N seconds
4. Items are sent to Azure Storage Queue
5. On failure, exponential backoff retry (30s, 60s, 120s, ...)
6. After max retries, items marked as FAILED

### Testing Locally with Azurite

Azurite is Microsoft's local Azure Storage emulator. It's automatically started with docker-compose.

**Connection string:**
```
DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://localhost:10001/devstoreaccount1;
```

**Inspect queues:**
```bash
# Using Azure Storage Explorer (GUI)
# Connect to: localhost:10001 with Azurite credentials

# Using Azure CLI
az storage queue list --connection-string "<azurite-connection-string>"
az storage message peek --queue-name lpg-transactions --connection-string "<azurite-connection-string>"
```

### Monitoring Sync Status

```bash
# Get sync statistics
curl -H "Authorization: Bearer dev-token-12345" \
  http://localhost:8080/api/v1/sync/status

# Response:
{
  "pendingCount": 5,
  "syncedCount": 100,
  "failedCount": 2,
  "lastSyncTime": "2025-12-13T19:45:00"
}
```

## Testing

### Integration Tests

Tests use **Testcontainers** to spin up real PostgreSQL database:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ApiIntegrationTest : BaseIntegrationTest() {
    // Tests with real database
}
```

### WireMock Stubs

Add mock responses to `wiremock/mappings/*.json`:

```json
{
  "request": {
    "method": "GET",
    "urlPath": "/api/external"
  },
  "response": {
    "status": 200,
    "jsonBody": { "mock": "data" }
  }
}
```

## Building for Production

```bash
# Build JAR
mvn clean package -pl lpg-ehl-api

# Run JAR
java -jar lpg-ehl-api/target/lpg-ehl-api-0.0.1-SNAPSHOT.jar

# Build Docker image
docker build -t lpg-ehl-api .

# Run in production mode
docker run -e AZURE_CONNECTION_STRING="<real-azure>" lpg-ehl-api
```

## Troubleshooting

### Database Connection Issues

```bash
# Test PostgreSQL connection
docker exec -it lpg-ehl-postgres-local psql -U lpg_user -d lpg_ehl

# Check if tables exist
\dt
```

### Azure Sync Not Working

```bash
# Check Azurite logs
docker logs lpg-ehl-azurite

# Manually trigger sync
curl -X POST -H "Authorization: Bearer dev-token-12345" \
  http://localhost:8080/api/v1/sync/trigger
```

### Authentication Failures

```bash
# Verify token in application.yaml
security.api-token=dev-token-12345

# Test with correct token
curl -H "Authorization: Bearer dev-token-12345" http://localhost:8080/api/v1/transactions
```

## Architecture

```
┌─────────────────┐
│   Dispenser     │
│   (EHL Core)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│   PostgreSQL    │◄────►│   REST API   │
│   (Master DB)   │      │ (Spring Boot)│
└────────┬────────┘      └──────┬───────┘
         │                       │
         │ Trigger               │ Scheduled
         ▼                       │
┌─────────────────┐              │
│ azure_sync_     │◄─────────────┘
│ queue (Outbox)  │
└────────┬────────┘
         │ Poll
         ▼
┌─────────────────┐      ┌──────────────┐
│ Azure Storage   │─────►│ Azure        │
│ Queue           │      │ Function     │
└─────────────────┘      └──────────────┘
```

## License

Proprietary - Cloudberries

## Support

For issues or questions, contact: Thomas Andersen
