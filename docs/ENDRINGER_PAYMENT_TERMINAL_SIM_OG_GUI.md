# Endringer: Payment Terminal Simulator og GUI

**Dato:** 2026-02-14  
**Utgangspunkt:** Rapport fra API-tester (`instructions/API_TEST_RESULTS.md`) og ønske om at simulatoren og GUI-en skal opptre som en ekte Nets/Ingenico-terminal.

---

## 1. Oversikt

Simulatoren (`lpg-ehl-payment-terminal-sim`) og GUI-modulen (`lpg-ehl-payment-terminal-gui`) er oppdatert slik at:

- API-responser og kvitteringstekster samsvarer med ekte terminal og testrapporten.
- GUI-en visuelt ligner en Ingenico-betalingsterminal (display, kontaktløs-symbol, tastatur, chip-spor).

Kun disse to modulene er endret; ingen endringer i service-modulen eller andre deler av repoet.

---

## 2. Endringer i Payment Terminal SIM

### 2.1 Terminal og status

| Endring | Før | Etter |
|--------|-----|-------|
| Melding ved åpning | `"Terminal opened and ready"` | `"Terminal opened"` |
| Sesjonsnummer | – | Økes ved hver `terminal/close` + `terminal/open` (f.eks. 020 → 021) |

### 2.2 Kjøp og timeout (ingen kort)

- **Scenario TIMEOUT:** Når scenario er `TIMEOUT`, venter simulatoren i `purchaseTimeoutSeconds` (standard 180 sekunder), deretter:
  - `success: false`
  - `errorCode: "operation_timeout"`
  - `lastDisplayText: "Kortet ikke presentert"`
  - `rejectionReason: "4:6"`
  - Kvitteringstekst med «Kortet ikke presentert», «Tidsavbrudd», «KJØP», «NOK X,XX»
- Hendelsen `OperationTimeout` publiseres; siste kvittering lagres for last-receipt.

### 2.3 Kvitteringer og rapporter

- **Kvitteringsformat (Bax):** `Bax: {terminalId}-{merchantId}`, dato `dd/MM/yyyy HH:mm`, «Overf.: 020», «KJØP», «NOK X,XX».
- **Timeout-kvittering:** «KOPI», «Kortet ikke presentert», «Ref.: ___», «Overf.: 020», «KJØP», «NOK X,XX», «Tidsavbrudd».
- **X-rapport:** Tekst som i testrapporten (Bax, Valuta NOK, Sesjon., X-rapport, Siste Z-Total, BankAxept, Antall, Total=).
- **Z-rapport:** Tilsvarende format; batch nullstilles etter Z-rapport; respons inkluderer `reportFields` med `reportType`, `zReportNumber`, `batchTotalCount`, `batchTotalAmount`, `scheme_BankAxept_count`, `scheme_BankAxept_amount`, `zLastTotalTimestampLocal`.
- **Avstemming:** Tekst som i testen: «Avstemming», «Innsamlet 0», «Total= 0,00», «Kortavtaler uten omsetning skrives ikke ut.»

### 2.4 Admin-operasjoner

| Operasjon | Endring |
|-----------|---------|
| **Cancel** | `lastDisplayText: "Avbrutt"` (som ekte terminal). |
| **Reversal** | Simulatoren har ingen transaksjon å reversere → `success: false`, `lastDisplayText: "Formatfeil"`, `rejectionReason: "4:6"`. |
| **Last-receipt** | Returnerer siste lagrede kvittering (fra kjøp/refund/timeout); ellers «ingen tidligere transaksjon». |
| **Admin/code** | Kode-mapping: 12592 → avstemming, 12594 → cancel, 12596 → reversal, 12598 → X-rapport, 12599 → Z-rapport, 12604 → last-receipt. |

### 2.5 Nye og endrede komponenter (SIM)

- **LastReceiptStore:** Lagrer siste utskrevne kvitteringstekst for last-receipt.
- **ReportState:** Holder sesjonsnummer (økes ved terminal open), X-/Z-rapportnummer, batch-totaler (nullstilles ved Z-rapport), `lastZTotalTimestamp`; metoder `resetAfterZReport()` og `advanceXReportNumber()`.
- **SimulatorConfig:** Ny parameter `purchaseTimeoutSeconds` (standard 180).
- **ReceiptGenerator:** Bruker `ReportState`; nye/oppdaterte metoder for timeout-kvittering, X-rapport, Z-rapport og avstemming i terminalformat.
- **OperationResponse:** Nye factory-metoder `timeout(...)` og `adminFormatError(...)`.
- **TerminalStateManager:** Kaller `reportState.incrementSession()` ved `open()`.
- **PaymentController:** TIMEOUT-scenario, lagring av siste kvittering, `reportState.addToBatch(amountMinor)` ved godkjent kjøp.
- **AdminController:** Cancel/reversal/last-receipt/code-dispatch og Z-rapport med `reportState.resetAfterZReport()` og `reportFields`.

---

## 3. Endringer i Payment Terminal GUI

- **Display:** Øverst, blå LCD-lignende område med hovedtekst, beløp og statusindikator.
- **Kontaktløs-symbol:** Tre buer og prikk under displayet (NFC-ikon).
- **Tastatur:** 4×3 numerisk grid (0–9, +/–) pluss tre funksjonstaster (oransje X, gul, grønn enter).
- **Chip-spor:** Horisontal stripe nederst med grønn «glow» (kortinnsats).
- **Merke:** «ingenico» nederst på enheten.
- **Vindustørrelse:** Justert for høyere terminalpanel (ca. 720 px høyde).

GUI-en endrer kun utseende og layout; funksjonalitet (åpne/lukke terminal, reservasjon, kjøp, scenario, logg) er uendret.

---

## 4. Filer som er berørt

### Payment Terminal SIM

- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../config/SimulatorConfig.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../controller/TerminalController.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../controller/PaymentController.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../controller/AdminController.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../model/response/OperationResponse.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../service/ReceiptGenerator.kt`
- `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../service/TerminalStateManager.kt`
- **Ny:** `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../service/LastReceiptStore.kt`
- **Ny:** `lpg-ehl-payment-terminal-sim/src/main/kotlin/.../service/ReportState.kt`

### Payment Terminal GUI

- `lpg-ehl-payment-terminal-gui/src/main/kotlin/.../TerminalDisplayPanel.kt`
- `lpg-ehl-payment-terminal-gui/src/main/kotlin/.../TerminalGuiFrame.kt`

---

## 5. Referanser

- **API-testrapport:** `instructions/API_TEST_RESULTS.md`
- **Admin-koder (referanse):** 12592 avstemming, 12594 cancel, 12596 reversal, 12598 X-rapport, 12599 Z-rapport, 12604 last-receipt
