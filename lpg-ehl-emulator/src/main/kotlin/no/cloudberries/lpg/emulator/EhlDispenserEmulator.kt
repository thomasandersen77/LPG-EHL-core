package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.protocol.EhlCodec
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketParseResult
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

/**
 * Emulator for an EHL-protocol LPG dispenser.
 * 
 * This emulator simulates a physical dispenser's behavior, including:
 * - State machine (IDLE → READY → DELIVERING → FINISHED)
 * - EHL protocol packet handling (STATE, UNBLOCK, STOP, VOLUME)
 * - Simulated fuel delivery with configurable flow rate
 * - Checksum validation and error responses
 * 
 * @property address Dispenser address (1-255)
 * @property pricePerLitreCents Price per litre in cents (øre)
 * @property litresPerSecond Simulated flow rate for testing
 */
class EhlDispenserEmulator(
    private val address: Int = 1,
    private val pricePerLitreCents: Int = 1126,      // 11.26 kr/l
    private val litresPerSecond: Double = 0.5        // Simulated flow rate
) {
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulator::class.java)

    private var state: DispenserState = DispenserState.IDLE
    private var startedAtMs: Long? = null

    private var volumeLitres: Double = 0.0
    private var amountCents: Int = 0
    private var currentPricePerLitreCents: Int = pricePerLitreCents

    /**
     * Dispenser state machine states.
     */
    enum class DispenserState(val code: Int) {
        IDLE(0),
        READY(1),
        DELIVERING(2),
        FINISHED(3),
        ERROR(9)
    }

    /**
     * Reset emulator to initial state.
     */
    fun reset() {
        state = DispenserState.IDLE
        startedAtMs = null
        volumeLitres = 0.0
        amountCents = 0
    }

    /**
     * Process raw bytes from the controller and return response packets.
     * 
     * @param bytes Raw bytes received from controller
     * @return List of raw response packets to send back
     */
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()

        return when (val parsed = EhlCodec.decode(bytes)) {
            is EhlPacketParseResult.Success -> {
                logger.info("Emulator received: ${parsed.packet}")
                handlePacket(parsed.packet).map { EhlCodec.encode(it) }
            }
            is EhlPacketParseResult.Incomplete -> {
                logger.warn("Emulator received incomplete packet")
                emptyList()
            }
            is EhlPacketParseResult.ChecksumError -> {
                logger.warn("Emulator checksum error: expected ${parsed.expected} vs ${parsed.actual}")
                listOf(EhlCodec.encode(buildErrorPacket(0x01)))   // Checksum error code
            }
            is EhlPacketParseResult.InvalidFormat -> {
                logger.warn("Emulator invalid format: ${parsed.reason}")
                listOf(EhlCodec.encode(buildErrorPacket(0x02)))   // Invalid format error code
            }
        }
    }

    private fun handlePacket(packet: EhlPacket): List<EhlPacket> {
        if (packet.address != address) {
            // Wrong address - ignore
            return emptyList()
        }

        return when (packet.command) {
            EhlCommand.STATE     -> listOf(buildStateResponse())
            EhlCommand.UNBLOCK   -> handleUnblock(packet)
            EhlCommand.STOP      -> handleStop(packet)
            EhlCommand.BLOCK     -> handleBlock(packet)
            EhlCommand.VOLUME    -> listOf(buildVolumeResponse())
            EhlCommand.PRICE     -> listOf(buildPriceResponse())
            EhlCommand.PROG_PRC  -> handlePriceProgram(packet)
            EhlCommand.PROG_W    -> handleValuePreset(packet)
            EhlCommand.PROG_I    -> handleVolumePreset(packet)
            EhlCommand.LINETEST  -> listOf(EhlPacket(address, EhlCommand.OK))
            EhlCommand.ZER       -> handleReset(packet)
            else                 -> listOf(buildErrorPacket(0x10)) // Unsupported command
        }
    }

    private fun handleUnblock(packet: EhlPacket): List<EhlPacket> {
        // Start delivery if in IDLE, READY, or FINISHED state
        if (state == DispenserState.IDLE || state == DispenserState.READY || state == DispenserState.FINISHED) {
            logger.info("Emulator: UNBLOCK - starting delivery")
            state = DispenserState.DELIVERING
            startedAtMs = System.currentTimeMillis()
            volumeLitres = 0.0
            amountCents = 0
        }
        // Respond with OK + STATE
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }

    private fun handleStop(packet: EhlPacket): List<EhlPacket> {
        if (state == DispenserState.DELIVERING) {
            logger.info("Emulator: STOP - finishing delivery")
            updateDelivery() // Calculate final volume/amount
            state = DispenserState.FINISHED
        }
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse(),
            buildVolumeResponse()
        )
    }
    
    private fun handleBlock(packet: EhlPacket): List<EhlPacket> {
        // BLOCK is similar to STOP - stops delivery and blocks further operations
        if (state == DispenserState.DELIVERING) {
            logger.info("Emulator: BLOCK - stopping and blocking delivery")
            updateDelivery() // Calculate final volume/amount
            state = DispenserState.FINISHED
        } else {
            logger.info("Emulator: BLOCK - dispenser blocked")
            state = DispenserState.IDLE
        }
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }
    
    private fun handlePriceProgram(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size != 4) {
            logger.warn("Emulator: Invalid PROG_PRC data size: ${packet.data.size}")
            return listOf(buildErrorPacket(0x03)) // Invalid data size
        }
        
        // Parse price from ASCII digits (reversed: pennies, dimes, ones, tens)
        try {
            val digit1 = (packet.data[3].toInt() and 0xFF).toChar()
            val digit2 = (packet.data[2].toInt() and 0xFF).toChar()
            val digit3 = (packet.data[1].toInt() and 0xFF).toChar()
            val digit4 = (packet.data[0].toInt() and 0xFF).toChar()
            
            if (!digit1.isDigit() || !digit2.isDigit() || !digit3.isDigit() || !digit4.isDigit()) {
                return listOf(buildErrorPacket(0x04)) // Invalid price format
            }
            
            val priceString = "$digit1$digit2.$digit3$digit4"
            currentPricePerLitreCents = (priceString.toDouble() * 100).toInt()
            logger.info("Emulator: Price programmed to $priceString kr/L ($currentPricePerLitreCents øre/L)")
            
            return listOf(
                EhlPacket(address, EhlCommand.OK),
                buildPriceResponse()
            )
        } catch (e: Exception) {
            logger.error("Emulator: Failed to parse price", e)
            return listOf(buildErrorPacket(0x04))
        }
    }
    
    private fun handleValuePreset(packet: EhlPacket): List<EhlPacket> {
        // PROG_W: Program value (amount) preset
        // For simplicity, emulator just acknowledges
        logger.info("Emulator: Value preset programmed (not implemented in emulator)")
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleVolumePreset(packet: EhlPacket): List<EhlPacket> {
        // PROG_I: Program volume preset
        // For simplicity, emulator just acknowledges
        logger.info("Emulator: Volume preset programmed (not implemented in emulator)")
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleReset(packet: EhlPacket): List<EhlPacket> {
        logger.info("Emulator: Reset (ZER) command received")
        reset()
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }

    /**
     * Update volume and amount based on time since delivery started.
     * This simulates fuel flowing at the configured rate.
     */
    private fun updateDelivery() {
        val start = startedAtMs ?: return
        val seconds = (System.currentTimeMillis() - start) / 1000.0
        volumeLitres = (seconds * litresPerSecond).coerceAtLeast(0.0)
        amountCents = (volumeLitres * currentPricePerLitreCents).roundToInt()
    }

    private fun buildStateResponse(): EhlPacket {
        // Update during delivery for "live" status
        if (state == DispenserState.DELIVERING) {
            updateDelivery()
        }
        val data = byteArrayOf(state.code.toByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }

    private fun buildVolumeResponse(): EhlPacket {
        // Update during delivery for "live" volume
        if (state == DispenserState.DELIVERING) {
            updateDelivery()
        }
        // Format: volume in deciliters (2 bytes) + amount in cents (2 bytes)
        val volDeci = (volumeLitres * 10).roundToInt()
        val data = ByteArray(4)
        data[0] = ((volDeci shr 8) and 0xFF).toByte()
        data[1] = (volDeci and 0xFF).toByte()
        data[2] = ((amountCents shr 8) and 0xFF).toByte()
        data[3] = (amountCents and 0xFF).toByte()
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }

    private fun buildPriceResponse(): EhlPacket {
        // Format: Price as 4 ASCII digits (reversed: pennies, dimes, ones, tens)
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
    
    private fun buildErrorPacket(code: Int): EhlPacket {
        val data = byteArrayOf(code.toByte())
        return EhlPacket(address, EhlCommand.ERROR, data)
    }
}
