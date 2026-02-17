# Task Breakdown: Station Supervisor v1

## Overview
Total Task Groups: 8
Implementation Language: Kotlin/JVM
Platform: Linux with systemd

## Task List

### Foundation Layer

#### Task Group 1: Configuration and Data Models
**Dependencies:** None

- [ ] 1.0 Complete configuration and data structures
  - [ ] 1.1 Write 2-8 focused tests for configuration loading
    - Test valid config parsing from YAML
    - Test required field validation fails appropriately
    - Test default value handling
    - Test invalid YAML format rejection
  - [ ] 1.2 Create data classes for configuration
    - SupervisorConfig: manifest URL, IoT Hub connection string, schedule time
    - TimeoutConfig: DRAIN_TIMEOUT (60s), STARTUP_TIMEOUT (180-240s)
    - PathsConfig: /opt/station, /run paths, service names
    - HealthCheckConfig: method (file/HTTP), file paths or HTTP endpoints
    - RetryConfig: max attempts per version per day, cutoff time (06:00)
    - Use pattern from BaxiIniConfig with data classes and init block validation
  - [ ] 1.3 Create data classes for manifest structure
    - ManifestLatest: version, artifactUrl, sha256
    - ManifestForce: enabled, minVersion, reason
    - Manifest: latest, force
    - Include JSON parsing annotations (kotlinx.serialization or Jackson)
  - [ ] 1.4 Create data classes for IoT Hub device twin
    - UpdateCommand: mode, targetVersion, issuedAt
    - UpdateStatus: state (updating/success/failed), currentVersion, errorMessage
  - [ ] 1.5 Implement config loader with YAML parsing
    - Load from /etc/station-supervisor/config.yaml
    - Parse into SupervisorConfig data class
    - Validate required fields and fail fast with clear error messages
    - Support environment variable overrides for testing
  - [ ] 1.6 Ensure configuration tests pass
    - Run ONLY the 2-8 tests written in 1.1
    - Verify config parsing and validation works correctly

**Acceptance Criteria:**
- The 2-8 tests written in 1.1 pass
- Configuration loads from YAML successfully
- Missing required fields fail with clear error messages
- Data classes are immutable and thread-safe

#### Task Group 2: State Management
**Dependencies:** Task Group 1

- [ ] 2.0 Complete state persistence layer
  - [ ] 2.1 Write 2-8 focused tests for state store
    - Test state save and load from JSON file
    - Test thread-safe concurrent access
    - Test attempt counter daily reset logic
    - Test state recovery from corrupted file
  - [ ] 2.2 Create StateStore data class
    - Fields: currentVersion, lastSuccessTimestamp, lastFailureTimestamp
    - lastFailedVersion, attemptCountByVersion (Map<String, Int>)
    - dailyAttemptResetDate for tracking daily reset
  - [ ] 2.3 Implement file-based StateManager
    - Save/load state from /var/lib/station-supervisor/state.json
    - Thread-safe access using mutex or synchronized blocks
    - Atomic write pattern (write to temp, then move)
    - Handle file not found (initialize with defaults)
    - Handle corrupted JSON (log error, reset to defaults)
  - [ ] 2.4 Implement attempt tracking and daily reset
    - incrementAttempt(version): Track attempts per version
    - shouldAttemptUpdate(version, maxAttempts): Check if version can be retried
    - resetAttemptsIfNeeded(): Reset counters daily based on dailyAttemptResetDate
    - recordSuccess(version): Update success timestamp and current version
    - recordFailure(version, reason): Update failure timestamp and failed version
  - [ ] 2.5 Ensure state management tests pass
    - Run ONLY the 2-8 tests written in 2.1
    - Verify state persistence and thread-safety work correctly

**Acceptance Criteria:**
- The 2-8 tests written in 2.1 pass
- State persists correctly across supervisor restarts
- Thread-safe concurrent access works without data corruption
- Daily attempt counter reset functions correctly

### Azure Integration Layer

#### Task Group 3: Azure Blob Manifest Client
**Dependencies:** Task Group 1, 2

- [ ] 3.0 Complete Azure Blob manifest fetching
  - [ ] 3.1 Write 2-8 focused tests for manifest client
    - Test successful manifest fetch and parsing
    - Test network failure handling with retry
    - Test malformed JSON rejection
    - Test manifest caching with TTL
  - [ ] 3.2 Implement HTTP client with OkHttp or Ktor
    - Configure HTTPS client with timeout settings
    - Add User-Agent header for Azure monitoring
    - Support SAS URL authentication (from manifest URL config)
  - [ ] 3.3 Implement ManifestClient class
    - fetchManifest(): Download manifest.json from Azure Blob
    - Parse JSON response into Manifest data class
    - Validate manifest structure (required fields present)
    - Reject malformed JSON with detailed error logging
  - [ ] 3.4 Add manifest caching with TTL
    - Cache manifest in-memory with timestamp
    - configurable TTL (e.g., 5 minutes) to avoid excessive requests
    - Return cached manifest if within TTL
    - Force refresh on cache miss or TTL expiration
  - [ ] 3.5 Implement retry with exponential backoff
    - Network failure handling: retry up to 5 times
    - Exponential backoff: 10s, 30s, 1m, 2m, 5m (max)
    - Log warnings on retry attempts
    - Treat fetch failures as non-fatal (continue running)
  - [ ] 3.6 Ensure manifest client tests pass
    - Run ONLY the 2-8 tests written in 3.1
    - Verify manifest fetching, parsing, caching, and retry logic work

**Acceptance Criteria:**
- The 2-8 tests written in 3.1 pass
- Manifest fetches successfully from Azure Blob with SAS URL
- Network failures handled with exponential backoff
- Malformed JSON rejected with clear error messages
- Caching reduces unnecessary requests

#### Task Group 4: Azure IoT Hub Device Twin Integration
**Dependencies:** Task Group 1, 2

- [ ] 4.0 Complete IoT Hub device twin integration
  - [ ] 4.1 Write 2-8 focused tests for IoT Hub client
    - Test desired property change callback handling
    - Test reported property update sending
    - Test reconnection on connection failure
    - Test emergency update trigger from twin change
  - [ ] 4.2 Set up Azure IoT Hub Device SDK
    - Add Azure IoT Hub Java SDK dependency
    - Configure DeviceClient with connection string from config
    - Set connection status callback for monitoring
    - Set up twin property callbacks
  - [ ] 4.3 Implement DeviceTwinListener class
    - Subscribe to desired property changes
    - Parse "update" object from desired properties
    - Extract UpdateCommand (mode, targetVersion, issuedAt)
    - Validate targetVersion is newer than currentVersion
    - Trigger emergency update callback if mode=force
  - [ ] 4.4 Implement twin reported property updates
    - reportUpdating(currentVersion): Set state to "updating"
    - reportSuccess(newVersion): Set state to "success" with version
    - reportFailure(version, error): Set state to "failed" with error message
    - Include currentVersion in all reported property updates
    - Handle IoT Hub connection failures gracefully (queue updates)
  - [ ] 4.5 Add connection management and reconnection
    - Handle IoT Hub disconnection events
    - Implement automatic reconnection with retry
    - Log connection state changes at INFO level
    - Continue running supervisor even if IoT Hub unavailable
  - [ ] 4.6 Ensure IoT Hub integration tests pass
    - Run ONLY the 2-8 tests written in 4.1
    - Verify twin property handling and reconnection work
    - Use mock IoT Hub client for testing

**Acceptance Criteria:**
- The 2-8 tests written in 4.1 pass
- Desired property changes trigger emergency updates correctly
- Reported properties update successfully
- Reconnection handles transient network failures
- Supervisor continues operating if IoT Hub unavailable

### Health Check Abstraction

#### Task Group 5: Busy and Healthy State Detection
**Dependencies:** Task Group 1

- [ ] 5.0 Complete health check abstraction
  - [ ] 5.1 Write 2-8 focused tests for health checkers
    - Test file-based busy/healthy detection
    - Test HTTP-based busy/healthy detection
    - Test timeout handling for slow health checks
    - Test factory method selecting correct implementation
  - [ ] 5.2 Create HealthChecker interface
    - isBusy(): Boolean - Check if station is processing transaction
    - isHealthy(): Boolean - Check if station app is healthy
    - Both methods should be suspendable for async HTTP calls
  - [ ] 5.3 Implement FileBasedHealthChecker
    - isBusy(): Check existence of /run/station.busy file
    - isHealthy(): Check existence of /run/station.healthy file
    - Add timeout for file polling operations
    - Handle file system errors gracefully (log and return safe default)
  - [ ] 5.4 Implement HttpBasedHealthChecker
    - isBusy(): GET http://127.0.0.1:<port>/internal/busy
    - Parse JSON response: {"busy": true/false}
    - isHealthy(): GET http://127.0.0.1:<port>/internal/health
    - Check for 200 OK status
    - Add timeout for HTTP requests (5-10 seconds)
    - Handle connection refused gracefully (app not started yet)
  - [ ] 5.5 Create HealthCheckerFactory
    - Create appropriate implementation based on config
    - Parse health check method from HealthCheckConfig
    - Return FileBasedHealthChecker or HttpBasedHealthChecker
    - Validate configuration (paths for file-based, port for HTTP-based)
  - [ ] 5.6 Ensure health checker tests pass
    - Run ONLY the 2-8 tests written in 5.1
    - Verify both file-based and HTTP-based implementations work

**Acceptance Criteria:**
- The 2-8 tests written in 5.1 pass
- File-based health checks work correctly
- HTTP-based health checks work correctly
- Factory creates correct implementation based on config
- Timeout handling prevents hanging on unresponsive app

### Core Update Engine

#### Task Group 6: Artifact Download and Verification
**Dependencies:** Task Group 1, 2, 5

- [ ] 6.0 Complete artifact download and verification
  - [ ] 6.1 Write 2-8 focused tests for artifact handling
    - Test successful download and SHA256 verification
    - Test SHA256 mismatch detection and rejection
    - Test download progress tracking
    - Test tarball extraction and validation
  - [ ] 6.2 Implement ArtifactDownloader class
    - download(url, targetPath): Download artifact via HTTPS
    - Stream download to /opt/station/releases/.incoming/<version>.tar.gz
    - Optional: Track download progress (bytes downloaded, percentage)
    - Handle network failures with retry (up to 3 attempts)
    - Clean up partial downloads on failure
  - [ ] 6.3 Implement SHA256 verification
    - computeSha256(file): Compute hash of downloaded file
    - verifySha256(file, expectedHash): Compare hashes
    - Use java.security.MessageDigest for SHA-256
    - Reject artifact if hash doesn't match (delete file, log error)
    - Report integrity failure via IoT Hub if applicable
  - [ ] 6.4 Implement tarball extraction
    - extract(tarballPath, targetDir): Unpack to version directory
    - Extract to /opt/station/releases/<version>/
    - Use Apache Commons Compress or Java's tar support
    - Validate expected structure after extraction (main JAR exists)
    - Clean up tarball after successful extraction
    - Handle extraction errors (corrupted archive, disk space)
  - [ ] 6.5 Implement cleanup utilities
    - cleanupOldReleases(keepCount): Keep only N most recent releases
    - Keep current + previous (max 2 releases)
    - Delete older release directories to bound disk usage
    - Clean up .incoming directory on success or failure
  - [ ] 6.6 Ensure artifact handling tests pass
    - Run ONLY the 2-8 tests written in 6.1
    - Verify download, verification, and extraction work correctly

**Acceptance Criteria:**
- The 2-8 tests written in 6.1 pass
- Artifacts download successfully via HTTPS
- SHA256 verification rejects mismatched files
- Tarball extraction creates correct directory structure
- Old releases cleaned up to maintain disk space bounds

#### Task Group 7: Update Execution with Rollback
**Dependencies:** Task Group 1, 2, 5, 6

- [ ] 7.0 Complete update execution engine
  - [ ] 7.1 Write 2-8 focused tests for update execution
    - Test successful update with health verification
    - Test rollback on failed health check
    - Test systemd service control (stop/start)
    - Test symlink management (current/previous)
  - [ ] 7.2 Implement SystemdController class
    - stopService(serviceName): Execute systemctl stop via ProcessBuilder
    - startService(serviceName): Execute systemctl start
    - restartService(serviceName): Execute systemctl restart
    - waitForTermination(serviceName, timeout): Wait for process to stop
    - Use sudo as configured (supervisor user has limited sudo privileges)
    - Parse systemctl output to verify success
    - Handle timeouts and errors gracefully
  - [ ] 7.3 Implement SymlinkManager class
    - updateSymlinks(newVersion, oldVersion): Atomic symlink updates
    - Set previous -> old current directory
    - Set current -> new version directory
    - Use Files.createSymbolicLink with atomic operations
    - Handle symlink creation errors
    - Verify symlinks point to correct targets after update
  - [ ] 7.4 Implement UpdateExecutor core flow
    - attemptPoliteDrain(timeout): Wait for not-busy state
    - For maintenance: abort if still busy after timeout
    - For emergency: continue after timeout (force override)
    - stopApplication(): Stop station-app service with timeout
    - startApplication(): Start station-app service
    - verifyHealth(timeout): Poll health checker until healthy or timeout
    - Wait up to STARTUP_TIMEOUT (180-240s for Spring boot)
  - [ ] 7.5 Implement update orchestration
    - performUpdate(manifest): Main update flow
    - Call ArtifactDownloader to download and verify
    - Extract tarball to version directory
    - Attempt polite drain based on update mode
    - Stop application via SystemdController
    - Update symlinks via SymlinkManager
    - Start application and verify health
    - Return success or failure with detailed reason
  - [ ] 7.6 Implement rollback mechanism
    - rollback(previousVersion): Revert to last known good
    - Stop application if running
    - Revert current symlink to previous
    - Start application and verify health
    - If rollback fails: log critical error, halt updates (manual intervention)
    - Record rollback in state store
    - Report rollback via IoT Hub
  - [ ] 7.7 Integrate with StateManager
    - Check shouldAttemptUpdate before starting
    - Increment attempt counter at start
    - Record success or failure after completion
    - Enforce max attempts per version per day
    - Skip update if attempt limit reached
  - [ ] 7.8 Ensure update execution tests pass
    - Run ONLY the 2-8 tests written in 7.1
    - Verify update flow and rollback work correctly
    - Use mock systemd controller for testing

**Acceptance Criteria:**
- The 2-8 tests written in 7.1 pass
- Update flow completes successfully on healthy new version
- Rollback triggers automatically on health check failure
- Systemd service control works correctly via sudo
- Symlinks update atomically
- Attempt limiting prevents infinite retry loops
- Critical rollback failures halt further updates

### Scheduling and Orchestration

#### Task Group 8: Scheduler and Update Triggers
**Dependencies:** Task Group 1-7

- [ ] 8.0 Complete scheduling and orchestration
  - [ ] 8.1 Write 2-8 focused tests for scheduler
    - Test daily scheduled check at 03:00
    - Test retry loop for deferred updates (busy state)
    - Test cutoff time enforcement (06:00)
    - Test emergency update trigger bypassing schedule
  - [ ] 8.2 Implement UpdateScheduler with coroutines
    - scheduleDaily(hour, minute): Schedule daily manifest check
    - Use kotlinx.coroutines with delay for scheduling
    - Follow pattern from TransactionWatchdog for scheduled dispatchers
    - Calculate delay until next scheduled time (03:00 local)
    - Trigger checkForUpdates() at scheduled time
  - [ ] 8.3 Implement maintenance update flow
    - checkForUpdates(): Fetch manifest from ManifestClient
    - Compare manifest version with current version
    - If new version available and not busy: start update immediately
    - If busy: defer and schedule retry
    - retryLoop(): Retry every 10 minutes until cutoff (06:00)
    - Skip update if still busy at cutoff time
    - Log all decisions at INFO level
  - [ ] 8.4 Implement startup update check
    - On supervisor startup: fetch manifest
    - Check for force update (force.minVersion > currentVersion)
    - If forced: treat as emergency update
    - Otherwise: optionally check for updates if idle (configurable flag)
    - Support "update-at-boot" config flag
  - [ ] 8.5 Implement emergency update handling
    - onEmergencyUpdate(targetVersion): Callback from DeviceTwinListener
    - Report "updating" status via IoT Hub immediately
    - Bypass schedule and trigger update ASAP
    - Use brief drain period (DRAIN_TIMEOUT, default 60s)
    - Force update even if still busy after drain timeout
    - Report success or failure back to IoT Hub
    - Handle already-in-progress updates (queue or skip)
  - [ ] 8.6 Add update coordination and locking
    - Ensure only one update runs at a time
    - Use mutex or atomic flag to prevent concurrent updates
    - Queue emergency updates if maintenance update in progress
    - Cancel retry loop if emergency update arrives
  - [ ] 8.7 Implement graceful shutdown
    - Handle SIGTERM and SIGINT signals
    - Cancel all coroutines on shutdown
    - Wait for in-progress update to complete or timeout
    - Close IoT Hub connection cleanly
    - Flush state to disk before exit
  - [ ] 8.8 Ensure scheduler tests pass
    - Run ONLY the 2-8 tests written in 8.1
    - Verify scheduling, retry logic, and emergency updates work

**Acceptance Criteria:**
- The 2-8 tests written in 8.1 pass
- Daily scheduled checks trigger at 03:00
- Retry loop defers updates when busy until cutoff
- Emergency updates bypass schedule and force update
- Only one update runs at a time
- Graceful shutdown completes in-progress updates

### Deployment and Integration

#### Task Group 9: systemd Service and Deployment
**Dependencies:** Task Group 1-8

- [ ] 9.0 Complete systemd integration and deployment
  - [ ] 9.1 Create systemd service file
    - File: station-supervisor.service
    - Type=simple, User=station-supervisor, Group=station-supervisor
    - ExecStart: Launch Kotlin JAR with config path
    - Restart=always, RestartSec=10s for resilience
    - WorkingDirectory=/opt/station/supervisor
    - StandardOutput=journal, StandardError=journal
    - SyslogIdentifier=station-supervisor for log filtering
    - Environment variables for config overrides
    - Follow pattern from lpg-ehl.service
  - [ ] 9.2 Configure user and permissions
    - Create station-supervisor system user (non-root)
    - Set up sudo rules for limited systemctl privileges:
      - Allow systemctl start/stop/restart station-app
      - No other sudo privileges
    - Config file permissions: 0600 (supervisor user only)
    - State directory: /var/lib/station-supervisor (owner: supervisor user)
    - Release directory: /opt/station/releases (supervisor can write)
  - [ ] 9.3 Set up logging configuration
    - Configure SLF4J with Logback
    - Log to stdout (captured by journald)
    - Log levels: TRACE for polling, INFO for state changes, WARN for retries, ERROR for failures
    - Include context in messages: version, timestamp, error details
    - Structured logging format for easy parsing
  - [ ] 9.4 Create deployment package
    - Build fat JAR with Gradle/Maven (include all dependencies)
    - Package configuration template (config.yaml.example)
    - Include systemd service file
    - Create installation script: copy files, create user, set permissions
    - Document required configuration (IoT Hub connection string, manifest URL)
  - [ ] 9.5 Write deployment documentation
    - Installation instructions for fresh deployment
    - Configuration guide (required settings, optional settings)
    - Systemd service management (start, stop, status, logs)
    - Troubleshooting guide (common errors, log locations)
    - Upgrade procedure for supervisor itself
  - [ ] 9.6 Test end-to-end deployment
    - Deploy to test VM or container
    - Verify supervisor starts and runs as systemd service
    - Check journald logs for proper output
    - Verify supervisor can control station-app service
    - Test full update flow on test environment

**Acceptance Criteria:**
- systemd service file correctly configured
- Supervisor runs as non-root user with limited sudo
- Logging integrates with journald
- Deployment package contains all required files
- Documentation covers installation and configuration
- End-to-end test succeeds on clean system

### Testing and Quality Assurance

#### Task Group 10: Integration Testing and Gap Analysis
**Dependencies:** Task Groups 1-9

- [ ] 10.0 Review existing tests and fill critical gaps only
  - [ ] 10.1 Review tests from Task Groups 1-9
    - Review the 2-8 tests written by each task group (1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1)
    - Total existing tests: approximately 16-64 tests
    - Identify which critical workflows are covered
  - [ ] 10.2 Analyze test coverage gaps for Station Supervisor only
    - Identify critical end-to-end workflows lacking coverage:
      - Full scheduled update flow (manifest fetch -> download -> update -> health check)
      - Emergency update via IoT Hub twin change -> immediate update
      - Rollback on health check failure -> previous version restored
      - Busy state deferral -> retry loop -> eventual success
      - Attempt limiting -> max retries reached -> skip update
    - Focus ONLY on gaps related to Station Supervisor feature requirements
    - Do NOT assess entire application test coverage
    - Prioritize integration tests over unit test gaps
  - [ ] 10.3 Write up to 10 additional strategic tests maximum
    - Add maximum of 10 new integration tests to fill critical gaps
    - Focus on end-to-end workflows and integration points:
      - Scheduled update happy path (manifest -> download -> update -> success)
      - Emergency update from IoT Hub -> force update -> report status
      - Health check failure -> rollback -> previous version running
      - Busy state -> deferred update -> retry until cutoff
      - SHA256 mismatch -> update rejected -> current version continues
      - Network failure -> retry with backoff -> eventual success
      - Attempt limit reached -> update skipped -> logged
      - Supervisor restart -> state restored -> continue operation
    - Do NOT write comprehensive coverage for all scenarios
    - Skip edge cases unless business-critical
    - Use mock Azure services and systemd controller
  - [ ] 10.4 Run feature-specific tests only
    - Run ONLY tests related to Station Supervisor (tests from 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1, and 10.3)
    - Expected total: approximately 26-74 tests maximum
    - Do NOT run the entire application test suite
    - Verify all critical workflows pass
    - Document any known issues or limitations

**Acceptance Criteria:**
- All feature-specific tests pass (approximately 26-74 tests total)
- Critical end-to-end workflows for Station Supervisor are covered
- No more than 10 additional tests added when filling in testing gaps
- Testing focused exclusively on Station Supervisor feature requirements
- Integration tests cover major update scenarios
- Mock Azure services used for repeatable testing

## Execution Order

Recommended implementation sequence:

1. **Foundation Layer** (Task Groups 1-2)
   - Start with configuration and data models
   - Then build state management
   - These are prerequisites for all other components

2. **Azure Integration Layer** (Task Groups 3-4)
   - Build manifest client for fetching updates
   - Build IoT Hub integration for emergency updates
   - Can be developed in parallel with health checks

3. **Health Check Abstraction** (Task Group 5)
   - Required for update execution safety
   - Can be developed in parallel with Azure integration

4. **Core Update Engine** (Task Groups 6-7)
   - Build artifact download and verification
   - Then build update execution with rollback
   - These are the heart of the system

5. **Scheduling and Orchestration** (Task Group 8)
   - Ties together all previous components
   - Implements the main supervisor logic
   - Handles scheduling and coordination

6. **Deployment and Integration** (Task Group 9)
   - Package everything for deployment
   - Create systemd service
   - Write deployment documentation

7. **Testing and Quality Assurance** (Task Group 10)
   - Review all existing tests
   - Fill critical integration test gaps
   - Validate end-to-end workflows

## Notes

- **Kotlin coroutines**: Use extensively for async operations (scheduling, HTTP, delays)
- **Pattern reuse**: Leverage existing patterns from TransactionWatchdog, BaxiIniConfig, lpg-ehl.service
- **Testing philosophy**: Write 2-8 focused tests per task group during development, then fill critical gaps at the end
- **Test scope**: Run only newly written tests during each task group, not the entire suite
- **Safety first**: Multiple layers of safety (busy checks, health verification, rollback, attempt limiting)
- **Fail-safe design**: Continue running previous version on any failure
- **No UI**: This is a background service with no user interface (logs only)
- **Single instance**: Only one supervisor per device, no database required
- **Minimal dependencies**: Use standard Kotlin/JVM libraries where possible
