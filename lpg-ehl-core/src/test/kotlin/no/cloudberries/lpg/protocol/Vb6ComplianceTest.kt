package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * VB6 Legacy Protocol Compliance Tests
 * 
 * These tests verify that the Kotlin implementation exactly matches
 * the VB6/Python legacy behavior based on pumpekontroll.frm.
 * 
 * Reference: /Users/tandersen/git/NorgesGass/lpg-ehl/legacy-curated/
 */
@DisplayName("VB6 Legacy Protocol Compliance")
class Vb6ComplianceTest {
    
    @Nested
    @DisplayName("STATE Bit Mapping (0x4B)")
    inner class StateBitMappingTests {
        
        @Test
        @DisplayName("Bit 0x04 should be START_BUTTON_PRESSED -> AUTHORIZED")
        fun `STATE bit 0x04 should be START_BUTTON_PRESSED`() {
            // VB6: If Mid(state_string, 6, 1) = "1" Then DISP_startbuttonpressed = True
            val payload = byteArrayOf(0x04)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.AUTHORIZED, "0x04 should map to AUTHORIZED, got $result")
        }
        
        @Test
        @DisplayName("Bit 0x02 alone should be UNKNOWN (need startbutton)")
        fun `STATE bit 0x02 alone should be UNKNOWN`() {
            // VB6: DISP_openfordelivery without DISP_startbuttonpressed is invalid
            val payload = byteArrayOf(0x02)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.UNKNOWN, "0x02 alone should map to UNKNOWN, got $result")
        }
        
        @Test
        @DisplayName("Bits 0x06 should be PUMPING (startbutton + openfordelivery)")
        fun `STATE bits 0x06 should be PUMPING`() {
            // VB6: DISP_startbuttonpressed (0x04) AND DISP_openfordelivery (0x02)
            val payload = byteArrayOf(0x06)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.PUMPING, "0x06 should map to PUMPING, got $result")
        }
        
        @Test
        @DisplayName("Bit 0x08 should be AUTOMODE -> PAYMENT_PENDING")
        fun `STATE bit 0x08 should be AUTOMODE`() {
            // VB6: If Mid(state_string, 5, 1) = "1" Then disp_automode = True
            assertEquals(0x08, StatusBitMasks.AUTOMODE, "AUTOMODE should be 0x08")
            
            val payload = byteArrayOf(0x08)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.PAYMENT_PENDING, "0x08 should map to PAYMENT_PENDING, got $result")
        }
        
        @Test
        @DisplayName("Bit 0x00 should be IDLE")
        fun `STATE 0x00 should be IDLE`() {
            val payload = byteArrayOf(0x00)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.IDLE, "0x00 should map to IDLE, got $result")
        }
        
        @Test
        @DisplayName("Bit 0x80 should be ERROR")
        fun `STATE bit 0x80 should be ERROR`() {
            val payload = byteArrayOf(0x80.toByte())
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            assertTrue(result is DispenserStatus.ERROR, "0x80 should map to ERROR, got $result")
        }
        
        @Test
        @DisplayName("VB6 bit masks should be correct values")
        fun `VB6 bit masks should match legacy code`() {
            // From pumpekontroll.frm decimaltobinn analysis
            assertEquals(0x02, StatusBitMasks.OPEN_FOR_DELIVERY, "OPEN_FOR_DELIVERY = bit1 = 0x02")
            assertEquals(0x04, StatusBitMasks.START_BUTTON_PRESSED, "START_BUTTON_PRESSED = bit2 = 0x04")
            assertEquals(0x08, StatusBitMasks.AUTOMODE, "AUTOMODE = bit3 = 0x08")
            assertEquals(0x80, StatusBitMasks.ERROR_FLAG, "ERROR_FLAG = bit7 = 0x80")
        }
    }
    
    @Nested
    @DisplayName("VOLUME Format (0x45)")
    inner class VolumeFormatTests {
        
        @Test
        @DisplayName("5-byte ASCII LSB-first parsing for 45.50 L")
        fun `VOLUME 5-byte ASCII LSB-first parsing`() {
            // VB6: tank_vol = CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
            // "04550" for 45.50 L, stored LSB-first: ['0','5','5','4','0']
            val data = byteArrayOf(
                '0'.code.toByte(),  // 0.01 L position
                '5'.code.toByte(),  // 0.1 L position
                '5'.code.toByte(),  // 1 L position
                '4'.code.toByte(),  // 10 L position
                '0'.code.toByte()   // 100 L position
            )
            val volume = EhlDataParser.parseVolumeDataVb6(data)
            assertEquals(45.50, volume, 0.001, "04550 LSB-first should parse to 45.50 L")
        }
        
        @Test
        @DisplayName("5-byte ASCII LSB-first parsing for 0.00 L")
        fun `VOLUME zero value parsing`() {
            val data = byteArrayOf(
                '0'.code.toByte(),
                '0'.code.toByte(),
                '0'.code.toByte(),
                '0'.code.toByte(),
                '0'.code.toByte()
            )
            val volume = EhlDataParser.parseVolumeDataVb6(data)
            assertEquals(0.0, volume, 0.001, "00000 should parse to 0.00 L")
        }
        
        @Test
        @DisplayName("5-byte ASCII LSB-first parsing for 123.45 L")
        fun `VOLUME large value parsing`() {
            // "12345" -> 123.45 L, LSB-first: ['5','4','3','2','1']
            val data = byteArrayOf(
                '5'.code.toByte(),
                '4'.code.toByte(),
                '3'.code.toByte(),
                '2'.code.toByte(),
                '1'.code.toByte()
            )
            val volume = EhlDataParser.parseVolumeDataVb6(data)
            assertEquals(123.45, volume, 0.001, "12345 LSB-first should parse to 123.45 L")
        }
        
        @Test
        @DisplayName("VOLUME rejects non-ASCII digits")
        fun `VOLUME rejects non-ASCII`() {
            val data = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
            assertThrows(IllegalArgumentException::class.java) {
                EhlDataParser.parseVolumeDataVb6(data)
            }
        }
        
        @Test
        @DisplayName("VOLUME rejects wrong length")
        fun `VOLUME rejects wrong length`() {
            val data = byteArrayOf('1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte())
            assertThrows(IllegalArgumentException::class.java) {
                EhlDataParser.parseVolumeDataVb6(data)
            }
        }
    }
    
    @Nested
    @DisplayName("PRICE Format (0x5C)")
    inner class PriceFormatTests {
        
        @Test
        @DisplayName("4-byte ASCII LSB-first parsing for 15.90")
        fun `PRICE 4-byte ASCII LSB-first parsing`() {
            // VB6: dispris.Caption = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
            // "15.90" stored as ['0','9','5','1'] LSB-first
            val data = byteArrayOf(
                '0'.code.toByte(),  // 0.01 kr position
                '9'.code.toByte(),  // 0.1 kr position
                '5'.code.toByte(),  // 1 kr position
                '1'.code.toByte()   // 10 kr position
            )
            val price = EhlDataParser.parsePriceData(data)
            assertEquals("15.90", price, "['0','9','5','1'] should parse to 15.90")
        }
        
        @Test
        @DisplayName("4-byte ASCII LSB-first parsing for 23.45")
        fun `PRICE different value parsing`() {
            // "23.45" stored as ['5','4','3','2'] LSB-first
            val data = byteArrayOf(
                '5'.code.toByte(),
                '4'.code.toByte(),
                '3'.code.toByte(),
                '2'.code.toByte()
            )
            val price = EhlDataParser.parsePriceData(data)
            assertEquals("23.45", price, "['5','4','3','2'] should parse to 23.45")
        }
    }
    
    @Nested
    @DisplayName("TANK Status (0xC5)")
    inner class TankStatusTests {
        
        @Test
        @DisplayName("Bit 0x01 is POWER_FAULT")
        fun `TANK bit 0x01 is POWER_FAULT`() {
            // VB6: If CInt(Mid(state_string_Tank, 8, 1)) = 1 Then trans_finished_powerfault = True
            val data = byteArrayOf(0x01)
            val tank = TankStatusMapper.parseTankStatus(data)
            assertTrue(tank.transactionFinishedPowerFault, "0x01 should set powerFault")
            assertFalse(tank.transactionUnaccounted, "0x01 should not set unaccounted")
        }
        
        @Test
        @DisplayName("Bit 0x08 is UNACCOUNTED")
        fun `TANK bit 0x08 is UNACCOUNTED`() {
            // VB6: If CInt(Mid(state_string_Tank, 5, 1)) = 1 Then trans_unaccounted = True
            val data = byteArrayOf(0x08)
            val tank = TankStatusMapper.parseTankStatus(data)
            assertFalse(tank.transactionFinishedPowerFault, "0x08 should not set powerFault")
            assertTrue(tank.transactionUnaccounted, "0x08 should set unaccounted")
        }
        
        @Test
        @DisplayName("Both bits can be set simultaneously")
        fun `TANK both bits set`() {
            val data = byteArrayOf(0x09)  // 0x08 | 0x01
            val tank = TankStatusMapper.parseTankStatus(data)
            assertTrue(tank.transactionFinishedPowerFault, "0x09 should set powerFault")
            assertTrue(tank.transactionUnaccounted, "0x09 should set unaccounted")
        }
        
        @Test
        @DisplayName("Zero means no flags")
        fun `TANK zero means no flags`() {
            val data = byteArrayOf(0x00)
            val tank = TankStatusMapper.parseTankStatus(data)
            assertFalse(tank.transactionFinishedPowerFault)
            assertFalse(tank.transactionUnaccounted)
        }
    }
    
    @Nested
    @DisplayName("LINETEST Validation (0x6A)")
    inner class LinetestTests {
        
        @Test
        @DisplayName("Response 0x55 0xAA is valid")
        fun `LINETEST response must be 0x55 0xAA`() {
            // VB6/Python: if data[0] == 0x55 and data[1] == 0xAA: disp_init = True
            val validData = byteArrayOf(0x55, 0xAA.toByte())
            assertTrue(LinetestValidator.validateLinetestResponse(validData), "0x55 0xAA should be valid")
        }
        
        @Test
        @DisplayName("Response 0x55 0x00 is invalid")
        fun `LINETEST invalid second byte`() {
            val invalidData = byteArrayOf(0x55, 0x00)
            assertFalse(LinetestValidator.validateLinetestResponse(invalidData), "0x55 0x00 should be invalid")
        }
        
        @Test
        @DisplayName("Response 0x00 0xAA is invalid")
        fun `LINETEST invalid first byte`() {
            val invalidData = byteArrayOf(0x00, 0xAA.toByte())
            assertFalse(LinetestValidator.validateLinetestResponse(invalidData), "0x00 0xAA should be invalid")
        }
        
        @Test
        @DisplayName("Empty response is invalid")
        fun `LINETEST empty is invalid`() {
            assertFalse(LinetestValidator.validateLinetestResponse(byteArrayOf()))
        }
        
        @Test
        @DisplayName("Single byte response is invalid")
        fun `LINETEST single byte is invalid`() {
            assertFalse(LinetestValidator.validateLinetestResponse(byteArrayOf(0x55)))
        }
    }
    
    @Nested
    @DisplayName("Emulator Round-Trip")
    inner class EmulatorRoundTripTests {
        
        @Test
        @DisplayName("Emulator STATE codes match parser expectations")
        fun `Emulator state codes match VB6 bit masks`() {
            // IDLE = 0x00
            val idlePayload = byteArrayOf(0x00)
            assertTrue(DispenserStateMapper.mapToDispenserStatus(idlePayload) is DispenserStatus.IDLE)
            
            // AUTHORIZED = 0x04 (START_BUTTON_PRESSED)
            val authorizedPayload = byteArrayOf(0x04)
            assertTrue(DispenserStateMapper.mapToDispenserStatus(authorizedPayload) is DispenserStatus.AUTHORIZED)
            
            // PUMPING = 0x06 (START_BUTTON_PRESSED + OPEN_FOR_DELIVERY)
            val pumpingPayload = byteArrayOf(0x06)
            assertTrue(DispenserStateMapper.mapToDispenserStatus(pumpingPayload) is DispenserStatus.PUMPING)
            
            // PAYMENT_PENDING = 0x08 (AUTOMODE)
            val paymentPayload = byteArrayOf(0x08)
            assertTrue(DispenserStateMapper.mapToDispenserStatus(paymentPayload) is DispenserStatus.PAYMENT_PENDING)
        }
    }
}
