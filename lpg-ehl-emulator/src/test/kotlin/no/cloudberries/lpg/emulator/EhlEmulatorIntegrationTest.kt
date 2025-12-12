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
 */
class EhlEmulatorIntegrationTest {

    private lateinit var emulator: EhlDispenserEmulator
    private lateinit var port: InMemorySerialPort
    private lateinit var comm: EhlCommunicator

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
        // State should be IDLE (0)
        assertEquals(0, response.data[0].toInt())
    }

    @Test
    fun `should handle UNBLOCK and start delivery`() = runBlocking {
        // Send UNBLOCK
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        
        // Should receive OK
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        // Should receive STATE
        val stateResponse = comm.receive()
        assertEquals(EhlCommand.STATE, stateResponse.command)
        // State should be DELIVERING (2)
        assertEquals(2, stateResponse.data[0].toInt())
    }

    @Test
    fun `should complete full delivery cycle`() = runBlocking {
        // 1. Query initial state
        comm.send(EhlPacket(1, EhlCommand.STATE))
        val initialState = comm.receive()
        assertEquals(0, initialState.data[0].toInt()) // IDLE

        // 2. Start delivery with UNBLOCK
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val ack1 = comm.receive()
        assertEquals(EhlCommand.OK, ack1.command)
        val state1 = comm.receive()
        assertEquals(2, state1.data[0].toInt()) // DELIVERING

        // 3. Wait for some fuel to be delivered (simulate time passing)
        delay(1500) // 1.5 seconds

        // 4. Stop delivery
        comm.send(EhlPacket(1, EhlCommand.STOP))
        val ack2 = comm.receive()
        assertEquals(EhlCommand.OK, ack2.command)
        
        val state2 = comm.receive()
        assertEquals(3, state2.data[0].toInt()) // FINISHED
        
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
        // Start delivery
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
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        comm.receive() // OK
        comm.receive() // STATE
        delay(500)
        comm.send(EhlPacket(1, EhlCommand.STOP))
        comm.receive() // OK
        comm.receive() // STATE
        comm.receive() // VOLUME

        // Second delivery
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val ack = comm.receive()
        assertEquals(EhlCommand.OK, ack.command)
        
        val state = comm.receive()
        assertEquals(2, state.data[0].toInt()) // Should be DELIVERING again
        
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
}
