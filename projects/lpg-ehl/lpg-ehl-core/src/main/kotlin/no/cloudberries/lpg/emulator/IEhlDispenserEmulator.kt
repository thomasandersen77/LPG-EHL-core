package no.cloudberries.lpg.emulator

/**
 * Interface for EHL Dispenser Emulator - simulates a real LPG dispenser.
 * 
 * Emulator behaves like a physical pump:
 * - STOP/BLOCK freezes totals in PAYMENT_PENDING state
 * - UNBLOCK after STOP does not start new filling until reset/clear
 * - No race conditions - atomic stop mechanism
 * 
 * State machine: IDLE → AUTHORIZED → DELIVERING → PAYMENT_PENDING → IDLE
 */
interface IEhlDispenserEmulator {
    /**
     * Update the price per litre.
     * Takes effect immediately for new transactions.
     */
    fun setPrice(pricePerLitreCents: Int)
    
    /**
     * Get the current price per litre in kr.
     */
    fun getPricePerLitreKr(): Double
    
    /**
     * Process bytes from controller and return response packets.
     * 
     * @param bytes Raw bytes from controller
     * @return List of response packet bytes
     */
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray>
    
    /**
     * Get current completed transaction (if any).
     */
    fun getCurrentTransaction(): CompletedTransaction?
    
    /**
     * Get current state.
     */
    fun getCurrentState(): EmulatorState
    
    /**
     * Mark transaction as paid and reset to IDLE.
     * This simulates the operator clearing the pump after payment.
     */
    fun markTransactionPaid(): Boolean
    
    /**
     * Clear transaction without payment (for testing).
     */
    fun clearTransaction(): Boolean
    
    /**
     * Reset to IDLE state with cleared totals.
     */
    fun resetToIdle()
}
