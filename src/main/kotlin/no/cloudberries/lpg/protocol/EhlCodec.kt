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
    
    /**
     * Encode an EHL packet to raw bytes for transmission
     * 
     * @param packet The packet to encode
     * @return ByteArray ready for transmission over RS-485
     */
    fun encode(packet: EhlPacket): ByteArray {
        val result = ByteArray(packet.packetLength)
        var idx = 0
        
        // STX
        result[idx++] = EhlProtocol.STX
        
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
        result[idx++] = packet.calculateChecksum()
        
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
        
        // Check STX
        if (data[0] != EhlProtocol.STX) {
            return EhlPacketParseResult.InvalidFormat("Invalid STX byte: 0x${"%02X".format(data[0])}")
        }
        
        // Get length
        val length = data[1].toInt() and 0xFF
        
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
        val calculatedChecksum = packet.calculateChecksum()
        
        if (receivedChecksum != calculatedChecksum) {
            logger.warn("Checksum mismatch: expected 0x${"%02X".format(calculatedChecksum)}, got 0x${"%02X".format(receivedChecksum)}")
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
     * Create a PROG_PRC (price programming) packet
     * 
     * @param address Dispenser address
     * @param price Price in format "XX.XX" (e.g., "15.90" for 15.90 kr/liter)
     */
    fun createPriceProgram(address: Int, price: String): EhlPacket {
        require(price.matches(Regex("\\d{2}\\.\\d{2}"))) { 
            "Price must be in format XX.XX" 
        }
        
        // Convert price string to EHL format (4 ASCII digits)
        val parts = price.split(".")
        val data = byteArrayOf(
            parts[1][1].code.toByte(),  // Last decimal digit
            parts[1][0].code.toByte(),  // First decimal digit
            parts[0][1].code.toByte(),  // Last whole digit
            parts[0][0].code.toByte()   // First whole digit
        )
        
        return EhlPacket(address, EhlCommand.PROG_PRC, data)
    }
    
    /**
     * Create a PROG_W (value preset) packet
     * 
     * @param address Dispenser address
     * @param amount Amount in øre/cents (e.g., 50000 for 500.00 kr)
     */
    fun createValuePreset(address: Int, amount: Int): EhlPacket {
        require(amount >= 0) { "Amount must be non-negative" }
        
        // Convert amount to 4-byte BCD format
        val amountStr = "%08d".format(amount)
        val data = ByteArray(4)
        for (i in 0..3) {
            data[i] = amountStr[i * 2].code.toByte()
        }
        
        return EhlPacket(address, EhlCommand.PROG_W, data)
    }
}
