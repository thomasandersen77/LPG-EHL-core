# Multi-Station Development Setup

**Complete guide for running 3 Edge stations + Cloud simultaneously**

---

## 📋 Table of Contents
1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [Configuration Options](#configuration-options)
4. [Testing Scenarios](#testing-scenarios)
5. [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# 1. Build MinLPG backend image
cd /Users/tandersen/git/NorgesGass/MinLPG
docker build -t minlpg-backend:latest backend/

# 2. Start all 3 stations + cloud + database
cd /Users/tandersen/git/NorgesGass/lpg-ehl
docker-compose -f docker-compose.stations.yml up --build

# Wait for services to start... (about 30 seconds)
```

**✅ You now have:**
- **Station S001** (Drammen): `tcp://localhost:9001`
- **Station S002** (Oslo): `tcp://localhost:9002`
- **Station S003** (Trondheim): `tcp://localhost:9003`
- **Cloud API**: `http://localhost:8081`
- **Database**: `localhost:5433`

### Option 2: Manual Start (Development)

**Terminal 1 - Cloud Backend:**
```bash
cd /Users/tandersen/git/NorgesGass/MinLPG
docker-compose up minlpg-db minlpg-backend
```

**Terminal 2 - Station S001:**
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-emulator
STATION_ID=S001 EDGE_ID=EDGE-S001-01 DISPENSER_ID=D001 \
lpg-api.base-url=http://localhost:8081 \
emulator.port=9001 \
mvn spring-boot:run
```

**Terminal 3 - Station S002:**
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-emulator
STATION_ID=S002 EDGE_ID=EDGE-S002-01 DISPENSER_ID=D001 \
lpg-api.base-url=http://localhost:8081 \
emulator.port=9002 \
emulator.price-per-litre-cents=1620 \
mvn spring-boot:run
```

**Terminal 4 - Station S003:**
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-emulator
STATION_ID=S003 EDGE_ID=EDGE-S003-01 DISPENSER_ID=D001 \
lpg-api.base-url=http://localhost:8081 \
emulator.port=9003 \
emulator.price-per-litre-cents=1580 \
mvn spring-boot:run
```

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     MULTI-STATION ECOSYSTEM                          │
└─────────────────────────────────────────────────────────────────────┘

EDGE LAYER (Arc-maskiner med kiosk)
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  STATION S001    │  │  STATION S002    │  │  STATION S003    │
│  Drammen         │  │  Oslo Vest       │  │  Trondheim       │
│                  │  │                  │  │                  │
│  Port: 9001      │  │  Port: 9002      │  │  Port: 9003      │
│  Price: 15.90    │  │  Price: 16.20    │  │  Price: 15.80    │
│  Dispenser: D001 │  │  Dispenser: D001 │  │  Dispenser: D001 │
│                  │  │                  │  │                  │
│  📡 Heartbeat    │  │  📡 Heartbeat    │  │  📡 Heartbeat    │
│  Every 30s       │  │  Every 30s       │  │  Every 30s       │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                     │
         └─────────────────────┼─────────────────────┘
                               │
                               │ HTTPS/JSON
                               ↓
              ┌────────────────────────────────┐
              │    CLOUD LAYER (Azure)         │
              │                                │
              │  ┌──────────────────────────┐ │
              │  │ MinLPG Backend (8081)    │ │
              │  │ - Transaction Sync       │ │
              │  │ - Heartbeat Receiver     │ │
              │  │ - RBAC                   │ │
              │  └──────────────────────────┘ │
              │                                │
              │  ┌──────────────────────────┐ │
              │  │ PostgreSQL (5433)        │ │
              │  │ - Transactions           │ │
              │  │ - Station Heartbeats     │ │
              │  │ - Customers              │ │
              │  └──────────────────────────┘ │
              │                                │
              │  ┌──────────────────────────┐ │
              │  │ Frontend (3001)          │ │
              │  │ - Multi-Station Dashboard│ │
              │  │ - Transaction Monitor    │ │
              │  └──────────────────────────┘ │
              └────────────────────────────────┘
```

---

## ⚙️ Configuration Options

### Station Identifiers
Each Edge instance requires unique identifiers:

```bash
STATION_ID=S001              # Station code (used in database, reports)
EDGE_ID=EDGE-S001-01         # Edge device identifier (for hardware tracking)
DISPENSER_ID=D001            # Dispenser within station (D001, D002, etc.)
```

### Emulator Configuration
```bash
emulator.address=1                      # EHL protocol address (1-255)
emulator.port=9000                      # TCP port for legacy Windows client
emulator.price-per-litre-cents=1590     # Price in øre (15.90 kr/L)
emulator.litres-per-second=0.5          # Simulation flow rate
```

### Heartbeat Configuration
```bash
heartbeat.enabled=true                  # Enable/disable heartbeat
heartbeat.interval-seconds=30           # Heartbeat frequency
```

### Cloud Integration
```bash
lpg-api.base-url=http://localhost:8081  # Cloud API endpoint
```

---

## 🧪 Testing Scenarios

### Scenario 1: Basic Transaction Flow (Single Station)

1. **Connect Windows Dispenserkontroll to S001:**
   ```
   Port: 9001
   ```

2. **Perform fueling:**
   - Click UNBLOCK in Windows app
   - Watch simulation run
   - Click STOP

3. **Verify in Cloud:**
   ```bash
   curl http://localhost:8081/api/transactions | jq
   ```
   
   Expected: Transaction with `stationId: "S001"`

### Scenario 2: Multi-Station Concurrent Transactions

1. **Connect 3 Windows instances** (or use REST API):
   - Instance 1 → Port 9001 (S001)
   - Instance 2 → Port 9002 (S002)
   - Instance 3 → Port 9003 (S003)

2. **Start fueling on all 3 simultaneously**

3. **Verify all transactions tagged correctly:**
   ```bash
   curl http://localhost:8081/api/transactions?stationId=S001
   curl http://localhost:8081/api/transactions?stationId=S002
   curl http://localhost:8081/api/transactions?stationId=S003
   ```

### Scenario 3: Station Online/Offline Monitoring

1. **Check all stations heartbeat:**
   ```bash
   curl http://localhost:8081/api/stations | jq
   ```

2. **Stop one station:**
   ```bash
   docker stop edge-s002
   ```

3. **Wait 2 minutes** (heartbeat timeout)

4. **Verify S002 shows as OFFLINE:**
   ```bash
   curl http://localhost:8081/api/stations/S002 | jq '.isOnline'
   # Expected: false
   ```

5. **Restart station:**
   ```bash
   docker start edge-s002
   ```

6. **Verify S002 back ONLINE** (within 30 seconds)

### Scenario 4: Payment Pending Lock

1. **Start fueling on S001**
2. **STOP without settling**
3. **Try UNBLOCK again** → Should be rejected
4. **Settle payment:**
   ```bash
   curl -X POST http://localhost:8091/api/emulator/1/settle?method=CARD
   ```
5. **UNBLOCK again** → Should work

### Scenario 5: Price Differentiation

Each station has different prices:
- S001 (Drammen): **15.90 kr/L**
- S002 (Oslo): **16.20 kr/L**
- S003 (Trondheim): **15.80 kr/L**

Verify transactions reflect correct prices:
```bash
curl http://localhost:8081/api/transactions/latest | jq '.[] | {station: .stationId, price: .pricePerLiter}'
```

---

## 🔍 Monitoring & Debugging

### View Logs (Docker)
```bash
# All services
docker-compose -f docker-compose.stations.yml logs -f

# Specific station
docker logs -f edge-s001

# Cloud backend
docker logs -f minlpg-backend
```

### Check Heartbeat Status
```bash
# Raw heartbeat table
docker exec -it minlpg-db psql -U minlpg_user -d minlpg \
  -c "SELECT station_id, status, last_heartbeat_at FROM station_heartbeats;"
```

### Transaction Counts per Station
```bash
docker exec -it minlpg-db psql -U minlpg_user -d minlpg \
  -c "SELECT station_id, COUNT(*) FROM transactions GROUP BY station_id;"
```

---

## 🛠️ Troubleshooting

### Problem: Station not sending heartbeat

**Symptoms:** `isOnline: false` in API response

**Solution:**
1. Check `heartbeat.enabled=true` in config
2. Verify `lpg-api.base-url` is correct
3. Check network connectivity:
   ```bash
   docker exec edge-s001 curl -v http://minlpg-backend:8081/health
   ```

### Problem: Transaction not syncing to Cloud

**Symptoms:** Transaction logged in Edge but missing in Cloud

**Solution:**
1. Check Edge logs for HTTP errors
2. Verify Cloud API is running:
   ```bash
   curl http://localhost:8081/api/transactions
   ```
3. Check database connection in backend logs

### Problem: Windows Dispenserkontroll not connecting

**Symptoms:** Connection refused or timeout

**Solution:**
1. Verify correct port:
   - S001: 9001
   - S002: 9002
   - S003: 9003
2. Check if container is running:
   ```bash
   docker ps | grep edge
   ```
3. Test TCP connection:
   ```bash
   telnet localhost 9001
   ```

### Problem: PAYMENT_PENDING lock not releasing

**Symptoms:** Cannot start new fueling after STOP

**Solution:**
1. Manually settle via API:
   ```bash
   curl -X POST http://localhost:8091/api/emulator/1/settle?method=CARD
   ```
2. Or restart Edge container (testing only):
   ```bash
   docker restart edge-s001
   ```

---

## 📊 Performance Testing

### Stress Test: 100 Transactions per Station

```bash
# S001
for i in {1..100}; do
  curl -X POST http://localhost:8091/api/dispenser/1/unblock
  sleep 2
  curl -X POST http://localhost:8091/api/dispenser/1/stop
  curl -X POST http://localhost:8091/api/emulator/1/settle?method=CARD
  echo "S001 Transaction $i complete"
done
```

### Monitor Resource Usage

```bash
docker stats edge-s001 edge-s002 edge-s003 minlpg-backend minlpg-db
```

---

## 🎯 Next Steps

1. **Add Frontend Dashboard** - React components for station monitoring
2. **Implement RBAC** - Role-based filtering (SUPER_ADMIN vs STATION_OWNER)
3. **Add Config Push** - Cloud can update station prices remotely
4. **Implement Alerting** - Notify when station goes offline
5. **Add Metrics** - Prometheus/Grafana for monitoring

---

## 📚 Related Documentation

- `README.md` - Project overview
- `WARP.md` - WARP agent guidance
- `docker-compose.stations.yml` - Multi-station Docker setup
- `IMPLEMENTATION_PLAN_PAYMENT_RESET.md` - Payment pending design

---

**Created:** 2025-12-27  
**Authors:** Thomas + Warp Agent  
**Version:** 1.0.0
