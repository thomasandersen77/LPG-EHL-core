package no.cloudberries.lpg.emulator

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Comprehensive tests for EHL Dispenser Emulator.
 * Tests the PAYMENT_PENDING lifecycle, atomic stop, and reset operations.
 */
class EhlDispenserEmulatorTest {
    
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulatorTest::class.java)
    private lateinit var emulator: EhlDispenserEmulator
    private lateinit var port: InMemorySerialPort
    private lateinit var communicator: EhlCommunicator
    
    @BeforeEach
    fun setup() {
        emulator = EhlDispenserEmulator(address = 1, pricePerLitreCents = 1590, litresPerSecond = 0.5)
        port = InMemorySerialPort(emulator)
        port.connect()
        communicator = EhlCommunicator(port)
    }
    
    @Test
    fun `STOP during pumping transitions to PAYMENT_PENDING and freezes totals`() = runBlocking {
        logger.info("=== Test: STOP during pumping -> PAYMENT_PENDING ===")
        
        // Start delivery
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive() // OK
        communicator.receive() // STATE
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive() // OK
        val stateAfterUnblock = communicator.receive() // STATE
        logger.info("State after UNBLOCK: ${stateAfterUnblock.data[0]}")
        
        // Let it pump for 1 second
        delay(1000)
        
        // Query volume before stop
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeBeforeStop = communicator.receive()
        val litresBeforeStop = parseVolume(volumeBeforeStop.data)
        logger.info("Volume before STOP: $litresBeforeStop L")
        assertTrue(litresBeforeStop > 0, "Should have pumped some fuel")
        
        // Send STOP
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive() // OK
        val stateAfterStop = communicator.receive() // STATE
        val volumeAfterStop = communicator.receive() // VOLUME
        
        // Verify PAYMENT_PENDING state (0x08)
        assertEquals(0x08.toByte(), stateAfterStop.data[0], "Should be in PAYMENT_PENDING state")
        logger.info("State after STOP: PAYMENT_PENDING (0x08)")
        
        // Verify totals are frozen
        val litresAfterStop = parseVolume(volumeAfterStop.data)
        logger.info("Volume after STOP: $litresAfterStop L")
        
        // Wait a bit more and verify totals don't change
        delay(500)
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeAfterWait = communicator.receive()
        val litresAfterWait = parseVolume(volumeAfterWait.data)
        
        assertEquals(litresAfterStop, litresAfterWait, "Totals should be frozen after STOP")
        logger.info("✅ Totals frozen: $litresAfterStop L = $litresAfterWait L")
    }
    
    @Test
    fun `UNBLOCK in PAYMENT_PENDING does not start new transaction`() = runBlocking {
        logger.info("=== Test: UNBLOCK in PAYMENT_PENDING denied ===")
        
        // Setup: Start, pump, then STOP
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive() // OK
        communicator.receive() // STATE
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive() // OK
        communicator.receive() // STATE
        
        delay(500)
        
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive() // OK
        val stateAfterStop = communicator.receive() // STATE
        communicator.receive() // VOLUME
        
        assertEquals(0x08.toByte(), stateAfterStop.data[0], "Should be PAYMENT_PENDING")
        
        // Get frozen totals
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeWhenPending = communicator.receive()
        val litresWhenPending = parseVolume(volumeWhenPending.data)
        logger.info("Frozen totals: $litresWhenPending L")
        
        // Try UNBLOCK (should be denied)
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val okResponse = communicator.receive()
        val stateResponse = communicator.receive()
        
        assertEquals(EhlCommand.OK, okResponse.command, "Should ACK the command")
        assertEquals(0x08.toByte(), stateResponse.data[0], "Should still be PAYMENT_PENDING")
        logger.info("UNBLOCK denied - still in PAYMENT_PENDING")
        
        // Verify totals haven't changed
        delay(500)
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeAfterUnblock = communicator.receive()
        val litresAfterUnblock = parseVolume(volumeAfterUnblock.data)
        
        assertEquals(litresWhenPending, litresAfterUnblock, "Totals should remain frozen")
        logger.info("✅ New transaction not started, totals still frozen")
    }
    
    @Test
    fun `markPaid resets to IDLE and clears totals`() = runBlocking {
        logger.info("=== Test: markPaid resets to IDLE ===")
        
        // Setup: Complete a transaction
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        communicator.receive()
        
        delay(500)
        
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive()
        communicator.receive()
        communicator.receive()
        
        // Verify PAYMENT_PENDING
        assertEquals(EmulatorState.PAYMENT_PENDING, emulator.getCurrentState())
        assertNotNull(emulator.getCurrentTransaction())
        
        // Mark as paid
        val marked = emulator.markTransactionPaid()
        assertTrue(marked, "Should successfully mark as paid")
        
        // Verify IDLE state
        assertEquals(EmulatorState.IDLE, emulator.getCurrentState())
        assertNull(emulator.getCurrentTransaction())
        logger.info("✅ Emulator reset to IDLE after payment")
        
        // Verify STATE query returns IDLE
        communicator.send(EhlPacket(1, EhlCommand.STATE))
        val stateResponse = communicator.receive()
        assertEquals(0x00.toByte(), stateResponse.data[0], "Should be IDLE (0x00)")
        
        // Verify totals are zeroed
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeResponse = communicator.receive()
        val litres = parseVolume(volumeResponse.data)
        assertEquals(0.0, litres, 0.01, "Totals should be zero after reset")
        logger.info("✅ Totals cleared to zero")
    }
    
    @Test
    fun `clear resets to IDLE without payment record`() = runBlocking {
        logger.info("=== Test: clear resets to IDLE ===")
        
        // Setup: Complete a transaction
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        communicator.receive()
        
        delay(500)
        
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive()
        communicator.receive()
        communicator.receive()
        
        // Clear transaction
        val cleared = emulator.clearTransaction()
        assertTrue(cleared, "Should successfully clear")
        
        // Verify IDLE state
        assertEquals(EmulatorState.IDLE, emulator.getCurrentState())
        assertNull(emulator.getCurrentTransaction())
        logger.info("✅ Transaction cleared, emulator reset to IDLE")
    }
    
    @Test
    fun `no simulation updates after STOP command`() = runBlocking {
        logger.info("=== Test: No updates after STOP ===")
        
        // Start delivery
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        communicator.receive()
        
        // Let it pump
        delay(800)
        
        // Get volume just before STOP
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeBeforeStop = communicator.receive()
        val litresBeforeStop = parseVolume(volumeBeforeStop.data)
        
        // Send STOP
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive()
        communicator.receive()
        val volumeRightAfterStop = communicator.receive()
        val litresRightAfterStop = parseVolume(volumeRightAfterStop.data)
        
        logger.info("Litres before STOP: $litresBeforeStop")
        logger.info("Litres right after STOP: $litresRightAfterStop")
        
        // Wait and check multiple times that volume doesn't change
        delay(200)
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volume1 = parseVolume(communicator.receive().data)
        
        delay(200)
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volume2 = parseVolume(communicator.receive().data)
        
        assertEquals(litresRightAfterStop, volume1, 0.01, "Volume should not change after STOP")
        assertEquals(litresRightAfterStop, volume2, 0.01, "Volume should not change after STOP")
        
        logger.info("✅ No simulation updates after STOP: $litresRightAfterStop L (frozen)")
    }
    
    @Test
    fun `new UNBLOCK after reset starts fresh transaction from zero`() = runBlocking {
        logger.info("=== Test: Fresh transaction after reset ===")
        
        // First transaction
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        communicator.receive()
        
        delay(500)
        
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        communicator.receive()
        communicator.receive()
        communicator.receive()
        
        // Reset
        emulator.markTransactionPaid()
        
        // Start new transaction
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        val stateAfterNewUnblock = communicator.receive()
        
        // Should be in DELIVERING state (VB6: 0x06 = START_BUTTON_PRESSED | OPEN_FOR_DELIVERY)
        assertEquals(0x06.toByte(), stateAfterNewUnblock.data[0], "Should be DELIVERING (0x06)")
        
        // Check volume starts from zero
        delay(200)
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val volumeNewTx = communicator.receive()
        val litresNewTx = parseVolume(volumeNewTx.data)
        
        assertTrue(litresNewTx < 0.2, "New transaction should start near zero, got $litresNewTx L")
        logger.info("✅ New transaction started from zero: $litresNewTx L")
    }
    
    @Test
    fun `atomic stop prevents race condition`() = runBlocking {
        logger.info("=== Test: Atomic stop mechanism ===")
        
        // Start delivery
        communicator.send(EhlPacket(1, EhlCommand.PRODUCT_SELECT, byteArrayOf(0x30)))
        communicator.receive()
        communicator.receive()
        
        communicator.send(EhlPacket(1, EhlCommand.UNBLOCK))
        communicator.receive()
        communicator.receive()
        
        // Let it pump briefly
        delay(300)
        
        // Send STOP and immediately query volume multiple times
        communicator.send(EhlPacket(1, EhlCommand.STOP))
        val okAfterStop = communicator.receive()
        val stateAfterStop = communicator.receive()
        val volumeAfterStop = communicator.receive()
        
        val litres1 = parseVolume(volumeAfterStop.data)
        
        // Query again immediately
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val litres2 = parseVolume(communicator.receive().data)
        
        // And again
        communicator.send(EhlPacket(1, EhlCommand.VOLUME))
        val litres3 = parseVolume(communicator.receive().data)
        
        // All should be exactly the same (no race condition)
        assertEquals(litres1, litres2, 0.001, "No race: volume queries should be identical")
        assertEquals(litres1, litres3, 0.001, "No race: volume queries should be identical")
        
        logger.info("✅ Atomic stop: all volume queries identical at $litres1 L")
    }
    
    @Test
    fun `LINETEST returns VB6-compatible magic bytes 0x55 0xAA`() = runBlocking {
        logger.info("=== Test: LINETEST VB6 compatibility ===")
        
        // Send LINETEST command
        communicator.send(EhlPacket(1, EhlCommand.LINETEST))
        val response = communicator.receive()
        
        // Verify response command and magic bytes
        assertEquals(EhlCommand.LINETEST, response.command, "Response should be LINETEST")
        assertEquals(2, response.data.size, "LINETEST response should have 2 bytes")
        assertEquals(0x55.toByte(), response.data[0], "First magic byte should be 0x55")
        assertEquals(0xAA.toByte(), response.data[1], "Second magic byte should be 0xAA")
        
        // Use LinetestValidator to verify
        val isValid = no.cloudberries.lpg.protocol.LinetestValidator.validateLinetestResponse(response.data)
        assertTrue(isValid, "LinetestValidator should confirm valid response")
        
        logger.info("✅ LINETEST response: 0x55 0xAA - communication line verified")
    }
    
    // Helper function to parse volume from VOLUME response (VB6 5-byte ASCII format)
    private fun parseVolume(data: ByteArray): Double {
        if (data.size < 5) return 0.0
        
        // VB6 format: 5 ASCII bytes LSB-first
        // Example: 45.50 L -> bytes ['0','5','5','4','0'] -> "04550" -> 45.50
        val d0 = (data[0].toInt() and 0xFF).toChar()
        val d1 = (data[1].toInt() and 0xFF).toChar()
        val d2 = (data[2].toInt() and 0xFF).toChar()
        val d3 = (data[3].toInt() and 0xFF).toChar()
        val d4 = (data[4].toInt() and 0xFF).toChar()
        
        val volumeString = "$d4$d3$d2$d1$d0"
        return try {
            volumeString.toInt() / 100.0
        } catch (e: NumberFormatException) {
            0.0
        }
    }
}
