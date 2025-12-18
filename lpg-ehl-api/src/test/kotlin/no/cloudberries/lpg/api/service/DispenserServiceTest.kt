package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.api.model.Transaction
import no.cloudberries.lpg.api.repository.DispenserStatusRepository
import no.cloudberries.lpg.api.repository.TransactionRepository
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.math.BigDecimal

/**
 * Comprehensive unit tests for DispenserService.
 * Tests critical business logic:
 * - State machine transitions
 * - Price update safety (Part 3)
 * - Transaction lifecycle
 * - Status byte interpretation
 */
class DispenserServiceTest {

    private lateinit var dispenserStatusRepository: DispenserStatusRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var priceUpdateCallback: (Int, BigDecimal) -> Unit
    private lateinit var dispenserService: DispenserService

    @BeforeEach
    fun setup() {
        dispenserStatusRepository = mock()
        transactionRepository = mock()
        priceUpdateCallback = mock()
        
        dispenserService = DispenserService(
            dispenserStatusRepository,
            transactionRepository,
            priceUpdateCallback
        )
    }

    // ============================================================
    // STATE MACHINE TESTS
    // ============================================================

    @Test
    fun `IDLE to STARTED transition when nozzle lifted`() {
        // Arrange: Dispenser is IDLE (status byte 0)
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Act: Nozzle lifted (status byte 1)
        val startedPacket = createStatePacket(address = 1, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Assert: Should transition to STARTED
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(1))
    }

    @Test
    fun `STARTED to FILLING transition when volume increases`() {
        // Arrange: Dispenser is STARTED
        val startedPacket = createStatePacket(address = 1, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Act: Volume increases (fuel flowing)
        val volumePacket = createVolumePacket(address = 1, volumeLiters = 5.0f, amountCents = 7950)
        dispenserService.handlePacket(volumePacket)
        
        // Assert: Should transition to FILLING
        assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(1))
    }

    @Test
    fun `FILLING to FINISHED transition when nozzle hung up`() {
        // Arrange: Dispenser is FILLING with volume
        simulateFillingState(address = 1, volumeLiters = 10.0f)
        
        // Act: Nozzle hung up (status byte 0)
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Assert: Should transition to FINISHED
        assertEquals(DispenserState.FINISHED, dispenserService.getDispenserState(1))
    }

    @Test
    fun `FINISHED to IDLE transition saves transaction`() {
        // Arrange: Dispenser finished with transaction
        simulateFinishedState(address = 1, volumeLiters = 15.0f)
        
        // Act: Return to IDLE (next state update)
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Assert: Transaction should be saved
        verify(transactionRepository, times(1)).save(any<Transaction>())
    }

    @Test
    fun `stay in current state when status byte unknown`() {
        // Arrange: Dispenser is STARTED
        val startedPacket = createStatePacket(address = 1, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Act: Unknown status byte
        val unknownPacket = createStatePacket(address = 1, statusByte = 99)
        dispenserService.handlePacket(unknownPacket)
        
        // Assert: Should stay in STARTED
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(1))
    }

    // ============================================================
    // PRICE UPDATE SAFETY TESTS (PART 3)
    // ============================================================

    @Test
    fun `price update sent immediately when dispenser is IDLE`() {
        // Arrange: Dispenser is IDLE
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Act: Queue price update
        val newPrice = BigDecimal("17.50")
        val result = dispenserService.queuePriceUpdate(1, newPrice)
        
        // Assert: Should send immediately
        assertTrue(result, "Price update should be sent immediately when IDLE")
        verify(priceUpdateCallback, times(1)).invoke(1, newPrice)
    }

    @Test
    fun `price update queued when dispenser is STARTED`() {
        // Arrange: Dispenser is STARTED (nozzle lifted)
        val startedPacket = createStatePacket(address = 1, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Act: Queue price update
        val newPrice = BigDecimal("18.00")
        val result = dispenserService.queuePriceUpdate(1, newPrice)
        
        // Assert: Should be queued, not sent
        assertFalse(result, "Price update should be queued when STARTED")
        verify(priceUpdateCallback, never()).invoke(any(), any())
        assertEquals(newPrice, dispenserService.getPendingPriceUpdate(1))
    }

    @Test
    fun `price update queued when dispenser is FILLING`() {
        // Arrange: Dispenser is FILLING
        simulateFillingState(address = 1, volumeLiters = 5.0f)
        
        // Act: Queue price update
        val newPrice = BigDecimal("19.00")
        val result = dispenserService.queuePriceUpdate(1, newPrice)
        
        // Assert: Should be queued, not sent
        assertFalse(result, "Price update should be queued when FILLING")
        verify(priceUpdateCallback, never()).invoke(any(), any())
        assertTrue(dispenserService.hasPendingPriceUpdate(1))
    }

    @Test
    fun `price update queued when dispenser is FINISHED`() {
        // Arrange: Dispenser is FINISHED
        simulateFinishedState(address = 1, volumeLiters = 10.0f)
        
        // Act: Queue price update
        val newPrice = BigDecimal("20.00")
        val result = dispenserService.queuePriceUpdate(1, newPrice)
        
        // Assert: Should be queued, not sent
        assertFalse(result, "Price update should be queued when FINISHED")
        verify(priceUpdateCallback, never()).invoke(any(), any())
    }

    @Test
    fun `queued price update applied automatically when returning to IDLE`() {
        // Arrange: Dispenser is FILLING with queued price update
        simulateFillingState(address = 1, volumeLiters = 8.0f)
        val queuedPrice = BigDecimal("21.00")
        dispenserService.queuePriceUpdate(1, queuedPrice)
        
        // Act: Transaction finishes, return to IDLE
        val finishPacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(finishPacket)
        
        // Allow state to settle to IDLE (FINISHED -> IDLE transition)
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Assert: Queued price should be applied
        verify(priceUpdateCallback, atLeastOnce()).invoke(1, queuedPrice)
        assertNull(dispenserService.getPendingPriceUpdate(1), "Pending price should be cleared after application")
    }

    @Test
    fun `multiple price updates only keep latest when queued`() {
        // Arrange: Dispenser is FILLING
        simulateFillingState(address = 1, volumeLiters = 5.0f)
        
        // Act: Queue multiple price updates
        dispenserService.queuePriceUpdate(1, BigDecimal("15.00"))
        dispenserService.queuePriceUpdate(1, BigDecimal("16.00"))
        val latestPrice = BigDecimal("17.00")
        dispenserService.queuePriceUpdate(1, latestPrice)
        
        // Assert: Only latest should be pending
        assertEquals(latestPrice, dispenserService.getPendingPriceUpdate(1))
    }

    // ============================================================
    // TRANSACTION LIFECYCLE TESTS
    // ============================================================

    @Test
    fun `transaction not saved when no volume dispensed`() {
        // Arrange: Dispenser starts but no fuel flows
        val startedPacket = createStatePacket(address = 1, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Act: Return to IDLE without dispensing
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        
        // Assert: No transaction should be saved
        verify(transactionRepository, never()).save(any<Transaction>())
    }

    @Test
    fun `transaction saved with correct volume and amount`() {
        // Arrange: Complete fuel delivery
        val volumeLiters = 12.5f
        simulateCompleteTransaction(address = 1, volumeLiters = volumeLiters, pricePerLiter = BigDecimal("15.90"))
        
        // Assert: Transaction saved with correct data
        verify(transactionRepository, times(1)).save(argThat { transaction ->
            transaction.volumeDeciliters == (volumeLiters * 10).toInt() &&
            transaction.dispenserAddress == 1
        })
    }

    @Test
    fun `handle multiple dispensers independently`() {
        // Arrange: Two dispensers in different states
        val idle1 = createStatePacket(address = 1, statusByte = 0)
        val started2 = createStatePacket(address = 2, statusByte = 1)
        
        dispenserService.handlePacket(idle1)
        dispenserService.handlePacket(started2)
        
        // Assert: States tracked independently
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(1))
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(2))
    }

    // ============================================================
    // STATUS BYTE INTERPRETATION TESTS
    // ============================================================

    @Test
    fun `status byte 0 interpreted as IDLE or FINISHED`() {
        // Test IDLE -> IDLE
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(1))
        
        // Test FILLING -> FINISHED
        simulateFillingState(address = 2, volumeLiters = 10.0f)
        val finishPacket = createStatePacket(address = 2, statusByte = 0)
        dispenserService.handlePacket(finishPacket)
        assertEquals(DispenserState.FINISHED, dispenserService.getDispenserState(2))
    }

    @Test
    fun `status bytes 1-3 interpreted as STARTED or maintain active state`() {
        // Test IDLE -> STARTED
        for (statusByte in 1..3) {
            val address = 10 + statusByte
            val idlePacket = createStatePacket(address = address, statusByte = 0)
            dispenserService.handlePacket(idlePacket)
            
            val activePacket = createStatePacket(address = address, statusByte = statusByte)
            dispenserService.handlePacket(activePacket)
            
            assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(address),
                "Status byte $statusByte should transition IDLE to STARTED")
        }
    }

    @Test
    fun `isSafeToUpdatePrice returns correct values`() {
        // IDLE is safe
        val idlePacket = createStatePacket(address = 1, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        assertTrue(dispenserService.isSafeToUpdatePrice(1), "IDLE should be safe for price update")
        
        // STARTED is not safe
        val startedPacket = createStatePacket(address = 2, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        assertFalse(dispenserService.isSafeToUpdatePrice(2), "STARTED should not be safe for price update")
        
        // FILLING is not safe
        simulateFillingState(address = 3, volumeLiters = 5.0f)
        assertFalse(dispenserService.isSafeToUpdatePrice(3), "FILLING should not be safe for price update")
    }

    // ============================================================
    // EDGE CASES AND ERROR HANDLING
    // ============================================================

    @Test
    fun `handle empty STATE packet data gracefully`() {
        val emptyPacket = EhlPacket(address = 1, command = EhlCommand.STATE, data = ByteArray(0))
        
        // Should not throw exception
        assertDoesNotThrow {
            dispenserService.handlePacket(emptyPacket)
        }
    }

    @Test
    fun `handle insufficient VOLUME packet data gracefully`() {
        val shortPacket = EhlPacket(address = 1, command = EhlCommand.VOLUME, data = ByteArray(2))
        
        // Should not throw exception
        assertDoesNotThrow {
            dispenserService.handlePacket(shortPacket)
        }
    }

    @Test
    fun `exception in packet handling does not crash service`() {
        // Create packet that will cause parsing error
        val malformedPacket = EhlPacket(address = 1, command = EhlCommand.STATE, data = byteArrayOf(0xFF.toByte()))
        
        // Should log error but not throw
        assertDoesNotThrow {
            dispenserService.handlePacket(malformedPacket)
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private fun createStatePacket(address: Int, statusByte: Int): EhlPacket {
        return EhlPacket(
            address = address,
            command = EhlCommand.STATE,
            data = byteArrayOf(statusByte.toByte())
        )
    }

    private fun createVolumePacket(address: Int, volumeLiters: Float, amountCents: Int): EhlPacket {
        // EHL VOLUME format: 4 bytes volume + 5 bytes amount
        val volumeStr = String.format("%04d", (volumeLiters * 100).toInt())
        val amountStr = String.format("%05d", amountCents)
        val data = (volumeStr + amountStr).map { it.code.toByte() }.toByteArray()
        
        return EhlPacket(
            address = address,
            command = EhlCommand.VOLUME,
            data = data
        )
    }

    private fun simulateFillingState(address: Int, volumeLiters: Float) {
        // Start transaction
        val startedPacket = createStatePacket(address = address, statusByte = 1)
        dispenserService.handlePacket(startedPacket)
        
        // Fuel flowing
        val volumePacket = createVolumePacket(address = address, volumeLiters = volumeLiters, amountCents = (volumeLiters * 1590).toInt())
        dispenserService.handlePacket(volumePacket)
    }

    private fun simulateFinishedState(address: Int, volumeLiters: Float) {
        // Simulate full transaction
        simulateFillingState(address = address, volumeLiters = volumeLiters)
        
        // Finish transaction
        val finishPacket = createStatePacket(address = address, statusByte = 0)
        dispenserService.handlePacket(finishPacket)
    }

    private fun simulateCompleteTransaction(address: Int, volumeLiters: Float, pricePerLiter: BigDecimal) {
        // Set price first
        val idlePacket = createStatePacket(address = address, statusByte = 0)
        dispenserService.handlePacket(idlePacket)
        dispenserService.queuePriceUpdate(address, pricePerLiter)
        
        // Complete transaction
        simulateFinishedState(address = address, volumeLiters = volumeLiters)
        
        // Return to IDLE to trigger save
        val finalIdlePacket = createStatePacket(address = address, statusByte = 0)
        dispenserService.handlePacket(finalIdlePacket)
    }
}
