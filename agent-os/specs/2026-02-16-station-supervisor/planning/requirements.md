# Requirements: Station Supervisor v1 (Kotlin)

## Purpose

Auto-update system for LPG station application with two operational modes:

1. **Normal maintenance**: Check Azure Blob manifest at app start + daily at 03:00; update only if safe (not busy)
2. **Emergency**: Receive "force update" via Azure IoT Hub (twin change or direct method) and apply ASAP (brief drain, then force)

## Runtime environment

- **Supervisor service**: `station-supervisor.service` (always-on, low CPU; mostly sleeping)
- **Main application**: `station-app.service` (Restart=always, managed by supervisor)
- **Platform**: Linux (systemd-based)
- **Language**: Kotlin/JVM

## Update storage (Azure Blob)

### Manifest structure

- **Blob location**: `.../station/manifest.json` (Azure Blob Storage)
- **Format**:
```json
{
  "latest": {
    "version": "1.8.2",
    "artifactUrl": "https://.../station-app-1.8.2.tar.gz",
    "sha256": "abc123..."
  },
  "force": {
    "enabled": false,
    "minVersion": "0.0.0",
    "reason": ""
  }
}
```

### Artifact access

- Use short-lived SAS URLs in manifest for private blob access
- `artifactUrl` points to private blob behind service that issues SAS tokens
- Follow Azure Device Update pattern for SAS-based downloads
- **Security**: SHA256 verification is mandatory for all downloads
- **Future**: Digital signatures can be added in v2

## "Busy" and "Healthy" signals

Supervisor must support **both** methods via configuration:

### Option A: File-based (lowest effort)
- Main app creates `/run/station.busy` during any transaction
- Main app creates `/run/station.healthy` when fully started
- Supervisor polls these files

### Option B: HTTP endpoints
- `GET http://127.0.0.1:<port>/internal/busy` → `{busy: true/false}`
- `GET http://127.0.0.1:<port>/internal/health` → `200 OK` when healthy
- Supervisor makes HTTP requests to check status

Configuration determines which method to use.

## Update triggers

### 1. On supervisor start
- Fetch manifest from Azure Blob
- If `force.minVersion > currentVersion` → treat as emergency update
- Otherwise, optionally check for updates if idle (configurable "update-at-boot" flag)

### 2. Scheduled maintenance (03:00 local time)
- Fetch manifest daily at 03:00
- If new version exists:
  - **Not busy**: start update immediately
  - **Busy**: defer update, retry every 10 minutes until cutoff (e.g., 06:00)
- If still busy at cutoff, skip until next scheduled check

### 3. Emergency push via Azure IoT Hub (recommended)

**Implementation**: Device Twin desired properties

Backend sets desired properties:
```json
{
  "update": {
    "mode": "force",
    "targetVersion": "1.8.2",
    "issuedAt": "2026-02-16T12:00:00Z"
  }
}
```

Device behavior:
- Receive twin desired property change callback
- If `mode=force` and `targetVersion` is newer than current → start emergency update immediately
- Report status back via twin reported properties:
  - States: `updating`, `success`, `failed`
  - Include `currentVersion` in all reports
  - Include error messages on failure

**Alternative** (not chosen for v1): Direct method `applyUpdate`
- More complex (request/response plumbing)
- Twin approach is simpler for v1

## Update procedure (single-instance with rollback)

### Filesystem layout (disk-bounded)

```
/opt/station/
├── releases/
│   ├── 1.8.1/          # previous version
│   ├── 1.8.2/          # current version
│   └── .incoming/      # temporary download location
├── current -> releases/1.8.2/   # symlink to active release
└── previous -> releases/1.8.1/  # symlink to last known good
```

**Constraint**: Keep maximum 2 releases (current + previous) to bound disk usage

### Update steps

1. **Download**
   - Download `artifactUrl` to `/opt/station/releases/.incoming/<version>.tar.gz`
   - Progress tracking optional (useful for large artifacts)

2. **Verify integrity**
   - Compute SHA256 hash of downloaded artifact
   - Compare with `manifest.sha256`
   - **Must match** or abort with error

3. **Extract**
   - Extract tarball to `/opt/station/releases/<version>/`
   - Validate expected structure exists (e.g., main JAR, config templates)

4. **Attempt polite drain**
   - Wait up to `DRAIN_TIMEOUT` (e.g., 60 seconds) for `not-busy` state
   - **For maintenance update**: if still busy after timeout → abort and defer
   - **For emergency update**: continue after timeout (force override)

5. **Stop application**
   - `systemctl stop station-app`
   - Wait for process termination (with timeout)

6. **Flip symlinks**
   - Set `previous` → old `current`
   - Set `current` → new release directory

7. **Start application**
   - `systemctl start station-app`
   - Begin health monitoring

8. **Health verification**
   - Wait up to `STARTUP_TIMEOUT` (180-240 seconds, Spring can take 2+ minutes)
   - Poll health endpoint or check `/run/station.healthy`

   **If healthy**:
   - Record success (version, timestamp)
   - Cleanup old releases (keep only current + previous)
   - Report success via IoT Hub twin reported properties

   **If not healthy** (rollback):
   - `systemctl stop station-app`
   - Revert `current` symlink to `previous`
   - `systemctl start station-app`
   - Record failure (version, timestamp, reason)
   - Report failure via IoT Hub twin reported properties
   - **Do not retry the same version endlessly** (see failure/backoff rules)

## Failure and backoff rules

To prevent infinite retry loops or bricking the device:

1. **Attempt limiting**: Don't attempt the same version more than `N` times per day (e.g., 3 attempts)
2. **Failed emergency update**: Keep running previous version, report failure via IoT Hub
3. **Persistent failures**: Log detailed errors; human intervention required
4. **Manifest fetch failures**: Retry with exponential backoff (max 5 minutes between retries)
5. **Rollback failures**: Critical error; log and alert (don't attempt further updates)

## Security requirements (v1 minimums)

1. **Process isolation**
   - Supervisor runs as dedicated non-root user (e.g., `station-supervisor`)
   - Use `sudo` rules to allow limited systemd control:
     - `systemctl start station-app`
     - `systemctl stop station-app`
     - `systemctl restart station-app`
   - No other sudo privileges

2. **Artifact integrity**
   - SHA256 verification is **mandatory** for all downloads
   - Reject artifacts that fail integrity check
   - Future: Add digital signature verification (v2)

3. **Azure IoT Hub authentication**
   - Use per-device identity with connection string or SAS token
   - Standard Azure IoT Hub SDK authentication mechanisms
   - Never expose credentials in logs or error messages

4. **Network security**
   - No inbound network listeners (supervisor doesn't open ports)
   - Only outbound connections:
     - HTTPS to Azure Blob Storage (download artifacts)
     - HTTPS to Azure IoT Hub (device twin sync)
   - Optional: HTTP to localhost for health checks (127.0.0.1 only)

5. **Secrets management**
   - Azure IoT Hub connection strings stored in secure config location
   - File permissions: 0600 (read/write for supervisor user only)
   - Consider using systemd credential storage or external secret managers (future)

## Kotlin implementation modules

### 1. Configuration loader
- Parse config file (YAML or JSON)
- Required settings:
  - Manifest URL (Azure Blob with SAS)
  - IoT Hub connection string
  - Schedule time (default: 03:00)
  - Timeouts: `DRAIN_TIMEOUT`, `STARTUP_TIMEOUT`
  - Health check method (file-based or HTTP)
  - Paths: `/opt/station`, `/run`, service names
  - Max update attempts per version per day

### 2. Manifest client
- HTTP client to fetch `manifest.json` from Azure Blob
- Parse JSON response
- Handle errors (network, malformed JSON, missing fields)
- Cache manifest with TTL to avoid excessive requests

### 3. Scheduler
- **On startup**: Check manifest, optionally update if idle
- **Daily at 03:00**: Scheduled maintenance check
- **Retry loop**: If update deferred due to busy state, retry every 10 minutes until cutoff
- Use coroutines with scheduled dispatchers

### 4. Azure IoT Hub listener (optional but recommended)
- **Device twin integration**:
  - Subscribe to desired property changes
  - Parse `update` object from desired properties
  - Trigger emergency update if `mode=force` and version is newer
  - Update reported properties with status (`updating`, `success`, `failed`)
  - Include `currentVersion` in all reported property updates
- Handle reconnection and connection failures gracefully
- Use Azure IoT Hub Java/Kotlin SDK

### 5. Updater (core update logic)
- **Download**: HTTP GET with progress tracking, write to temp location
- **SHA256 verification**: Compute hash and compare
- **Extraction**: Unpack tarball to target directory
- **Systemd control**: Call systemctl via ProcessBuilder or direct systemd API
  - Requires sudo rules configured externally
- **Health checks**: Poll file or HTTP endpoint
- **Rollback**: Revert symlinks and restart service
- Exception handling and detailed error logging

### 6. Busy/Health checker
- Abstraction to support both file-based and HTTP-based checks
- Interface: `isBusy(): Boolean`, `isHealthy(): Boolean`
- Implementations:
  - `FileBasedChecker`: Check existence of `/run/station.busy` and `/run/station.healthy`
  - `HttpBasedChecker`: Make HTTP requests to local endpoints
- Configurable via settings

### 7. State store
- Persistent state tracking (JSON or SQLite)
- Fields:
  - `currentVersion`: Currently running version
  - `lastSuccessTimestamp`: Last successful update
  - `lastFailureTimestamp`: Last failed update attempt
  - `lastFailedVersion`: Version that failed
  - `attemptCountByVersion`: Map of version → attempt count (reset daily)
  - `dailyAttemptResetDate`: Track when to reset attempt counters
- Thread-safe access (use locks or atomic operations)

## Constraints and assumptions

1. **Platform**: Linux with systemd (Debian/Ubuntu or similar)
2. **JVM**: Kotlin targets JVM (Java 17+)
3. **Single instance**: Only one supervisor process per device
4. **No database required**: Use file-based state or embedded SQLite
5. **Logging**: Use structured logging (SLF4J with Logback), integrate with journald
6. **Dependencies**:
   - Azure IoT Hub Device SDK (Java)
   - HTTP client (OkHttp or Ktor client)
   - JSON parsing (kotlinx.serialization or Jackson)
   - Coroutines for async operations
7. **Testing**: Unit tests for core logic, integration tests for update flow (mock Azure services)

## Acceptance criteria

1. **Scheduled update (happy path)**
   - Supervisor starts, schedules daily check at 03:00
   - At 03:00, fetches manifest, finds new version
   - Station app is idle (not busy)
   - Downloads artifact, verifies SHA256
   - Stops station app, updates symlink, starts station app
   - New version runs and reports healthy
   - Previous version is kept; old releases cleaned up
   - State store updated with success

2. **Emergency update via IoT Hub**
   - Backend sets twin desired property with `mode=force`
   - Supervisor receives callback, validates new version
   - Waits for brief drain (up to timeout)
   - Performs update (even if still busy after timeout)
   - Reports status back via twin reported properties

3. **Update deferred due to busy state**
   - Scheduled update triggers at 03:00
   - Station app is busy (transaction in progress)
   - Supervisor defers update, retries every 10 minutes
   - Update succeeds when app becomes idle
   - If still busy at 06:00 cutoff, skips until next day

4. **Rollback on failed health check**
   - Update proceeds normally
   - New version starts but fails health check (timeout or error)
   - Supervisor automatically rolls back to previous version
   - Previous version starts and reports healthy
   - Failure recorded in state store
   - Same version not retried more than N times per day

5. **Integrity check failure**
   - Download completes but SHA256 doesn't match manifest
   - Supervisor rejects artifact and logs error
   - No update attempted, current version continues running
   - Failure reported via IoT Hub

6. **Network failures handled gracefully**
   - Manifest fetch fails → retry with backoff, log error
   - Artifact download fails → retry (up to limit), log error
   - IoT Hub connection drops → reconnect automatically
   - All failures non-fatal; supervisor continues running

## Out of scope for v1

- Multi-stage rollout (blue/green, canary)
- Differential updates / binary diffs
- Digital signature verification (only SHA256 in v1)
- Automatic telemetry/metrics collection (manual logging only)
- Web UI for update management
- Multiple update channels (stable/beta/alpha)
- Peer-to-peer update distribution
- Update scheduling by external calendar system
