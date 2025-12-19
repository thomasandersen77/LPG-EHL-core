package no.cloudberries.lpg.emulator

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for EHL emulator.
 * 
 * These tests verify the full EHL protocol stack by testing communication
 * between EhlCommunicator and EhlDispenserEmulator through InMemorySerialPort.
 * 
 * ## State Bit-Flags (VB6 Compatible):
 * - Bit 0 (0x01): START_SWITCH_ACTIVE - Pump ready/authorized
 * - Bit 1 (0x02): NOZZLE_LIFTED - Physical nozzle removed
 * - Bit 2 (0x04): DELIVERY_IN_PROGRESS - Fuel flowing
 * - Bit 3 (0x08): TRANSACTION_COMPLETE - Delivery finished
 * - Bit 7 (0x80): ERROR_FLAG - Hardware error
 * 
 * ## State Mapping:
 * - IDLE = 0x00 (all bits clear)
 * - AUTHORIZED = 0x01 (start switch only)
 * - PUMPING = 0x07 (start + nozzle + delivery)
 * - STOPPED = 0x08 (transaction complete)
 */
class EhlEmulatorIntegrationTest {

    private lateinit var emulator: EhlDispenserEmulator
    private lateinit var port: InMemorySerialPort
    private lateinit var comm: EhlCommunicator
    
    // VB6-compatible bit masks (duplicated here to avoid module dependency issues)
    companion object {
        const val START_SWITCH_ACTIVE = 0x01
        const val NOZZLE_LIFTED = 0x02
        const val DELIVERY_IN_PROGRESS = 0x04
        const val TRANSACTION_COMPLETE = 0x08
        const val ERROR_FLAG = 0x80
    }
    
    // Helper to check bit-flags
    private fun hasFlag(status: Int, mask: Int) = (status and mask) != 0

    @BeforeEach
    fun setup() {
        emulator = EhlDispenserEmulator(
            address = 1,
            pricePerLitreCents = 1000,  // 10.00 kr/l for easy calculation
            litresPerSecond = 1.0        // 1 liter per second
        )
        port = InMemorySerialPort(emulator)
        comm = EhlCommunicator(port)
        port.connect()
    }

    @AfterEach
    fun teardown() {
        port.disconnect()
    }

    @Test
    fun `should query initial state`() = runBlocking {
        // Send STATE query
        comm.send(EhlPacket(1, EhlCommand.STATE))
        val response = comm.receive()

        assertNotNull(response)
        assertEquals(EhlCommand.STATE, response.command)
        assertEquals(1, response.address)
        assertTrue(response.data.isNotEmpty())
        // State should be IDLE (0x00 - all flags clear)
        assertEquals(0x00, response.data[0].toInt() and 0xFF)
    }

    @Test
    fun `should handle UNBLOCK and start delivery`() = runBlocking {
        // Simulate nozzle lift BEFORE UNBLOCK (customer lifts nozzle)
        emulator.simulateNozzleLift(true)
        
        // Send UNBLOCK
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        
        // Should receive OK
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        // Should receive STATE with PUMPING flags (0x07 = start + nozzle + delivery)
        val stateResponse = comm.receive()
        assertEquals(EhlCommand.STATE, stateResponse.command)
        val status = stateResponse.data[0].toInt() and 0xFF
        assertTrue(hasFlag(status, DELIVERY_IN_PROGRESS), "Expected DELIVERY_IN_PROGRESS flag")
        assertTrue(hasFlag(status, NOZZLE_LIFTED), "Expected NOZZLE_LIFTED flag")
    }

    @Test
    fun `should complete full delivery cycle`() = runBlocking {
        // 1. Query initial state
        comm.send(EhlPacket(1, EhlCommand.STATE))
        val initialState = comm.receive()
        assertEquals(0x00, initialState.data[0].toInt() and 0xFF) // IDLE (all flags clear)

        // 2. Simulate nozzle lift and start delivery with UNBLOCK
        emulator.simulateNozzleLift(true)
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val ack1 = comm.receive()
        assertEquals(EhlCommand.OK, ack1.command)
        val state1 = comm.receive()
        val status1 = state1.data[0].toInt() and 0xFF
        assertTrue(hasFlag(status1, DELIVERY_IN_PROGRESS), "Expected PUMPING state")

        // 3. Wait for some fuel to be delivered (simulate time passing)
        delay(1500) // 1.5 seconds

        // 4. Stop delivery
        comm.send(EhlPacket(1, EhlCommand.STOP))
        val ack2 = comm.receive()
        assertEquals(EhlCommand.OK, ack2.command)
        
        val state2 = comm.receive()
        val status2 = state2.data[0].toInt() and 0xFF
        assertTrue(hasFlag(status2, TRANSACTION_COMPLETE), "Expected STOPPED state")
        
        // Should also receive VOLUME response
        val volume = comm.receive()
        assertEquals(EhlCommand.VOLUME, volume.command)
        assertEquals(4, volume.data.size)
        
        // Parse volume and amount
        val volDeci = ((volume.data[0].toInt() and 0xFF) shl 8) or (volume.data[1].toInt() and 0xFF)
        val amount = ((volume.data[2].toInt() and 0xFF) shl 8) or (volume.data[3].toInt() and 0xFF)
        
        // Should have delivered approximately 1.5 liters
        val litres = volDeci / 10.0
        assertTrue(litres > 1.0 && litres < 2.0, "Expected ~1.5L, got $litres L")
        
        // At 10 kr/l, 1.5L should cost ~1500 øre
        assertTrue(amount > 1000 && amount < 2000, "Expected ~1500 øre, got $amount øre")
        
        println("Delivered: $litres L for $amount øre")
    }

    @Test
    fun `should query volume during delivery`() = runBlocking {
        // Simulate nozzle lift and start delivery
        emulator.simulateNozzleLift(true)
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        comm.receive() // OK
        comm.receive() // STATE

        // Wait a bit
        delay(500)

        // Query volume while delivering
        comm.send(EhlPacket(1, EhlCommand.VOLUME))
        val volume = comm.receive()
        
        assertEquals(EhlCommand.VOLUME, volume.command)
        
        val volDeci = ((volume.data[0].toInt() and 0xFF) shl 8) or (volume.data[1].toInt() and 0xFF)
        val litres = volDeci / 10.0
        
        // Should have delivered approximately 0.5 liters
        assertTrue(litres > 0.3 && litres < 0.7, "Expected ~0.5L, got $litres L")
        
        println("Mid-delivery volume: $litres L")
    }

    @Test
    fun `should handle multiple delivery cycles`() = runBlocking {
        // First delivery
        emulator.simulateNozzleLift(true)
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        comm.receive() // OK
        comm.receive() // STATE
        delay(500)
        comm.send(EhlPacket(1, EhlCommand.STOP))
        comm.receive() // OK
        comm.receive() // STATE
        comm.receive() // VOLUME

        // Second delivery - need to lift nozzle again (STOP resets nozzle state)
        emulator.simulateNozzleLift(true)
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        val state = comm.receive()
        val status = state.data[0].toInt() and 0xFF
        assertTrue(hasFlag(status, DELIVERY_IN_PROGRESS), "Expected PUMPING state for second delivery")
        
        delay(500)
        comm.send(EhlPacket(1, EhlCommand.STOP))
        comm.receive() // OK
        comm.receive() // STATE
        val volume2 = comm.receive() // VOLUME
        
        assertEquals(EhlCommand.VOLUME, volume2.command)
    }

    @Test
    fun `should handle wrong address`() = runBlocking {
        // Send to wrong address (2 instead of 1)
        comm.send(EhlPacket(2, EhlCommand.STATE))
        
        // Emulator should ignore it, so receive buffer should timeout or be empty
        delay(100)
        
        // Try to read - should be nothing
        val bufferSize = comm.getBufferSize()
        assertEquals(0, bufferSize, "Emulator should ignore packets with wrong address")
    }
    
    @Test
    fun `should handle BLOCK command`() = runBlocking {
        // Simulate nozzle lift and start delivery
        emulator.simulateNozzleLift(true)
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        comm.receive() // OK
        comm.receive() // STATE
        
        delay(500)
        
        // Send BLOCK to stop
        comm.send(EhlPacket(1, EhlCommand.BLOCK))
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        val state = comm.receive()
        assertEquals(EhlCommand.STATE, state.command)
        val status = state.data[0].toInt() and 0xFF
        assertTrue(hasFlag(status, TRANSACTION_COMPLETE), "Expected STOPPED state")
    }
    
    @Test
    fun `should handle PRICE query`() = runBlocking {
        // Query current price
        comm.send(EhlPacket(1, EhlCommand.PRICE))
        val priceResponse = comm.receive()
        
        assertEquals(EhlCommand.PRICE, priceResponse.command)
        assertEquals(4, priceResponse.data.size)
        
        // Price should be 10.00 (from setup)
        // Encoded as ASCII '0', '0', '0', '1' (reversed)
        assertEquals('0'.code.toByte(), priceResponse.data[0])
        assertEquals('0'.code.toByte(), priceResponse.data[1])
    }
    
    @Test
    fun `should handle PROG_PRC command`() = runBlocking {
        // Program new price: 15.90 kr/l
        // Encoded as ASCII '0', '9', '5', '1' (reversed)
        val priceData = byteArrayOf(
            '0'.code.toByte(),
            '9'.code.toByte(),
            '5'.code.toByte(),
            '1'.code.toByte()
        )
        
        comm.send(EhlPacket(1, EhlCommand.PROG_PRC, priceData))
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        val priceResponse = comm.receive()
        assertEquals(EhlCommand.PRICE, priceResponse.command)
        
        // Verify price was updated
        assertEquals('0'.code.toByte(), priceResponse.data[0])
        assertEquals('9'.code.toByte(), priceResponse.data[1])
        assertEquals('5'.code.toByte(), priceResponse.data[2])
        assertEquals('1'.code.toByte(), priceResponse.data[3])
    }
    
    @Test
    fun `should handle LINETEST command`() = runBlocking {
        comm.send(EhlPacket(1, EhlCommand.LINETEST))
        val response = comm.receive()
        
        assertEquals(EhlCommand.OK, response.command)
    }
    
    @Test
    fun `should handle ZER reset command`() = runBlocking {
        // Start delivery
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        comm.receive() // OK
        comm.receive() // STATE
        
        delay(500)
        
        // Reset
        comm.send(EhlPacket(1, EhlCommand.ZER))
        val ack = comm.receive()
        assertEquals(EhlCommand.ZER, ack.command)
        
        val state = comm.receive()
        assertEquals(EhlCommand.STATE, state.command)
        assertEquals(0, state.data[0].toInt()) // Should be IDLE after reset
    }
}
