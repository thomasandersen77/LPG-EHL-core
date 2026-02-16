# Testing Documentation

Denne katalogen inneholder dokumentasjon relatert til testing og funksjonsutvikling.

## Innhold

Denne katalogen inneholder diverse notater og funksjonsforslag:

- `add_more_functions.md` - Forslag til flere funksjoner
- `demo-frontend.md` - Frontend demo notater
- `product_all_functoins.md` - Produktfunksjoner oversikt
- `vipps_payment.md` - Vipps betalingsintegrasjon notater

## Testing Guides

For offisiell testdokumentasjon, se:

- **[Payment Pending Testing](../implementation/TESTING_PAYMENT_PENDING.md)** - Payment flow testing
- **[VB6 Compatibility Test](../implementation/VB6_COMPATIBILITY_TEST.md)** - Legacy compatibility testing
- **[Multi-Station Setup](../development/MULTI-STATION-SETUP.md)** - Testing scenarios for multi-station

## Unit Testing

Prosjektet har omfattende unit tests:

```bash
# Kjør alle tester
mvn test

# Kjør kun core tester
cd lpg-ehl-core && mvn test

# Kjør kun emulator tester  
cd lpg-ehl-emulator && mvn test

# Kjør kun API tester
cd lpg-ehl-api && mvn test
```

**Test Coverage:**
- `lpg-ehl-core`: 52 tests
- `lpg-ehl-emulator`: 11 integration tests
- `lpg-ehl-api`: REST API tests
- **Total**: 61+ tests

## Integration Testing

### Med Emulator
```bash
# Start emulator
cd lpg-ehl-emulator
mvn spring-boot:run

# I annen terminal: Test med curl
curl -X POST http://localhost:9000/api/dispenser/1/unblock
curl -X POST http://localhost:9000/api/dispenser/1/stop
```

### Med Windows Dispenserkontroll
1. Start full stack i IntelliJ
2. Koble Windows-klient til `localhost:9000`
3. Test komplett fueling-flow
4. Verifiser i frontend på `http://localhost:8080`

### Multi-Station Testing
Se [Multi-Station Setup](../development/MULTI-STATION-SETUP.md) for:
- 3 stasjoner samtidig
- Concurrent transactions
- Heartbeat monitoring
- Online/offline scenarios

## Performance Testing

```bash
# Stress test: 100 transaksjoner
for i in {1..100}; do
  curl -X POST http://localhost:9000/api/dispenser/1/unblock
  sleep 2
  curl -X POST http://localhost:9000/api/dispenser/1/stop
  curl -X POST http://localhost:9000/api/emulator/1/settle?method=CARD
  echo "Transaction $i complete"
done
```

## Test Data

For testing med realistisk data:
- Use emulator med konfigurerbar pris og flow rate
- Generer transaksjoner med `scripts/simulate-traffic.sh`
- Monitor Azure queue med `scripts/view-azurite-messages.sh`
