package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Production-ready EHL packet processor
 * Integrates RS-485 communication with business logic state management
 */
@Service
class EhlPacketProcessor(
    private val dispenserService: DispenserService
) {
    private val logger = LoggerFactory.getLogger(EhlPacketProcessor::class.java)

    /**
     * Process incoming EHL response packets from dispensers
     * This is the main integration point between communication and business logic
     * 
     * @param packet Response packet from dispenser
     */
    @Async
    fun processIncomingPacket(packet: EhlPacket) {
        try {
            // Log packet processing for production monitoring
            if (logger.isTraceEnabled) {
                logger.trace("Processing ${packet.command.name} response from dispenser ${packet.address}")
            }
            
            // Route to appropriate handler based on packet type
            when (packet.command) {
                EhlCommand.STATE -> {
                    // STATE responses contain critical lifecycle information
                    dispenserService.handlePacket(packet)
                }
                EhlCommand.VOLUME -> {
                    // VOLUME responses track fuel flow progress
                    dispenserService.handlePacket(packet)
                }
                EhlCommand.ERROR_QUERY -> {
                    // ERROR responses need special handling for fault monitoring
                    handleErrorPacket(packet)
                }
                EhlCommand.TANK -> {
                    // TANK responses for inventory monitoring (if needed)
                    handleTankPacket(packet)
                }
                else -> {
                    // Other responses may not need state processing
                    if (logger.isDebugEnabled) {
                        logger.debug("Unhandled response type: ${packet.command.name} from dispenser ${packet.address}")
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error("Failed to process packet from dispenser ${packet.address}: ${e.message}", e)
        }
    }

    /**
     * Handle error response packets for fault monitoring
     */
    private fun handleErrorPacket(packet: EhlPacket) {
        if (packet.data.isNotEmpty()) {
            try {
                // Parse error data depending on format (VB6 vs legacy)
                if (packet.data.size == 2) {
                    // VB6 format: 2 ASCII bytes
                    val mainCode = packet.data[0].toInt().toChar()
                    val subCode = packet.data[1].toInt().toChar()
                    
                    if (mainCode != '0' || subCode != '0') {
                        logger.warn("Dispenser ${packet.address} reported error: main=$mainCode, sub=$subCode")
                        // TODO: Integrate with fault monitoring system
                    }
                } else if (packet.data.size == 1) {
                    // Legacy format: 1 byte
                    val errorCode = packet.data[0].toInt() and 0xFF
                    if (errorCode != 0) {
                        logger.warn("Dispenser ${packet.address} reported error code: $errorCode")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse error data from dispenser ${packet.address}: ${e.message}")
            }
        }
    }

    /**
     * Handle tank response packets for inventory monitoring
     */
    private fun handleTankPacket(packet: EhlPacket) {
        // Tank monitoring logic can be added here if needed
        // For now, just log for debugging
        if (logger.isDebugEnabled) {
            logger.debug("Tank status from dispenser ${packet.address}: ${packet.data.size} bytes")
        }
    }

    /**
     * Validate that a price change is safe before allowing it
     * Prevents price changes during active transactions
     */
    fun validatePriceChangeRequest(address: Int): Boolean {
        return dispenserService.isSafeToUpdatePrice(address)
    }

    /**
     * Get the current business state of a dispenser
     */
    fun getDispenserBusinessState(address: Int): DispenserState {
        return dispenserService.getDispenserState(address)
    }
}