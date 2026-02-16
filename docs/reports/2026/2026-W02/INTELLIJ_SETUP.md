# IntelliJ IDEA Setup Guide

## Quick Start for Development

### Prerequisites
- IntelliJ IDEA Ultimate (for Spring Boot support)
- Docker Desktop running
- Java 21 (Temurin 21.0.7-tem via SDKMAN)

### Step-by-Step Setup

#### 1. Start Infrastructure Services

Start PostgreSQL and Azurite in Docker:

```bash
docker-compose -f docker-compose.postgres.yaml up -d
```

This starts:
- **PostgreSQL** on port 5432
- **Azurite** (Azure Storage Emulator) on port 10001

Verify services are running:
```bash
docker-compose -f docker-compose.postgres.yaml ps
```

#### 2. Run Application in IntelliJ

1. Open the project in IntelliJ IDEA
2. Wait for Maven dependencies to download
3. Find the run configuration: **"LPG-EHL-API (Local Dev)"** in the top toolbar
4. Click the green play button ▶️ to run
5. Or click the debug button 🐛 to run with debugger attached

The application will start on **http://localhost:8080**

#### 3. Access the Application

- **Frontend/Dashboard**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### Configuration Details

The run configuration uses:
- **Profile**: `local` (via `SPRING_PROFILES_ACTIVE=local`)
- **Port**: `8080`
- **Java Version**: 21
- **Hot Reload**: Enabled (Update Classes and Resources on save)

All database and Azure settings are pre-configured in `application-local.yaml`:
- PostgreSQL connection to `localhost:5432`
- Azurite connection to `localhost:10001`
- No additional environment variables needed!

### Debugging

To debug:
1. Set breakpoints in your code (click left gutter)
2. Click the debug button 🐛 instead of run
3. Make requests to trigger your breakpoints
4. Use IntelliJ's debugging tools to inspect variables, step through code, etc.

### Common Issues

#### Port 8080 Already in Use
```bash
# Find and kill the process using port 8080
lsof -ti:8080 | xargs kill -9
```

#### Database Connection Errors
```bash
# Restart PostgreSQL
docker-compose -f docker-compose.postgres.yaml restart postgres
```

#### Azure Queue Errors
```bash
# Restart Azurite
docker-compose -f docker-compose.postgres.yaml restart azurite
```

#### Clean Database Start
```bash
# Wipe all data and restart
docker-compose -f docker-compose.postgres.yaml down -v
docker-compose -f docker-compose.postgres.yaml up -d
```

### Useful Commands

```bash
# View logs from both services
docker-compose -f docker-compose.postgres.yaml logs -f

# View only PostgreSQL logs
docker-compose -f docker-compose.postgres.yaml logs -f postgres

# View only Azurite logs
docker-compose -f docker-compose.postgres.yaml logs -f azurite

# Stop all services
docker-compose -f docker-compose.postgres.yaml down

# Connect to database with psql
docker exec -it lpg-postgres-dev psql -U lpg_user -d lpg_ehl
```

### Alternative: Maven Command Line

If you prefer running from terminal:

```bash
cd lpg-ehl-api
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### Production vs Local Differences

| Aspect | Local (IntelliJ) | Production |
|--------|------------------|------------|
| Port | 8080 | 8080 (configurable) |
| Database | Local PostgreSQL (Docker) | Cloud PostgreSQL |
| Azure Storage | Azurite (emulator) | Real Azure Storage |
| Logs | Console + IntelliJ | Structured JSON |
| Security | Relaxed (dev token) | Strict tokens |
| Liquibase | `drop-first: true` | `drop-first: false` |

### Next Steps

- Read [DEVELOPER_GUIDE.md](docs/general/DEVELOPER_GUIDE.md) for architecture details
- Check [WARP.md](WARP.md) for AI-friendly project overview
- See [AZURE-STORAGE-PAGE.md](docs/AZURE-STORAGE-PAGE.md) for cloud sync details
