# Troubleshooting Guide

## Kotlin Daemon Issues

### Problem: Maven hangs on "Options for KOTLIN DAEMON"

**Symptoms:**
- Maven build hangs during Kotlin compilation
- Log shows "retrying connecting to the daemon"
- Process appears stuck

**Root Cause:**
Version mismatch or corruption in Kotlin daemon state can cause the daemon to crash before responding to Maven.

**Quick Fix:**

```bash
# Use the provided script
./build-clean.sh
```

Or manually:

```bash
# 1. Kill all Kotlin-related processes
pkill -f kotlin || true
pkill -f KotlinCompileDaemon || true

# 2. Delete daemon cache
rm -rf "$HOME/Library/Application Support/kotlin/daemon" || true

# 3. Build with in-process compiler
mvn clean install -Dkotlin.compiler.execution.strategy=in-process
```

**Permanent Fix:**
The project is now configured to prefer in-process compilation. If issues persist, use the `-Dkotlin.compiler.execution.strategy=in-process` flag.

---

## Headless Application "Dies" After Startup

### Problem: Headless application starts then immediately exits

**Symptoms:**
- Application logs startup messages
- Process exits shortly after "Application started successfully"
- No errors shown

**Root Cause:**
1. ~~`CommandLineRunner` was being called manually instead of letting Spring Boot handle it automatically~~
2. ~~`WebApplicationType` was not explicitly set to `NONE`, potentially causing web server startup attempts~~
3. Scheduled tasks (@Scheduled) keep the application alive - without them, the JVM exits

**Fixed:**
- HeadlessApplication.kt now properly sets `WebApplicationType.NONE`
- CommandLineRunner executes automatically via Spring Boot lifecycle
- Component scanning includes all service packages with @Scheduled tasks

**Verify Fix:**

```bash
# Build the headless module
mvn clean install -pl lpg-ehl-app-headless -am

# Run headless application
cd lpg-ehl-app-headless
mvn spring-boot:run
```

The application should now:
1. Start without trying to launch a web server
2. Execute HeadlessStartupRunner automatically
3. Keep running due to @Scheduled tasks in service module
4. Log periodic activity from scheduled jobs

---

## Module Structure

The refactored project follows a clean modular architecture:

### Core Modules
- **lpg-ehl-core**: Pure EHL protocol codec (no IO, no Spring)
- **lpg-transport**: Physical communication (serial/TCP)
- **lpg-ehl-service**: Business logic, state machine, scheduling

### Entry Points
- **lpg-ehl-webapp**: REST API + UI (web server)
- **lpg-ehl-app-headless**: Background daemon (no web server)
- **lpg-ehl-cli**: Command-line tool

### Support
- **lpg-ehl-emulator**: Hardware emulator for testing
- **lpg-ehl-serialport-sim**: Serial port simulator

---

## Common Build Commands

```bash
# Full build with clean Kotlin state
./build-clean.sh

# Quick build (skip tests)
mvn clean install -DskipTests

# Build specific module and dependencies
mvn clean install -pl lpg-ehl-app-headless -am

# Run headless application with debug logging
cd lpg-ehl-app-headless
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.no.cloudberries.lpg=DEBUG"

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=headless"
```

---

## Reporting Issues to Team

When reporting Maven/Kotlin issues to Herman or Alan, include:

1. **Kotlin version info:**
   ```bash
   mvn dependency:tree -Dincludes=org.jetbrains.kotlin:*
   ```

2. **Build output:**
   ```bash
   mvn clean install -X 2>&1 | tee build.log
   ```

3. **Daemon state:**
   ```bash
   ls -la "$HOME/Library/Application Support/kotlin/daemon/"
   ps aux | grep kotlin
   ```

---

## Contact

For persistent issues not covered here, contact the development team or check project documentation in `/docs/`.
