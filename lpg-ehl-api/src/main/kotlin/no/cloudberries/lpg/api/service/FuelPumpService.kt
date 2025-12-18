package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * FuelPumpService - Orchestrates the complete fueling workflow.
 * 
 * ## Responsibilities:
 * - Initiate fueling (PRODUCT_SELECT → UNBLOCK → verify state)
 * - Stop fueling (BLOCK)
 * - Coordinate with DispenserService and protocol layer
 * 
 * ## Flow:
 * 1. User authorizes fueling (external system)
 * 2. `startFueling(pumpId, productId)` called
 * 3. Send PRODUCT_SELECT command with product/grade
 * 4. Await ACK/response
 * 5. Send UNBLOCK to enable dispenser
 * 6. Poll STATE until AUTHORIZED → PUMPING
 * 7. User physically lifts nozzle and fuels
 * 8. User replaces nozzle or calls `stopFueling()`
 * 9. Send BLOCK to halt fueling
 * 10. Transition to STOPPED, read final volume
 */
@Service
class FuelPumpService(
    private val dispenserService: DispenserService
) {
    private val logger = LoggerFactory.getLogger(FuelPumpService::class.java)
    
    /**
     * Start fueling sequence for a specific pump and product.
     * 
     * ## Protocol Steps:
     * 1. Verify pump is IDLE
     * 2. Send PRODUCT_SELECT (0xC3)
     * 3. Send UNBLOCK (0x77)
     * 4. Verify state transitions to AUTHORIZED
     * 
     * @param pumpId Dispenser address (1-255)
     * @param productId Fuel product/grade ID (e.g., 1=Regular, 2=Premium)
     * @return StartFuelingResult - Success or specific error
     * @throws IllegalStateException if pump is not IDLE
     */
    fun startFueling(pumpId: Int, productId: Int): StartFuelingResult {
        logger.info("Starting fueling for pump $pumpId, product $productId")
        
        // Step 1: Verify pump is IDLE
        val currentStatus = dispenserService.getCurrentStatus(pumpId)
        if (currentStatus !is DispenserStatus.IDLE) {
            logger.warn("Cannot start fueling - pump $pumpId is not IDLE (current: $currentStatus)")
            return StartFuelingResult.PumpNotIdle(currentStatus)
        }
        
        // Step 2: Send PRODUCT_SELECT command
        logger.debug("Sending PRODUCT_SELECT for pump $pumpId, product $productId")
        val productSelectPacket = EhlPacket(
            address = pumpId,
            command = EhlCommand.PRODUCT_SELECT,
            data = byteArrayOf(productId.toByte())
        )
        
        val productSelectResponse = dispenserService.sendCommandAndWaitForResponse(
            packet = productSelectPacket,
            timeoutMs = 3000
        )
        
        if (productSelectResponse == null) {
            logger.error("No response to PRODUCT_SELECT from pump $pumpId")
            return StartFuelingResult.NoResponse
        }
        
        // Step 3: Send UNBLOCK command
        logger.debug("Sending UNBLOCK for pump $pumpId")
        val unblockPacket = EhlPacket(
            address = pumpId,
            command = EhlCommand.UNBLOCK,
            data = byteArrayOf()
        )
        
        val unblockResponse = dispenserService.sendCommandAndWaitForResponse(
            packet = unblockPacket,
            timeoutMs = 3000
        )
        
        if (unblockResponse == null) {
            logger.error("No response to UNBLOCK from pump $pumpId")
            return StartFuelingResult.NoResponse
        }
        
        // Step 4: Poll STATE to verify AUTHORIZED
        logger.debug("Verifying pump $pumpId transitioned to AUTHORIZED")
        val verifiedStatus = waitForStateTransition(
            pumpId = pumpId,
            expectedState = DispenserStatus.AUTHORIZED::class,
            maxAttempts = 10,
            delayMs = 500
        )
        
        return when (verifiedStatus) {
            is DispenserStatus.AUTHORIZED -> {
                logger.info("Pump $pumpId successfully authorized for fueling")
                StartFuelingResult.Success
            }
            else -> {
                logger.warn("Pump $pumpId did not reach AUTHORIZED state (current: $verifiedStatus)")
                StartFuelingResult.StateTransitionFailed(verifiedStatus)
            }
        }
    }
    
    /**
     * Stop fueling sequence for a specific pump.
     * 
     * ## Protocol Steps:
     * 1. Send BLOCK (0x69) command
     * 2. Verify state transitions to STOPPED
     * 3. Read final volume/amount
     * 
     * @param pumpId Dispenser address (1-255)
     * @return StopFuelingResult - Success with final volume or error
     */
    fun stopFueling(pumpId: Int): StopFuelingResult {
        logger.info("Stopping fueling for pump $pumpId")
        
        // Step 1: Verify pump is PUMPING
        val currentStatus = dispenserService.getCurrentStatus(pumpId)
        if (currentStatus !is DispenserStatus.PUMPING) {
            logger.warn("Cannot stop fueling - pump $pumpId is not PUMPING (current: $currentStatus)")
            return StopFuelingResult.PumpNotPumping(currentStatus)
        }
        
        // Step 2: Send BLOCK command
        logger.debug("Sending BLOCK for pump $pumpId")
        val blockPacket = EhlPacket(
            address = pumpId,
            command = EhlCommand.BLOCK,
            data = byteArrayOf()
        )
        
        val blockResponse = dispenserService.sendCommandAndWaitForResponse(
            packet = blockPacket,
            timeoutMs = 3000
        )
        
        if (blockResponse == null) {
            logger.error("No response to BLOCK from pump $pumpId")
            return StopFuelingResult.NoResponse
        }
        
        // Step 3: Poll STATE to verify STOPPED
        logger.debug("Verifying pump $pumpId transitioned to STOPPED")
        val verifiedStatus = waitForStateTransition(
            pumpId = pumpId,
            expectedState = DispenserStatus.STOPPED::class,
            maxAttempts = 10,
            delayMs = 500
        )
        
        return when (verifiedStatus) {
            is DispenserStatus.STOPPED -> {
                // Step 4: Read final volume
                val finalVolume = dispenserService.queryVolume(pumpId)
                logger.info("Pump $pumpId successfully stopped. Final volume: $finalVolume liters")
                StopFuelingResult.Success(finalVolume)
            }
            else -> {
                logger.warn("Pump $pumpId did not reach STOPPED state (current: $verifiedStatus)")
                StopFuelingResult.StateTransitionFailed(verifiedStatus)
            }
        }
    }
    
    /**
     * Wait for a specific state transition by polling STATE command.
     * 
     * @param pumpId Dispenser address
     * @param expectedState Expected state class (e.g., DispenserStatus.AUTHORIZED::class)
     * @param maxAttempts Maximum number of polling attempts
     * @param delayMs Delay between polling attempts in milliseconds
     * @return DispenserStatus - the final state (may not match expected)
     */
    private fun waitForStateTransition(
        pumpId: Int,
        expectedState: kotlin.reflect.KClass<out DispenserStatus>,
        maxAttempts: Int,
        delayMs: Long
    ): DispenserStatus {
        repeat(maxAttempts) { attempt ->
            val currentStatus = dispenserService.getCurrentStatus(pumpId)
            
            if (currentStatus::class == expectedState) {
                logger.debug("State transition verified on attempt ${attempt + 1}: $currentStatus")
                return currentStatus
            }
            
            if (attempt < maxAttempts - 1) {
                Thread.sleep(delayMs)
            }
        }
        
        // Return last known status even if not expected state
        return dispenserService.getCurrentStatus(pumpId)
    }
}

/**
 * Result of startFueling operation.
 */
sealed interface StartFuelingResult {
    data object Success : StartFuelingResult
    data class PumpNotIdle(val currentStatus: DispenserStatus) : StartFuelingResult
    data object NoResponse : StartFuelingResult
    data class StateTransitionFailed(val currentStatus: DispenserStatus) : StartFuelingResult
}

/**
 * Result of stopFueling operation.
 */
sealed interface StopFuelingResult {
    data class Success(val finalVolumeLiters: Float) : StopFuelingResult
    data class PumpNotPumping(val currentStatus: DispenserStatus) : StopFuelingResult
    data object NoResponse : StopFuelingResult
    data class StateTransitionFailed(val currentStatus: DispenserStatus) : StopFuelingResult
}
