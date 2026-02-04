package no.cloudberries.lpg.service.simulator

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Transport-agnostic EHL dispenser simulator engine.
 * 
 * This is the core simulation logic extracted from EhlDispenserEmulator,
 * with all Spring/REST dependencies removed. Can be used by:
 * - LAB mode (InMemorySerialPort via EhlDispenserEmulator wrapper)
 * - Standalone serial simulator (SerialPortSimulatorAdapter)
 * 
 * @property address Dispenser EHL address (1-255)
 * @property pricePerLitreCents Initial price per litre in cents (øre)
 * @property litresPerSecond Simulated flow rate for testing
 */
class SimulatorEngine(
    private val address: Int = 1,
    pricePerLitreCents: Int = 1126,
    private val litresPerSecond: Double = 0.5
) {
    private val logger = LoggerFactory.getLogger(SimulatorEngine::class.java)

    private var state: DispenserState = DispenserState.IDLE
    private var startedAtMs: Long? = null
    private var nozzleLifted: Boolean = false
    private var productSelected: Boolean = false

    private var volumeLitres: Double = 0.0
    private var amountCents: Int = 0
    private var currentPricePerLitreCents: Int = pricePerLitreCents
    
    @Volatile
    private var commandCount: Long = 0

    /**
     * Dispenser state machine states.
     */
    enum class DispenserState {
        IDLE,
        AUTHORIZED,
        PUMPING,
        STOPPED,
        PAYMENT_PENDING,
        ERROR
    }
    
    /**
     * Process an EHL command packet and return response packet(s).
     * This is the main entry point for protocol handling.
     * 
     * @param packet Incoming EHL packet from controller
     * @return List of response packets to send back
     */
    fun processCommand(packet: EhlPacket): List<EhlPacket> {
        if (packet.address != address) {
            logger.debug("Ignoring packet for address ${packet.address} (I am $address)")
            return emptyList()
        }
        
        commandCount++
        
        return when (packet.command) {
            // Query commands
            EhlCommand.STATE         -> listOf(buildStateResponse())
            EhlCommand.VOLUME        -> {
                updateDelivery()
                listOf(buildVolumeResponse())
            }
            EhlCommand.PRICE         -> listOf(buildPriceResponse())
            EhlCommand.ERROR_QUERY   -> listOf(EhlPacket(address, EhlCommand.ERROR, byteArrayOf('0'.code.toByte(), '0'.code.toByte())))
            EhlCommand.TANK          -> listOf(buildTankResponse())
            EhlCommand.LINETEST      -> listOf(EhlPacket(address, EhlCommand.OK))
            
            // Control commands
            EhlCommand.UNBLOCK       -> handleUnblock()
            EhlCommand.BLOCK         -> handleBlock()
            EhlCommand.STOP          -> handleStop()
            EhlCommand.ZER           -> handleReset()
            
            // Programming commands
            EhlCommand.PROG_PRC      -> handlePriceProgram(packet)
            EhlCommand.PROG_AMOUNT   -> handleAmountPreset(packet)
            EhlCommand.PROG_VOLUME   -> handleVolumePreset(packet)
            EhlCommand.PRODUCT_SELECT -> handleProductSelect(packet)
            
            // Responses (normally not received as commands, but handle gracefully)
            EhlCommand.OK            -> emptyList()  // ACK - no response needed
            EhlCommand.ERROR         -> emptyList()  // Error response - no response needed
            
            // Unknown/unsupported
            EhlCommand.UNKNOWN       -> listOf(buildErrorPacket(0x10))
            else                     -> listOf(buildErrorPacket(0x10))
        }
    }
    
    /**
     * Update the price per litre.
     */
    fun setPrice(newPriceCents: Int) {
        currentPricePerLitreCents = newPriceCents
        logger.info("Price updated to ${newPriceCents / 100.0} NOK/L")
    }
    
    /**
     * Get current price in cents.
     */
    fun getPriceCents(): Int = currentPricePerLitreCents
    
    /**
     * Get current state.
     */
    fun getState(): DispenserState = state
    
    /**
     * Get current volume in litres.
     */
    fun getVolumeLitres(): Double {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        return volumeLitres
    }
    
    /**
     * Get current amount in cents.
     */
    fun getAmountCents(): Int {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        return amountCents
    }
    
    /**
     * Reset simulator to initial state.
     */
    fun reset() {
        state = DispenserState.IDLE
        startedAtMs = null
        nozzleLifted = false
        productSelected = false
        volumeLitres = 0.0
        amountCents = 0
    }
    
    /**
     * Build VB6-compatible status byte from current state.
     */
    private fun buildStatusByte(): Byte {
        var statusByte = 0
        
        when (state) {
            DispenserState.IDLE -> statusByte = 0x00
            DispenserState.AUTHORIZED -> statusByte = 0x01  // START_SWITCH_ACTIVE
            DispenserState.PUMPING -> statusByte = 0x01 or 0x02 or 0x04  // START + NOZZLE + DELIVERY
            DispenserState.STOPPED, DispenserState.PAYMENT_PENDING -> statusByte = 0x08  // TRANSACTION_COMPLETE
            DispenserState.ERROR -> statusByte = 0x80  // ERROR_FLAG
        }
        
        if (nozzleLifted && state == DispenserState.AUTHORIZED) {
            statusByte = statusByte or 0x02  // Add NOZZLE_LIFTED
        }
        
        return statusByte.toByte()
    }
    
    private fun handleUnblock(): List<EhlPacket> {
        when (state) {
            DispenserState.IDLE, DispenserState.AUTHORIZED, DispenserState.STOPPED -> {
                // In FIELD mode, simulate automatic nozzle lift for realistic testing
                // Real hardware detects physical nozzle lift via sensor
                // Simulator auto-lifts nozzle on UNBLOCK to emulate customer action
                nozzleLifted = true
                state = DispenserState.PUMPING
                startedAtMs = System.currentTimeMillis()
                volumeLitres = 0.0
                amountCents = 0
                logger.info("State: UNBLOCK → PUMPING (auto-simulated nozzle lift)")
            }
            DispenserState.PUMPING -> {
                logger.debug("UNBLOCK ignored - already pumping")
            }
            DispenserState.PAYMENT_PENDING -> {
                logger.warn("UNBLOCK rejected - payment pending")
            }
            DispenserState.ERROR -> {
                logger.warn("UNBLOCK rejected - error state")
            }
        }
        
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }
    
    private fun handleStop(): List<EhlPacket> {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
            
            state = if (volumeLitres > 0.0) {
                DispenserState.PAYMENT_PENDING
            } else {
                DispenserState.STOPPED
            }
            
            nozzleLifted = false
            
            logger.info("State: PUMPING → ${state.name} | Volume: %.2f L, Amount: %.2f kr".format(
                volumeLitres, amountCents / 100.0
            ))
        }
        
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse(),
            buildVolumeResponse()
        )
    }
    
    private fun handleBlock(): List<EhlPacket> {
        when (state) {
            DispenserState.PUMPING -> {
                updateDelivery()
                
                state = if (volumeLitres > 0.0) {
                    DispenserState.PAYMENT_PENDING
                } else {
                    DispenserState.STOPPED
                }
                
                nozzleLifted = false
                
                logger.info("State: PUMPING → ${state.name} (BLOCK) | Volume: %.2f L".format(volumeLitres))
            }
            DispenserState.AUTHORIZED -> {
                state = DispenserState.IDLE
                productSelected = false
                logger.info("State: AUTHORIZED → IDLE (cancelled)")
            }
            DispenserState.PAYMENT_PENDING -> {
                logger.debug("BLOCK ignored - payment pending")
            }
            else -> {
                state = DispenserState.IDLE
                productSelected = false
                nozzleLifted = false
            }
        }
        
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }
    
    private fun handlePriceProgram(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size != 4) {
            logger.warn("Invalid PROG_PRC data size: ${packet.data.size}")
            return listOf(buildErrorPacket(0x03))
        }
        
        try {
            val digit1 = (packet.data[3].toInt() and 0xFF).toChar()
            val digit2 = (packet.data[2].toInt() and 0xFF).toChar()
            val digit3 = (packet.data[1].toInt() and 0xFF).toChar()
            val digit4 = (packet.data[0].toInt() and 0xFF).toChar()
            
            if (!digit1.isDigit() || !digit2.isDigit() || !digit3.isDigit() || !digit4.isDigit()) {
                logger.warn("Invalid price format - non-digit characters")
                return listOf(buildErrorPacket(0x04))
            }
            
            val priceString = "$digit1$digit2.$digit3$digit4"
            currentPricePerLitreCents = (priceString.toDouble() * 100).toInt()
            
            logger.info("Price programmed: %.2f kr/L".format(currentPricePerLitreCents / 100.0))
            
            return listOf(
                EhlPacket(address, EhlCommand.OK),
                buildPriceResponse()
            )
        } catch (e: Exception) {
            logger.error("Price parse failed: ${e.message}")
            return listOf(buildErrorPacket(0x04))
        }
    }
    
    private fun handleAmountPreset(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size == 5) {
            val digits = packet.data.map { (it.toInt() and 0xFF).toChar() }.reversed().joinToString("")
            logger.debug("Amount preset: $digits øre (acknowledged)")
        }
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleVolumePreset(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size == 6) {
            val digits = packet.data.map { (it.toInt() and 0xFF).toChar() }.reversed().joinToString("")
            logger.debug("Volume preset: $digits (acknowledged)")
        }
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleProductSelect(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size == 1) {
            val product = (packet.data[0].toInt() and 0xFF).toChar()
            logger.debug("Product selected: '$product'")
        }
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleReset(): List<EhlPacket> {
        reset()
        logger.info("Dispenser reset to IDLE")
        
        return listOf(
            EhlPacket(address, EhlCommand.ZER, byteArrayOf(0x1E)),
            buildStateResponse()
        )
    }
    
    /**
     * Update volume and amount based on elapsed time.
     */
    private fun updateDelivery() {
        val start = startedAtMs ?: return
        val seconds = (System.currentTimeMillis() - start) / 1000.0
        volumeLitres = (seconds * litresPerSecond).coerceAtLeast(0.0)
        amountCents = (volumeLitres * currentPricePerLitreCents).roundToInt()
    }
    
    private fun buildStateResponse(): EhlPacket {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        
        val data = byteArrayOf(buildStatusByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }
    
    private fun buildVolumeResponse(): EhlPacket {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        
        // VB6 format: 5 ASCII bytes LSB-first (centilitres)
        val centilitres = (volumeLitres * 100).roundToInt()
        val volumeStr = "%05d".format(centilitres)
        
        val data = ByteArray(5)
        for (i in 0..4) {
            data[i] = volumeStr[4 - i].code.toByte()  // Reverse order
        }
        
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }
    
    private fun buildPriceResponse(): EhlPacket {
        val priceString = "%.2f".format(currentPricePerLitreCents / 100.0)
        val parts = priceString.split(".")
        val data = byteArrayOf(
            parts[1][1].code.toByte(),  // Pennies
            parts[1][0].code.toByte(),  // Dimes
            parts[0][parts[0].length - 1].code.toByte(),  // Ones
            parts[0][parts[0].length - 2].code.toByte()   // Tens
        )
        return EhlPacket(address, EhlCommand.PRICE, data)
    }
    
    private fun buildTankResponse(): EhlPacket {
        var tankStatus = 0x00
        
        // Set trans_unaccounted bit when stopped with volume
        if (state == DispenserState.STOPPED && volumeLitres > 0) {
            tankStatus = tankStatus or 0x08
        }
        
        val data = byteArrayOf(tankStatus.toByte())
        return EhlPacket(address, EhlCommand.TANK, data)
    }
    
    private fun buildErrorPacket(code: Int): EhlPacket {
        val data = byteArrayOf(code.toByte())
        return EhlPacket(address, EhlCommand.ERROR, data)
    }
}
