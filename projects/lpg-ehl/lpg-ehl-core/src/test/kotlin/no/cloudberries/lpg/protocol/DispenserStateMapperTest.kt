package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DispenserStateMapper - Protocol Byte to Domain State Mapping")
class DispenserStateMapperTest {
    
    @Nested
    @DisplayName("Bit-Mask Interpretation")
    inner class BitMaskTests {
        
        @Test
        fun `0x00 - All bits clear should map to IDLE`() {
            val payload = byteArrayOf(0x00)
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.IDLE, "Expected IDLE, got $result")
        }
        
        @Test
        fun `0x04 - Start button pressed should map to AUTHORIZED`() {
            // VB6: DISP_startbuttonpressed = bit2 = 0x04
            val payload = byteArrayOf(0x04)  // START_BUTTON_PRESSED
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.AUTHORIZED, "Expected AUTHORIZED, got $result")
        }
        
        @Test
        fun `0x06 - Start button + Open for delivery should map to PUMPING`() {
            // VB6: DISP_startbuttonpressed (0x04) + DISP_openfordelivery (0x02)
            val payload = byteArrayOf(0x06)  // START_BUTTON_PRESSED | OPEN_FOR_DELIVERY
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.PUMPING, "Expected PUMPING, got $result")
        }
        
        @Test
        fun `0x08 - Automode should map to PAYMENT_PENDING`() {
            // VB6: disp_automode = bit3 = 0x08 (indicates transaction complete)
            val payload = byteArrayOf(0x08)  // AUTOMODE
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.PAYMENT_PENDING, "Expected PAYMENT_PENDING, got $result")
        }
        
        @Test
        fun `0x80 - Error flag set should map to ERROR`() {
            val payload = byteArrayOf(0x80.toByte())  // ERROR_FLAG
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.ERROR, "Expected ERROR, got $result")
        }
        
        @Test
        fun `0x81 - Error flag with error code should extract code`() {
            val payload = byteArrayOf(0x80.toByte(), 0x42)  // ERROR_FLAG + error code 0x42
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.ERROR, "Expected ERROR state")
            assertEquals(0x42, (result as DispenserStatus.ERROR).errorCode, "Error code mismatch")
        }
    }
    
    @Nested
    @DisplayName("State Transitions")
    inner class StateTransitionTests {
        
        @Test
        fun `IDLE to AUTHORIZED is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.IDLE,
                DispenserStatus.AUTHORIZED
            )
            assertTrue(valid, "IDLE → AUTHORIZED should be valid")
        }
        
        @Test
        fun `AUTHORIZED to PUMPING is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.AUTHORIZED,
                DispenserStatus.PUMPING
            )
            assertTrue(valid, "AUTHORIZED → PUMPING should be valid")
        }
        
        @Test
        fun `PUMPING to PAYMENT_PENDING is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.PUMPING,
                DispenserStatus.PAYMENT_PENDING
            )
            assertTrue(valid, "PUMPING → PAYMENT_PENDING should be valid")
        }
        
        @Test
        fun `PAYMENT_PENDING to IDLE is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.PAYMENT_PENDING,
                DispenserStatus.IDLE
            )
            assertTrue(valid, "PAYMENT_PENDING → IDLE should be valid")
        }
        
        @Test
        fun `Any state to ERROR is valid`() {
            assertTrue(DispenserStateMapper.isValidTransition(DispenserStatus.IDLE, DispenserStatus.ERROR(1)))
            assertTrue(DispenserStateMapper.isValidTransition(DispenserStatus.AUTHORIZED, DispenserStatus.ERROR(2)))
            assertTrue(DispenserStateMapper.isValidTransition(DispenserStatus.PUMPING, DispenserStatus.ERROR(3)))
        }
        
        @Test
        fun `IDLE to PUMPING is invalid (must go through AUTHORIZED)`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.IDLE,
                DispenserStatus.PUMPING
            )
            assertFalse(valid, "IDLE → PUMPING should be invalid (missing AUTHORIZED step)")
        }
        
        @Test
        fun `PUMPING to IDLE is invalid (must go through PAYMENT_PENDING)`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.PUMPING,
                DispenserStatus.IDLE
            )
            assertFalse(valid, "PUMPING → IDLE should be invalid (missing PAYMENT_PENDING step)")
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {
        
        @Test
        fun `Empty payload should return UNKNOWN`() {
            val payload = byteArrayOf()
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.UNKNOWN, "Expected UNKNOWN for empty payload")
        }
        
        @Test
        fun `Unknown bit combination should return UNKNOWN`() {
            // VB6: OPEN_FOR_DELIVERY without START_BUTTON_PRESSED is invalid
            val payload = byteArrayOf(0x02)  // OPEN_FOR_DELIVERY only
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.UNKNOWN, "Expected UNKNOWN for invalid bit combo")
            assertEquals(0x02, (result as DispenserStatus.UNKNOWN).rawByte, "Raw byte mismatch")
        }
        
        @Test
        fun `Error flag takes priority over other flags`() {
            // 0x8F = ERROR_FLAG | TRANSACTION_COMPLETE | DELIVERY | NOZZLE | START_SWITCH
            val payload = byteArrayOf(0x8F.toByte())
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.ERROR, "ERROR should take priority over all other flags")
        }
    }
    
    @Nested
    @DisplayName("Happy Path Flow")
    inner class HappyPathFlowTests {
        
        @Test
        fun `Complete fueling flow - IDLE to AUTHORIZED to PUMPING to PAYMENT_PENDING to IDLE`() {
            // VB6-compatible state codes:
            // IDLE = 0x00, AUTHORIZED = 0x04, PUMPING = 0x06, PAYMENT_PENDING = 0x08
            
            // Step 1: IDLE (0x00)
            val idlePayload = byteArrayOf(0x00)
            val idleState = DispenserStateMapper.mapToDispenserStatus(idlePayload)
            assertTrue(idleState is DispenserStatus.IDLE)
            
            // Step 2: AUTHORIZED (0x04 - START_BUTTON_PRESSED)
            val authorizedPayload = byteArrayOf(0x04)
            val authorizedState = DispenserStateMapper.mapToDispenserStatus(authorizedPayload)
            assertTrue(authorizedState is DispenserStatus.AUTHORIZED, "0x04 should be AUTHORIZED")
            assertTrue(DispenserStateMapper.isValidTransition(idleState, authorizedState))
            
            // Step 3: PUMPING (0x06 - START_BUTTON_PRESSED | OPEN_FOR_DELIVERY)
            val pumpingPayload = byteArrayOf(0x06)
            val pumpingState = DispenserStateMapper.mapToDispenserStatus(pumpingPayload)
            assertTrue(pumpingState is DispenserStatus.PUMPING, "0x06 should be PUMPING")
            assertTrue(DispenserStateMapper.isValidTransition(authorizedState, pumpingState))
            
            // Step 4: PAYMENT_PENDING (0x08 - AUTOMODE)
            val paymentPendingPayload = byteArrayOf(0x08)
            val paymentPendingState = DispenserStateMapper.mapToDispenserStatus(paymentPendingPayload)
            assertTrue(paymentPendingState is DispenserStatus.PAYMENT_PENDING, "0x08 should be PAYMENT_PENDING")
            assertTrue(DispenserStateMapper.isValidTransition(pumpingState, paymentPendingState))
            
            // Step 5: Back to IDLE (0x00) after payment/reset
            val finalIdlePayload = byteArrayOf(0x00)
            val finalIdleState = DispenserStateMapper.mapToDispenserStatus(finalIdlePayload)
            assertTrue(finalIdleState is DispenserStatus.IDLE)
            assertTrue(DispenserStateMapper.isValidTransition(paymentPendingState, finalIdleState))
        }
    }
    
    @Nested
    @DisplayName("mapFromPacket - EhlPacket Integration")
    inner class PacketIntegrationTests {
        
        @Test
        fun `mapFromPacket with STATE command should extract status`() {
            val packet = EhlPacket(
                address = 1,
                command = EhlCommand.STATE,
                data = byteArrayOf(0x00)
            )
            
            val result = DispenserStateMapper.mapFromPacket(packet)
            assertTrue(result is DispenserStatus.IDLE)
        }
        
        @Test
        fun `mapFromPacket with non-STATE command should return UNKNOWN`() {
            val packet = EhlPacket(
                address = 1,
                command = EhlCommand.VOLUME,
                data = byteArrayOf(0x00, 0x00, 0x00, 0x00)
            )
            
            val result = DispenserStateMapper.mapFromPacket(packet)
            assertTrue(result is DispenserStatus.UNKNOWN, "Expected UNKNOWN for non-STATE command")
        }
        
        @Test
        fun `mapFromPacket with PUMPING state should work correctly`() {
            val packet = EhlPacket(
                address = 1,
                command = EhlCommand.STATE,
                data = byteArrayOf(0x06)  // NOZZLE_LIFTED | DELIVERY_IN_PROGRESS
            )
            
            val result = DispenserStateMapper.mapFromPacket(packet)
            assertTrue(result is DispenserStatus.PUMPING)
        }
    }
    
    @Nested
    @DisplayName("VB6 Legacy Protocol Compliance")
    inner class LegacyProtocolComplianceTests {
        
        @Test
        fun `Verify bit masks match VB6 legacy definitions`() {
            // VB6 (pumpekontroll.frm lines 2734-2805):
            // bit1 (0x02) = DISP_openfordelivery
            // bit2 (0x04) = DISP_startbuttonpressed
            // bit3 (0x08) = disp_automode
            // bit7 (0x80) = error
            assertEquals(0x02, StatusBitMasks.OPEN_FOR_DELIVERY, "OPEN_FOR_DELIVERY = bit1 = 0x02")
            assertEquals(0x04, StatusBitMasks.START_BUTTON_PRESSED, "START_BUTTON_PRESSED = bit2 = 0x04")
            assertEquals(0x08, StatusBitMasks.AUTOMODE, "AUTOMODE = bit3 = 0x08")
            assertEquals(0x80, StatusBitMasks.ERROR_FLAG, "ERROR_FLAG = bit7 = 0x80")
        }
        
        @Test
        fun `Protocol requirement - UNBLOCK before PUMPING`() {
            // This test verifies the state machine prevents PUMPING without AUTHORIZED
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.IDLE,
                DispenserStatus.PUMPING
            )
            assertFalse(valid, "Protocol violation: Cannot transition IDLE → PUMPING without AUTHORIZED")
        }
    }
}
