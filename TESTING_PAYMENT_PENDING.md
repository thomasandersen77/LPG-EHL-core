# Testing Payment Pending Hardening

Branch: `feature/payment-pending-hardening`

## What Was Implemented

Based on ChatGPT's analysis, implemented robust "pumpestreng" behavior:

### 1. PAYMENT_PENDING State
- New enum value `PAYMENT_PENDING(8)` in `DispenserState`
- Transaction freezes after STOP/BLOCK
- UNBLOCK rejected until settlement
- Returns `TRANSACTION_COMPLETE (0x08)` to Windows for compatibility

### 2. Transaction Freeze
- `CompletedTransaction` data class with UUID idempotency key
- Immutable snapshot created on STOP/BLOCK
- Stored in `pendingTransaction` field
- `settleAndReset()` method for cleanup

### 3. Async Persistence
- `TransactionSink` with Channel-based queue
- Background coroutine consumer
- Automatic retry: 2s, 4s, 8s, 16s (max 5 attempts)
- Non-blocking enqueue from STOP handler

### 4. Coroutine-Based Simulation
- Replaced Thread + AtomicBoolean with Job + isActive
- Immediate cancellation via `job.cancel()`
- No extra ticks after STOP
- Clean structured concurrency

### 5. Settlement Endpoint
- `POST /api/emulator/{id}/settle?method=CARD`
- Clears pending transaction
- Resets to IDLE
- Returns settled transaction details

## Testing Flow

### Setup
1. Start PostgreSQL: `docker-compose -f docker-compose-local.yaml up -d`
2. Start API: `cd lpg-ehl-api && mvn spring-boot:run`
3. Start Emulator: `cd lpg-ehl-emulator && mvn spring-boot:run`
4. Start Windows Dispenserkontroll

### Test Scenario 1: Happy Path
1. **UNBLOCK** from Windows → Emulator starts PUMPING
2. Wait 5-10 seconds
3. **STOP** from Windows → State becomes PAYMENT_PENDING
4. Check logs: Should see "🧊 Transaction frozen" and "🔒 STATE: PAYMENT_PENDING"
5. **UNBLOCK again** → Should be REJECTED with warning
6. **Settle**: `curl -X POST "http://localhost:8090/api/emulator/settle/1?method=CARD"`
7. Check logs: Should see "💳 SETTLEMENT: CARD" and "✅ Dispenser reset to IDLE"
8. **UNBLOCK again** → Should work, new fueling starts from 0

### Test Scenario 2: Verify No Extra Tick
1. **UNBLOCK** from Windows
2. Watch emulator logs for "⛽ Update #N" messages
3. **STOP** immediately after seeing an update
4. Verify: No additional "⛽ Update" after STOP
5. Verify: "🏁 FUEL SIMULATION STOPPED" appears immediately

### Test Scenario 3: Async DB Save with Retry
1. **Stop API** to simulate network failure
2. **UNBLOCK** → **STOP** to create transaction
3. Check logs: Should see "📥 Enqueueing transaction" and retry attempts
4. **Start API** again
5. Watch logs: Transaction should save successfully after API comes back

### Expected Log Output

#### On STOP:
```
🏁 DELIVERY FINISHED: 5.0 L delivered for 79.50 kr
🧊 Transaction frozen: 5.0 L @ 15.9 NOK/L = 79.5 NOK (abc123-uuid)
🔒 STATE: PAYMENT_PENDING - Awaiting settlement
📥 Enqueueing transaction abc123-uuid for async save
```

#### On UNBLOCK (when pending):
```
⚠️ UNBLOCK REJECTED: Payment pending (79.5 NOK)
💳 Please settle transaction via /api/emulator/1/settle
```

#### On Settle:
```
┌────────────────────────────────────────────────────────────
│ 💳 SETTLEMENT: CARD
│ Transaction: abc123-uuid
│ Volume: 5.0 L
│ Amount: 79.5 NOK
│ Unit Price: 15.9 NOK/L
└────────────────────────────────────────────────────────────
✅ Dispenser reset to IDLE - ready for next customer
```

## Verification Checklist

- [ ] STOP freezes transaction with UUID
- [ ] UNBLOCK rejected when payment pending
- [ ] Settle endpoint clears pending transaction
- [ ] New UNBLOCK works after settle
- [ ] No extra tick after STOP
- [ ] Transaction saves to database asynchronously
- [ ] Retry works if API is down

## API Endpoints

### Settle Transaction
```bash
curl -X POST "http://localhost:8090/api/emulator/settle/1?method=CARD"
```

Response (success):
```json
{
  "status": "settled",
  "method": "CARD",
  "transaction": {
    "dispenserId": 1,
    "liters": 5.0,
    "amountNok": 79.5,
    "unitPrice": 15.9,
    "finishedAt": "2025-12-22T15:45:30Z",
    "idempotencyKey": "abc123-uuid"
  }
}
```

Response (no pending):
```json
{
  "status": "no_pending_transaction",
  "message": "No pending transaction to settle"
}
```

## Architecture Changes

### Files Modified
- `EhlDispenserEmulator.kt`: Added PAYMENT_PENDING state, freeze/settle methods
- `EmulatorService.kt`: Refactored to coroutines, integrated TransactionSink
- `EmulatorController.kt`: Added settle endpoint

### Files Created
- `TransactionSink.kt`: Async persistence with retry logic

### Key Classes
- `CompletedTransaction`: Immutable transaction snapshot with UUID
- `TransactionSink`: Channel-based queue with coroutine consumer
- `DispenserState.PAYMENT_PENDING`: New state for payment lock

## Next Steps (Future Work)

1. **API Idempotency**: Add unique constraint on `idempotencyKey` in database
2. **Nets Integration**: Implement card capture in settle endpoint
3. **B2B Credit**: Implement credit settlement variant
4. **Monitoring**: Add metrics for retry counts and queue depth
