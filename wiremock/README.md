# WireMock Configuration

This directory contains WireMock mappings for testing external API integrations locally.

## Directory Structure

```
wiremock/
├── mappings/          # JSON stub mappings (request/response definitions)
│   └── health-check.json
└── __files/           # Static response files (optional)
```

## Usage

### Starting WireMock

WireMock starts automatically when you run:

```bash
docker-compose -f docker-compose-local.yaml up
```

WireMock admin UI: http://localhost:8081/__admin

### Creating Stubs

Add JSON files to `mappings/` directory. Example:

```json
{
  "request": {
    "method": "GET",
    "urlPath": "/api/example"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "message": "Mock response"
    }
  }
}
```

### Testing Azure Sync Locally

1. Start Azurite (Azure Storage emulator): Already running in docker-compose
2. Azure Storage Queue endpoint: `http://localhost:10001`
3. Use Azure Storage Explorer or `az` CLI to inspect queues:
   ```bash
   # Install Azure Storage Explorer or use az CLI
   az storage queue list --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://localhost:10001/devstoreaccount1;"
   ```

### Verifying WireMock

```bash
# Test health endpoint
curl http://localhost:8081/health

# List all mappings
curl http://localhost:8081/__admin/mappings

# Reset all stubs
curl -X POST http://localhost:8081/__admin/reset
```

## Example Test Scenarios

### 1. Azure Queue Success
- Application sends transaction to Azurite queue
- Verify message in queue using Azure Storage Explorer

### 2. Azure Queue Failure (Retry Logic)
- Stop Azurite temporarily
- Observe retry with exponential backoff in logs
- Restart Azurite
- Verify eventual success

### 3. API Authentication
- Test with valid token: `Authorization: Bearer dev-token-12345`
- Test with invalid token → 401 Unauthorized

## References

- [WireMock Documentation](https://wiremock.org/docs/)
- [Azurite Documentation](https://github.com/Azure/Azurite)
