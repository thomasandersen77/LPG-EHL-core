package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.service.pump.DispenserService
import no.cloudberries.lpg.service.pump.DispenserState
import no.cloudberries.lpg.service.pump.DispenserStatusRepository
import no.cloudberries.lpg.service.transaction.Transaction
import no.cloudberries.lpg.service.transaction.TransactionRepository
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
        
        // Stub transactionRepository.save() to return the saved transaction
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { invocation ->
            invocation.getArgument<Transaction>(0)
        }
        
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

    @Test
    fun `complex state machine scenario - complete fuel delivery cycle`() {
        val address = 1
        val finalVolumeLiters = 25.5f
        val pricePerLiter = BigDecimal("16.75")
        
        // 1. Set initial price when idle
        val idlePacket = createStatePacket(address, 0)
        dispenserService.handlePacket(idlePacket)
        dispenserService.queuePriceUpdate(address, pricePerLiter)
        
        // 2. Customer lifts nozzle (IDLE -> STARTED)
        val startedPacket = createStatePacket(address, 1)
        dispenserService.handlePacket(startedPacket)
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(address))
        
        // 3. First volume pulse (STARTED -> FILLING)
        val volumePacket1 = createVolumePacket(address, 2.0f, 3350)
        dispenserService.handlePacket(volumePacket1)
        assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(address))
        
        // 4. Multiple volume updates during filling
        for (volume in listOf(5.0f, 10.0f, 15.0f, 20.0f, finalVolumeLiters)) {
            val volumeUpdate = createVolumePacket(address, volume, (volume * pricePerLiter.toFloat() * 100).toInt())
            dispenserService.handlePacket(volumeUpdate)
            assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(address))
        }
        
        // 5. Customer hangs up nozzle (FILLING -> FINISHED)
        val finishedPacket = createStatePacket(address, 0)
        dispenserService.handlePacket(finishedPacket)
        assertEquals(DispenserState.FINISHED, dispenserService.getDispenserState(address))
        
        // 6. Return to idle triggers transaction save (FINISHED -> IDLE)
        val finalIdlePacket = createStatePacket(address, 0)
        dispenserService.handlePacket(finalIdlePacket)
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(address))
        
        // Assert: Transaction should be saved exactly once with correct details
        verify(transactionRepository, times(1)).save(argThat { transaction ->
            transaction.volumeDeciliters == (finalVolumeLiters * 10).toInt() &&
            transaction.dispenserAddress == address &&
            transaction.amountOre == (finalVolumeLiters * pricePerLiter.toFloat() * 100).toInt()
        })
    }
    
    @Test
    fun `price update safety - prevent mid-transaction price changes`() {
        val address = 1
        
        // Start with idle dispenser
        val idlePacket = createStatePacket(address, 0)
        dispenserService.handlePacket(idlePacket)
        
        // Set initial price (should work)
        val initialPrice = BigDecimal("15.90")
        assertTrue(dispenserService.queuePriceUpdate(address, initialPrice))
        verify(priceUpdateCallback, times(1)).invoke(address, initialPrice)
        
        // Customer lifts nozzle (IDLE -> STARTED)
        val startedPacket = createStatePacket(address, 1)
        dispenserService.handlePacket(startedPacket)
        
        // Try to change price during STARTED state (should be queued)
        val newPrice = BigDecimal("17.50")
        assertFalse(dispenserService.queuePriceUpdate(address, newPrice))
        assertEquals(newPrice, dispenserService.getPendingPriceUpdate(address))
        
        // Start filling
        val volumePacket = createVolumePacket(address, 5.0f, 7950)
        dispenserService.handlePacket(volumePacket)
        assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(address))
        
        // Try to change price during FILLING (should update the queued price)
        val newerPrice = BigDecimal("18.00")
        assertFalse(dispenserService.queuePriceUpdate(address, newerPrice))
        assertEquals(newerPrice, dispenserService.getPendingPriceUpdate(address))
        
        // Finish transaction
        val finishPacket = createStatePacket(address, 0)
        dispenserService.handlePacket(finishPacket)
        assertEquals(DispenserState.FINISHED, dispenserService.getDispenserState(address))
        
        // Try to change price during FINISHED (should still be queued)
        val finalPrice = BigDecimal("19.25")
        assertFalse(dispenserService.queuePriceUpdate(address, finalPrice))
        assertEquals(finalPrice, dispenserService.getPendingPriceUpdate(address))
        
        // Return to IDLE should apply the queued price
        val idleAgainPacket = createStatePacket(address, 0)
        dispenserService.handlePacket(idleAgainPacket)
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(address))
        
        // Verify the final price was sent to hardware
        verify(priceUpdateCallback, times(1)).invoke(address, finalPrice)
        assertNull(dispenserService.getPendingPriceUpdate(address))
    }
    
    @Test
    fun `multiple dispenser independence - concurrent transactions`() {
        val dispenser1 = 1
        val dispenser2 = 2
        val price1 = BigDecimal("15.90")
        val price2 = BigDecimal("16.50")
        
        // Set up both dispensers as idle with different prices
        dispenserService.handlePacket(createStatePacket(dispenser1, 0))
        dispenserService.handlePacket(createStatePacket(dispenser2, 0))
        dispenserService.queuePriceUpdate(dispenser1, price1)
        dispenserService.queuePriceUpdate(dispenser2, price2)
        
        // Dispenser 1: Start transaction
        dispenserService.handlePacket(createStatePacket(dispenser1, 1))
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(dispenser1))
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(dispenser2))
        
        // Dispenser 2: Should still be safe for price updates
        assertTrue(dispenserService.isSafeToUpdatePrice(dispenser2))
        assertFalse(dispenserService.isSafeToUpdatePrice(dispenser1))
        
        // Dispenser 1: Start filling
        dispenserService.handlePacket(createVolumePacket(dispenser1, 5.0f, 7950))
        assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(dispenser1))
        
        // Dispenser 2: Start transaction while 1 is filling
        dispenserService.handlePacket(createStatePacket(dispenser2, 2))
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(dispenser2))
        assertEquals(DispenserState.FILLING, dispenserService.getDispenserState(dispenser1))
        
        // Both dispensers now unsafe for price updates
        assertFalse(dispenserService.isSafeToUpdatePrice(dispenser1))
        assertFalse(dispenserService.isSafeToUpdatePrice(dispenser2))
        
        // Finish dispenser 1 transaction
        dispenserService.handlePacket(createVolumePacket(dispenser1, 10.0f, 15900))
        dispenserService.handlePacket(createStatePacket(dispenser1, 0))
        dispenserService.handlePacket(createStatePacket(dispenser1, 0))
        
        // Dispenser 1 should be idle, 2 still started
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(dispenser1))
        assertEquals(DispenserState.STARTED, dispenserService.getDispenserState(dispenser2))
        
        // Verify independent transaction saving
        verify(transactionRepository, times(1)).save(argThat { transaction ->
            transaction.dispenserAddress == dispenser1 && 
            transaction.volumeDeciliters == 100
        })
    }
    
    @Test
    fun `transaction not saved for zero volume deliveries`() {
        val address = 1
        
        // Complete transaction cycle without any fuel delivery
        dispenserService.handlePacket(createStatePacket(address, 0)) // IDLE
        dispenserService.handlePacket(createStatePacket(address, 1)) // STARTED
        dispenserService.handlePacket(createStatePacket(address, 0)) // Back to IDLE without volume
        dispenserService.handlePacket(createStatePacket(address, 0)) // Still IDLE
        
        // No transaction should be saved
        verify(transactionRepository, never()).save(any<Transaction>())
        assertEquals(DispenserState.IDLE, dispenserService.getDispenserState(address))
    }
    
    @Test
    fun `accurate volume and amount calculation in transactions`() {
        val address = 1
        val volumeLiters = 12.345f
        val pricePerLiter = BigDecimal("17.85")
        val expectedVolumeDeciliters = (volumeLiters * 10).toInt() // 123
        // Service calculates amountOre from volumeDeciliters, not original volumeLiters
        // This matches the precision available in the protocol (deciliters)
        val volumeLitersFromDeciliters = expectedVolumeDeciliters / 10.0
        val expectedAmountOre = (BigDecimal(volumeLitersFromDeciliters) * pricePerLiter * BigDecimal(100)).toInt() // 21955
        
        // Set price and complete transaction
        dispenserService.handlePacket(createStatePacket(address, 0))
        dispenserService.queuePriceUpdate(address, pricePerLiter)
        
        // Full transaction
        dispenserService.handlePacket(createStatePacket(address, 1))
        dispenserService.handlePacket(createVolumePacket(address, volumeLiters, expectedAmountOre))
        dispenserService.handlePacket(createStatePacket(address, 0))
        dispenserService.handlePacket(createStatePacket(address, 0))
        
        // Verify exact amounts
        verify(transactionRepository, times(1)).save(argThat { transaction ->
            transaction.volumeDeciliters == expectedVolumeDeciliters &&
            transaction.amountOre == expectedAmountOre &&
            transaction.pricePerLiter?.compareTo(pricePerLiter) == 0
        })
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
        // VB6 VOLUME format: 5 ASCII digit bytes in LSB-first order
        // Example: 45.50 liters -> "04550" -> bytes ['0','5','5','4','0'] (LSB first)
        val volumeCentiliters = (volumeLiters * 100).toInt()  // 45.50L -> 4550
        val volumeString = volumeCentiliters.toString().padStart(5, '0')  // "04550"
        
        val data = ByteArray(5)
        // LSB-first: data[0]=0.01L, data[1]=0.1L, data[2]=1L, data[3]=10L, data[4]=100L
        for (i in 0..4) {
            data[i] = volumeString[4 - i].code.toByte()  // Reverse order
        }
        
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
