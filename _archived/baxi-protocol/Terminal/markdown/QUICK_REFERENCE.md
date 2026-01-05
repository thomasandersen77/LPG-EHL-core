# LPG-EHL Quick Reference

## 🚀 Start Everything

```bash
./start-local.sh
```

## 🌐 URLs

| What | URL |
|------|-----|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

## 🗄️ Database

```bash
# Connect
psql -h localhost -U lpg_user -d lpg_ehl

# Password: lpg_dev_password
```

## 📋 Common Commands

```bash
# View all logs
docker-compose -f docker-compose-local.yaml logs -f

# View API logs only
docker-compose -f docker-compose-local.yaml logs -f api

# Stop everything
docker-compose -f docker-compose-local.yaml down

# Restart API
docker-compose -f docker-compose-local.yaml restart api

# Rebuild and restart
docker-compose -f docker-compose-local.yaml up -d --build
```

## 🧪 Quick API Tests

```bash
# Health check
curl http://localhost:8080/actuator/health

# Get dispenser state
curl http://localhost:8080/api/v1/dispenser/state

# List transactions
curl http://localhost:8080/api/v1/transactions

# List credit accounts
curl http://localhost:8080/api/v1/credit/accounts

# Create credit account
curl -X POST http://localhost:8080/api/v1/credit/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Test AS", "customerNumber": "CUST001", "creditLimitNok": 10000}'

# Start payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"amountCents": 10000, "method": "CARD", "reference": "TEST-001"}'

# Get emulator status
curl http://localhost:8080/api/v1/emulator/status/1

# Set emulator to timeout scenario
curl -X POST http://localhost:8080/api/v1/emulator/scenario \
  -H "Content-Type: application/json" \
  -d '{"dispenserAddress": 1, "scenario": "TIMEOUT"}'
```

## 🔧 Troubleshooting

```bash
# Port in use
lsof -i :8080
lsof -i :5432
kill -9 <PID>

# Check container status
docker ps

# View container logs
docker logs lpg-api
docker logs lpg-postgres

# Restart container
docker restart lpg-api

# Full reset (removes all data!)
docker-compose -f docker-compose-local.yaml down -v
./start-local.sh
```

## 📦 Database Queries

```sql
-- Count transactions
SELECT COUNT(*) FROM transactions;

-- Recent transactions
SELECT * FROM transactions ORDER BY started_at DESC LIMIT 10;

-- Credit accounts with balance
SELECT c.name, ca.balance_cents / 100.0 as balance_kr, ca.credit_limit_cents / 100.0 as limit_kr
FROM credit_accounts ca
JOIN customers c ON c.id = ca.customer_id
WHERE ca.active = true;

-- Transactions by payment type
SELECT payment_type, COUNT(*), SUM(amount_cents) / 100.0 as total_kr
FROM transactions
GROUP BY payment_type;

-- Daily summary
SELECT * FROM daily_summary ORDER BY transaction_date DESC LIMIT 7;
```

## 🎯 Feature Testing Checklist

- [ ] Frontend loads on http://localhost:3000
- [ ] Can navigate between all pages
- [ ] Dispenser simulator works (start/stop/reset)
- [ ] Payment flow works (cash/card/credit)
- [ ] Emulator debug page shows scenarios
- [ ] Transactions page loads (backend needs TransactionController)
- [ ] Credit accounts page loads (backend complete!)
- [ ] Can create new credit account via API
- [ ] Swagger UI accessible and functional
- [ ] Database accepts connections

## 📚 Documentation

- [DEVELOPMENT.md](DEVELOPMENT.md) - Full development guide
- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) - What's implemented
- [README.md](README.md) - Project overview
- [lpg-ehl-core/WARP.md](lpg-ehl-core/WARP.md) - Protocol details
