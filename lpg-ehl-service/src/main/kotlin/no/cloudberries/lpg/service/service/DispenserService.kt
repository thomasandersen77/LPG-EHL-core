package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.service.dto.DispenserStatusResponse
import no.cloudberries.lpg.service.repository.DispenserStatusRepository
import no.cloudberries.lpg.service.repository.TransactionRepository
import no.cloudberries.lpg.service.model.Transaction
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * Dispenser physical state enum for tracking pump lifecycle
 * Critical for knowing when it's safe to update prices and save transactions
 */
enum class DispenserState {
    IDLE,       // Pump is hung up, ready for instructions - SAFE for price updates
    STARTED,    // Nozzle lifted / Start button pressed - NOT safe for price changes
    FILLING,    // Pulses are coming in (Gas flowing) - Transaction in progress
    FINISHED    // Nozzle hung up, transaction ready - TRIGGER transaction save
}

/**
 * Data class to track dispenser state and transaction progress
 */
data class DispenserStateInfo(
    val state: DispenserState = DispenserState.IDLE,
    val lastVolumeDeciliters: Int = 0,
    val currentTransactionId: String? = null,
    val transactionStartTime: LocalDateTime? = null,
    val pricePerLiterNok: BigDecimal? = null,
    val pendingPriceUpdate: BigDecimal? = null  // Queued price update waiting for IDLE state
)

@Service
@Transactional(readOnly = true)
class DispenserService(
    private val dispenserStatusRepository: DispenserStatusRepository,
    private val transactionRepository: TransactionRepository,
    private val priceUpdateCallback: ((Int, BigDecimal) -> Unit)? = null  // Callback for sending price to hardware
) {
    private val logger = LoggerFactory.getLogger(DispenserService::class.java)
    
    // Track the current state of each dispenser (address -> state info)
    private val dispenserStates = ConcurrentHashMap<Int, DispenserStateInfo>()

    fun getAllDispensers(): List<DispenserStatusResponse> {
        return dispenserStatusRepository.findAll()
            .map { DispenserStatusResponse.from(it) }
    }

    fun getDispenserStatus(address: Int): DispenserStatusResponse? {
        return dispenserStatusRepository.findById(address)
            .map { DispenserStatusResponse.from(it) }
            .orElse(null)
    }

    fun getActiveDispensers(minutesSinceLastSeen: Long = 60): List<DispenserStatusResponse> {
        val cutoffTime = LocalDateTime.now().minusMinutes(minutesSinceLastSeen)
        return dispenserStatusRepository.findActiveDispensers(cutoffTime)
            .map { DispenserStatusResponse.from(it) }
    }

    fun dispenserExists(address: Int): Boolean {
        return dispenserStatusRepository.existsByAddress(address)
    }
    
    /**
     * CORE STATE MACHINE: Handle incoming EHL packets to track dispenser lifecycle
     * This is the critical business logic for knowing when transactions start/end
     * 
     * @param packet Incoming EHL packet from dispenser
     */
    @Transactional
    fun handlePacket(packet: EhlPacket) {
        val address = packet.address
        val currentState = dispenserStates.getOrDefault(address, DispenserStateInfo())
        
        try {
            when (packet.command) {
                EhlCommand.STATE -> handleStatePacket(address, packet, currentState)
                EhlCommand.VOLUME -> handleVolumePacket(address, packet, currentState)
                // Add other command handlers as needed
                else -> {
                    // Log unknown commands for debugging
                    if (logger.isDebugEnabled) {
                        logger.debug("Unhandled packet command: ${packet.command.name} from dispenser $address")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling packet from dispenser $address: ${e.message}", e)
        }
    }
    
    /**
     * Handle STATE response packets to detect pump lifecycle transitions
     */
    private fun handleStatePacket(address: Int, packet: EhlPacket, currentState: DispenserStateInfo) {
        if (packet.data.isEmpty()) {
            logger.warn("STATE packet from dispenser $address has no data")
            return
        }
        
        try {
            val statusByte = EhlDataParser.parseStateData(packet.data)
            val newState = interpretStatusByte(statusByte, currentState)
            
            if (newState != currentState.state) {
                logger.info("Dispenser $address state transition: ${currentState.state} -> $newState (status: $statusByte)")
                handleStateTransition(address, currentState, newState)
            }
            
            // Update state tracking - preserve volume data and pending price from latest state
            val latestState = dispenserStates.getOrDefault(address, currentState)
            if (dispenserStates[address]?.state != newState) {
                dispenserStates[address] = latestState.copy(state = newState)
            }
            
        } catch (e: Exception) {
            logger.error("Failed to parse STATE data from dispenser $address: ${e.message}")
        }
    }
    
    /**
     * Handle VOLUME response packets to detect fuel flow and transaction progress
     */
    private fun handleVolumePacket(address: Int, packet: EhlPacket, currentState: DispenserStateInfo) {
        if (packet.data.size < 4) {
            logger.warn("VOLUME packet from dispenser $address has insufficient data (${packet.data.size} bytes)")
            return
        }
        
        try {
            val (volumeLiters, amountCents) = EhlDataParser.parseVolumeData(packet.data)
            val volumeDeciliters = (volumeLiters * 10).toInt()
            
            // Detect volume increase (fuel flowing)
            if (volumeDeciliters > currentState.lastVolumeDeciliters && currentState.state == DispenserState.STARTED) {
                logger.info("Dispenser $address: Fuel flow detected, transitioning to FILLING")
                handleStateTransition(address, currentState, DispenserState.FILLING)
                dispenserStates[address] = currentState.copy(
                    state = DispenserState.FILLING,
                    lastVolumeDeciliters = volumeDeciliters
                )
            } else if (volumeDeciliters > 0) {
                // Update volume without state change
                dispenserStates[address] = currentState.copy(lastVolumeDeciliters = volumeDeciliters)
            }
            
        } catch (e: Exception) {
            logger.error("Failed to parse VOLUME data from dispenser $address: ${e.message}")
        }
    }
    
    /**
     * Interpret the raw status byte to determine dispenser state
     * This maps hardware status to business logic states
     */
    private fun interpretStatusByte(statusByte: Int, currentState: DispenserStateInfo): DispenserState {
        return when {
            // Status 0: Idle/Ready state
            statusByte == 0 -> {
                when (currentState.state) {
                    // If we were FILLING and now IDLE, transaction is finished
                    DispenserState.FILLING -> DispenserState.FINISHED
                    // If we were FINISHED and get another status 0, transition to IDLE
                    DispenserState.FINISHED -> DispenserState.IDLE
                    // Otherwise just stay/go to IDLE
                    else -> DispenserState.IDLE
                }
            }
            // Status 1-3: Various busy/active states - nozzle lifted or pumping
            statusByte in 1..3 -> {
                if (currentState.state == DispenserState.IDLE) {
                    DispenserState.STARTED
                } else {
                    currentState.state // Keep current state if already active
                }
            }
            // Status > 3: Error states or unknown - stay in current state
            else -> {
                logger.warn("Unknown status byte $statusByte from dispenser ${currentState}, keeping current state")
                currentState.state
            }
        }
    }
    
    /**
     * Handle state transitions and trigger appropriate business logic
     */
    private fun handleStateTransition(address: Int, currentState: DispenserStateInfo, newState: DispenserState) {
        // Always get the latest state from the map for accurate data (volume, pending price, etc.)
        val latestState = dispenserStates.getOrDefault(address, currentState)
        
        when (newState) {
            DispenserState.STARTED -> {
                if (currentState.state == DispenserState.IDLE) {
                    logger.info("Dispenser $address: Transaction starting (nozzle lifted)")
                    startNewTransaction(address)
                }
            }
            DispenserState.FINISHED -> {
                if (currentState.state == DispenserState.FILLING) {
                    logger.info("Dispenser $address: Transaction finished, saving to database")
                    finishTransaction(address, latestState)
                }
            }
            DispenserState.IDLE -> {
                // Check for pending price from latest state
                val pendingPrice = latestState.pendingPriceUpdate
                
                // Save transaction if coming from FINISHED state
                if (currentState.state == DispenserState.FINISHED && latestState.lastVolumeDeciliters > 0) {
                    logger.info("Dispenser $address: FINISHED -> IDLE, saving transaction")
                    finishTransaction(address, latestState)
                }
                
                // PRICE UPDATE SAFETY: Apply pending price updates when returning to IDLE
                if (pendingPrice != null) {
                    logger.info(
                        "Dispenser $address returned to IDLE - applying queued price update: " +
                        "$pendingPrice NOK/L"
                    )
                    sendPriceToHardware(address, pendingPrice)
                    
                    // Update state with new price and clear pending update - reset to fresh IDLE state
                    dispenserStates[address] = DispenserStateInfo(
                        state = DispenserState.IDLE,
                        pricePerLiterNok = pendingPrice,
                        pendingPriceUpdate = null
                    )
                }
            }
            else -> {
                // No special action for other transitions
            }
        }
    }
    
    /**
     * Start a new transaction when pump becomes active
     */
    private fun startNewTransaction(address: Int) {
        val transactionId = "TXN-$address-${System.currentTimeMillis()}"
        val startTime = LocalDateTime.now()
        
        dispenserStates[address] = dispenserStates.getOrDefault(address, DispenserStateInfo()).copy(
            currentTransactionId = transactionId,
            transactionStartTime = startTime
        )
        
        logger.info("Started transaction $transactionId for dispenser $address")
    }
    
    /**
     * Finish transaction and save to database
     * This is triggered when the pump returns to IDLE after FILLING
     */
    private fun finishTransaction(address: Int, stateInfo: DispenserStateInfo) {
        if (stateInfo.currentTransactionId == null) {
            logger.warn("Dispenser $address finished but no active transaction ID")
            return
        }
        
        // Only save if we actually dispensed fuel
        if (stateInfo.lastVolumeDeciliters > 0) {
            try {
                val transaction = Transaction(
                    dispenserAddress = address,
                    nozzleNumber = 1, // Default nozzle
                    volumeDeciliters = stateInfo.lastVolumeDeciliters,
                    amountOre = calculateAmount(stateInfo.lastVolumeDeciliters, stateInfo.pricePerLiterNok),
                    pricePerLiter = stateInfo.pricePerLiterNok ?: BigDecimal("15.90"), // Default price
                    paymentType = "UNKNOWN", // Will be updated by payment processing
                    includesRoadTax = true,
                    timestamp = stateInfo.transactionStartTime ?: LocalDateTime.now(),
                    productCode = "LPG"
                )
                
                val saved = transactionRepository.save(transaction)
                logger.info("Saved transaction ${saved.transactionId} for dispenser $address: ${stateInfo.lastVolumeDeciliters/10.0}L")
                
            } catch (e: Exception) {
                logger.error("Failed to save transaction for dispenser $address: ${e.message}", e)
            }
        }
        
        // Reset state for next transaction, but PRESERVE pending price update
        dispenserStates[address] = DispenserStateInfo(
            pendingPriceUpdate = stateInfo.pendingPriceUpdate,
            pricePerLiterNok = stateInfo.pricePerLiterNok
        )
    }
    
    /**
     * Calculate amount in øre from volume and price
     */
    private fun calculateAmount(volumeDeciliters: Int, pricePerLiter: BigDecimal?): Int {
        val price = pricePerLiter ?: BigDecimal("15.90")
        val volumeLiters = BigDecimal(volumeDeciliters).divide(BigDecimal(10))
        return (volumeLiters * price * BigDecimal(100)).toInt() // Convert to øre
    }
    
    /**
     * Check if it's safe to update prices for a dispenser
     * Only allow price changes when pump is IDLE
     */
    fun isSafeToUpdatePrice(address: Int): Boolean {
        val state = dispenserStates[address]?.state ?: DispenserState.IDLE
        return state == DispenserState.IDLE
    }
    
    /**
     * Get current state of a dispenser
     */
    fun getDispenserState(address: Int): DispenserState {
        return dispenserStates[address]?.state ?: DispenserState.IDLE
    }
    
    /**
     * PART 3: PRICE UPDATE SAFETY
     * Queue a price update for a dispenser.
     * 
     * CRITICAL SAFETY: Only sends the price command to hardware if dispenser is IDLE.
     * If dispenser is FILLING or STARTED, caches the price and waits until FINISHED -> IDLE
     * transition to prevent changing unit price during an active transaction.
     * 
     * @param address Dispenser address
     * @param newPrice New price per liter in NOK (e.g., 15.90)
     * @return true if price update was sent immediately, false if queued for later
     */
    @Transactional
    fun queuePriceUpdate(address: Int, newPrice: BigDecimal): Boolean {
        val currentState = dispenserStates.getOrDefault(address, DispenserStateInfo())
        
        return when (currentState.state) {
            DispenserState.IDLE -> {
                // SAFE: Pump is idle, send price update immediately
                logger.info("Dispenser $address is IDLE - sending price update immediately: $newPrice NOK/L")
                sendPriceToHardware(address, newPrice)
                
                // Update state with new price
                dispenserStates[address] = currentState.copy(pricePerLiterNok = newPrice)
                true
            }
            
            DispenserState.STARTED, DispenserState.FILLING, DispenserState.FINISHED -> {
                // NOT SAFE: Transaction in progress, queue for later
                logger.warn(
                    "Dispenser $address is ${currentState.state} - CANNOT update price during transaction. " +
                    "Queuing price $newPrice NOK/L for next IDLE state"
                )
                dispenserStates[address] = currentState.copy(pendingPriceUpdate = newPrice)
                false
            }
        }
    }
    
    /**
     * Send price update command to hardware via callback.
     * This is called only when it's SAFE (dispenser is IDLE).
     */
    private fun sendPriceToHardware(address: Int, price: BigDecimal) {
        try {
            priceUpdateCallback?.invoke(address, price)
                ?: logger.warn("No price update callback configured - price not sent to hardware")
        } catch (e: Exception) {
            logger.error("Failed to send price update to hardware for dispenser $address: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Check if there's a pending price update for a dispenser.
     */
    fun hasPendingPriceUpdate(address: Int): Boolean {
        return dispenserStates[address]?.pendingPriceUpdate != null
    }
    
    /**
     * Get the pending price update for a dispenser (if any).
     */
    fun getPendingPriceUpdate(address: Int): BigDecimal? {
        return dispenserStates[address]?.pendingPriceUpdate
    }
    
    /**
     * Get current domain status of a dispenser (for FuelPumpService).
     * Maps from internal DispenserState to protocol-level DispenserStatus.
     * 
     * @param pumpId Dispenser address
     * @return DispenserStatus - current domain state
     */
    fun getCurrentStatus(pumpId: Int): no.cloudberries.lpg.protocol.DispenserStatus {
        // Send STATE query and parse response
        val statePacket = EhlPacket(
            address = pumpId,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        val response = sendCommandAndWaitForResponse(statePacket, 2000)
        return if (response != null) {
            no.cloudberries.lpg.protocol.DispenserStateMapper.mapFromPacket(response)
        } else {
            logger.warn("No response from pump $pumpId for STATE query")
            no.cloudberries.lpg.protocol.DispenserStatus.UNKNOWN(0x00)
        }
    }
    
    /**
     * Send a command packet and wait for response (synchronous).
     * 
     * @param packet Command packet to send
     * @param timeoutMs Timeout in milliseconds
     * @return Response packet or null if timeout
     */
    fun sendCommandAndWaitForResponse(packet: EhlPacket, timeoutMs: Long): EhlPacket? {
        // TODO: Implement actual serial communication
        // For now, this is a placeholder that will be implemented in SerialPortManager integration
        logger.warn("sendCommandAndWaitForResponse not fully implemented - returning mock response")
        
        // Mock response for testing
        return EhlPacket(
            address = packet.address,
            command = packet.command,
            data = byteArrayOf(0x00)  // Mock IDLE response
        )
    }
    
    /**
     * Query current volume from dispenser.
     * 
     * @param pumpId Dispenser address
     * @return Current volume in liters
     */
    fun queryVolume(pumpId: Int): Float {
        val volumePacket = EhlPacket(
            address = pumpId,
            command = EhlCommand.VOLUME,
            data = byteArrayOf()
        )
        
        val response = sendCommandAndWaitForResponse(volumePacket, 2000)
        return if (response != null && response.data.size >= 4) {
            try {
                val (volumeLiters, _) = EhlDataParser.parseVolumeData(response.data)
                volumeLiters.toFloat()
            } catch (e: Exception) {
                logger.error("Failed to parse volume from pump $pumpId: ${e.message}")
                0.0f
            }
        } else {
            logger.warn("No valid volume response from pump $pumpId")
            0.0f
        }
    }
}
