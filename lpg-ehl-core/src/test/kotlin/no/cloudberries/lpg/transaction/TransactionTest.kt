package no.cloudberries.lpg.transaction

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach

@DisplayName("Transaction Tests")
class TransactionTest {
    
    private lateinit var transaction: Transaction
    
    @BeforeEach
    fun setup() {
        transaction = Transaction(
            id = "TEST-001",
            dispenserAddress = 1
        )
    }
    
    @Test
    @DisplayName("New transaction starts in NOT_STARTED state")
    fun testInitialState() {
        assertEquals(TransactionState.NOT_STARTED, transaction.state)
        assertFalse(transaction.isFinalized())
        assertTrue(transaction.canStart())
        assertFalse(transaction.isActive())
    }
    
    @Test
    @DisplayName("Transition from NOT_STARTED to READY")
    fun testTransitionToReady() {
        assertTrue(transaction.transitionTo(TransactionState.READY))
        assertEquals(TransactionState.READY, transaction.state)
        assertTrue(transaction.canStart())
    }
    
    @Test
    @DisplayName("Transition from READY to ACTIVE")
    fun testTransitionToActive() {
        transaction.transitionTo(TransactionState.READY)
        
        assertTrue(transaction.transitionTo(TransactionState.ACTIVE))
        assertEquals(TransactionState.ACTIVE, transaction.state)
        assertTrue(transaction.isActive())
        assertFalse(transaction.canStart())
    }
    
    @Test
    @DisplayName("Transition from ACTIVE to FINISHED")
    fun testTransitionToFinished() {
        transaction.transitionTo(TransactionState.READY)
        transaction.transitionTo(TransactionState.ACTIVE)
        
        assertNull(transaction.endTime)
        assertTrue(transaction.transitionTo(TransactionState.FINISHED))
        assertEquals(TransactionState.FINISHED, transaction.state)
        assertTrue(transaction.isFinalized())
        assertNotNull(transaction.endTime)
    }
    
    @Test
    @DisplayName("Invalid state transition fails")
    fun testInvalidTransition() {
        // Cannot go directly from NOT_STARTED to ACTIVE
        assertFalse(transaction.transitionTo(TransactionState.ACTIVE))
        assertEquals(TransactionState.NOT_STARTED, transaction.state)
    }
    
    @Test
    @DisplayName("Cannot transition from ACCOUNTED state")
    fun testCannotTransitionFromAccounted() {
        transaction.transitionTo(TransactionState.READY)
        transaction.transitionTo(TransactionState.ACTIVE)
        transaction.transitionTo(TransactionState.FINISHED)
        transaction.transitionTo(TransactionState.ACCOUNTED)
        
        // ACCOUNTED is final - no transitions allowed
        assertFalse(transaction.transitionTo(TransactionState.READY))
        assertFalse(transaction.transitionTo(TransactionState.ACTIVE))
        assertEquals(TransactionState.ACCOUNTED, transaction.state)
    }
    
    @Test
    @DisplayName("Transition from READY to ANNULATED")
    fun testAnnulateFromReady() {
        transaction.transitionTo(TransactionState.READY)
        
        assertTrue(transaction.transitionTo(TransactionState.ANNULATED))
        assertEquals(TransactionState.ANNULATED, transaction.state)
        assertTrue(transaction.isFinalized())
    }
    
    @Test
    @DisplayName("Payment type enum resolution")
    fun testPaymentType() {
        assertEquals(PaymentType.DEFAULT, PaymentType.fromCode(0))
        assertEquals(PaymentType.CASH, PaymentType.fromCode(1))
        assertEquals(PaymentType.BANK_CARD, PaymentType.fromCode(2))
        assertEquals(PaymentType.STATION_CARD, PaymentType.fromCode(3))
        assertEquals(PaymentType.DEFAULT, PaymentType.fromCode(99)) // Unknown code
    }
    
    @Test
    @DisplayName("Transaction state enum resolution")
    fun testTransactionStateFromCode() {
        assertEquals(TransactionState.NOT_STARTED, TransactionState.fromCode(0))
        assertEquals(TransactionState.READY, TransactionState.fromCode(1))
        assertEquals(TransactionState.ACTIVE, TransactionState.fromCode(2))
        assertEquals(TransactionState.FINISHED, TransactionState.fromCode(3))
        assertNull(TransactionState.fromCode(99)) // Unknown code
    }
    
    @Test
    @DisplayName("Transaction with delivery details")
    fun testTransactionWithDetails() {
        transaction.deliveredVolume = 45.5f
        transaction.deliveredAmount = 72950 // 729.50 kr
        transaction.unitPrice = 16.04f
        transaction.paymentType = PaymentType.BANK_CARD
        
        assertEquals(45.5f, transaction.deliveredVolume, 0.01f)
        assertEquals(72950, transaction.deliveredAmount)
        assertEquals(PaymentType.BANK_CARD, transaction.paymentType)
        
        val str = transaction.toString()
        assertTrue(str.contains("45.5"))
        assertTrue(str.contains("BANK_CARD"))
    }
}

@DisplayName("Transaction Manager Tests")
class TransactionManagerTest {
    
    private lateinit var manager: TransactionManager
    
    @BeforeEach
    fun setup() {
        manager = TransactionManager()
    }
    
    @Test
    @DisplayName("Start new transaction")
    fun testStartTransaction() {
        val transaction = manager.startTransaction(1)
        
        assertNotNull(transaction)
        assertEquals(1, transaction.dispenserAddress)
        assertEquals(TransactionState.READY, transaction.state)
        assertNotNull(transaction.id)
    }
    
    @Test
    @DisplayName("Get active transaction")
    fun testGetTransaction() {
        val created = manager.startTransaction(1)
        val retrieved = manager.getTransaction(1)
        
        assertNotNull(retrieved)
        assertEquals(created.id, retrieved?.id)
    }
    
    @Test
    @DisplayName("Get non-existent transaction returns null")
    fun testGetNonExistentTransaction() {
        assertNull(manager.getTransaction(99))
    }
    
    @Test
    @DisplayName("Finalize transaction removes it from active list")
    fun testFinalizeTransaction() {
        manager.startTransaction(1)
        
        val finalized = manager.finalizeTransaction(1)
        assertNotNull(finalized)
        
        // Should no longer be in active list
        assertNull(manager.getTransaction(1))
    }
    
    @Test
    @DisplayName("Get all active transactions")
    fun testGetAllActiveTransactions() {
        manager.startTransaction(1)
        manager.startTransaction(2)
        manager.startTransaction(3)
        
        val active = manager.getAllActiveTransactions()
        assertEquals(3, active.size)
        
        manager.finalizeTransaction(2)
        
        val activeAfter = manager.getAllActiveTransactions()
        assertEquals(2, activeAfter.size)
    }
    
    @Test
    @DisplayName("Invalid dispenser address throws exception")
    fun testInvalidDispenserAddress() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.startTransaction(0)
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            manager.startTransaction(256)
        }
    }
    
    @Test
    @DisplayName("Multiple transactions for different dispensers")
    fun testMultipleTransactions() {
        val txn1 = manager.startTransaction(1)
        val txn2 = manager.startTransaction(2)
        
        assertNotEquals(txn1.id, txn2.id)
        assertEquals(1, txn1.dispenserAddress)
        assertEquals(2, txn2.dispenserAddress)
        
        assertEquals(txn1.id, manager.getTransaction(1)?.id)
        assertEquals(txn2.id, manager.getTransaction(2)?.id)
    }
}
