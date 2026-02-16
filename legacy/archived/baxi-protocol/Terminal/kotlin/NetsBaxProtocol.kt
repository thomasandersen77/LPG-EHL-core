package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * Nets/Bax Protocol Implementation
 * 
 * Used by Ingenico/Verifone terminals over TCP port 8009.
 * 
 * Protocol format:
 * ```
 * <STX> <Payload> <ETX> <LRC>
 * ```
 * 
 * Where:
 * - STX = 0x02 (Start of Text)
 * - ETX = 0x03 (End of Text)  
 * - LRC = XOR checksum of all bytes from payload through ETX (exclusive of STX)
 * 
 * Common commands:
 * - Purchase: "P,<OperatorID>,<AmountCents>"
 * - Cancel: "C"
 * - Status: "S"
 *
 * @see BaxResponse for response types
 */
object NetsBaxProtocol {
    
    private val logger = LoggerFactory.getLogger(NetsBaxProtocol::class.java)
    
    // Protocol control characters - public for diagnostic tools
    const val STX: Byte = 0x02
    const val ETX: Byte = 0x03
    const val ACK: Byte = 0x06
    const val NAK: Byte = 0x15
    const val ENQ: Byte = 0x05
    const val EOT: Byte = 0x04
    
    /**
     * Create Purchase command
     * 
     * @param amountCents Amount in øre/cents (100 = 1.00 NOK)
     * @param operatorId Operator ID (default "1")
     * @return Byte array with complete BAX frame ready to send
     */
    fun createPurchaseCommand(amountCents: Int, operatorId: String = "1"): ByteArray {
        require(amountCents > 0) { "Amount must be positive" }
        
        val commandString = "P,$operatorId,$amountCents"
        logger.debug("Creating Purchase command: $commandString")
        
        return buildFrame(commandString)
    }
    
    /**
     * Create Pre-authorization command (reserve amount)
     * 
     * @param amountCents Maximum amount to reserve
     * @param operatorId Operator ID
     */
    fun createPreauthCommand(amountCents: Int, operatorId: String = "1"): ByteArray {
        require(amountCents > 0) { "Amount must be positive" }
        
        val commandString = "A,$operatorId,$amountCents"
        logger.debug("Creating Preauth command: $commandString")
        
        return buildFrame(commandString)
    }
    
    /**
     * Create Capture/Complete command (finalize pre-auth)
     * 
     * @param amountCents Actual amount to capture
     * @param transactionId Reference to original preauth
     */
    fun createCaptureCommand(amountCents: Int, transactionId: String): ByteArray {
        require(amountCents > 0) { "Amount must be positive" }
        
        val commandString = "F,$transactionId,$amountCents"
        logger.debug("Creating Capture command: $commandString")
        
        return buildFrame(commandString)
    }
    
    /**
     * Create Status query command
     */
    fun createStatusCommand(): ByteArray {
        return buildFrame("S")
    }
    
    /**
     * Create Cancel command
     */
    fun createCancelCommand(): ByteArray {
        return buildFrame("C")
    }
    
    /**
     * Build protocol frame with STX, ETX, and LRC
     * 
     * @param payload The command string without framing
     * @return Complete frame: [STX][payload bytes][ETX][LRC]
     */
    fun buildFrame(payload: String): ByteArray {
        val payloadBytes = payload.toByteArray(StandardCharsets.ISO_8859_1)
        
        // Pre-allocate exact size: STX + payload + ETX + LRC
        val frame = ByteArray(1 + payloadBytes.size + 1 + 1)
        
        frame[0] = STX
        System.arraycopy(payloadBytes, 0, frame, 1, payloadBytes.size)
        frame[payloadBytes.size + 1] = ETX
        
        // Calculate LRC: XOR of all bytes after STX (payload + ETX)
        val lrc = calculateLrc(frame, startIndex = 1, endIndex = frame.size - 1)
        frame[frame.size - 1] = lrc
        
        if (logger.isDebugEnabled) {
            logger.debug("Built frame: ${frame.toHexString()}")
            logger.debug("  Payload: $payload")
            logger.debug("  LRC: 0x${"$02X".format(lrc)}")
        }
        
        return frame
    }
    
    /**
     * Calculate LRC (Longitudinal Redundancy Check)
     * 
     * @param data The byte array
     * @param startIndex Start index (inclusive)
     * @param endIndex End index (exclusive)
     * @return XOR of all bytes in range
     */
    fun calculateLrc(data: ByteArray, startIndex: Int = 0, endIndex: Int = data.size): Byte {
        var lrc: Byte = 0
        for (i in startIndex until endIndex) {
            lrc = (lrc.toInt() xor data[i].toInt()).toByte()
        }
        return lrc
    }
    
    /**
     * Verify LRC checksum of a complete frame
     * 
     * @param frame Complete frame including STX, payload, ETX, and LRC
     * @return true if LRC is valid
     */
    fun verifyLrc(frame: ByteArray): Boolean {
        if (frame.size < 4) return false // Minimum: STX + 1 byte + ETX + LRC
        
        val stxIndex = frame.indexOf(STX)
        val etxIndex = frame.indexOf(ETX)
        
        if (stxIndex < 0 || etxIndex < 0 || etxIndex <= stxIndex) return false
        if (etxIndex + 1 >= frame.size) return false
        
        val receivedLrc = frame[etxIndex + 1]
        val calculatedLrc = calculateLrc(frame, stxIndex + 1, etxIndex + 1)
        
        return receivedLrc == calculatedLrc
    }
    
    /**
     * Check if byte array contains a complete frame (STX...ETX+LRC)
     * 
     * @param data Accumulated bytes from socket
     * @return FrameStatus indicating completeness
     */
    fun checkFrameComplete(data: ByteArray): FrameStatus {
        if (data.isEmpty()) return FrameStatus.Empty
        
        val stxIndex = data.indexOf(STX)
        if (stxIndex < 0) {
            // No STX - might be just ACK/NAK
            return if (data.size == 1 && (data[0] == ACK || data[0] == NAK)) {
                FrameStatus.Complete(frameEnd = 1)
            } else {
                FrameStatus.NoFrame
            }
        }
        
        // Look for ETX after STX
        for (i in (stxIndex + 1) until data.size) {
            if (data[i] == ETX) {
                // Found ETX - check if we have LRC byte
                return if (i + 1 < data.size) {
                    FrameStatus.Complete(frameEnd = i + 2) // Include LRC
                } else {
                    FrameStatus.NeedMoreData("Waiting for LRC byte")
                }
            }
        }
        
        return FrameStatus.NeedMoreData("Waiting for ETX")
    }
    
    /**
     * Parse response from terminal
     * 
     * @param data Raw bytes from terminal
     * @return Parsed BaxResponse
     */
    fun parseResponse(data: ByteArray): BaxResponse {
        if (data.isEmpty()) {
            return BaxResponse.Error("Empty response")
        }
        
        // Check for simple ACK/NAK (single byte)
        if (data.size == 1) {
            return when (data[0]) {
                ACK -> BaxResponse.Ack
                NAK -> BaxResponse.Nak
                else -> BaxResponse.Unknown(data)
            }
        }
        
        // Handle ACK followed by frame: [ACK][STX]...[ETX][LRC]
        val frameStart = if (data[0] == ACK && data.size > 1 && data[1] == STX) 1 else 0
        val frameData = if (frameStart > 0) data.copyOfRange(frameStart, data.size) else data
        
        // Check for framed response
        if (frameData[0] != STX) {
            // Could be multiple ACKs or other control chars
            if (data.all { it == ACK }) return BaxResponse.Ack
            if (data.any { it == NAK }) return BaxResponse.Nak
            
            logger.warn("Response does not start with STX: ${data.toHexString()}")
            return BaxResponse.Unknown(data)
        }
        
        // Find ETX
        val etxIndex = frameData.indexOf(ETX)
        if (etxIndex == -1) {
            logger.warn("Response does not contain ETX: ${frameData.toHexString()}")
            return BaxResponse.Incomplete
        }
        
        // Verify we have LRC byte
        if (etxIndex + 1 >= frameData.size) {
            logger.warn("Response missing LRC: ${frameData.toHexString()}")
            return BaxResponse.Incomplete
        }
        
        // Verify LRC
        val receivedLrc = frameData[etxIndex + 1]
        val calculatedLrc = calculateLrc(frameData, 1, etxIndex + 1)
        
        if (receivedLrc != calculatedLrc) {
            logger.warn("LRC mismatch! Received: 0x${"$02X".format(receivedLrc)}, Calculated: 0x${"$02X".format(calculatedLrc)}")
            logger.warn("Frame: ${frameData.toHexString()}")
            return BaxResponse.Error("LRC checksum failed")
        }
        
        // Extract payload (between STX and ETX)
        val payload = frameData.copyOfRange(1, etxIndex)
        val payloadString = String(payload, StandardCharsets.ISO_8859_1)
        
        logger.debug("Parsed response payload: $payloadString")
        
        return parsePayload(payloadString)
    }
    
    /**
     * Parse the payload string into appropriate response type
     */
    private fun parsePayload(payloadString: String): BaxResponse {
        val parts = payloadString.split(",")
        
        return when {
            // Success codes
            parts[0] == "00" || parts[0].equals("OK", ignoreCase = true) -> {
                BaxResponse.Success(
                    payload = payloadString,
                    transactionId = parts.getOrNull(1),
                    authCode = parts.getOrNull(2)
                )
            }
            // Numeric error code
            parts[0].matches(Regex("\\d+")) && parts[0] != "00" -> {
                BaxResponse.Error("Terminal error code: ${parts[0]}")
            }
            // Status or data response
            else -> {
                BaxResponse.Data(payloadString)
            }
        }
    }
    
    /**
     * Convert byte array to hex string for logging/debugging
     */
    fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
    
    /**
     * Convert byte array to mixed hex/ascii for readability
     */
    fun ByteArray.toDebugString(): String {
        return this.map { byte ->
            val c = byte.toInt() and 0xFF
            when {
                c == 0x02 -> "<STX>"
                c == 0x03 -> "<ETX>"
                c == 0x06 -> "<ACK>"
                c == 0x15 -> "<NAK>"
                c in 32..126 -> c.toChar().toString()
                else -> "[%02X]".format(byte)
            }
        }.joinToString("")
    }
}

/**
 * Frame completeness status
 */
sealed class FrameStatus {
    object Empty : FrameStatus()
    object NoFrame : FrameStatus()
    data class NeedMoreData(val reason: String) : FrameStatus()
    data class Complete(val frameEnd: Int) : FrameStatus()
}

/**
 * BAX protocol response types
 * 
 * Represents all possible responses from a payment terminal
 */
sealed class BaxResponse {
    /** Simple ACK (0x06) - terminal acknowledged receipt */
    object Ack : BaxResponse() {
        override fun toString() = "ACK"
    }
    
    /** Simple NAK (0x15) - terminal rejected message */
    object Nak : BaxResponse() {
        override fun toString() = "NAK"
    }
    
    /** Success response with transaction details */
    data class Success(
        val payload: String,
        val transactionId: String?,
        val authCode: String? = null
    ) : BaxResponse()
    
    /** Error response with message */
    data class Error(val message: String) : BaxResponse()
    
    /** Data/status response */
    data class Data(val payload: String) : BaxResponse()
    
    /** Incomplete frame - need more data from socket */
    object Incomplete : BaxResponse() {
        override fun toString() = "Incomplete"
    }
    
    /** Unknown response format */
    data class Unknown(val data: ByteArray) : BaxResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Unknown) return false
            return data.contentEquals(other.data)
        }
        override fun hashCode(): Int = data.contentHashCode()
        override fun toString() = "Unknown(${data.joinToString(" ") { "%02X".format(it) }})"
    }
}
