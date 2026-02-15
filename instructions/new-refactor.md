# CONTEXT-AWARE IMPLEMENTATION TASK (READ ENTIRE REPO FIRST)

IMPORTANT:
Before writing any code, scan the entire repository and understand:

- lpg-ehl-service
- lpg-ehl-payment-terminal-sim
- lpg-ehl-payment-terminal-gui
- lpg-ehl-emulator (PLS simulator)
- lpg-ehl-core
- WebSocket infrastructure
- Existing pump release logic
- Existing manual release controller
- Existing transaction persistence logic

Do NOT assume structure.
Use actual classes from the repo.

Do NOT duplicate logic that already exists.

---

# GOAL

Implement full event-based pump release flow in addition to existing manual release.

The system must support two release mechanisms:

1. Manual release (operator via GUI) – already works
2. Automatic release triggered by CARD_RESERVED event from payment terminal simulator

Both mechanisms must use the SAME service-level release method.

No duplicated release logic.

---

# CRITICAL ARCHITECTURE RULES

1. Pump address must NOT exist in:
    - REST paths
    - Controllers
    - Frontend API calls

Pump address must ONLY exist in Spring external configuration.

If AddressResolver exists:
- Simplify it.
- It should only read config.
- No logic duplication.

2. Controllers must be thin.
3. Business logic stays in service module.
4. No REST calls between modules internally.
5. Single source of truth for fueling state.

---

# PART 1 – PAYMENT TERMINAL SIMULATOR CHANGES

Locate simulator module.

When user presses:

- “Trekke kort”
- or similar card simulation button

It must:

1. Reserve fixed amount (1500 kr configurable)
2. Generate event:

{
eventType: "CARD_RESERVED",
reservationAmount: 1500,
currency: "NOK",
operationId: UUID,
timestamp: ISO-8601
}

3. Publish this event to:

- SSE endpoint
- WebSocket endpoint `/v1/events/ws`

Important:

- Use ONE internal event stream
- SSE and WebSocket must subscribe to same source
- Do NOT duplicate event creation

If event infrastructure exists, extend it.
Do not rewrite it.

---

# PART 2 – SERVICE MODULE: EVENT CONSUMER

In lpg-ehl-service:

Create or extend a component:

PaymentTerminalEventConsumer

It must:

- Start on application boot
- Read config:

payment.events.mode = websocket | sse

If websocket:
- Connect to simulator `/v1/events/ws`
- Auto-reconnect with backoff

If sse:
- Use WebClient stream

---

# EVENT HANDLING LOGIC

When eventType == CARD_RESERVED:

Call existing service-level method:

dispenserService.releasePump()

NOT controller.
NOT REST.
NOT duplicate logic.

If release method doesn’t exist:
Extract existing manual release logic into service method and reuse it.

---

# PART 3 – RELEASE FLOW

releasePump() must:

1. Signal PLS emulator that pump is ready
2. Transition internal state to READY
3. Broadcast WebSocket update to all UI
4. Be idempotent (safe if called twice)

---

# PART 4 – FUELING SYNCHRONIZATION

There must be ONE state owner for:

- liters
- amount
- pricePerLiter
- fueling status

Frontend must never calculate price.

When fueling starts:

Backend must:

- Simulate volume increments
- Calculate amount = liters * price
- Broadcast:

{
eventType: "FUELING_UPDATE",
liters,
amount,
pricePerLiter
}

All UI must render backend values only.

Verify:

- Control panel
- Station owner GUI
- /fueling view
- PLS simulator GUI

All show identical numbers.

---

# PART 5 – PAYMENT AFTER FUELING

After fueling stops:

System transitions to PAYMENT_PENDING.

Two options:

1. Card
2. Credit

When payment confirmed:

- Persist transaction
- Generate receipt
- Broadcast PAYMENT_CONFIRMED
- Reset pump to IDLE

---

# PART 6 – DO NOT BREAK EXISTING MANUAL FLOW

Manual “Fri dispenser” must still work.

It must internally call the SAME releasePump() method.

---

# PART 7 – REMOVE PUMP ADDRESS FROM API

Search entire repo for:

- pumpId
- dispenserId
- pumpAddress in REST paths

Refactor so:

Controllers do NOT require pump address in path.

Instead:

- Read from configuration
- Inject via Spring configuration properties

Frontend API calls must not send pump address.

---

# PART 8 – VALIDATION CHECKLIST

After implementation verify:

[ ] Manual release works
[ ] Card event release works
[ ] No duplicate release logic
[ ] No pump address in REST API
[ ] All UI show identical liters and amount
[ ] No frontend price calculation
[ ] No REST self-calls inside backend
[ ] WebSocket reconnect works
[ ] Duplicate CARD_RESERVED does not double-release

---

# IMPORTANT

Do not refactor entire project.
Do not change modules unless necessary.
Respect current architecture.
Improve it, don’t replace it.

Now:
1. Show analysis of current flow.
2. Show exact changes required.
3. Then implement.