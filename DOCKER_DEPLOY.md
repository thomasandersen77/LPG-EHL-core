# LPG-EHL Docker Deployment

## 🚀 Quick Start

```bash
# Start all services
docker-compose -f docker-compose-local.yml --env-file .env.local up -d

# View logs
docker-compose -f docker-compose-local.yml logs -f

# Stop all services
docker-compose -f docker-compose-local.yml down

# Stop and remove volumes (clean slate)
docker-compose -f docker-compose-local.yml down -v
```

## 📍 Access URLs

### Local Development

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | React/Vite GUI |
| **API** | http://localhost:8081 | Spring Boot REST API |
| **Emulator** | http://localhost:9001 | PLS + Payment Simulator |
| **PostgreSQL** | localhost:5432 | Database (user: lpguser, db: lpg_ehl) |
| **Azurite** | localhost:10000-10002 | Azure Storage Emulator |
| **ngrok Dashboard** | http://localhost:4040 | ngrok web interface |

### Public Access (for Tobias)

**API via ngrok**: https://1f72e231dd24.ngrok-free.app

This URL exposes the API to the internet for testing with Gemini.

## 🏗️ Architecture

```
┌─────────────┐
│   Frontend  │  Port 3000 (nginx)
│  (React)    │
└──────┬──────┘
       │
       ↓
┌──────────────┐     ┌─────────────┐
│     API      │────→│  PostgreSQL │  Port 5432
│ (Spring Boot)│     │   Database  │
└──────┬───────┘     └─────────────┘
       │
       ├──────────→  Azurite (Blob/Queue/Table) Port 10000-10002
       │
       └──────────→  Emulator (PLS + Payment) Port 9001
                     └─→ FakeNetsCloudServer: Port 6001 (SSL)

ngrok ────→ API (exposes to internet)
```

## 🔧 Services

### 1. PostgreSQL Database
- **Image**: postgres:16-alpine
- **Port**: 5432
- **Credentials**: lpguser / lpgpass
- **Database**: lpg_ehl

### 2. Azurite (Azure Storage Emulator)
- **Blob service**: Port 10000
- **Queue service**: Port 10001
- **Table service**: Port 10002

### 3. Emulator (PLS + Payment Terminal Simulator)
- **Port**: 9001 (Spring Boot)
- **Port**: 6001 (FakeNetsCloudServer - SSL/TLS)
- **Station**: S001
- **Edge**: EDGE-LOCAL
- **Dispenser**: D001

Simulates:
- Legacy dispenserkontroll (PLS)
- Nets Cloud Connect payment terminal (Baxi protocol over SSL)

### 4. API (Spring Boot Backend)
- **Port**: 8081
- **Profile**: local
- Connects to postgres, azurite, and emulator

### 5. Web (React Frontend)
- **Port**: 3000 (mapped from nginx port 80)
- **Build**: Vite + TypeScript + Tailwind CSS

### 6. ngrok
- **Port**: 4040 (web interface)
- **Tunnel**: Exposes API (port 8081) to internet
- **Auth**: Uses NGROK_AUTH_TOKEN from .env.local

## 🔐 Environment Variables

Create `.env.local`:
```bash
NGROK_AUTH_TOKEN=your_ngrok_token_here
```

## 🧪 Testing

### Check Service Health

```bash
# API health
curl http://localhost:8081/actuator/health

# Emulator health
curl http://localhost:9001/actuator/health

# Frontend
curl http://localhost:3000
```

### View Logs

```bash
# All services
docker-compose -f docker-compose-local.yml logs -f

# Single service
docker-compose -f docker-compose-local.yml logs -f api
docker-compose -f docker-compose-local.yml logs -f emulator
```

### Database Access

```bash
# Connect to PostgreSQL
docker exec -it lpg-postgres-local psql -U lpguser -d lpg_ehl

# Or from host
psql -h localhost -U lpguser -d lpg_ehl
```

## 🐛 Troubleshooting

### Port Conflicts

If you get "port already in use" errors:

```bash
# Check what's using the port
lsof -i :9001
lsof -i :8081
lsof -i :3000

# Stop conflicting services
docker-compose -f docker-compose-local.yml down
```

### Emulator Health Check Warnings

The emulator logs may show "Invalid STX byte" warnings - this is normal. These occur when the health check `curl` command hits the Baxi protocol handler. The emulator is working correctly.

### Rebuild After Code Changes

```bash
# Rebuild specific service
docker-compose -f docker-compose-local.yml up --build -d api

# Rebuild all
docker-compose -f docker-compose-local.yml up --build -d
```

## 📦 What Gets Simulated

### PLS (Pump Logic System)
- Dispenserkontroll commands
- Transaction handling
- Station heartbeat

### Payment Terminal (Nets Cloud Connect)
- Baxi protocol over SSL/TLS
- Simulated payment transactions
- FakeNetsCloudServer on port 6001

## 🌍 Share with Tobias

Send Tobias the ngrok URL for API testing:

```
API (public): https://1f72e231dd24.ngrok-free.app
```

He can use this with Gemini to test the system remotely.

## 📝 Notes

- The emulator runs both Spring Boot (port 9001) and FakeNetsCloudServer (port 6001)
- Health checks are disabled for emulator due to socket server on same port
- All data is stored in Docker volumes (removed with `down -v`)
- Frontend connects to API via environment variable `VITE_API_BASE_URL`
