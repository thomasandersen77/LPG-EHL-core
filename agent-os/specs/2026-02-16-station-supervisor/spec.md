# Specification: Station Supervisor v1

## Goal
Create an auto-update system for LPG station applications with scheduled maintenance updates and emergency push capabilities via Azure IoT Hub.

## User Stories
- As an operator, I want the station app to update automatically during off-peak hours so that software stays current without manual intervention.
- As a system administrator, I want to force critical updates remotely via Azure IoT Hub so that security patches can be deployed immediately to the fleet.

## Specific Requirements

**Supervisor service deployment**
- Run as systemd service `station-supervisor.service` with always-on behavior and minimal CPU usage
- Execute as dedicated non-root user `station-supervisor` with limited sudo privileges for systemctl commands only
- Manage `station-app.service` lifecycle (start, stop, restart) through systemd integration
- Store configuration in `/etc/station-supervisor/config.yaml` with 0600 permissions for supervisor user only
- Log to journald with structured logging using SLF4J and Logback
- Maintain single instance per device with no database requirements (file-based or embedded SQLite state)

**Azure Blob manifest polling**
- Fetch manifest from Azure Blob Storage at `manifest.json` location using short-lived SAS URLs
- Parse JSON manifest containing latest version, artifact URL, SHA256 hash, and force update flags
- Check manifest at supervisor startup, daily at 03:00 local time, and on-demand via IoT Hub
- Cache manifest with TTL to avoid excessive requests during retry scenarios
- Handle network failures with exponential backoff (max 5 minutes between retries)
- Validate manifest structure and reject malformed JSON with detailed error logging

**Busy and healthy state detection**
- Support two configurable health check methods via config: file-based or HTTP-based
- File-based: Poll `/run/station.busy` (exists during transactions) and `/run/station.healthy` (exists when started)
- HTTP-based: Query `GET http://127.0.0.1:<port>/internal/busy` for busy state and `/internal/health` for health status
- Implement abstraction interface with `isBusy(): Boolean` and `isHealthy(): Boolean` methods
- Use timeout-aware polling with configurable intervals for health verification

**Scheduled maintenance update flow**
- Trigger manifest check daily at 03:00 local time using coroutine-based scheduler
- If new version available and station not busy, initiate update immediately
- If busy, defer update and retry every 10 minutes until 06:00 cutoff
- Skip update until next scheduled check if still busy at cutoff time
- Support optional "update-at-boot" flag to check for updates on supervisor startup

**Emergency update via Azure IoT Hub**
- Subscribe to Device Twin desired property changes for `update` object with `mode`, `targetVersion`, and `issuedAt` fields
- Trigger emergency update when `mode=force` and `targetVersion` is newer than current
- Wait for brief drain period (configurable `DRAIN_TIMEOUT`, default 60 seconds) then proceed even if busy
- Report status back via twin reported properties with states: `updating`, `success`, `failed`
- Include `currentVersion` in all reported property updates for monitoring visibility
- Handle IoT Hub reconnection and connection failures gracefully with automatic retry

**Artifact download and verification**
- Download artifact to `/opt/station/releases/.incoming/<version>.tar.gz` using HTTPS
- Compute SHA256 hash of downloaded file and compare against manifest value
- Reject artifacts that fail integrity check with error logging and IoT Hub failure report
- Implement optional progress tracking for large artifacts (useful for monitoring)
- Cleanup temporary download files on success or failure

**Filesystem-based release management**
- Maintain releases in `/opt/station/releases/` with subdirectories per version
- Use symlinks: `current` points to active release, `previous` points to last known good
- Keep maximum 2 releases (current + previous) to bound disk usage
- Extract tarballs to version-specific directories and validate expected structure (JAR, config templates)
- Cleanup old releases after successful update, keeping only current and previous

**Update execution with rollback**
- Stop station app with `systemctl stop station-app` and wait for process termination with timeout
- Flip symlinks atomically: set `previous` to old `current`, then `current` to new release
- Start station app with `systemctl start station-app` and begin health monitoring
- Wait up to `STARTUP_TIMEOUT` (180-240 seconds) for health verification via configured method
- If healthy, record success with version and timestamp, cleanup old releases, report success to IoT Hub
- If not healthy, execute rollback: stop app, revert `current` symlink to `previous`, restart app, record failure, report to IoT Hub
- Never retry the same failed version more than N times per day (configurable attempt limit)

**Failure handling and safety**
- Limit update attempts to max N per version per day (default 3) to prevent infinite retry loops
- Track attempt counts in state store with daily reset mechanism
- On rollback failure, log critical error and halt further update attempts (require manual intervention)
- Implement fail-safe for persistent failures with detailed error logging for troubleshooting
- Keep running previous version on failed emergency update and report failure via IoT Hub
- Handle manifest fetch failures with retry and exponential backoff, treating as non-fatal

**State persistence**
- Store state in JSON file or embedded SQLite at `/var/lib/station-supervisor/state.json`
- Track fields: `currentVersion`, `lastSuccessTimestamp`, `lastFailureTimestamp`, `lastFailedVersion`, `attemptCountByVersion`, `dailyAttemptResetDate`
- Implement thread-safe access using locks or atomic operations
- Update state synchronously after each update attempt outcome
- Reset attempt counters daily based on `dailyAttemptResetDate`

**Configuration management**
- Parse YAML or JSON config file at `/etc/station-supervisor/config.yaml`
- Required settings: manifest URL with SAS, IoT Hub connection string, schedule time (default 03:00)
- Timeouts: `DRAIN_TIMEOUT` (60s), `STARTUP_TIMEOUT` (180-240s), manifest fetch retry intervals
- Health check method selection (file-based or HTTP), paths for `/opt/station`, `/run`, service names
- Max update attempts per version per day, retry cutoff time (default 06:00)
- Validate configuration on load and fail fast with clear error messages for missing required fields

## Visual Design
No visuals provided.

## Existing Code to Leverage

**Kotlin coroutines patterns from TransactionWatchdog**
- Use coroutines with scheduled dispatchers for daily 03:00 checks and retry loops
- Implement timeout tracking with parallel jobs similar to absolute timeout pattern in TransactionWatchdog
- Apply structured concurrency with coroutineScope for managing update flow
- Use suspend functions for async operations like download, health checks, and systemd control
- Leverage cancellation support for graceful shutdown and retry abort scenarios

**Configuration data classes pattern from BaxiIniConfig**
- Use Kotlin data classes for configuration structures with clear property names
- Implement validation in init blocks to enforce required field constraints
- Structure config as immutable data classes for thread-safety
- Parse from YAML or JSON into strongly-typed Kotlin objects

**Logging patterns from existing codebase**
- Use SLF4J with LoggerFactory.getLogger for structured logging
- Log at appropriate levels: trace for polling, info for state changes, warn for retries, error for failures
- Include context in log messages: version, dispenser ID equivalents (version numbers), timestamps
- Integrate with journald for systemd-native logging with SyslogIdentifier

**systemd service file structure from lpg-ehl.service**
- Define Type=simple with User/Group for non-root execution
- Set Restart=always with RestartSec for resilience
- Configure WorkingDirectory, StandardOutput=journal, StandardError=journal
- Use SyslogIdentifier for log filtering and Environment variables for config overrides

**State management and polling patterns from TransactionWatchdog**
- Implement state tracking with mutable properties protected by synchronization
- Use polling loops with delay intervals for manifest checks and health verification
- Track consecutive failures (similar to consecutiveNulls pattern) for circuit breaking
- Implement retry logic with configurable backoff and max attempts

## Out of Scope
- Multi-stage rollout strategies (blue/green, canary deployments)
- Differential updates or binary diffs for bandwidth optimization
- Digital signature verification (only SHA256 integrity checks in v1)
- Automatic telemetry and metrics collection (manual logging only)
- Web UI or dashboard for update management
- Multiple update channels (stable/beta/alpha) for different deployment tiers
- Peer-to-peer update distribution for bandwidth sharing
- Update scheduling by external calendar systems
- Azure IoT Hub Direct Method implementation (using Device Twin only)
- Hardware security module integration for credential storage
