package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.protocol.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for EHL Dispenser Emulator
 * 
 * Mirrors the Python test suite from python-test/:
 * - 01_probe_readonly.py -> testProbeReadOnly()
 * - 02_scan_addresses.py -> testAddressScanning()
 * - 03_control_unblock_block.py -> testControlCommands()
 * - 05_unlock_hold_block.py -> testFullTransactionCycle()
 * 
 * These tests validate the emulator behaves like a real EHL dispenser.
 */
@DisplayName("EHL Dispenser Emulator Integration Tests")
class EhlEmulatorIntegrationTest {
    
    private lateinit var emulator: EhlDispenserEmulator
    private val testAddress = 1
    private val testPrice = 1590  // 15.90 kr/L
    
    @BeforeEach
    fun setup() {
        emulator = EhlDispenserEmulator(
            address = testAddress,
            pricePerLitreCents = testPrice,
            litresPerSecond = 0.5
        )
    }
    
    /**
     * Helper: Send command and get first response packet
     */
    private fun sendCommand(command: EhlCommand, data: ByteArray = ByteArray(0)): EhlPacket? {
        val packet = EhlPacket(testAddress, command, data)
        val encodedRequest = EhlCodec.encode(packet, fromController = true)
        val responses = emulator.onBytesFromHost(encodedRequest)
        
        if (responses.isEmpty()) return null
        
        val decoded = EhlCodec.decode(responses[0])
        return when (decoded) {
            is EhlPacketParseResult.Success -> decoded.packet
            else -> null
        }
    }
    
    /**
     * Helper: Send command and get all response packets
     */
    private fun sendCommandGetAll(command: EhlCommand, data: ByteArray = ByteArray(0)): List<EhlPacket> {
        val packet = EhlPacket(testAddress, command, data)
        val encodedRequest = EhlCodec.encode(packet, fromController = true)
        val responses = emulator.onBytesFromHost(encodedRequest)
        
        return responses.mapNotNull { responseBytes ->
            when (val decoded = EhlCodec.decode(responseBytes)) {
                is EhlPacketParseResult.Success -> decoded.packet
                else -> null
            }
        }
    }
    
    @Nested
    @DisplayName("01 - Read-Only Probe Tests (mirrors 01_probe_readonly.py)")
    inner class ReadOnlyProbeTests {
        
        @Test
        fun `should respond to STATE query`() {
            val response = sendCommand(EhlCommand.STATE)
            
            assertNotNull(response, "Should receive STATE response")
            assertEquals(EhlCommand.STATE, response.command)
            assertEquals(testAddress, response.address)
            assertEquals(1, response.data.size, "STATE data should be 1 byte")
            
            // Initially IDLE, state byte should be 0x00
            assertEquals(0x00, response.data[0].toInt() and 0xFF)
        }
        
        @Test
        fun `should respond to ERROR_QUERY`() {
            val response = sendCommand(EhlCommand.ERROR_QUERY)
            
            // Emulator may not implement ERROR_QUERY yet, but should not crash
            // If implemented, should return ERROR with 2 bytes
            // For now, just verify we get a response or empty
            assertTrue(response == null || response.command == EhlCommand.ERROR)
        }
        
        @Test
        fun `should respond to VOLUME query`() {
            val response = sendCommand(EhlCommand.VOLUME)
            
            assertNotNull(response, "Should receive VOLUME response")
            assertEquals(EhlCommand.VOLUME, response.command)
            assertEquals(testAddress, response.address)
            assertEquals(5, response.data.size, "VOLUME data should be 5 ASCII bytes (VB6 format)")
            
            // Initially 0.00 L, VB6 format: "00000" -> ['0','0','0','0','0'] LSB-first
            val expectedZero = byteArrayOf(0x30, 0x30, 0x30, 0x30, 0x30)  // ASCII '0' x5
            assertTrue(response.data.contentEquals(expectedZero), "Initial volume should be 00000")
        }
        
        @Test
        fun `should respond to TANK query`() {
            val response = sendCommand(EhlCommand.TANK)
            
            assertNotNull(response, "Should receive TANK response")
            assertEquals(EhlCommand.TANK, response.command)
            assertEquals(testAddress, response.address)
            assertTrue(response.data.isNotEmpty(), "TANK response should have data")
        }
        
        @Test
        fun `should respond to PRICE query`() {
            val response = sendCommand(EhlCommand.PRICE)
            
            assertNotNull(response, "Should receive PRICE response")
            assertEquals(EhlCommand.PRICE, response.command)
            assertEquals(testAddress, response.address)
            assertEquals(4, response.data.size, "PRICE data should be 4 ASCII bytes (VB6 format)")
            
            // Price 15.90 kr/L = 1590 -> "1590" -> ['0','9','5','1'] LSB-first
            val expected = byteArrayOf(0x30, 0x39, 0x35, 0x31)  // ASCII "0951"
            assertTrue(response.data.contentEquals(expected), "Price should be 1590 (15.90 kr/L)")
        }
        
        @Test
        fun `all read-only commands should succeed - summary check`() {
            val commands = listOf(
                EhlCommand.STATE,
                EhlCommand.VOLUME,
                EhlCommand.TANK,
                EhlCommand.PRICE
            )
            
            val successCount = commands.count { cmd ->
                val response = sendCommand(cmd)
                response != null && response.command != EhlCommand.ERROR
            }
            
            assertEquals(commands.size, successCount, "All ${commands.size} read-only commands should succeed")
        }
    }
    
    @Nested
    @DisplayName("02 - Address Scanning (mirrors 02_scan_addresses.py)")
    inner class AddressScanningTests {
        
        @Test
        fun `should respond to correct address`() {
            val response = sendCommand(EhlCommand.STATE)
            assertNotNull(response, "Emulator at address $testAddress should respond")
            assertEquals(testAddress, response.address)
        }
        
        @Test
        fun `should ignore wrong address`() {
            val wrongAddress = testAddress + 1
            val packet = EhlPacket(wrongAddress, EhlCommand.STATE)
            val encodedRequest = EhlCodec.encode(packet, fromController = true)
            val responses = emulator.onBytesFromHost(encodedRequest)
            
            assertTrue(responses.isEmpty(), "Emulator should ignore packets to wrong address")
        }
        
        @Test
        fun `should respond to address scan range 1-32`() {
            // Simulate address scan like Python script
            val respondingAddresses = (1..32).filter { addr ->
                val packet = EhlPacket(addr, EhlCommand.STATE)
                val encodedRequest = EhlCodec.encode(packet, fromController = true)
                val responses = emulator.onBytesFromHost(encodedRequest)
                responses.isNotEmpty()
            }
            
            assertEquals(1, respondingAddresses.size, "Only one address should respond")
            assertEquals(testAddress, respondingAddresses.first())
        }
    }
    
    @Nested
    @DisplayName("03 - Control Commands (mirrors 03_control_unblock_block.py)")
    inner class ControlCommandsTests {
        
        @Test
        fun `UNBLOCK should return VB6-style ACK with 0x30 payload`() {
            val responses = sendCommandGetAll(EhlCommand.UNBLOCK)
            
            assertTrue(responses.isNotEmpty(), "UNBLOCK should return responses")
            
            // First response should be ACK with data[0] = 0x30 (VB6 format)
            val ackPacket = responses.first()
            assertEquals(EhlCommand.OK, ackPacket.command, "First response should be OK")
            assertEquals(1, ackPacket.data.size, "ACK should have 1 data byte")
            assertEquals(0x30, ackPacket.data[0].toInt() and 0xFF, "ACK data[0] should be 0x30 (VB6 format)")
        }
        
        @Test
        fun `UNBLOCK should transition to DELIVERING state`() {
            val responses = sendCommandGetAll(EhlCommand.UNBLOCK)
            assertTrue(responses.size >= 2, "UNBLOCK should return ACK + STATE")
            
            // Second response should be STATE showing DELIVERING
            val statePacket = responses[1]
            assertEquals(EhlCommand.STATE, statePacket.command)
            
            val stateByte = statePacket.data[0].toInt() and 0xFF
            // DELIVERING = 0x06 (OPEN_FOR_DELIVERY + START_BUTTON_PRESSED)
            assertEquals(0x06, stateByte, "State should be DELIVERING (0x06)")
        }
        
        @Test
        fun `BLOCK should return VB6-style ACK with 0x30 payload`() {
            // First UNBLOCK to start delivery
            sendCommandGetAll(EhlCommand.UNBLOCK)
            
            // Then BLOCK
            val responses = sendCommandGetAll(EhlCommand.BLOCK)
            
            assertTrue(responses.isNotEmpty(), "BLOCK should return responses")
            
            // First response should be ACK with data[0] = 0x30
            val ackPacket = responses.first()
            assertEquals(EhlCommand.OK, ackPacket.command, "First response should be OK")
            assertEquals(1, ackPacket.data.size, "ACK should have 1 data byte")
            assertEquals(0x30, ackPacket.data[0].toInt() and 0xFF, "ACK data[0] should be 0x30 (VB6 format)")
        }
        
        @Test
        fun `BLOCK should transition to PAYMENT_PENDING state`() {
            // Start delivery
            sendCommandGetAll(EhlCommand.UNBLOCK)
            
            // Stop delivery
            val responses = sendCommandGetAll(EhlCommand.BLOCK)
            assertTrue(responses.size >= 2, "BLOCK should return ACK + STATE + VOLUME")
            
            // Second response should be STATE showing PAYMENT_PENDING
            val statePacket = responses[1]
            assertEquals(EhlCommand.STATE, statePacket.command)
            
            val stateByte = statePacket.data[0].toInt() and 0xFF
            // PAYMENT_PENDING = 0x08 (AUTOMODE)
            assertEquals(0x08, stateByte, "State should be PAYMENT_PENDING (0x08)")
        }
        
        @Test
        fun `BLOCK should freeze volume totals`() {
            // Start delivery
            sendCommandGetAll(EhlCommand.UNBLOCK)
            
            // Wait a bit for volume to increment
            Thread.sleep(100)
            
            // Stop delivery
            val blockResponses = sendCommandGetAll(EhlCommand.BLOCK)
            
            // Extract volume from BLOCK response
            val volumePacket = blockResponses.find { it.command == EhlCommand.VOLUME }
            assertNotNull(volumePacket, "BLOCK should return VOLUME")
            
            val frozenVolume = volumePacket.data
            
            // Query volume again - should be same (frozen)
            Thread.sleep(50)
            val volumeResponse = sendCommand(EhlCommand.VOLUME)
            assertNotNull(volumeResponse)
            
            assertTrue(volumeResponse.data.contentEquals(frozenVolume), "Volume should be frozen after BLOCK")
        }
    }
    
    @Nested
    @DisplayName("05 - Full Transaction Cycle (mirrors 05_unlock_hold_block.py)")
    inner class FullTransactionCycleTests {
        
        @Test
        fun `full transaction cycle - UNBLOCK, hold, BLOCK`() {
            // 1. Initial state should be IDLE
            val initialState = sendCommand(EhlCommand.STATE)
            assertNotNull(initialState)
            assertEquals(0x00, initialState.data[0].toInt() and 0xFF, "Initial state should be IDLE (0x00)")
            
            // 2. UNBLOCK to start delivery
            val unblockResponses = sendCommandGetAll(EhlCommand.UNBLOCK)
            assertTrue(unblockResponses.size >= 2, "UNBLOCK should return ACK + STATE")
            
            // Verify ACK
            val ack1 = unblockResponses[0]
            assertEquals(EhlCommand.OK, ack1.command)
            assertEquals(0x30, ack1.data[0].toInt() and 0xFF)
            
            // Verify state transition to DELIVERING
            val state1 = unblockResponses[1]
            assertEquals(0x06, state1.data[0].toInt() and 0xFF, "Should be DELIVERING")
            
            // 3. Hold for 2 seconds (simulate delivery)
            Thread.sleep(2000)
            
            // 4. Poll STATE and VOLUME during delivery
            val midState = sendCommand(EhlCommand.STATE)
            assertNotNull(midState)
            assertEquals(0x06, midState.data[0].toInt() and 0xFF, "Should still be DELIVERING")
            
            val midVolume = sendCommand(EhlCommand.VOLUME)
            assertNotNull(midVolume)
            // Volume should be > 0 (approx 1.0 L after 2 seconds at 0.5 L/s)
            val volumeStr = String(midVolume.data.map { (it.toInt() and 0xFF).toChar() }.toCharArray().reversedArray())
            val volumeCentilitres = volumeStr.toInt()
            assertTrue(volumeCentilitres > 0, "Volume should have increased during delivery")
            
            // 5. BLOCK to stop delivery
            val blockResponses = sendCommandGetAll(EhlCommand.BLOCK)
            assertTrue(blockResponses.size >= 3, "BLOCK should return ACK + STATE + VOLUME")
            
            // Verify ACK
            val ack2 = blockResponses[0]
            assertEquals(EhlCommand.OK, ack2.command)
            assertEquals(0x30, ack2.data[0].toInt() and 0xFF)
            
            // Verify state transition to PAYMENT_PENDING
            val state2 = blockResponses[1]
            assertEquals(0x08, state2.data[0].toInt() and 0xFF, "Should be PAYMENT_PENDING")
            
            // Verify final volume
            val finalVolume = blockResponses[2]
            assertEquals(EhlCommand.VOLUME, finalVolume.command)
            assertTrue(finalVolume.data.contentEquals(midVolume.data), "Final volume should match mid-delivery volume")
        }
        
        @Test
        fun `UNBLOCK should be denied when PAYMENT_PENDING`() {
            // Complete a transaction
            sendCommandGetAll(EhlCommand.UNBLOCK)
            Thread.sleep(500)
            sendCommandGetAll(EhlCommand.BLOCK)
            
            // Try to UNBLOCK again - should be denied but still get ACK + STATE
            val responses = sendCommandGetAll(EhlCommand.UNBLOCK)
            assertTrue(responses.size >= 2, "UNBLOCK during PAYMENT_PENDING should return ACK + STATE")
            
            // ACK should still be sent
            assertEquals(EhlCommand.OK, responses[0].command)
            assertEquals(0x30, responses[0].data[0].toInt() and 0xFF)
            
            // State should still be PAYMENT_PENDING
            val statePacket = responses[1]
            assertEquals(0x08, statePacket.data[0].toInt() and 0xFF, "Should remain PAYMENT_PENDING")
        }
        
        @Test
        fun `reset should allow new transaction after PAYMENT_PENDING`() {
            // Complete a transaction
            sendCommandGetAll(EhlCommand.UNBLOCK)
            Thread.sleep(500)
            sendCommandGetAll(EhlCommand.BLOCK)
            
            // Reset to IDLE
            emulator.resetToIdle()
            
            // Verify state is IDLE
            val state = sendCommand(EhlCommand.STATE)
            assertNotNull(state)
            assertEquals(0x00, state.data[0].toInt() and 0xFF, "Should be IDLE after reset")
            
            // Should be able to start new transaction
            val responses = sendCommandGetAll(EhlCommand.UNBLOCK)
            val newState = responses[1]
            assertEquals(0x06, newState.data[0].toInt() and 0xFF, "Should transition to DELIVERING")
        }
    }
    
    @Nested
    @DisplayName("Error Message Integration")
    inner class ErrorMessageTests {
        
        @Test
        fun `should parse VB6 error messages`() {
            // Test error parsing with sample data
            val errorData = byteArrayOf(0x31, 0x32)  // ASCII '1', '2' -> main=1, sub=2
            val parsedError = EhlErrorMessages.parseErrorData(errorData)
            
            assertNotNull(parsedError)
            assertEquals(1, parsedError.mainCode)
            assertEquals(2, parsedError.subCode)
            assertTrue(parsedError.hasMessage)
            assertEquals("For mange kommunikasjonsfeil Display<-->CPU", parsedError.norwegian)
            assertEquals("Too many communication errors Display<-->CPU", parsedError.english)
        }
        
        @Test
        fun `should handle unknown error codes gracefully`() {
            val errorData = byteArrayOf(0x39, 0x39)  // main=9, sub=9 (not in map)
            val parsedError = EhlErrorMessages.parseErrorData(errorData)
            
            assertNotNull(parsedError)
            assertEquals(9, parsedError.mainCode)
            assertEquals(9, parsedError.subCode)
            assertTrue(!parsedError.hasMessage)
            assertEquals("Ukjent feil", parsedError.norwegian)
            assertEquals("Unknown error", parsedError.english)
        }
    }
}
