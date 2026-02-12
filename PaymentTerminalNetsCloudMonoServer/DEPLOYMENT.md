# PaymentTerminalNetsCloudMonoServer Deployment

## Runtime Environment

- **Target:** Headless Linux (Debian/Ubuntu recommended)
- **Runtime:** Mono 6.x or later
- **Framework:** .NET Framework 4.8

### Mono Installation

```bash
# Debian/Ubuntu
sudo apt install mono-complete

# Verify
mono --version  # Should be 6.x or higher
```

### TLS Requirements

- Minimum TLS 1.2 for Connect@Cloud
- Ensure Mono is built with modern OpenSSL
- If TLS handshake fails: `export MONO_TLS_PROVIDER=btls`
- Verify cert store: `mozroots --sync` (if needed)

## Configuration

Copy `server.json.example` to `server.json` and configure:

```json
{
  "bindAddress": "127.0.0.1",
  "bindPort": 8080,
  "databasePath": "./data/payment_terminal.db",
  "receiptStoragePath": "./receipts",
  "connectCloud": {
    "environment": "QA",
    "baseUrl": null,
    "username": "$CONNECTCLOUD_USERNAME",
    "password": "$CONNECTCLOUD_PASSWORD",
    "terminalId": "12345678",
    "ecrIdPrefix": "POS-",
    "operatorIdDefault": "4321",
    "webSocketPath": "/ws/json",
    "loginTimeoutSeconds": 12,
    "openReadyTimeoutSeconds": 60
  }
}
```

### Connect@Cloud Hosts

| Environment | Host |
|-------------|------|
| QA | `connectcloud-test.aws.nets.eu` |
| PROD | `connectcloud.aws.nets.eu` |

Set `connectCloud.environment` to `"QA"` or `"PROD"`. Or override with `connectCloud.baseUrl`.

### Credential Handling

- Use environment variables: `"$CONNECTCLOUD_USERNAME"` and `"$CONNECTCLOUD_PASSWORD"`
- Never commit real credentials
- Restrict `server.json` permissions: `chmod 600 server.json`

## Build

```bash
cd PaymentTerminalNetsCloudMonoServer
dotnet build -f net48
```

## Run

```bash
export CONNECTCLOUD_USERNAME="your-username"
export CONNECTCLOUD_PASSWORD="your-password"
mono bin/Debug/net48/payment-terminal-nets-cloud-mono-server.exe server.json
```

## Tests

Requires Mono on the host (e.g. Linux):

```bash
cd PaymentTerminalNetsCloudMonoServer.Tests
dotnet test -f net48
```

On macOS without Mono, unit tests may fail. Integration tests require `INTEGRATION_TESTS=1` and valid QA credentials.

## Deployment Checklist

- [ ] Mono 6.x+ installed
- [ ] TLS 1.2+ verified (test against QA host)
- [ ] `server.json` configured with Connect@Cloud settings
- [ ] Credentials via env vars
- [ ] Database and receipt directories writable
- [ ] Firewall allows outbound HTTPS (443) to Connect@Cloud hosts
