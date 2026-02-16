package no.cloudberries.lpg.emulator

/**
 * Interface for dispenser flow simulation with atomic stop capability.
 * 
 * Simulates fuel delivery with configurable flow rate. Implementations should use atomic operations
 * and coroutine cancellation to ensure immediate stop without race conditions.
 */
interface IDispenserSimulator {
    /**
     * Update the price per litre. Takes effect for future calculations.
     */
    fun updatePrice(pricePerLitreCents: Int)
    
    /**
     * Start simulation for an active transaction.
     * Updates volumeLitres and amountCents based on elapsed time.
     * 
     * @param activeTx The active transaction to update
     * @param onUpdate Callback invoked on each update with current values
     */
    fun start(
        activeTx: ActiveTransaction,
        onUpdate: (volumeLitres: Double, amountCents: Int) -> Unit = { _, _ -> }
    )
    
    /**
     * Stop simulation immediately without further updates.
     * Should use atomic operations to prevent race conditions.
     */
    fun stopImmediately()
    
    /**
     * Check if simulation is currently running.
     */
    fun isRunning(): Boolean
}
