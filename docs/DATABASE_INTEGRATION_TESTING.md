# Database Integration Testing Guide

This guide describes how to test the complete database integration stack: Postgres → API → Emulator → Frontend.

## Architecture Overview

```
Windows Dispenserkontroll (Legacy Client)
    ↓ (Legacy text commands via TCP port 9000)
lpg-ehl-emulator (IntelliJ)
    ├─ EhlDispenserEmulator (Core protocol handling)
    ├─ EmulatorService (TCP server + transaction persistence)
    └─ TransactionPersistenceService → REST call
        ↓ (HTTP POST to localhost:8080)
lpg-ehl-api (Docker or IntelliJ)
    ├─ TransactionController (REST endpoint)
    ├─ TransactionService (Business logic)
    └─ TransactionRepository (JPA)
        ↓ (JDBC)
PostgreSQL (Docker)
    └─ lpg_ehl database

lpg-web (Terminal - Vite dev server)
    └─ Fetches transactions from API
```

## Prerequisites

1. **Docker** installed and running
2. **IntelliJ IDEA** with Kotlin support
3. **Node.js** and **npm** installed (for frontend)
4. **Java 21** via SDKMAN

## Step 1: Start PostgreSQL Database

First, start the Postgres database in Docker:

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
docker-compose -f docker-compose-local.yaml up postgres -d
```

Verify the database is running:

```bash
docker ps | grep postgres
```

You should see the postgres container running on port 5432.

### Database Connection Info
- **Host**: localhost:5432
- **Database**: lpg_ehl
- **User**: lpg_user
- **Password**: lpg_dev_password

## Step 2: (Optional) Start the API

You can start the API in Docker or IntelliJ:

### Option A: API in Docker (Easiest)

```bash
docker-compose -f docker-compose-local.yaml up api -d
```

### Option B: API in IntelliJ (For debugging)

1. Open `lpg-ehl-api` module in IntelliJ
2. Run `LpgEhlApiApplication.kt`
3. API will start on port 8080

Verify the API is running:

```bash
curl http://localhost:8080/actuator/health
```

## Step 3: Start the Emulator in IntelliJ

1. Open `lpg-ehl-emulator` module in IntelliJ
2. Make sure you're using Java 21:
   ```bash
   sdk use java 21.0.7-tem
   ```
3. Run `LpgEhlEmulatorApplication.kt`
4. Emulator will start on port 9000 (TCP server)

You should see:

```
🚀 EHL EMULATOR STARTED - LEGACY INTEGRATION BRIDGE
   Port: 9000
   Mode: Dual Protocol (EHL Binary + Legacy Text Tags)
   Dispenser Address: 1
   Price: 15.90 NOK/L
   Flow Rate: 0.5 L/s
   Ready to accept connections from Windows Dispenserkontroll...
```

## Step 4: Start the Frontend

Open a terminal and start the frontend dev server:

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-web
npm install  # First time only
npm run dev
```

The frontend will start on http://localhost:3000

## Step 5: Run Windows Dispenserkontroll

1. Open Parallels and start your Windows VM
2. Run the Dispenserkontroll application
3. Configure it to connect to:
   - **Host**: Your Mac's IP address (e.g. 192.168.1.100)
   - **Port**: 9000

## Testing Scenarios

### Test 1: Single Dispenser Transaction

1. In Dispenserkontroll, send UNBLOCK command
2. Wait for filling simulation (totals increasing)
3. Send STOP command
4. Check emulator logs in IntelliJ:
   ```
   💾 Transaction saved: 12.5L, 198.75 kr
   ✅ Transaction saved successfully to database
   ```
5. Open frontend (http://localhost:3000)
6. Navigate to Transactions page
7. Verify the transaction appears in the list

### Test 2: Multiple Dispenser Addresses

To test multiple dispensers, you need to run multiple emulator instances with different addresses:

1. Stop the first emulator in IntelliJ
2. Edit Run Configuration → Environment Variables:
   - `EMULATOR_ADDRESS=2`
   - `EMULATOR_PORT=9001`
3. Run the second emulator instance
4. In Dispenserkontroll, configure connection to port 9001
5. Repeat transaction test
6. Verify dispenser address 2 appears in database

Repeat for dispenser 3 with:
- `EMULATOR_ADDRESS=3`
- `EMULATOR_PORT=9002`

### Test 3: Verify Database Directly

Connect to Postgres and verify data:

```bash
docker exec -it lpg-ehl-postgres-1 psql -U lpg_user -d lpg_ehl
```

Run query:

```sql
SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 10;
```

You should see:
- `transaction_id` (auto-generated UUID)
- `dispenser_address` (1, 2, or 3)
- `volume_deciliters` (e.g. 125 for 12.5L)
- `amount_ore` (e.g. 19875 for 198.75 kr)
- `price_per_liter` (e.g. 1590 for 15.90 kr/L)
- `timestamp` (auto-generated)

Exit psql with `\q`

## Troubleshooting

### Emulator won't start
- Check Java version: `java -version` (should be 21)
- Check if port 9000 is in use: `lsof -i :9000`

### API connection errors
- Verify API is running: `curl http://localhost:8080/actuator/health`
- Check emulator logs for HTTP errors
- Verify `lpg-api.base-url` in `application.yaml`

### Database connection errors
- Check Postgres container: `docker ps | grep postgres`
- Check logs: `docker logs lpg-ehl-postgres-1`
- Verify credentials in API's `application.yaml`

### Frontend shows no transactions
- Open browser DevTools → Network tab
- Verify API calls to http://localhost:8080/api/v1/transactions
- Check for CORS errors (shouldn't happen in dev mode)

### Transaction not saved
- Check emulator logs in IntelliJ
- Verify volume > 0 (no save if volume is 0)
- Check API logs for save errors
- Verify database connection

## Multi-Dispenser Setup

To simulate a full station with 3 pumps:

1. **Terminal 1** - Start database:
   ```bash
   docker-compose -f docker-compose-local.yaml up postgres -d
   ```

2. **Terminal 2** - Start API (optional):
   ```bash
   docker-compose -f docker-compose-local.yaml up api -d
   ```

3. **IntelliJ Instance 1** - Dispenser 1 (address=1, port=9000)
4. **IntelliJ Instance 2** - Dispenser 2 (address=2, port=9001)
5. **IntelliJ Instance 3** - Dispenser 3 (address=3, port=9002)

Each IntelliJ instance needs separate run configurations with different environment variables.

6. **Terminal 3** - Start frontend:
   ```bash
   cd lpg-web && npm run dev
   ```

7. **Parallels** - 3 instances of Dispenserkontroll, each connected to different ports

## Expected Flow

1. **UNBLOCK** → Dispenser transitions to AUTHORIZED → DELIVERING
2. **Simulation** → Volume and amount increase every 100ms
3. **STOP** → Dispenser transitions to PAYMENT_PENDING
   - Transaction saved to database via REST API
   - Totals frozen in PAYMENT_PENDING state
4. **Frontend** → Fetches and displays all transactions
5. **RESET** → Dispenser transitions back to IDLE

## Logs to Watch

### Emulator Logs (IntelliJ Console)
```
📥 RECEIVED from client (20 bytes)
   ├─ Protocol: LEGACY TEXT
   └─ Command: <TANK_DISP_STOP>
💾 Transaction saved: 12.5L, 198.75 kr
✅ Transaction saved successfully to database
```

### API Logs (Docker or IntelliJ)
```
POST /api/v1/transactions/demo/save
Saving demo transaction: Dispenser=1, Volume=12.5L, Amount=198.75
Transaction saved with ID: 123e4567-e89b-12d3-a456-426614174000
```

### Frontend Console (Browser DevTools)
```
GET http://localhost:8080/api/v1/transactions → 200 OK
Loaded 5 transactions
```

## Cleanup

Stop all services:

```bash
# Stop Docker containers
docker-compose -f docker-compose-local.yaml down

# Stop IntelliJ emulator(s) - press Stop button
# Stop frontend - press Ctrl+C in terminal
```

Clean database (CAUTION: deletes all data):

```bash
docker-compose -f docker-compose-local.yaml down -v
docker-compose -f docker-compose-local.yaml up postgres -d
```

---

## Summary

✅ **Database**: Postgres in Docker on port 5432  
✅ **API**: Spring Boot in Docker/IntelliJ on port 8080  
✅ **Emulator**: Kotlin in IntelliJ on port 9000 (or 9001, 9002 for multi-dispenser)  
✅ **Frontend**: React/Vite in terminal on port 3000  
✅ **Legacy Client**: Windows Dispenserkontroll connects to emulator TCP port

**Transaction Flow**: Windows Client → Emulator (STOP) → API (REST) → Postgres → Frontend (displays)
