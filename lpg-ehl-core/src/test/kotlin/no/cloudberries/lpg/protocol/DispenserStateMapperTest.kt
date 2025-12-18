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
        fun `0x01 - Start switch active only should map to AUTHORIZED`() {
            val payload = byteArrayOf(0x01)  // START_SWITCH_ACTIVE
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.AUTHORIZED, "Expected AUTHORIZED, got $result")
        }
        
        @Test
        fun `0x06 - Nozzle lifted + Delivery active should map to PUMPING`() {
            val payload = byteArrayOf(0x06)  // NOZZLE_LIFTED | DELIVERY_IN_PROGRESS
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.PUMPING, "Expected PUMPING, got $result")
        }
        
        @Test
        fun `0x08 - Transaction complete should map to STOPPED`() {
            val payload = byteArrayOf(0x08)  // TRANSACTION_COMPLETE
            val result = DispenserStateMapper.mapToDispenserStatus(payload)
            
            assertTrue(result is DispenserStatus.STOPPED, "Expected STOPPED, got $result")
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
        fun `PUMPING to STOPPED is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.PUMPING,
                DispenserStatus.STOPPED
            )
            assertTrue(valid, "PUMPING → STOPPED should be valid")
        }
        
        @Test
        fun `STOPPED to IDLE is valid transition`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.STOPPED,
                DispenserStatus.IDLE
            )
            assertTrue(valid, "STOPPED → IDLE should be valid")
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
        fun `PUMPING to IDLE is invalid (must go through STOPPED)`() {
            val valid = DispenserStateMapper.isValidTransition(
                DispenserStatus.PUMPING,
                DispenserStatus.IDLE
            )
            assertFalse(valid, "PUMPING → IDLE should be invalid (missing STOPPED step)")
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
            // Example: NOZZLE_LIFTED without DELIVERY_IN_PROGRESS
            val payload = byteArrayOf(0x02)  // NOZZLE_LIFTED only
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
        fun `Complete fueling flow - IDLE to AUTHORIZED to PUMPING to STOPPED to IDLE`() {
            // Step 1: IDLE (0x00)
            val idlePayload = byteArrayOf(0x00)
            val idleState = DispenserStateMapper.mapToDispenserStatus(idlePayload)
            assertTrue(idleState is DispenserStatus.IDLE)
            
            // Step 2: AUTHORIZED (0x01 - START_SWITCH_ACTIVE)
            val authorizedPayload = byteArrayOf(0x01)
            val authorizedState = DispenserStateMapper.mapToDispenserStatus(authorizedPayload)
            assertTrue(authorizedState is DispenserStatus.AUTHORIZED)
            assertTrue(DispenserStateMapper.isValidTransition(idleState, authorizedState))
            
            // Step 3: PUMPING (0x06 - NOZZLE_LIFTED | DELIVERY_IN_PROGRESS)
            val pumpingPayload = byteArrayOf(0x06)
            val pumpingState = DispenserStateMapper.mapToDispenserStatus(pumpingPayload)
            assertTrue(pumpingState is DispenserStatus.PUMPING)
            assertTrue(DispenserStateMapper.isValidTransition(authorizedState, pumpingState))
            
            // Step 4: STOPPED (0x08 - TRANSACTION_COMPLETE)
            val stoppedPayload = byteArrayOf(0x08)
            val stoppedState = DispenserStateMapper.mapToDispenserStatus(stoppedPayload)
            assertTrue(stoppedState is DispenserStatus.STOPPED)
            assertTrue(DispenserStateMapper.isValidTransition(pumpingState, stoppedState))
            
            // Step 5: Back to IDLE (0x00)
            val finalIdlePayload = byteArrayOf(0x00)
            val finalIdleState = DispenserStateMapper.mapToDispenserStatus(finalIdlePayload)
            assertTrue(finalIdleState is DispenserStatus.IDLE)
            assertTrue(DispenserStateMapper.isValidTransition(stoppedState, finalIdleState))
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
            // These are the exact values from the VB6 codebase
            assertEquals(0x01, StatusBitMasks.START_SWITCH_ACTIVE, "START_SWITCH_ACTIVE mismatch")
            assertEquals(0x02, StatusBitMasks.NOZZLE_LIFTED, "NOZZLE_LIFTED mismatch")
            assertEquals(0x04, StatusBitMasks.DELIVERY_IN_PROGRESS, "DELIVERY_IN_PROGRESS mismatch")
            assertEquals(0x08, StatusBitMasks.TRANSACTION_COMPLETE, "TRANSACTION_COMPLETE mismatch")
            // 0x80 as signed byte is -128, but as unsigned int is 128
            assertEquals(0x80.toByte().toInt() and 0xFF, StatusBitMasks.ERROR_FLAG.toInt() and 0xFF, "ERROR_FLAG mismatch")
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
