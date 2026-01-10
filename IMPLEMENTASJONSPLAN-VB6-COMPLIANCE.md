# Implementasjonsplan: Korriger EHL-protokoll mot VB6-fasit

**Dato:** 9. januar 2026  
**Prosjekt:** lpg-ehl-core  
**Formål:** Rette kritiske protokollavvik mellom Kotlin-implementasjon og VB6/Python legacy-fasit

---

## Sammendrag av avvik

| # | Avvik | Alvorlighet | Fil(er) som må endres |
|---|-------|-------------|----------------------|
| 1 | STATE bit-mapping | KRITISK | DispenserStatus.kt, DispenserStateMapper.kt |
| 2 | VOLUME format (5 ASCII vs 4 binary) | KRITISK | EhlCodec.kt, EhlDispenserEmulator.kt |
| 3 | PRICE emulator format | HØY | EhlDispenserEmulator.kt |
| 4 | TANK status bits mangler | MEDIUM | Ny: TankStatusMapper.kt |
| 5 | LINETEST validering mangler | LAV | Ny: LinetestValidator.kt |

---

## Fase 1: STATE Bit-Mapping (KRITISK)

### 1.1 Opprett VB6-kompatible bit-definisjoner

**Fil:** `src/main/kotlin/no/cloudberries/lpg/protocol/DispenserStatus.kt`

**Endringer:**
- Rename og re-map StatusBitMasks til VB6-fasit:
  - `AUTOMODE = 0x08` (bit3) - VB6: `disp_automode`
  - `START_BUTTON_PRESSED = 0x04` (bit2) - VB6: `DISP_startbuttonpressed`
  - `OPEN_FOR_DELIVERY = 0x02` (bit1) - VB6: `DISP_openfordelivery`
- Behold `ERROR_FLAG = 0x80` (bit7) som er korrekt

**VB6-referanse (pumpekontroll.frm linje 2734-2805):**
```vb
state_string = decimaltobinn(x(4))
If Mid(state_string, 5, 1) = "1" Then disp_automode = True      ' bit3 = 0x08
If Mid(state_string, 6, 1) = "1" Then DISP_startbuttonpressed = True  ' bit2 = 0x04
If Mid(state_string, 7, 1) = "1" Then DISP_openfordelivery = True     ' bit1 = 0x02
```

### 1.2 Oppdater DispenserStateMapper

**Fil:** `src/main/kotlin/no/cloudberries/lpg/protocol/DispenserStateMapper.kt`

**Ny mapping-logikk (basert på VB6):**
```kotlin
fun mapToDispenserStatus(payload: ByteArray): DispenserStatus {
    val statusByte = payload[0]
    val automode = (statusByte.toInt() and 0x08) != 0
    val startButtonPressed = (statusByte.toInt() and 0x04) != 0
    val openForDelivery = (statusByte.toInt() and 0x02) != 0
    val hasError = (statusByte.toInt() and 0x80) != 0
    
    return when {
        hasError -> DispenserStatus.ERROR(errorCode)
        startButtonPressed && openForDelivery -> DispenserStatus.PUMPING
        startButtonPressed && !openForDelivery -> DispenserStatus.AUTHORIZED
        !startButtonPressed && !openForDelivery -> DispenserStatus.IDLE
        else -> DispenserStatus.UNKNOWN(statusByte)
    }
}
```

### 1.3 Oppdater tester

**Fil:** `src/test/kotlin/no/cloudberries/lpg/protocol/DispenserStateMapperTest.kt`

- Endre alle test-verdier til VB6-kompatible:
  - `0x04` = START_BUTTON_PRESSED → AUTHORIZED
  - `0x06` = START_BUTTON_PRESSED + OPEN_FOR_DELIVERY → PUMPING
  - `0x08` = AUTOMODE (ikke TRANSACTION_COMPLETE)
- Legg til VB6-compliance tester

---

## Fase 2: VOLUME Format (KRITISK)

### 2.1 Ny VOLUME parser for VB6-format

**Fil:** `src/main/kotlin/no/cloudberries/lpg/protocol/EhlCodec.kt` (EhlDataParser)

**VB6-fasit (5 ASCII bytes LSB-first):**
```vb
tank_vol = CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
' Eksempel: bytes ['0','4','5','5','0'] → "04550" → 45.50 L
```

**Ny implementasjon:**
```kotlin
fun parseVolumeDataVb6(data: ByteArray): Double {
    require(data.size == 5) { "VB6 VOLUME expects 5 ASCII bytes, got ${data.size}" }
    
    // LSB-first: data[0]=0.01L, data[1]=0.1L, data[2]=1L, data[3]=10L, data[4]=100L
    val d0 = data[0].toInt().toChar()
    val d1 = data[1].toInt().toChar()
    val d2 = data[2].toInt().toChar()
    val d3 = data[3].toInt().toChar()
    val d4 = data[4].toInt().toChar()
    
    val volumeString = "$d4$d3$d2$d1$d0"  // "04550"
    require(volumeString.all { it.isDigit() }) { "Invalid VOLUME digits: $volumeString" }
    
    return volumeString.toInt() / 100.0  // 45.50
}
```

### 2.2 Oppdater emulator VOLUME response

**Fil:** `src/main/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulator.kt`

**Endre `buildVolumeResponse()`:**
```kotlin
private fun buildVolumeResponse(): EhlPacket {
    val volumeLitres = completedTx?.volumeLitres ?: activeTx?.volumeLitres ?: 0.0
    
    // VB6 format: 5 ASCII bytes LSB-first for volume (xxx.xx liters)
    val volumeCentilitres = (volumeLitres * 100).roundToInt()
    val volumeString = "%05d".format(volumeCentilitres)  // "04550" for 45.50L
    
    val data = ByteArray(5)
    for (i in 0..4) {
        data[i] = volumeString[4 - i].code.toByte()  // LSB-first
    }
    
    return EhlPacket(address, EhlCommand.VOLUME, data)
}
```

### 2.3 Deprecate gammel parser

- Merk `parseVolumeData()` (4-byte binary) som `@Deprecated`
- Bruk `parseVolumeDataVb6()` som standard

---

## Fase 3: PRICE Format i Emulator (HØY)

### 3.1 Fiks emulator PRICE response

**Fil:** `src/main/kotlin/no/cloudberries/lpg/emulator/EhlDispenserEmulator.kt`

**VB6-fasit (4 ASCII bytes):**
```vb
dispris.Caption = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
' Bytes ['0','9','5','1'] → "15.90"
```

**Ny implementasjon:**
```kotlin
private fun buildPriceResponse(): EhlPacket {
    // VB6 format: 4 ASCII bytes LSB-first
    // pricePerLitreCents = 1590 → "1590" → ['0','9','5','1']
    val priceString = "%04d".format(pricePerLitreCents)  // "1590"
    
    val data = ByteArray(4)
    for (i in 0..3) {
        data[i] = priceString[3 - i].code.toByte()  // LSB-first
    }
    
    return EhlPacket(address, EhlCommand.PRICE, data)
}
```

---

## Fase 4: TANK Status (MEDIUM)

### 4.1 Opprett TankStatusMapper

**Ny fil:** `src/main/kotlin/no/cloudberries/lpg/protocol/TankStatusMapper.kt`

**VB6-fasit (pumpekontroll.frm linje 2850-2590):**
```vb
state_string_Tank = decimaltobinn_tank(x(4))
If CInt(Mid(state_string_Tank, 8, 1)) = 1 Then trans_finished_powerfault = True  ' bit0 = 0x01
If CInt(Mid(state_string_Tank, 5, 1)) = 1 Then trans_unaccounted = True          ' bit3 = 0x08
```

**Implementasjon:**
```kotlin
package no.cloudberries.lpg.protocol

/**
 * TANK Status from EHL command 197 (0xC5)
 * 
 * Maps raw protocol bytes to transaction status flags.
 * Based on VB6 legacy implementation.
 */
data class TankStatus(
    /** Transaction not yet accounted for (bit3 = 0x08) */
    val transactionUnaccounted: Boolean,
    /** Transaction finished due to power fault (bit0 = 0x01) */
    val transactionFinishedPowerFault: Boolean,
    /** Raw status byte for debugging */
    val rawByte: Byte
)

object TankStatusMapper {
    /** Bit 3: Transaction unaccounted */
    const val TRANS_UNACCOUNTED = 0x08
    /** Bit 0: Transaction finished due to power fault */
    const val TRANS_POWER_FAULT = 0x01
    
    /**
     * Parse TANK status response data.
     * 
     * @param data Raw data bytes from TANK (0xC5) response
     * @return TankStatus with parsed flags
     */
    fun parseTankStatus(data: ByteArray): TankStatus {
        require(data.isNotEmpty()) { "TANK status requires at least 1 byte" }
        val statusByte = data[0]
        return TankStatus(
            transactionUnaccounted = (statusByte.toInt() and TRANS_UNACCOUNTED) != 0,
            transactionFinishedPowerFault = (statusByte.toInt() and TRANS_POWER_FAULT) != 0,
            rawByte = statusByte
        )
    }
}
```

### 4.2 Integrer i EhlDataParser

- Legg til `parseTankData()` metode som delegerer til TankStatusMapper

---

## Fase 5: LINETEST Validering (LAV)

### 5.1 Opprett LinetestValidator

**Ny fil:** `src/main/kotlin/no/cloudberries/lpg/protocol/LinetestValidator.kt`

**VB6-fasit (Python model.py linje 152-159):**
```python
def on_linetest(self, data: bytes) -> None:
    if len(data) >= 2 and data[0] == 0x55 and data[1] == 0xAA:
        self.disp_init = True
    else:
        self.disp_init = False
```

**Implementasjon:**
```kotlin
package no.cloudberries.lpg.protocol

/**
 * LINETEST Response Validator
 * 
 * Validates the response from LINETEST command (0x6A).
 * A valid response contains the magic bytes 0x55 0xAA.
 */
object LinetestValidator {
    const val EXPECTED_BYTE_1: Byte = 0x55
    val EXPECTED_BYTE_2: Byte = 0xAA.toByte()  // 0xAA = -86 as signed byte
    
    /**
     * Validate LINETEST response data.
     * 
     * @param data Raw data bytes from LINETEST response
     * @return true if response contains valid magic bytes (0x55 0xAA)
     */
    fun validateLinetestResponse(data: ByteArray): Boolean {
        return data.size >= 2 && 
               data[0] == EXPECTED_BYTE_1 && 
               data[1] == EXPECTED_BYTE_2
    }
}
```

---

## Fase 6: Testing og Validering

### 6.1 Opprett VB6-compliance testsuite

**Ny fil:** `src/test/kotlin/no/cloudberries/lpg/protocol/Vb6ComplianceTest.kt`

Tester som verifiserer eksakt match med VB6-fasiten:

```kotlin
package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("VB6 Legacy Protocol Compliance")
class Vb6ComplianceTest {
    
    @Test
    fun `STATE bit 0x04 should be START_BUTTON_PRESSED`() {
        val payload = byteArrayOf(0x04)
        val result = DispenserStateMapper.mapToDispenserStatus(payload)
        assertTrue(result is DispenserStatus.AUTHORIZED)
    }
    
    @Test
    fun `STATE bit 0x02 should be OPEN_FOR_DELIVERY`() {
        // 0x02 alone is not a valid state (need startbutton too)
        val payload = byteArrayOf(0x02)
        val result = DispenserStateMapper.mapToDispenserStatus(payload)
        assertTrue(result is DispenserStatus.UNKNOWN)
    }
    
    @Test
    fun `STATE bits 0x06 should be PUMPING`() {
        // 0x06 = START_BUTTON_PRESSED (0x04) + OPEN_FOR_DELIVERY (0x02)
        val payload = byteArrayOf(0x06)
        val result = DispenserStateMapper.mapToDispenserStatus(payload)
        assertTrue(result is DispenserStatus.PUMPING)
    }
    
    @Test
    fun `STATE bit 0x08 should be AUTOMODE`() {
        assertEquals(0x08, Vb6StateBits.AUTOMODE)
    }
    
    @Test
    fun `VOLUME 5-byte ASCII LSB-first parsing`() {
        // "04550" for 45.50 L, stored LSB-first: ['0','5','5','4','0']
        val data = byteArrayOf(
            '0'.code.toByte(),  // 0.01 L
            '5'.code.toByte(),  // 0.1 L
            '5'.code.toByte(),  // 1 L
            '4'.code.toByte(),  // 10 L
            '0'.code.toByte()   // 100 L
        )
        val volume = EhlDataParser.parseVolumeDataVb6(data)
        assertEquals(45.50, volume, 0.001)
    }
    
    @Test
    fun `PRICE 4-byte ASCII LSB-first parsing`() {
        // "15.90" stored as ['0','9','5','1'] LSB-first
        val data = byteArrayOf(
            '0'.code.toByte(),  // 0.01 kr
            '9'.code.toByte(),  // 0.1 kr
            '5'.code.toByte(),  // 1 kr
            '1'.code.toByte()   // 10 kr
        )
        val price = EhlDataParser.parsePriceData(data)
        assertEquals("15.90", price)
    }
    
    @Test
    fun `TANK bit 0x01 is POWER_FAULT`() {
        val data = byteArrayOf(0x01)
        val tank = TankStatusMapper.parseTankStatus(data)
        assertTrue(tank.transactionFinishedPowerFault)
        assertFalse(tank.transactionUnaccounted)
    }
    
    @Test
    fun `TANK bit 0x08 is UNACCOUNTED`() {
        val data = byteArrayOf(0x08)
        val tank = TankStatusMapper.parseTankStatus(data)
        assertFalse(tank.transactionFinishedPowerFault)
        assertTrue(tank.transactionUnaccounted)
    }
    
    @Test
    fun `LINETEST response must be 0x55 0xAA`() {
        val validData = byteArrayOf(0x55, 0xAA.toByte())
        assertTrue(LinetestValidator.validateLinetestResponse(validData))
        
        val invalidData = byteArrayOf(0x55, 0x00)
        assertFalse(LinetestValidator.validateLinetestResponse(invalidData))
    }
}
```

### 6.2 Integrasjonstester

- Emulator-to-parser round-trip tester
- Full transaksjonsflyt test

---

## Implementasjonsrekkefølge

| Dag | Fase | Beskrivelse |
|-----|------|-------------|
| 1 | Fase 1 | STATE bit-mapping - Kritisk, påvirker all state-håndtering |
| 1-2 | Fase 2 | VOLUME format - Kritisk, påvirker transaksjonsdata |
| 2 | Fase 3 | PRICE emulator - Høy, inkonsistens |
| 3 | Fase 4 | TANK status - Medium, ny funksjonalitet |
| 3 | Fase 5 | LINETEST - Lav, enkel |
| 4 | Fase 6 | Testing - Full validering |

---

## Bakoverkompatibilitet

For å støtte eksisterende kode som bruker gammel mapping:

1. Behold gamle metoder merket `@Deprecated`
2. Legg til `ProtocolMode.LEGACY_KOTLIN` vs `ProtocolMode.VB6_COMPLIANT`
3. Bruk VB6_COMPLIANT som default

---

## Risiko og mitigering

| Risiko | Mitigering |
|--------|------------|
| Breaking changes i API | Deprecate, ikke fjern gamle metoder |
| Emulator bruker binært format | Oppdater emulator først, deretter tester |
| lpg-ehl-api avhengighet | Sjekk API-bruk før endring |

---

## Akseptansekriterier

- [ ] Alle VB6-compliance tester passerer
- [ ] Eksisterende tester oppdatert til VB6-format
- [ ] Emulator sender korrekt format
- [ ] Round-trip parsing fungerer
- [ ] Dokumentasjon oppdatert

---

## Referanser

- **VB6 Fasit:** `/Users/tandersen/git/NorgesGass/lpg-ehl/legacy-curated/pumpekontroll/pumpekontroll.frm`
- **Python Port:** `/Users/tandersen/git/NorgesGass/lpg-ehl/legacy-curated/Python/ehl_pumpekontroll_clone/ehl/protocol.py`
- **Kotlin Core:** `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/`
