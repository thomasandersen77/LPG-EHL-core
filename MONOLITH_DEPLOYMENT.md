# LPG-EHL Monolith Deployment Guide

**Branch**: `feature/monolith-spa`

This guide describes how to build and deploy the LPG-EHL system as a single executable JAR file (monolith) containing both backend and frontend.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│   lpg-ehl-monolith.jar (Single Executable JAR)     │
│                                                     │
│  ┌────────────────────────────────────────────┐   │
│  │  Spring Boot (Port 8080)                   │   │
│  │                                             │   │
│  │  ┌──────────────┐  ┌────────────────────┐  │   │
│  │  │   REST API   │  │  Static Resources  │  │   │
│  │  │   /api/*     │  │  React Frontend    │  │   │
│  │  │              │  │  (/, /fueling,     │  │   │
│  │  │  - Dispenser │  │   /transactions,   │  │   │
│  │  │  - Payments  │  │   /reports, etc.)  │  │   │
│  │  │  - Reports   │  │                    │  │   │
│  │  └──────────────┘  └────────────────────┘  │   │
│  │                                             │   │
│  │  ┌────────────────────────────────────┐    │   │
│  │  │  lpg-ehl-core                      │    │   │
│  │  │  (EHL Protocol, Transaction Mgmt)  │    │   │
│  │  └────────────────────────────────────┘    │   │
│  └────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Building the Monolith

### Prerequisites
- **Node.js 18+** (for React build)
- **Maven 3.8+** (for Spring Boot build)
- **Java 21** (Temurin recommended)

### Build Command
```bash
./build_monolith.sh
```

### Build Process
The script performs the following steps:

1. **Build React Frontend**
   - Runs `npm install` (if needed)
   - Runs `npm run build` in `lpg-web/`
   - Creates production-optimized bundle in `lpg-web/dist/`

2. **Clean Static Resources**
   - Removes old files from `lpg-ehl-api/src/main/resources/static/`

3. **Copy Frontend to Backend**
   - Copies all files from `lpg-web/dist/` to `lpg-ehl-api/src/main/resources/static/`
   - React app becomes part of Spring Boot JAR

4. **Build Spring Boot JAR**
   - Runs `mvn clean package -DskipTests`
   - Creates Fat JAR with all dependencies
   - Includes lpg-ehl-core, lpg-ehl-api, and React frontend

5. **Prepare Release**
   - Copies JAR to `release/` directory
   - Renames to `lpg-ehl-monolith-VERSION.jar`

### Output
```
release/
└── lpg-ehl-monolith-0.0.1-SNAPSHOT.jar  (~80MB)
```

## How It Works

### SPA Support (React Router)
The monolith includes `SpaRedirectConfig` which enables React Router to work correctly:

- **Static Resources** (CSS, JS, images) → Served directly
- **API Calls** (`/api/*`) → Handled by Spring controllers
- **Client Routes** (`/fueling`, `/transactions`, etc.) → Return `index.html`
  - This allows page refresh (F5) and deep linking to work

### Request Flow
```
Browser Request
    ↓
Spring Boot (Port 8080)
    ↓
┌─ Is it /api/* ? ────→ Spring Controller
│                           ↓
│                       JSON Response
│
└─ Is it a static file? → Serve from /static
       (CSS, JS, etc.)
    
└─ Otherwise ──────────→ Return index.html
                            ↓
                        React Router takes over
```

## Running Locally

### From JAR
```bash
cd release
java -jar lpg-ehl-monolith-0.0.1-SNAPSHOT.jar
```

Or on Linux (executable JAR):
```bash
./lpg-ehl-monolith-0.0.1-SNAPSHOT.jar
```

### From IntelliJ
1. Open `lpg-ehl-api` module
2. Run `LpgEhlApiApplication.kt`
3. Frontend served at: `http://localhost:8080`
4. API at: `http://localhost:8080/api/*`
5. Swagger at: `http://localhost:8080/swagger-ui.html`

**Note**: You must run `./build_monolith.sh` first to copy React files to `static/`

## Deployment to ARK Machine (32-bit Linux)

### 1. Transfer JAR
```bash
scp release/lpg-ehl-monolith-0.0.1-SNAPSHOT.jar user@ark-machine:/opt/lpg-ehl/
```

### 2. Install Java 21 on ARK (if needed)
```bash
# On ARK machine (32-bit Linux)
sudo apt update
sudo apt install openjdk-21-jre-headless
```

### 3. Create Systemd Service
Create `/etc/systemd/system/lpg-ehl.service`:

```ini
[Unit]
Description=LPG-EHL Edge System
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl.jar
Restart=always
RestartSec=10

# Environment variables
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="DB_HOST=localhost"
Environment="DB_PORT=5432"
Environment="DB_NAME=lpg_ehl"
Environment="DB_USER=lpg_user"
Environment="DB_PASSWORD=<secret>"

# Resource limits (adjust for 32-bit)
MemoryLimit=512M
CPUQuota=50%

[Install]
WantedBy=multi-user.target
```

### 4. Deploy and Start
```bash
# Stop old version
sudo systemctl stop lpg-ehl

# Copy new version
sudo cp /opt/lpg-ehl/lpg-ehl-monolith-0.0.1-SNAPSHOT.jar /opt/lpg-ehl/lpg-ehl.jar

# Set permissions
sudo chown lpg:lpg /opt/lpg-ehl/lpg-ehl.jar
sudo chmod +x /opt/lpg-ehl/lpg-ehl.jar

# Start service
sudo systemctl start lpg-ehl

# Enable on boot
sudo systemctl enable lpg-ehl

# Check status
sudo systemctl status lpg-ehl
sudo journalctl -u lpg-ehl -f
```

## Configuration

### Application Properties
The monolith uses the same configuration as the separate API module.

**Development** (`application-local.yaml`):
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lpg_ehl
    username: lpg_user
    password: lpg_dev_password
```

**Production** (environment variables):
```bash
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lpg_ehl
DB_USER=lpg_user
DB_PASSWORD=<secret>
```

## Advantages of Monolith

✅ **Single Deployment Unit** - One JAR to deploy, no separate web server  
✅ **Simplified Architecture** - No CORS, no separate frontend server  
✅ **Easy Updates** - Update both frontend and backend together  
✅ **Smaller Resource Footprint** - Single JVM process  
✅ **Better for Edge Devices** - Less complexity on resource-constrained ARK machines

## Disadvantages

❌ **Larger JAR Size** - ~80MB vs ~40MB for API-only  
❌ **Slower Builds** - Must rebuild backend when frontend changes  
❌ **No Independent Scaling** - Frontend and backend scale together

## Development Workflow

### Frontend Changes Only
```bash
cd lpg-web
npm run dev  # Development server on port 5173
```
Then proxy API calls to `localhost:8080` in `vite.config.ts`.

### Backend Changes Only
Run `LpgEhlApiApplication` in IntelliJ (uses last built frontend).

### Full Build
```bash
./build_monolith.sh
java -jar release/lpg-ehl-monolith-*.jar
```

## Troubleshooting

### Frontend Shows 404
- Ensure `build_monolith.sh` was run
- Check `lpg-ehl-api/src/main/resources/static/` contains files
- Verify `SpaRedirectConfig` is loaded

### React Router Doesn't Work on Refresh
- Check `SpaRedirectConfig` is properly configured
- Verify `index.html` is returned for non-API, non-file requests
- Check browser console for errors

### API Calls Fail
- Ensure API controllers use `/api/*` prefix
- Check `SpaRedirectConfig` doesn't intercept API calls
- Verify CORS configuration if calling from external origin

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Build Monolith

on:
  push:
    branches: [main, feature/monolith-spa]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: 18
      - uses: actions/setup-java@v3
        with:
          java-version: 21
      - name: Build Monolith
        run: ./build_monolith.sh
      - name: Upload JAR
        uses: actions/upload-artifact@v3
        with:
          name: lpg-ehl-monolith
          path: release/*.jar
```

## Related Files

- `build_monolith.sh` - Build script
- `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/SpaRedirectConfig.kt` - React Router support
- `lpg-ehl-api/pom.xml` - Fat JAR configuration
- `lpg-web/vite.config.ts` - Frontend build configuration

## Next Steps

1. Test monolith locally: `./build_monolith.sh && java -jar release/*.jar`
2. Verify frontend at `http://localhost:8080`
3. Test all React Router routes (refresh pages)
4. Deploy to test ARK machine
5. Monitor resource usage (memory, CPU)
6. Update deployment documentation

---

**Questions?** See `README.md` or `WARP.md` for more information.
