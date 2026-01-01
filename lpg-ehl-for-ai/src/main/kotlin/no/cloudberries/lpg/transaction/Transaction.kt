package no.cloudberries.lpg.transaction

import java.time.Instant

/**
 * Transaction State
 * 
 * Represents the lifecycle of a dispenser transaction.
 */
enum class TransactionState(val code: Int, val description: String) {
    /** Transaction not started */
    NOT_STARTED(0, "Not started"),
    
    /** Dispenser ready for delivery */
    READY(1, "Ready"),
    
    /** Delivery in progress */
    ACTIVE(2, "Active"),
    
    /** Delivery finished */
    FINISHED(3, "Finished"),
    
    /** Transaction unaccounted for (power failure during transaction) */
    UNACCOUNTED(4, "Unaccounted"),
    
    /** Financial return in progress */
    FINANCIAL_RETURN(5, "Financial return"),
    
    /** Financial technical return */
    FINANCIAL_TECH_RETURN(6, "Financial technical return"),
    
    /** Transaction annulated/cancelled */
    ANNULATED(7, "Annulated"),
    
    /** Transaction accounted for */
    ACCOUNTED(8, "Accounted");
    
    companion object {
        fun fromCode(code: Int): TransactionState? {
            return entries.find { it.code == code }
        }
    }
}

/**
 * Payment Type
 */
enum class PaymentType(val code: Int, val description: String) {
    /** Default/undefined */
    DEFAULT(0, "Default"),
    
    /** Cash payment */
    CASH(1, "Cash"),
    
    /** Bank card payment */
    BANK_CARD(2, "Bank card"),
    
    /** Station credit card */
    STATION_CARD(3, "Station card");
    
    companion object {
        fun fromCode(code: Int): PaymentType {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}

/**
 * Transaction
 * 
 * Represents a complete fuel dispenser transaction with payment and delivery details.
 * 
 * @property id Unique transaction identifier
 * @property dispenserAddress Dispenser address (1-255)
 * @property state Current transaction state
 * @property paymentType Type of payment
 * @property presetAmount Preset amount in øre/cents (if any)
 * @property deliveredVolume Delivered volume in liters
 * @property deliveredAmount Delivered amount in øre/cents
 * @property unitPrice Unit price in øre/cents per liter
 * @property cashbackAmount Cashback amount in øre/cents
 * @property startTime Transaction start timestamp
 * @property endTime Transaction end timestamp
 */
data class Transaction(
    val id: String,
    val dispenserAddress: Int,
    var state: TransactionState = TransactionState.NOT_STARTED,
    var paymentType: PaymentType = PaymentType.DEFAULT,
    var presetAmount: Int = 0,
    var deliveredVolume: Float = 0.0f,
    var deliveredAmount: Int = 0,
    var unitPrice: Float = 0.0f,
    var cashbackAmount: Int = 0,
    val startTime: Instant = Instant.now(),
    var endTime: Instant? = null
) {
    /**
     * Check if transaction is in a final state
     */
    fun isFinalized(): Boolean {
        return state in listOf(
            TransactionState.FINISHED,
            TransactionState.ACCOUNTED,
            TransactionState.ANNULATED
        )
    }
    
    /**
     * Check if transaction can be started
     */
    fun canStart(): Boolean {
        return state == TransactionState.NOT_STARTED || state == TransactionState.READY
    }
    
    /**
     * Check if transaction is active
     */
    fun isActive(): Boolean {
        return state == TransactionState.ACTIVE
    }
    
    /**
     * Transition to a new state
     * 
     * @param newState The target state
     * @return true if transition was valid, false otherwise
     */
    fun transitionTo(newState: TransactionState): Boolean {
        val validTransitions = when (state) {
            TransactionState.NOT_STARTED -> listOf(TransactionState.READY)
            TransactionState.READY -> listOf(TransactionState.ACTIVE, TransactionState.ANNULATED)
            TransactionState.ACTIVE -> listOf(TransactionState.FINISHED, TransactionState.UNACCOUNTED)
            TransactionState.FINISHED -> listOf(TransactionState.ACCOUNTED, TransactionState.FINANCIAL_RETURN)
            TransactionState.UNACCOUNTED -> listOf(TransactionState.ACCOUNTED)
            TransactionState.FINANCIAL_RETURN -> listOf(TransactionState.ACCOUNTED)
            TransactionState.FINANCIAL_TECH_RETURN -> listOf(TransactionState.ACCOUNTED)
            TransactionState.ANNULATED -> emptyList()
            TransactionState.ACCOUNTED -> emptyList()
        }
        
        return if (newState in validTransitions) {
            state = newState
            if (newState == TransactionState.FINISHED && endTime == null) {
                endTime = Instant.now()
            }
            true
        } else {
            false
        }
    }
    
    override fun toString(): String {
        return "Transaction(id=$id, addr=$dispenserAddress, state=${state.name}, " +
               "payment=${paymentType.name}, volume=$deliveredVolume L, " +
               "amount=${deliveredAmount / 100.0} kr)"
    }
}

/**
 * Transaction Manager
 * 
 * Manages active transactions and state transitions.
 */
class TransactionManager {
    private val activeTransactions = mutableMapOf<Int, Transaction>()
    
    /**
     * Start a new transaction for a dispenser
     * 
     * @param dispenserAddress Dispenser address
     * @return The created transaction
     */
    fun startTransaction(dispenserAddress: Int): Transaction {
        require(dispenserAddress in 1..255) { "Invalid dispenser address" }
        
        val transaction = Transaction(
            id = generateTransactionId(),
            dispenserAddress = dispenserAddress
        )
        transaction.transitionTo(TransactionState.READY)
        activeTransactions[dispenserAddress] = transaction
        
        return transaction
    }
    
    /**
     * Get active transaction for a dispenser
     */
    fun getTransaction(dispenserAddress: Int): Transaction? {
        return activeTransactions[dispenserAddress]
    }
    
    /**
     * Remove transaction when finalized
     */
    fun finalizeTransaction(dispenserAddress: Int): Transaction? {
        return activeTransactions.remove(dispenserAddress)
    }
    
    /**
     * Get all active transactions
     */
    fun getAllActiveTransactions(): List<Transaction> {
        return activeTransactions.values.toList()
    }
    
    private fun generateTransactionId(): String {
        return "TXN-${System.currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
    }
}
