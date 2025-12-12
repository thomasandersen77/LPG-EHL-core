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
            EhlCommand.STATE   -> listOf(buildStateResponse())
            EhlCommand.UNBLOCK -> handleUnblock(packet)
            EhlCommand.STOP    -> handleStop(packet)
            EhlCommand.VOLUME  -> listOf(buildVolumeResponse())
            else               -> listOf(buildErrorPacket(0x10)) // Unsupported command
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

    /**
     * Update volume and amount based on time since delivery started.
     * This simulates fuel flowing at the configured rate.
     */
    private fun updateDelivery() {
        val start = startedAtMs ?: return
        val seconds = (System.currentTimeMillis() - start) / 1000.0
        volumeLitres = (seconds * litresPerSecond).coerceAtLeast(0.0)
        amountCents = (volumeLitres * pricePerLitreCents).roundToInt()
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

    private fun buildErrorPacket(code: Int): EhlPacket {
        val data = byteArrayOf(code.toByte())
        return EhlPacket(address, EhlCommand.ERROR, data)
    }
}
