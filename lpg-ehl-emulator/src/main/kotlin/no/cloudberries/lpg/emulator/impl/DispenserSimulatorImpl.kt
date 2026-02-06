package no.cloudberries.lpg.emulator.impl

import kotlinx.coroutines.*
import no.cloudberries.lpg.emulator.ActiveTransaction
import no.cloudberries.lpg.emulator.IDispenserSimulator
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Dispenser flow simulation with atomic stop capability.
 * 
 * Simulates fuel delivery with configurable flow rate. Uses atomic operations
 * and coroutine cancellation to ensure immediate stop without race conditions.
 * 
 * Key features:
 * - AtomicBoolean for thread-safe stop signal
 * - Checks stop flag before AND after each delay
 * - Immediate coroutine cancellation
 * - No updates after stopImmediately() is called
 */
@Component
@Profile("LAB")
class DispenserSimulatorImpl(
    private val litresPerSecond: Double = 0.5,
    pricePerLitreCents: Int = 1590  // 15.90 kr/l
) : IDispenserSimulator {
    private val logger = LoggerFactory.getLogger(DispenserSimulatorImpl::class.java)
    private val simRunning = AtomicBoolean(false)
    private var simJob: Job? = null
    
    // Mutable price that can be updated dynamically
    @Volatile
    private var currentPricePerLitreCents: Int = pricePerLitreCents
    
    /**
     * Update the price per litre. Takes effect for future calculations.
     */
    override fun updatePrice(pricePerLitreCents: Int) {
        this.currentPricePerLitreCents = pricePerLitreCents
    }
    
    /**
     * Start simulation for an active transaction.
     * Updates volumeLitres and amountCents based on elapsed time.
     * 
     * @param activeTx The active transaction to update
     * @param onUpdate Callback invoked on each update with current values
     */
    override fun start(
        activeTx: ActiveTransaction,
        onUpdate: (volumeLitres: Double, amountCents: Int) -> Unit
    ) {
        if (simRunning.get()) {
            logger.warn("Simulator already running, stopping previous simulation")
            stopImmediately()
        }
        
        simRunning.set(true)
        logger.info("Starting simulation: ${litresPerSecond} L/s, ${currentPricePerLitreCents/100.0} kr/L")
        
        simJob = CoroutineScope(Dispatchers.Default).launch {
            val startMs = activeTx.startMs
            var updateCount = 0
            
            while (simRunning.get() && isActive) {
                delay(100) // 100ms update interval
                
                // Check again after delay (critical for race prevention)
                if (!simRunning.get()) {
                    logger.debug("Simulation stop detected after delay, breaking")
                    break
                }
                
                val elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000.0
                activeTx.volumeLitres = elapsedSeconds * litresPerSecond
                activeTx.amountCents = (activeTx.volumeLitres * currentPricePerLitreCents).roundToInt()
                
                updateCount++
                if (logger.isTraceEnabled && updateCount % 10 == 0) {
                    logger.trace("Update #${updateCount}: ${activeTx.volumeLitres} L, ${activeTx.amountCents/100.0} kr")
                }
                
                onUpdate(activeTx.volumeLitres, activeTx.amountCents)
            }
            
            logger.info("Simulation stopped after ${updateCount} updates: ${activeTx.volumeLitres} L, ${activeTx.amountCents/100.0} kr")
        }
    }
    
    /**
     * Stop simulation immediately without further updates.
     * Uses atomic operations to prevent race conditions.
     */
    override fun stopImmediately() {
        logger.debug("Stopping simulation immediately")
        simRunning.set(false)
        simJob?.cancel()
        simJob = null
    }
    
    /**
     * Check if simulation is currently running.
     */
    override fun isRunning(): Boolean = simRunning.get()
}
