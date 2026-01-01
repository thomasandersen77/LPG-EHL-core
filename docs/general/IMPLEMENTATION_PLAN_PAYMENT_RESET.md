# Implementasjonsplan: Payment Reset & Windows Dispenserkontroll Integrasjon

> ⚠️ **DENNE VERSJONEN ER UTDATERT**  
> Se den konsoliderte versjonen: **[IMPLEMENTATION_PLAN_FINAL.md](./IMPLEMENTATION_PLAN_FINAL.md)**

**Dato:** 22. desember 2025  
**Branch:** `feature/payment-pending-hardening`  
**Prosjekt:** LPG-EHL Emulator & Windows Dispenserkontroll

---

## 📋 Sammendrag (Versjon 1.0 - Kun Gemini analyse)

Dette dokumentet beskriver implementasjonen av følgende kritiske features:

1. **PAYMENT_PENDING State (0x08)**: Fikse logging så state 8 vises riktig
2. **Windows-klient Nullstilling**: Broadcast reset-kommando til Windows Dispenserkontroll etter betaling
3. **Fjerne Kontant Betalingsmetode**: Kun kortbetaling (Terminal/Vipps) og kreditt skal støttes
4. **Settlement API**: Verifisere og optimalisere eksisterende `/api/emulator/settle/{id}` endpoint

---

## 🎯 Hovedmål

**Windows-klienten skal automatisk nullstilles når betaling er godkjent via lpg-web.**

### Nåværende Problem
1. Etter fylling og stopp: Emulator går til `PAYMENT_PENDING` (state 8)
2. Betaling simuleres via lpg-web
3. Emulator resettes til `IDLE` internt
4. **PROBLEM**: Windows Dispenserkontroll viser fortsatt gammelt beløp/volum og er ikke klar for ny fylling

### Ønsket Løsning
1. Settlement API kaller `settleAndReset()`
2. Emulator broadcaster `<TANK>` og `<STATE_TANK>` med nullstilte verdier
3. Windows-klient mottar melding og nullstiller UI
4. Pumpe er klar for ny kunde

---

## 🔧 Fase 1: Fix State 8 Logging (Kritisk - må gjøres først)

### Problem
`EhlPacketFormatter.formatStateData()` logger state 8 som "UNKNOWN" i stedet for "PAYMENT_PENDING".

### Løsning

**Fil:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlPacketFormatter.kt`

**Linje 52-65 (formatStateData):**

```kotlin
private fun formatStateData(data: ByteArray): String {
    if (data.isEmpty()) return "Query"
    
    val stateCode = data[0].toInt() and 0xFF
    val stateName = when (stateCode) {
        0 -> "IDLE (Ready for new transaction)"
        1 -> "READY (Authorized, waiting for nozzle)"
        2 -> "DELIVERING (Fuel flowing)"
        3 -> "FINISHED (Transaction complete)"
        8 -> "PAYMENT_PENDING (Awaiting settlement)"  // <-- LEGG TIL DENNE
        9 -> "ERROR (Dispenser error)"
        else -> "UNKNOWN"
    }
    return "State=$stateCode ($stateName)"
}
```

**Status:** ⏳ Må implementeres

---

## 🔧 Fase 2: Broadcast Reset til Windows-klient (Kritisk)

### Arkitektur
Windows Dispenserkontroll kommuniserer med emulator via legacy text-protokoll (`<TANK>`, `<STATE_TANK>` etc.).

**Nåværende flyt:**
- EmulatorService → ClientHandler → Socket OutputStream (kun inbound commands)
- Mangler: Broadcast fra emulator til alle klienter

### Implementasjon

#### 2.1 Legg til `sendLegacy()` i ClientHandler

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**I `ClientHandler` inner class (linje ~106), legg til:**

```kotlin
/**
 * Send a legacy text command to this client.
 * Thread-safe for concurrent broadcasts.
 */
@Synchronized
fun sendLegacy(message: String) {
    try {
        synchronized(output) {
            output.write((message + "\n").toByteArray(Charsets.UTF_8))
            output.flush()
        }
        logger.debug("📤 Sent legacy message to $clientId: $message")
    } catch (e: Exception) {
        logger.error("❌ Failed to send legacy message to $clientId", e)
    }
}
```

**Status:** ⏳ Må implementeres

---

#### 2.2 Legg til `broadcastLegacy()` i EmulatorService

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**Rett etter `acceptConnections()` metoden (linje ~88), legg til:**

```kotlin
/**
 * Broadcast a legacy text message to all connected clients.
 * Used to notify Windows Dispenserkontroll of state changes.
 */
private fun broadcastLegacy(message: String) {
    if (clientHandlers.isEmpty()) {
        logger.debug("📭 No clients connected - skipping broadcast")
        return
    }
    
    logger.info("📢 Broadcasting to ${clientHandlers.size} client(s): $message")
    clientHandlers.values.forEach { client ->
        runCatching { client.sendLegacy(message) }
            .onFailure { e -> 
                logger.warn("⚠️ Failed to broadcast to ${client.clientId}", e) 
            }
    }
}
```

**Status:** ⏳ Må implementeres

---

#### 2.3 Opprett `settlePaymentAndBroadcast()` metode

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`

**Legg til ny public metode (før `getStatus()` på linje ~365):**

```kotlin
/**
 * Settle pending transaction and broadcast reset to all Windows clients.
 * This ensures Windows Dispenserkontroll UI is nullified and ready for next customer.
 * 
 * @param method Payment method ("CARD" or "CREDIT")
 * @return Settled transaction or null if no pending transaction
 */
fun settleAndBroadcast(method: String = "CARD"): no.cloudberries.lpg.emulator.service.CompletedTransaction? {
    // 1. Settle internal state via emulator
    val settledTransaction = emulator.settleAndReset(method)
    
    if (settledTransaction == null) {
        logger.warn("⚠️ No transaction to settle")
        return null
    }
    
    // 2. Broadcast reset to Windows clients
    logger.info("┌────────────────────────────────────────────────────────────")
    logger.info("│ 📢 BROADCASTING RESET TO WINDOWS CLIENTS")
    logger.info("└────────────────────────────────────────────────────────────")
    
    val price = String.format(java.util.Locale.US, "%.2f", pricePerLitreCents / 100.0)
    
    // <TANK> format: parts[2]=beløp, parts[3]=volum(liter), parts[4]=pris
    broadcastLegacy("<TANK>;0;0.00;0.00;$price;0;")
    
    // <STATE_TANK> format: 8 chars, index 4 = '0' means not released (idle)
    broadcastLegacy("<STATE_TANK>;00000000")
    
    logger.info("✅ Broadcast complete - Windows clients should now show 0.00 / 0.00")
    
    return settledTransaction
}
```

**Status:** ⏳ Må implementeres

---

#### 2.4 Oppdater EmulatorController til å bruke ny metode

**Fil:** `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorController.kt`

**Endre `settle()` endpoint (linje 70-96):**

```kotlin
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
            "broadcastSent" to true,  // <-- NEW FIELD
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

**Status:** ⏳ Må implementeres

---

## 🔧 Fase 3: Fjerne Kontant Betalingsmetode

### Bakgrunn
Per Tobias sine krav: Kun Terminal (kort/Vipps) og Kreditt skal støttes. Kontant skal fjernes.

### 3.1 Backend: Fjern CASH fra DTOs og Enums

**Søk gjennom prosjektet etter:**
- `PaymentMethod` enum
- `PaymentType` enum
- `CASH` / `KONTANT` konstanter

**Filer som trolig må endres:**
- `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/model/PaymentMethod.kt` (hvis eksisterer)
- Database migrasjoner (Liquibase/Flyway) - kan vente til senere

**Aksjon:**
```kotlin
// FJERN CASH fra enum:
enum class PaymentMethod {
    CARD,      // Terminal (kort/Vipps)
    CREDIT     // Bedriftskreditt
    // CASH    <-- FJERN DENNE
}
```

**Status:** ⏳ Må verifiseres (sjekk om PaymentMethod enum eksisterer)

---

### 3.2 Frontend: Fjern Kontant fra UI

**Prosjekt:** `lpg-web` (React/TypeScript)

**Søk etter:**
- Dropdown/select med betalingsmetoder
- "Kontant" / "Cash" buttons/options
- Payment type selectors

**Typiske filer:**
- `src/pages/EmulatorDebugPage.tsx`
- `src/components/PaymentSelector.tsx` (hvis eksisterer)
- Generated OpenAPI types: `src/api/types.ts` (regenereres etter backend-endring)

**Aksjon:**
```tsx
// FJERN kontant-knapp:
{status === 'PAYMENT_PENDING' && (
  <div className="payment-buttons">
    {/* KUN kortbetaling - kontant fjernet */}
    <button onClick={handleCardPayment} className="btn-card">
      💳 Kortbetaling (Terminal)
    </button>
    <button onClick={handleCreditPayment} className="btn-credit">
      🏢 Bedriftskreditt
    </button>
  </div>
)}
```

**Status:** ⏳ Må verifiseres (avhenger av lpg-web struktur)

---

## 🧪 Fase 4: Testing & Verifisering

### 4.1 Enhetstester

**Fil:** `lpg-ehl-emulator/src/test/kotlin/no/cloudberries/lpg/emulator/EmulatorServiceTest.kt`

**Test cases:**
```kotlin
@Test
fun `settleAndBroadcast should send reset messages to all clients`() {
    // Given: Client connected and transaction pending
    // When: settleAndBroadcast() is called
    // Then: Verify <TANK> and <STATE_TANK> messages were sent
}

@Test
fun `settleAndBroadcast should handle no pending transaction gracefully`() {
    // Given: No pending transaction
    // When: settleAndBroadcast() is called
    // Then: Should return null and log warning
}
```

**Status:** ⏳ Må implementeres

---

### 4.2 Integrasjonstest (Manuelt med Windows-klient)

**Test-scenarion:**

1. **Setup:**
   - Start emulator: `mvn spring-boot:run` (lpg-ehl-emulator)
   - Start Windows Dispenserkontroll
   - Koble til emulator (localhost:9000)

2. **Scenario 1: Normal fylling med reset**
   - Windows: Trykk "Frigi" (UNBLOCK)
   - Vent til fylling starter (volum/beløp øker)
   - Windows: Trykk "Stopp" (STOP)
   - Emulator: State skal være PAYMENT_PENDING (8)
   - lpg-web: Trykk "Simuler Kortbetaling"
   - **Forventet:** Windows viser 0.00 kr / 0.00 L og er klar for ny fylling

3. **Scenario 2: Flere fylling etter hverandre**
   - Gjenta Scenario 1 tre ganger
   - Verifiser at Windows aldri "henger" på gammelt beløp

4. **Scenario 3: Ingen pending transaction**
   - Kall `/api/emulator/settle/1` uten pending transaction
   - **Forventet:** HTTP 200 med `status: "no_pending_transaction"`

**Success Criteria:**
- ✅ Windows viser 0.00 / 0.00 etter settlement
- ✅ Ny fylling starter fra 0
- ✅ Ingen "ghost transactions" i Windows UI
- ✅ Logger viser "PAYMENT_PENDING" i stedet for "UNKNOWN"

**Status:** ⏳ Venter på implementasjon

---

## 📊 Fase 5: Fremtidig Arbeid (Post-MVP)

### Prioritert Backlog (Basert på Gemini & ChatGPT analyse)

#### Høy Prioritet (Q1 2025)
1. **Nets Terminal-integrasjon** (Ekte kortbetaling)
   - Erstatt simulering med faktisk ECR-protokoll
   - Integrer med Nets terminal API
   - Håndter timeout, avbrudd, feil

2. **Bedriftskreditt per Stasjon**
   - Database: Stasjon-spesifikke kredittavtaler
   - Logikk: Sjekk kreditt før fylling starter
   - API: `/api/credit/check/{customerId}/{stationId}`

3. **Stasjonseier Admin-panel**
   - Prisjustering per pumpe
   - Daglig/ukentlig rapport
   - Klippekort-administrasjon

#### Medium Prioritet (Q2 2025)
4. **Klippekort-logikk** (8 fyllinger = 1 gratis)
   - Database: Teller på `Customer` entitet
   - Logikk: Auto-inkrement ved hver fylling
   - UI: Vis gjenstående "stempler" til kunde

5. **Privatkunde "Min Side"**
   - Kvitteringer (PDF/epost)
   - Fyllehistorikk
   - Stasjonskart (Google Maps API)

#### Lav Prioritet (Q3-Q4 2025)
6. **Sjåfør/Logistikk-modul**
   - Leveringsruter
   - Tankbil-tracking
   - Inventory management

---

## 📁 Filstruktur & Endrede Filer

### Filer som skal endres:

```
lpg-ehl-core/
└── src/main/kotlin/no/cloudberries/lpg/protocol/
    └── EhlPacketFormatter.kt                    [ENDRE: formatStateData()]

lpg-ehl-emulator/
└── src/main/kotlin/no/cloudberries/lpg/emulator/
    ├── EmulatorService.kt                       [ENDRE: Legg til sendLegacy(), broadcastLegacy(), settleAndBroadcast()]
    └── EmulatorController.kt                    [ENDRE: Oppdater settle() til å bruke settleAndBroadcast()]

lpg-ehl-emulator/
└── src/test/kotlin/no/cloudberries/lpg/emulator/
    └── EmulatorServiceTest.kt                   [NY: Test broadcast-logikk]

lpg-web/                                         [PROSJEKT IKKE I GIT - MÅ VERIFISERES]
└── src/
    ├── pages/EmulatorDebugPage.tsx              [ENDRE: Fjern kontant-knapp]
    └── api/types.ts                             [AUTO-GENERERT: Regenerer etter backend-endring]
```

---

## 🚀 Implementasjonsrekkefølge

**Uke 52 (22-29. des 2025):**
1. ✅ Fase 1: Fix state 8 logging (15 min)
2. ⏳ Fase 2.1-2.2: Legg til broadcast-infrastruktur (1 time)
3. ⏳ Fase 2.3-2.4: Implementer settleAndBroadcast (1 time)
4. ⏳ Fase 3: Fjern kontant (30 min)
5. ⏳ Fase 4: Testing med Windows-klient (2 timer)

**Total estimert tid:** ~5 timer

---

## 🔍 Open Questions / Decisions Needed

1. **lpg-web prosjekt:**
   - Hvor ligger lpg-web? (Ikke funnet i `/Users/tandersen/git/NorgesGass/lpg-ehl/`)
   - Separat repo?
   - React/Next.js/Vite?

2. **Database migrering:**
   - Skal vi fjerne CASH fra eksisterende transaksjoner?
   - Eller kun blokkere nye kontant-transaksjoner?

3. **Multi-dispenser support:**
   - Nåværende kode støtter kun 1 dispenser (hardkodet address=1)
   - Skal broadcast gå til alle dispensers eller filtreres per address?

4. **Nets Terminal:**
   - Skal vi starte Nets-integrasjon i Q1 eller vente?
   - Trenger vi test-terminal fra Nets?

---

## 📞 Kontaktpunkter

**Teknisk Lead:** Terje Andersen  
**Stakeholder:** Tobias (Norges Gass)  
**Prosjekt:** LPG-EHL Core + Emulator  
**Branch:** `feature/payment-pending-hardening`

---

## 🎓 Referanser

**Emulator Dokumentasjon:**
- `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/WARP.md`
- `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/docs/EMULATOR.md`

**Legacy Protokoll:**
- Windows Dispenserkontroll forventer text-format: `<TANK>;...` og `<STATE_TANK>;...`
- Se `EmulatorService.handleLegacyCommand()` for eksempler

**EHL Binary Protokoll:**
- State codes: 0=IDLE, 1=READY, 2=DELIVERING, 3=FINISHED, 8=PAYMENT_PENDING, 9=ERROR
- Se `EhlDispenserEmulator.buildStatusByte()` for bit-flag mapping

---

**Sist oppdatert:** 22. desember 2025  
**Versjon:** 1.0  
**Status:** 🟡 Planlagt - Venter på implementasjon
