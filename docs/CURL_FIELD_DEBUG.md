# Field Debugging with Curl

This guide describes how to use the REST API for debugging in the field when running the `lpg-ehl-headless` application with the `debug-api` profile.

## Security

**Note: During the current testing phase, security is disabled. No Authorization header is required.**

```bash
java -Dspring.profiles.active=field,debug-api -jar lpg-ehl-headless.jar
```

## Basic Health Checks

### System Health
Health check is publicly accessible:
```bash
curl http://localhost:8090/actuator/health
```

### Application Info
```bash
curl http://localhost:8090/actuator/info
```

## Dispenser Debugging

### Get Dispenser State
Poll the current state of a dispenser (e.g., address 1):
```bash
curl http://localhost:8090/api/debug/state/1
```

### Get Full Status
```bash
curl http://localhost:8090/api/v1/pump/1/status
```

## Serial Port Debugging

### List Available Serial Ports
```bash
curl http://localhost:8090/api/debug/serial/ports
```

### Scan RS-485 Addresses
Scan for dispensers on a specific port:
```bash
curl -X POST "http://localhost:8090/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=10"
```

## Transaction & Authorization

### List Recent Transactions
```bash
curl http://localhost:8090/api/v1/transactions/recent
```

### Cleanup Stuck Authorizations
Resets all pumps to IDLE and cancels pending authorizations:
```bash
curl -X POST http://localhost:8090/api/v1/admin/cleanup-authorizations
```

## Logs

Logs are typically available via `journalctl`:
```bash
journalctl -u lpg-ehl -f
```
