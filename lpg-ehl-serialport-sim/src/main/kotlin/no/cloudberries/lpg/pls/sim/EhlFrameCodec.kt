package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory

/**
 * EHL Protocol Frame Codec
 * 
 * Handles encoding and decoding of EHL protocol binary frames.
 * 
 * Frame format: STX LEN ADDR CMD [DATA ...] CHK ETX
 * - STX: 0x10 (Controller->Dispenser) or 0x20 (Dispenser->Controller)
 * - LEN: Total packet length including STX and ETX
 * - ADDR: Dispenser address (typically 0x31 = '1')
 * - CMD: Command code (e.g., 0x6A = LINETEST, 0x4B = STATE, 0x1E = OK)
 * - DATA: Optional payload bytes
 * - CHK: XOR checksum of all bytes from STX to last DATA byte (excluding CHK and ETX)
 * - ETX: 0x36
 * 
 * Minimum frame length: 6 bytes (STX LEN ADDR CMD CHK ETX)
 */
object EhlFrameCodec {
    private val log = LoggerFactory.getLogger(EhlFrameCodec::class.java)
    
    const val STX_CONTROLLER: Byte = 0x10  // PC -> Dispenser
    const val STX_DISPENSER: Byte = 0x20   // Dispenser -> PC
    const val ETX: Byte = 0x36
    const val MIN_FRAME_LENGTH = 6
    
    // EHL Command codes
    const val CMD_OK: Byte = 0x1E          // 30 - Command acknowledgement
    const val CMD_LINETEST: Byte = 0x6A    // 106 - Transmission channel test
    const val CMD_STATE: Byte = 0x4B       // 75 - Give/take calculator state
    const val CMD_VOLUME: Byte = 0x45      // 69 - Give/take fuel amount
    const val CMD_PRICE: Byte = 0x4F       // 79 - Give/take unit price
    const val CMD_BLOCK: Byte = 0x69       // 105 - Block/stop dispenser
    const val CMD_UNBLOCK: Byte = 0x77     // 119 - Start delivery mode
    const val CMD_STOP: Byte = 0x2F        // 47 - Stop the dispenser
    const val CMD_RESET: Byte = 0x52       // 82 - Reset dispenser
    
    /**
     * Decode an EHL frame from raw bytes.
     * Returns null if frame is invalid.
     */
    fun decode(frameBytes: ByteArray): EhlFrame? {
        if (frameBytes.size < MIN_FRAME_LENGTH) {
            log.warn("Frame too short: {} bytes (minimum {})", frameBytes.size, MIN_FRAME_LENGTH)
            return null
        }
        
        val stx = frameBytes[0]
        val len = frameBytes[1].toInt() and 0xFF
        val addr = frameBytes[2]
        val cmd = frameBytes[3]
        val etx = frameBytes[frameBytes.size - 1]
        val chk = frameBytes[frameBytes.size - 2]
        
        // Validate STX
        if (stx != STX_CONTROLLER && stx != STX_DISPENSER) {
            log.warn("Invalid STX: 0x{} (expected 0x10 or 0x20)", stx.toHex())
            return null
        }
        
        // Validate ETX
        if (etx != ETX) {
            log.warn("Invalid ETX: 0x{} (expected 0x36)", etx.toHex())
            return null
        }
        
        // Validate length
        if (len != frameBytes.size) {
            log.warn("Length mismatch: LEN={} but frame size={}", len, frameBytes.size)
            return null
        }
        
        // Extract data (if any)
        val data = if (frameBytes.size > MIN_FRAME_LENGTH) {
            frameBytes.copyOfRange(4, frameBytes.size - 2)
        } else {
            ByteArray(0)
        }
        
        // Calculate expected checksum
        val expectedChk = calculateChecksum(frameBytes, 0, frameBytes.size - 2)
        
        // Validate checksum
        if (chk != expectedChk) {
            log.warn("Checksum mismatch: expected 0x{}, got 0x{}", expectedChk.toHex(), chk.toHex())
            return null
        }
        
        return EhlFrame(stx, addr, cmd, data)
    }
    
    /**
     * Encode an EHL frame to raw bytes.
     */
    fun encode(addr: Byte, cmd: Byte, data: ByteArray = ByteArray(0)): ByteArray {
        val len = MIN_FRAME_LENGTH + data.size
        val frame = ByteArray(len)
        
        frame[0] = STX_DISPENSER  // Simulator always responds as dispenser
        frame[1] = len.toByte()
        frame[2] = addr
        frame[3] = cmd
        
        // Copy data if present
        if (data.isNotEmpty()) {
            data.copyInto(frame, 4)
        }
        
        // Calculate checksum (XOR of all bytes except CHK and ETX)
        val chk = calculateChecksum(frame, 0, len - 2)
        frame[len - 2] = chk
        frame[len - 1] = ETX
        
        return frame
    }
    
    /**
     * Calculate XOR checksum of frame bytes from startIdx to endIdx (exclusive).
     */
    private fun calculateChecksum(bytes: ByteArray, startIdx: Int, endIdx: Int): Byte {
        var chk: Byte = 0
        for (i in startIdx until endIdx) {
            chk = (chk.toInt() xor bytes[i].toInt()).toByte()
        }
        return chk
    }
}

/**
 * Decoded EHL frame.
 */
data class EhlFrame(
    val stx: Byte,
    val addr: Byte,
    val cmd: Byte,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EhlFrame

        if (stx != other.stx) return false
        if (addr != other.addr) return false
        if (cmd != other.cmd) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stx.toInt()
        result = 31 * result + addr.toInt()
        result = 31 * result + cmd.toInt()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

// Extension function for byte to hex string
private fun Byte.toHex(): String = "%02X".format(this)
