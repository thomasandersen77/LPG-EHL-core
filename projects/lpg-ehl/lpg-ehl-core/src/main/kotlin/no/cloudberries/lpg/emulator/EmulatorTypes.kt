package no.cloudberries.lpg.emulator

/**
 * Represents an active transaction being simulated.
 * Values are updated by DispenserSimulator during delivery.
 */
data class ActiveTransaction(
    val startMs: Long,
    var volumeLitres: Double = 0.0,
    var amountCents: Int = 0
)

/**
 * Represents a completed transaction with frozen totals.
 * Created when STOP/BLOCK is received during delivery.
 */
data class CompletedTransaction(
    val id: String,
    val volumeLitres: Double,
    val amountCents: Int,
    val unitPriceCents: Int,
    val startedAt: Long,
    val stoppedAt: Long
)

/**
 * Emulator state machine.
 */
enum class EmulatorState {
    IDLE,           // No transaction, ready to start
    AUTHORIZED,     // Product selected, ready for UNBLOCK
    DELIVERING,     // Active delivery in progress
    PAYMENT_PENDING // Transaction complete, totals frozen, awaiting reset
}
