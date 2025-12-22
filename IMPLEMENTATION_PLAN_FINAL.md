# Implementasjonsplan: Payment Reset & Windows Dispenserkontroll Integrasjon

**Versjon:** 2.0 (KONSOLIDERT)  
**Dato:** 22. desember 2025  
**Branch:** `feature/payment-pending-hardening`  
**Prosjekt:** LPG-EHL Emulator & Windows Dispenserkontroll

**Kilder:**
- ✅ Gemini analyse (komplett implementasjonsforslag)
- ✅ ChatGPT analyse (verifisering + edge cases)
- ✅ Egen kodebase-analyse (eksisterende arkitektur verifisert)

---

## 🔍 Kritisk Analyse: Gemini vs ChatGPT vs Faktisk Kode

### Viktige Funn

**1. ✅ ENDPOINT EKSISTERER ALLEREDE**
- `/api/emulator/settle/{id}` finnes i `EmulatorController.kt` (linje 70-96)
- Kaller `emulatorService.settle()` som kaller `emulator.settleAndReset()`
- **Konklusjon:** Vi trenger IKKE nytt endpoint - kun utvide eksisterende med broadcast

**2. ❌ ChatGPT hadde feil signatur på formatStateData()**
```kotlin
// ChatGPT foreslo:
private fun formatStateData(stateCode: Int)  // ❌ FEIL

// Faktisk:
private fun formatStateData(data: ByteArray)  // ✅ RIKTIG
```

**3. ✅ Begge foreslo broadcast-mekanisme**
- Gemini: Mer detaljert med feilhåndtering og logging
- ChatGPT: Enklere, men mangler edge cases
- **Anbefaling:** Bruk Gemini sin tilnærming (mer robust)

**4. ✅ Navnekonvensjoner avklart**
| AI | Forslag | Vurdering |
|---|---|---|
| Gemini | `resetAfterPayment()` | Beskrivende |
| ChatGPT | `settlePaymentAndReset()` | Langt navn |
| **Eksisterende** | `settleAndReset()` | ✅ **Brukes allerede** |
| **Ny (vår)** | `settleAndBroadcast()` | ✅ **Konsistent navnevalg** |

**5. ⚠️ ChatGPT advarte om reset()-metoden**
ChatGPT sa: "Sørg for at reset() faktisk nuller alle tellere"

**Verifisert i koden (EhlDispenserEmulator.kt linje 146-171):**
```kotlin
fun settleAndReset(method: String = "CARD"): CompletedTransaction? {
    pendingTransaction = null
    volumeLitres = 0.0      // ✅ OK
    amountCents = 0         // ✅ OK
    state = DispenserState.IDLE  // ✅ OK
    nozzleLifted = false    // ✅ OK
    productSelected = false // ✅ OK
}
```
**Konklusjon:** ✅ Eksisterende kode er korrekt!

---

## 🎯 Hovedmål

**Windows Dispenserkontroll skal automatisk nullstilles når betaling er godkjent via lpg-web.**

### Nåværende Problem
1. Fylling → Stopp → Emulator går til `PAYMENT_PENDING` (state 8)
2. lpg-web: Simuler betaling → Kaller `/api/emulator/settle/1`
3. Emulator: Intern state resettes til `IDLE`
4. **❌ PROBLEM:** Windows viser fortsatt gammelt beløp/volum

### Ønsket Løsning
1. `/api/emulator/settle/{id}` kaller `settleAndBroadcast()`
2. Emulator sender `<TANK>;0;0.00;0.00;...` til Windows
3. Emulator sender `<STATE_TANK>;00000000` til Windows
4. Windows UI nullstilles og viser 0.00 / 0.00
5. ✅ Klar for ny fylling

---

## 🔧 FASE 1: Fix State 8 Logging (15 min)

### Problem
Logger viser "State=8 (UNKNOWN)" i stedet for "PAYMENT_PENDING"

### Løsning

**Fil:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlPacketFormatter.kt`

**Endre linje 56-64:**
```kotlin
private fun formatStateData(data: ByteArray): String {
    if (data.isEmpty()) return "Query"
    
    val stateCode = data[0].toInt() and 0xFF
    val stateName = when (stateCode) {
        0 -> "IDLE (Ready for new transaction)"
        1 -> "READY (Authorized, waiting for nozzle)"
        2 -> "DELIVERING (Fuel flowing)"
        3 -> "FINISHED (Transaction complete)"
        8 -> "PAYMENT_PENDING (Awaiting settlement)"  // <-- NY LINJE
        9 -> "ERROR (Dispenser error)"
        else -> "UNKNOWN"
    }
    return "State=$stateCode ($stateName)"
}
```

**Validering:**
- ✅ Gemini: Korrekt
- ✅ ChatGPT: Samme intensjon, men feil signatur
- ✅ Min kode: Korrekt med ByteArray

**Status:** ✅ **IMPLEMENTERT** (22. des 2025 kl. 18:10)
- Fil: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlPacketFormatter.kt`
- Endring: Linje 61 - Lagt til case 8 → "PAYMENT_PENDING (Awaiting settlement)"
- Kompilering: ✅ OK
- Tester: ✅ Alle 29 tester passerte

---

## 🔧 FASE 2: Broadcast til Windows (2 timer) ⭐ KRITISK

### Arkitektur-oversikt

```
┌─────────────┐
│  lpg-web    │ POST /api/emulator/settle/1
└──────┬──────┘
       │
       v
┌──────────────────────────────────────────┐
│  EmulatorController.settle()             │
│  → emulatorService.settleAndBroadcast()  │ <-- NY METODE
└──────┬───────────────────────────────────┘
       │
       ├─> emulator.settleAndReset()         (eksisterer)
       │   └─> state=IDLE, volumeLitres=0
       │
       └─> broadcastLegacy("<TANK>;...")     (NY)
           broadcastLegacy("<STATE_TANK>;...") (NY)
                │
                v
        ┌───────────────────┐
        │  ClientHandler    │
        │  .sendLegacy()    │ <-- NY METODE
        └───────┬───────────┘
                │
                v
        ┌───────────────────┐
        │ Windows           │
        │ Dispenserkontroll │ (mottar og nullstiller UI)
        └───────────────────┘
```

---

### STEG 2.1: Legg til sendLegacy() i ClientHandler

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**Plassering:** I `ClientHandler` inner class (etter linje 111, etter `private val output = ...`)

```kotlin
/**
 * Send a legacy text message to this client.
 * Thread-safe for concurrent broadcasts.
 * 
 * @param message Text message WITHOUT newline (will be added automatically)
 */
@Synchronized
fun sendLegacy(message: String) {
    try {
        synchronized(output) {
            output.write((message + "\n").toByteArray(Charsets.UTF_8))
            output.flush()
        }
        logger.debug("📤 Sent legacy to $clientId: $message")
    } catch (e: Exception) {
        logger.error("❌ Failed to send legacy to $clientId", e)
        throw e  // Re-throw so broadcast kan logge feil
    }
}
```

**ChatGPT kommentar:** Verifisert at `output` eksisterer som felt (linje 111) ✅

**Status:** ⏳ Må implementeres

---

### STEG 2.2: Legg til broadcastLegacy() i EmulatorService

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**Plassering:** Etter `acceptConnections()` metoden (etter linje 87)

```kotlin
/**
 * Broadcast a legacy text message to all connected Windows clients.
 * Used to notify Windows Dispenserkontroll of state changes (e.g., reset after payment).
 * 
 * This method is fail-safe: if one client fails, others still receive the message.
 * 
 * @param message Text message (without newline)
 */
private fun broadcastLegacy(message: String) {
    if (clientHandlers.isEmpty()) {
        logger.debug("📭 No clients connected - skipping broadcast")
        return
    }
    
    logger.info("📢 Broadcasting to ${clientHandlers.size} client(s): $message")
    
    var successCount = 0
    var failCount = 0
    
    clientHandlers.values.forEach { client ->
        runCatching { 
            client.sendLegacy(message)
            successCount++
        }.onFailure { e -> 
            logger.warn("⚠️ Failed to broadcast to ${client.clientId}: ${e.message}")
            failCount++
        }
    }
    
    logger.info("✅ Broadcast complete: $successCount OK, $failCount failed")
}
```

**Gemini vs ChatGPT:**
- Gemini: Hadde feilhåndtering ✅
- ChatGPT: Enklere versjon
- **Valg:** Gemini sin versjon med forbedret logging

**ChatGPT kommentar:** Bruk `clientHandlers` (ikke `activeClients`) ✅

**Status:** ⏳ Må implementeres

---

### STEG 2.3: Legg til settleAndBroadcast() metode

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**Plassering:** Før `getStatus()` metoden (før linje 365)

```kotlin
/**
 * Settle pending transaction and broadcast reset to all Windows clients.
 * 
 * This is the CRITICAL method that solves the "Windows shows old values" problem.
 * 
 * Flow:
 * 1. Settle transaction internally (emulator.settleAndReset())
 * 2. Broadcast <TANK> with 0.00 / 0.00 to Windows
 * 3. Broadcast <STATE_TANK> with idle state to Windows
 * 
 * @param method Payment method ("CARD" or "CREDIT")
 * @return Settled transaction, or null if no pending transaction
 */
fun settleAndBroadcast(method: String = "CARD"): no.cloudberries.lpg.emulator.service.CompletedTransaction? {
    logger.info("┌────────────────────────────────────────────────────────────")
    logger.info("│ 💳 SETTLE AND BROADCAST")
    logger.info("│ Method: $method")
    logger.info("└────────────────────────────────────────────────────────────")
    
    // 1. Settle internal state via emulator
    val settledTransaction = emulator.settleAndReset(method)
    
    if (settledTransaction == null) {
        logger.warn("⚠️ No transaction to settle - broadcast skipped")
        return null
    }
    
    logger.info("✅ Transaction settled: ${settledTransaction.liters} L @ ${settledTransaction.amountNok} NOK")
    
    // 2. Broadcast reset to all Windows clients
    logger.info("📢 Broadcasting reset to Windows clients...")
    
    val price = String.format(java.util.Locale.US, "%.2f", pricePerLitreCents / 100.0)
    
    // <TANK> format: <TANK>;<ignored>;<beløp>;<volum>;<pris>;<bank_status>;<bank_text>
    // Windows parser: parts[2]=amount, parts[3]=volume, parts[4]=price
    broadcastLegacy("<TANK>;0;0.00;0.00;$price;0;")
    
    // <STATE_TANK> format: 8-character string
    // Index 4 = '0' means idle (not released)
    broadcastLegacy("<STATE_TANK>;00000000")
    
    logger.info("✅ Broadcast complete - Windows should now show 0.00 / 0.00")
    logger.info("🟢 Dispenser ready for next customer")
    
    return settledTransaction
}
```

**Navnevalg-rasjonale:**
- Konsistent med eksisterende `settleAndReset()`
- Klar om hva metoden gjør: "settle AND broadcast"
- Ikke for langt navn (ref ChatGPT sin `settlePaymentAndReset`)

**ChatGPT anbefaling implementert:**
- ✅ Verifisert at `settleAndReset()` nuller alle tellere
- ✅ Logging av hva som skjer
- ✅ Håndtering av null-case

**Status:** ⏳ Må implementeres

---

### STEG 2.4: Oppdater EmulatorController

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorController.kt`

**Endre `settle()` endpoint (linje 70-96):**

```kotlin
/**
 * Settle pending transaction and reset dispenser to IDLE.
 * This endpoint now BROADCASTS reset to Windows Dispenserkontroll.
 * 
 * @param id Dispenser address (currently only 1 is supported)
 * @param method Payment method: "CARD" (default) or "CREDIT"
 * @return Settled transaction details or error message
 */
@PostMapping("/settle/{id}")
fun settle(
    @PathVariable id: Int,
    @RequestParam(defaultValue = "CARD") method: String
): ResponseEntity<Map<String, Any>> {
    // Validate payment method
    if (method !in listOf("CARD", "CREDIT")) {
        return ResponseEntity.badRequest().body(mapOf(
            "status" to "error",
            "message" to "Invalid payment method. Use CARD or CREDIT"
        ))
    }
    
    // Use new broadcast method instead of direct settle
    val settledTransaction = emulatorService.settleAndBroadcast(method)
    
    return if (settledTransaction != null) {
        ResponseEntity.ok(mapOf(
            "status" to "settled",
            "method" to method,
            "windowsBroadcastSent" to true,  // NEW: Indicate broadcast happened
            "transaction" to mapOf(
                "dispenserId" to settledTransaction.dispenserId,
                "liters" to settledTransaction.liters,
                "amountNok" to settledTransaction.amountNok,
                "unitPrice" to settledTransaction.unitPrice,
                "finishedAt" to settledTransaction.finishedAt.toString(),
                "idempotencyKey" to settledTransaction.idempotencyKey
            )
        ))
    } else {
        ResponseEntity.ok(mapOf(
            "status" to "no_pending_transaction",
            "message" to "No pending transaction to settle"
        ))
    }
}
```

**Endringer fra eksisterende:**
1. ✅ Validate payment method (ChatGPT anbefaling)
2. ✅ Kall `settleAndBroadcast()` i stedet for `settle()`
3. ✅ Legg til `windowsBroadcastSent: true` i response
4. ✅ Forbedret KDoc

**Status:** ⏳ Må implementeres

---

## 🔧 FASE 3: Fjern Kontant (30 min)

### Bakgrunn
Per Tobias (Norges Gass): Kun Terminal (kort/Vipps) og Kreditt skal støttes.

### 3.1 Backend: Søk etter PaymentMethod enum

**Aksjon:**
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
grep -r "PaymentMethod\|CASH\|KONTANT" --include="*.kt" lpg-ehl-api/
```

**Hvis PaymentMethod enum eksisterer:**
```kotlin
// FJERN CASH fra enum:
enum class PaymentMethod {
    CARD,      // Terminal (kort/Vipps)
    CREDIT     // Bedriftskreditt
    // CASH    <-- FJERN
}
```

**Hvis ikke:** ✅ Backend støtter ikke kontant allerede

**Status:** ⏳ Må verifiseres

---

### 3.2 Frontend: Fjern kontant fra lpg-web

**Problem:** lpg-web repo ikke funnet i `/Users/tandersen/git/NorgesGass/lpg-ehl/`

**Antatt lokasjon:**
- Separat repo?
- Mulig: `/Users/tandersen/git/lpg-web` eller lignende?

**Når du finner lpg-web:**

1. Søk etter "Kontant" / "Cash" / "CASH":
```bash
cd /path/to/lpg-web
grep -r "Kontant\|Cash\|CASH" src/
```

2. Fjern kontant-knapp fra UI:
```tsx
// FJERN dette:
<button onClick={handleCashPayment}>
  💵 Kontant
</button>

// BEHOLD kun:
<button onClick={handleCardPayment}>
  💳 Kortbetaling
</button>
<button onClick={handleCreditPayment}>
  🏢 Kreditt
</button>
```

**Status:** ⏳ Avventer lokasjon av lpg-web

---

## 🧪 FASE 4: Testing (2 timer)

### 4.1 Enhetstester (30 min)

**Ny fil:** `lpg-ehl-emulator/src/test/kotlin/no/cloudberries/lpg/emulator/EmulatorServiceBroadcastTest.kt`

```kotlin
package no.cloudberries.lpg.emulator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmulatorServiceBroadcastTest {
    
    @Test
    fun `settleAndBroadcast should send messages to all connected clients`() {
        // TODO: Mock ClientHandler og verifiser at sendLegacy() blir kalt
        // Given: 2 connected clients + pending transaction
        // When: settleAndBroadcast() is called
        // Then: Verify <TANK> and <STATE_TANK> sent to both clients
    }
    
    @Test
    fun `settleAndBroadcast should handle no clients gracefully`() {
        // Given: No clients connected + pending transaction
        // When: settleAndBroadcast() is called
        // Then: Should settle transaction but skip broadcast
    }
    
    @Test
    fun `settleAndBroadcast should handle no pending transaction`() {
        // Given: Client connected but no pending transaction
        // When: settleAndBroadcast() is called
        // Then: Should return null and not crash
    }
    
    @Test
    fun `broadcastLegacy should continue if one client fails`() {
        // Given: 3 clients, 1 with closed socket
        // When: broadcast is called
        // Then: 2 clients receive message, 1 logs error
    }
}
```

**Status:** ⏳ Må implementeres

---

### 4.2 Integrasjonstest med Windows (1.5 timer)

#### Setup
1. Start emulator:
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-emulator
mvn spring-boot:run
```

2. Start Windows Dispenserkontroll
3. Koble til `localhost:9000`

---

#### Test Scenario 1: Normal fylling + reset

| Steg | Aksjon | Forventet Resultat |
|------|--------|-------------------|
| 1 | Windows: Trykk "Frigi" | Windows viser "Frigitt" |
| 2 | Vent 5 sekunder | Beløp/volum øker (0.5 L/s) |
| 3 | Windows: Trykk "Stopp" | Emulator logger "PAYMENT_PENDING (8)" |
| 4 | Verifiser Windows UI | Viser **frosset** beløp/volum |
| 5 | lpg-web: Trykk "Kortbetaling" | Kaller `/api/emulator/settle/1` |
| 6 | Sjekk emulator-logger | "Broadcasting to 1 client(s)" |
| 7 | Sjekk Windows UI | **Viser 0.00 kr / 0.00 L** ✅ |
| 8 | Windows: Trykk "Frigi" igjen | Ny fylling starter fra 0 ✅ |

**Success Criteria:**
- ✅ Windows nullstilles etter betaling
- ✅ Logger viser "PAYMENT_PENDING" (ikke UNKNOWN)
- ✅ Ny fylling starter clean

---

#### Test Scenario 2: Flere fyllinger etter hverandre

Gjenta Scenario 1 **tre ganger** uten å restarte emulator.

**Forventet:**
- ✅ Ingen "ghost transactions"
- ✅ Hver fylling starter fra 0.00 / 0.00
- ✅ Windows viser riktig pris hele tiden

---

#### Test Scenario 3: Feilhåndtering

| Test Case | Forventet |
|-----------|-----------|
| Kall `/settle/1` uten pending transaction | HTTP 200, `status: "no_pending_transaction"` |
| Kall `/settle/1?method=INVALID` | HTTP 400, "Invalid payment method" |
| Disconnect Windows under broadcast | Logger viser "Failed to broadcast", andre klienter OK |

---

## 📊 FASE 5: Fremtidig Arbeid (Post-MVP)

### Prioritert Backlog

| Prioritet | Feature | Estimat | Q |
|-----------|---------|---------|---|
| 🔴 Høy | Nets Terminal-integrasjon | 2 uker | Q1 |
| 🔴 Høy | Bedriftskreditt per stasjon | 1 uke | Q1 |
| 🔴 Høy | Stasjonseier Admin-panel | 2 uker | Q1 |
| 🟡 Medium | Klippekort (8=1 gratis) | 1 uke | Q2 |
| 🟡 Medium | Privatkunde "Min Side" | 2 uker | Q2 |
| 🟢 Lav | Sjåfør/Logistikk | 3 uker | Q3 |

### Detaljer fra Gemini

#### Nets Terminal-integrasjon
```kotlin
// Erstatt simulering i settleAndBroadcast():
val terminalResult = netsService.capturePayment(
    amount = settledTransaction.amountNok,
    reference = settledTransaction.idempotencyKey
)

if (terminalResult.success) {
    broadcastLegacy("<TANK>;...")
} else {
    // Håndter feil - reverser transaction?
}
```

#### Bedriftskreditt per Stasjon
```kotlin
// Before UNBLOCK:
val creditOk = creditService.checkCredit(
    customerId = request.customerId,
    stationId = stationId,
    estimatedAmount = 500.0  // eller forhåndsvalgt beløp
)

if (!creditOk) {
    return "Kreditt ikke tilgjengelig"
}
```

#### Klippekort-logikk
```kotlin
// After transaction completed:
customerService.incrementStamp(customerId)

val stamps = customerService.getStamps(customerId)
if (stamps % 8 == 0) {
    // 8th stamp - denne var gratis
    refundService.createCreditNote(settledTransaction)
}
```

---

## 📁 Oppsummering: Filer som Endres

```
lpg-ehl-core/
└── src/main/kotlin/no/cloudberries/lpg/protocol/
    └── EhlPacketFormatter.kt
        └── formatStateData() - Legg til case 8

lpg-ehl-emulator/
└── src/main/kotlin/no/cloudberries/lpg/emulator/
    ├── EmulatorService.kt
    │   ├── ClientHandler.sendLegacy()      [NY]
    │   ├── broadcastLegacy()               [NY]
    │   └── settleAndBroadcast()            [NY]
    │
    └── EmulatorController.kt
        └── settle() - Kall settleAndBroadcast()

lpg-ehl-emulator/
└── src/test/kotlin/no/cloudberries/lpg/emulator/
    └── EmulatorServiceBroadcastTest.kt     [NY FIL]

lpg-web/                                    [AVVENTER LOKASJON]
└── src/pages/EmulatorDebugPage.tsx
    └── Fjern kontant-knapp
```

**Totalt:** 3 eksisterende filer + 1 ny testfil

---

## 🚀 Implementasjonsrekkefølge

### Uke 52 (22-29. des 2025)

**Dag 1 (22. des - IDAG):**
- [x] Fase 1: Fix state 8 logging (15 min) ✅ **FULLFØRT kl. 18:10**
- [ ] Fase 2.1: sendLegacy() (30 min)
- [ ] Fase 2.2: broadcastLegacy() (30 min)

**Dag 2 (24. des - Julaften):**
- [ ] Pause / minimalt arbeid

**Dag 3 (26. des):**
- [ ] Fase 2.3: settleAndBroadcast() (1 time)
- [ ] Fase 2.4: Oppdater Controller (30 min)
- [ ] Commit + push

**Dag 4 (27. des):**
- [ ] Fase 3: Søk etter PaymentMethod (15 min)
- [ ] Fase 4.1: Skriv tester (1 time)

**Dag 5 (28. des):**
- [ ] Fase 4.2: Manuell testing med Windows (2 timer)
- [ ] Dokumenter resultater
- [ ] Merge til main (hvis OK)

**Total estimat:** ~6 timer effektiv tid

---

## 🔍 Åpne Spørsmål

### Kritiske (må avklares før start)
1. **lpg-web lokasjon?**
   - Separat repo eller i lpg-ehl?
   - Hvor ligger frontend-koden?

2. **Multi-dispenser?**
   - Skal broadcast gå til alle dispensers eller kun én?
   - Nåværende kode støtter kun address=1

### Ikke-kritiske (kan vente)
3. **Database migrering for CASH?**
   - Skal vi fjerne CASH fra eksisterende transaksjoner?
   - Eller kun blokkere nye?

4. **Nets Terminal?**
   - Start Q1 2025 eller vente?
   - Trenger vi test-terminal fra Nets?

---

## 🎓 Referanser

**Dokumentasjon:**
- Emulator: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/WARP.md`
- Protokoll: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/docs/EMULATOR.md`

**Legacy Protokoll (Windows format):**
```
<TANK>;[ignored];[amount];[volume];[price];[bank_status];[bank_text]
<STATE_TANK>;[8 chars where index 4 = release status]
```

**EHL Binary Protokoll (State codes):**
- 0x00 = IDLE
- 0x01 = READY (authorized)
- 0x02 = DELIVERING
- 0x03 = FINISHED
- **0x08 = PAYMENT_PENDING** ← Dette fikser vi
- 0x09 = ERROR

---

## 📞 Kontakt

**Teknisk Lead:** Terje Andersen  
**Stakeholder:** Tobias (Norges Gass)  
**Git Branch:** `feature/payment-pending-hardening`

---

**Sist oppdatert:** 22. desember 2025 kl. 18:00  
**Versjon:** 2.0 (KONSOLIDERT - Gemini + ChatGPT + Kodebase-analyse)  
**Status:** 🟢 Klar for implementasjon
