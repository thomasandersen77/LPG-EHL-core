package no.cloudberries.lpg.protocol

import org.slf4j.LoggerFactory

/**
 * EHL Protocol Codec
 * 
 * Encodes and decodes EHL protocol packets for RS-485 communication with dispensers.
 * Handles packet framing, checksum calculation/validation, and data marshalling.
 */
object EhlCodec {
    private val logger = LoggerFactory.getLogger(EhlCodec::class.java)
    
    // SAFETY LIMITS: Typical EHL packets are small; this prevents buffer abuse in noisy environments
    private const val MAX_PACKET_LENGTH = 64
    
    /**
     * Encode an EHL packet to raw bytes for transmission
     * 
     * @param packet The packet to encode
     * @param fromController True if packet is from controller to dispenser (uses 0x10)
     * @return ByteArray ready for transmission over RS-485
     */
    fun encode(packet: EhlPacket, fromController: Boolean = true): ByteArray {
        val result = ByteArray(packet.packetLength)
        var idx = 0
        
        // STX - Choose correct direction
        result[idx++] = if (fromController) EhlProtocol.STX_CONTROLLER else EhlProtocol.STX_DISPENSER
        
        // Length
        result[idx++] = packet.packetLength.toByte()
        
        // Address
        result[idx++] = packet.address.toByte()
        
        // Command
        result[idx++] = packet.command.code.toByte()
        
        // Data
        for (byte in packet.data) {
            result[idx++] = byte
        }
        
        // Checksum
        result[idx++] = packet.calculateChecksum(fromController)
        
        // ETX
        result[idx] = EhlProtocol.ETX
        
        if (logger.isDebugEnabled) {
            logger.debug("Encoded: {}", result.toHexString())
        }
        
        return result
    }
    
    /**
     * Decode raw bytes into an EHL packet
     * 
     * @param data Raw bytes received from RS-485
     * @return Parse result with either success or error information
     */
    fun decode(data: ByteArray): EhlPacketParseResult {
        // Minimum length check
        if (data.size < EhlProtocol.MIN_PACKET_LENGTH) {
            return EhlPacketParseResult.Incomplete
        }
        
        // Check STX - Accept both controller (0x10) and dispenser (0x20) STX values
        val stx = data[0]
        if (stx != EhlProtocol.STX_CONTROLLER && stx != EhlProtocol.STX_DISPENSER) {
            return EhlPacketParseResult.InvalidFormat("Invalid STX byte: 0x${"%02X".format(stx)} (expected 0x10 or 0x20)")
        }
        
        // SAFETY CHECK: Get length and validate bounds before proceeding
        // This prevents waiting for data that will never arrive in noisy RS-485 environments
        val length = data[1].toInt() and 0xFF
        if (length > MAX_PACKET_LENGTH) {
            logger.warn("Packet length $length exceeds maximum $MAX_PACKET_LENGTH, discarding")
            return EhlPacketParseResult.InvalidFormat("Packet length $length exceeds maximum $MAX_PACKET_LENGTH")
        }
        if (length < EhlProtocol.MIN_PACKET_LENGTH) {
            logger.warn("Packet length $length below minimum ${EhlProtocol.MIN_PACKET_LENGTH}, discarding")
            return EhlPacketParseResult.InvalidFormat("Packet length $length below minimum ${EhlProtocol.MIN_PACKET_LENGTH}")
        }
        
        // Check if we have enough data
        if (data.size < length) {
            return EhlPacketParseResult.Incomplete
        }
        
        // Check ETX
        if (data[length - 1] != EhlProtocol.ETX) {
            return EhlPacketParseResult.InvalidFormat("Invalid ETX byte: 0x${"%02X".format(data[length - 1])}")
        }
        
        // Extract fields
        val address = data[2].toInt() and 0xFF
        val commandCode = data[3].toInt() and 0xFF
        val command = EhlCommand.fromCode(commandCode)
        
        // Extract data payload
        val dataLength = length - EhlProtocol.MIN_PACKET_LENGTH
        val payload = if (dataLength > 0) {
            data.copyOfRange(4, 4 + dataLength)
        } else {
            ByteArray(0)
        }
        
        // Extract and verify checksum
        val receivedChecksum = data[length - 2]
        val packet = EhlPacket(address, command, payload)
        val fromController = stx == EhlProtocol.STX_CONTROLLER
        val calculatedChecksum = packet.calculateChecksum(fromController)
        
        // ROBUST CHECKSUM VALIDATION: Standard EHL checksum is XOR of all bytes after STX up to checksum
        if (receivedChecksum != calculatedChecksum) {
            // Log detailed checksum failure information for production debugging
            logger.warn("CHECKSUM FAILURE - Packet corrupted in RS-485 transmission:")
            logger.warn("  Expected: 0x${"%02X".format(calculatedChecksum)}, Received: 0x${"%02X".format(receivedChecksum)}")
            logger.warn("  Address: $address, Command: ${command.name}(${command.code}), Length: $length")
            if (logger.isDebugEnabled) {
                logger.debug("  Raw packet: ${data.take(length).toByteArray().joinToString(" ") { "%02X".format(it) }}")
            }
            return EhlPacketParseResult.ChecksumError(calculatedChecksum, receivedChecksum)
        }
        
        if (logger.isDebugEnabled) {
            logger.debug("Decoded: {}", packet)
        }
        
        return EhlPacketParseResult.Success(packet)
    }
    
    /**
     * Convert byte array to hex string for logging
     */
    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { "%02X".format(it) }
    }
}

/**
 * Helper functions for creating common EHL packets
 */
object EhlPacketBuilder {
    
    /**
     * Create a STATE query packet
     */
    fun createStateQuery(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.STATE)
    }
    
    /**
     * Create an UNBLOCK packet to start delivery
     */
    fun createUnblock(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.UNBLOCK)
    }
    
    /**
     * Create a BLOCK packet to stop the dispenser
     */
    fun createBlock(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.BLOCK)
    }
    
    /**
     * Create a LINETEST packet for communication test
     */
    fun createLineTest(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.LINETEST)
    }
    
    /**
     * Create a ZER (reset) packet
     */
    fun createReset(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.ZER)
    }
    
    /**
     * Create an ERROR_QUERY packet (VB6: &H4C)
     */
    fun createErrorQuery(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.ERROR_QUERY)
    }
    
    /**
     * Create a TANK query packet
     */
    fun createTankQuery(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.TANK)
    }
    
    /**
     * Create a VOLUME query packet
     */
    fun createVolumeQuery(address: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.VOLUME)
    }
    
    /**
     * Create a PROG_PRC (price programming) packet (VB6: 0xA9)
     * Uses LSB-first encoding to match VB6 disp_setprice()
     * 
     * @param address Dispenser address
     * @param price Price in format "XX.XX" (e.g., "15.90" for 15.90 kr/liter)
     */
    fun createPriceProgram(address: Int, price: String): EhlPacket {
        require(price.matches(Regex("\\d{2}\\.\\d{2}"))) { 
            "Price must be in format XX.XX" 
        }
        
        // VB6 format: 4 ASCII digits LSB-first, øre/kr without decimal
        // Example: "15.90" → "1590" → bytes ['0','9','5','1']
        val priceStr = price.replace(".", "")  // "1590"
        val data = ByteArray(4)
        for (i in 0..3) {
            data[i] = priceStr[3 - i].code.toByte()  // LSB-first order
        }
        
        return EhlPacket(address, EhlCommand.PROG_PRC, data)
    }
    
    /**
     * Create a PRODUCT_SELECT packet (VB6: 0xC3)
     * Used for pistol/product selection before pricing operations
     * 
     * @param address Dispenser address
     * @param product Product selector (typically 0x30 for ASCII '0')
     */
    fun createProductSelect(address: Int, product: Byte = 0x30): EhlPacket {
        return EhlPacket(address, EhlCommand.PRODUCT_SELECT, byteArrayOf(product))
    }
    
    /**
     * Create a PROG_AMOUNT (amount preset) packet (VB6: &H75)
     * Uses LSB-first encoding to match VB6 set_preset_amount()
     * 
     * @param address Dispenser address
     * @param amountString Amount as 5-digit string (e.g., "12345" for 123.45 kr)
     */
    fun createAmountPreset(address: Int, amountString: String): EhlPacket {
        require(amountString.matches(Regex("\\d{5}"))) { 
            "Amount must be exactly 5 digits (e.g., '12345' for 123.45 kr)" 
        }
        
        // VB6 format: LSB-first (reverse order) ASCII encoding
        // amountString "12345" -> bytes ['5', '4', '3', '2', '1']
        val data = ByteArray(5)
        for (i in 0..4) {
            data[i] = amountString[4 - i].code.toByte()  // Reverse order
        }
        
        return EhlPacket(address, EhlCommand.PROG_AMOUNT, data)
    }
    
    /**
     * Create a PROG_VOLUME (volume preset) packet (VB6: &H70)
     * Uses LSB-first encoding to match VB6 set_preset_volume()
     * 
     * @param address Dispenser address
     * @param volumeString Volume as 6-digit string (e.g., "123456" for 1234.56 liters)
     */
    fun createVolumePreset(address: Int, volumeString: String): EhlPacket {
        require(volumeString.matches(Regex("\\d{6}"))) { 
            "Volume must be exactly 6 digits (e.g., '123456' for 1234.56 L)" 
        }
        
        // VB6 format: LSB-first (reverse order) ASCII encoding
        // volumeString "123456" -> bytes ['6', '5', '4', '3', '2', '1']
        val data = ByteArray(6)
        for (i in 0..5) {
            data[i] = volumeString[5 - i].code.toByte()  // Reverse order
        }
        
        return EhlPacket(address, EhlCommand.PROG_VOLUME, data)
    }
}

/**
 * Helper functions for parsing EHL packet data payloads
 */
object EhlDataParser {
    
    /**
     * Parse VOLUME response data
     * Format: volume in deciliters (2 bytes, big-endian) + amount in øre (2 bytes, big-endian)
     * 
     * @param data Raw data bytes from VOLUME response
     * @return Pair of (volumeLitres, amountCents)
     * @throws IllegalArgumentException if data format is invalid
     */
    fun parseVolumeData(data: ByteArray): Pair<Double, Int> {
        require(data.size == 4) { "VOLUME data must be exactly 4 bytes" }
        
        // Parse volume in deciliters (big-endian)
        val volumeDeciliters = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val volumeLitres = volumeDeciliters / 10.0
        
        // Parse amount in øre (big-endian)
        val amountCents = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        
        return Pair(volumeLitres, amountCents)
    }
    
    /**
     * Parse STATE response data
     * 
     * @param data Raw data bytes from STATE response
     * @return State code (0-9)
     * @throws IllegalArgumentException if data format is invalid
     */
    fun parseStateData(data: ByteArray): Int {
        require(data.size == 1) { "STATE data must be exactly 1 byte" }
        return data[0].toInt() and 0xFF
    }
    
    /**
     * Parse PRICE response data
     * Format: Price as 4 ASCII digits (reversed: pennies, dimes, ones, tens)
     * Example: "15.90" is encoded as ASCII '0', '9', '5', '1'
     * 
     * @param data Raw data bytes from PRICE response
     * @return Price as string in format "XX.XX"
     * @throws IllegalArgumentException if data format is invalid
     */
    fun parsePriceData(data: ByteArray): String {
        require(data.size == 4) { "PRICE data must be exactly 4 bytes" }
        
        // Extract ASCII digits (reversed order)
        val digit1 = (data[3].toInt() and 0xFF).toChar()  // Tens
        val digit2 = (data[2].toInt() and 0xFF).toChar()  // Ones
        val digit3 = (data[1].toInt() and 0xFF).toChar()  // Dimes
        val digit4 = (data[0].toInt() and 0xFF).toChar()  // Pennies
        
        require(digit1.isDigit() && digit2.isDigit() && digit3.isDigit() && digit4.isDigit()) {
            "PRICE data contains non-ASCII digits"
        }
        
        return "$digit1$digit2.$digit3$digit4"
    }
    
    /**
     * Parse ERROR response data (VB6 format)
     * VB6 returns 2 ASCII bytes: main code + sub code (logdisp_err function)
     * 
     * @param data Raw data bytes from ERROR response
     * @return Pair of (mainCode, subCode) as ASCII characters
     * @throws IllegalArgumentException if data format is invalid
     */
    fun parseErrorData(data: ByteArray): Pair<Char, Char> {
        require(data.size == 2) { "ERROR data must be exactly 2 ASCII bytes (VB6 format)" }
        
        val mainCode = (data[0].toInt() and 0xFF).toChar()
        val subCode = (data[1].toInt() and 0xFF).toChar()
        
        return Pair(mainCode, subCode)
    }
    
    /**
     * Parse ERROR response data (legacy format for compatibility)
     * 
     * @param data Raw data bytes from ERROR response  
     * @return Error code as integer
     * @throws IllegalArgumentException if data format is invalid
     */
    fun parseErrorDataLegacy(data: ByteArray): Int {
        require(data.size == 1) { "Legacy ERROR data must be exactly 1 byte" }
        return data[0].toInt() and 0xFF
    }
}
