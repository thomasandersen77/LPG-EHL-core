# Quick Start Guide

## What's Fixed

✅ **Database**: Clean Liquibase schema (drops and recreates on startup)  
✅ **Transactions**: Properly saved with correct payment type (CASH/CARD/CREDIT)  
✅ **Frontend Pages**: 
   - Transactions page with table, filters, and pagination
   - Reports page with daily summary dashboard
✅ **Simulator**: Passes payment method to backend correctly

## Start the System

```bash
./start-system.sh
```

This will:
1. Start PostgreSQL and Azurite (Docker)
2. Build the backend
3. Start the API on port 8080
4. Start the frontend on port 3000

## Access

- **Frontend**: http://localhost:3000
- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html

## Test the Flow

1. Go to **Simulator** (http://localhost:3000/simulator)
2. Select payment method:
   - 💰 Kontant (CASH)
   - 💳 Kort (CARD)
   - 🏪 Stasjonskreditt (CREDIT)
3. Click **▶ Start** (begins fuel delivery)
4. Wait a few seconds (it simulates at 0.5 L/s)
5. Click **■ Stopp** (saves transaction to DB)
6. Go to **Transaksjoner** - you should see your transaction
7. Go to **Rapporter** - you should see the volume/amount reflected

## Verify Data

### Check transactions via API:
```bash
curl http://localhost:8080/api/v1/transactions | jq
```

### Check database directly:
```bash
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl -c "SELECT transaction_id, dispenser_address, volume_deciliters, amount_ore, payment_type, timestamp FROM transactions ORDER BY timestamp DESC LIMIT 5;"
```

## Stop the System

```bash
# Stop processes (PIDs shown in start script output)
kill <API_PID> <FRONTEND_PID>

# Stop Docker services
docker-compose -f docker-compose-local.yaml down
```

## Troubleshooting

### API won't start
```bash
# Check logs
tail -f api.log

# Check if port is in use
lsof -i :8080
```

### Frontend errors
```bash
# Check logs
tail -f frontend.log

# Verify node_modules
cd lpg-web && npm install
```

### Database issues
```bash
# Reset database (WARNING: deletes all data)
docker-compose -f docker-compose-local.yaml down -v
docker-compose -f docker-compose-local.yaml up -d postgres
```

### "Feil: Kan ikke koble til API"
- Make sure API is running: `curl http://localhost:8080/actuator/health`
- Check CORS settings in `application-local.yaml`
- Check browser console for errors

## Architecture

```
┌─────────────┐         ┌──────────────┐         ┌────────────┐
│   Frontend  │◄───────►│     API      │◄───────►│ PostgreSQL │
│   (React)   │  HTTP   │ (Spring Boot)│  JDBC   │  Database  │
│   :3000     │         │    :8080     │         │   :5432    │
└─────────────┘         └──────────────┘         └────────────┘
                               │
                               │ Liquibase
                               │ (Schema Mgmt)
                               ▼
                        ┌──────────────┐
                        │ transactions │
                        │ dispenser_   │
                        │   status     │
                        │ etc.         │
                        └──────────────┘
```

## Key Files Changed

### Backend
- `lpg-ehl-api/src/main/resources/db/changelog/` - Liquibase schemas
- `DemoDispenserController.kt` - Accepts `paymentType` parameter
- `TransactionService.kt` - Added `updatePaymentType()` method
- `DispenserStatus.kt` - Refactored to match new schema
- `ApiResponses.kt` - Updated DTOs

### Frontend
- `src/pages/TransactionsPage.tsx` - Full implementation with table/filters
- `src/pages/ReportsPage.tsx` - Dashboard with daily summary
- `src/components/DispenserSimulator.tsx` - Passes payment method
- `src/api/dispenser.ts` - Updated to send `paymentType`
- `src/api/transactions.ts` - Fixed DTO to match backend
- `src/api/reports.ts` - Fixed DTO to match backend

## Notes

- Database is **wiped on every API startup** (configured for development)
- Default payment type is CASH if none specified
- Simulator runs at 0.5 L/s (configurable in `DemoDispenserController`)
- Price is hardcoded to 15.90 kr/L in the simulator
