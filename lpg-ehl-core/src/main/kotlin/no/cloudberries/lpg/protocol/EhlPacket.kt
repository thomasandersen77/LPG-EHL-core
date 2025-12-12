package no.cloudberries.lpg.protocol

/**
 * EHL Protocol Packet
 * 
 * Represents a complete EHL protocol packet with the structure:
 * STX (1 byte) + Length (1 byte) + Address (1 byte) + Command (1 byte) + Data (0-n bytes) + Checksum (1 byte) + ETX (1 byte)
 * 
 * Example packet: 0x20 0x07 0x01 0x4B 0x05 0x6F 0x36
 *   - STX: 0x20
 *   - Length: 0x07 (total length of packet)
 *   - Address: 0x01 (dispenser address)
 *   - Command: 0x4B (STATE command, code 75)
 *   - Data: 0x05 (state value)
 *   - Checksum: 0x6F (XOR of bytes 1-4)
 *   - ETX: 0x36
 * 
 * @property address Dispenser address (1-255)
 * @property command EHL command type
 * @property data Optional data payload
 */
data class EhlPacket(
    val address: Int,
    val command: EhlCommand,
    val data: ByteArray = ByteArray(0)
) {
    init {
        require(address in 1..255) { "Address must be between 1 and 255" }
        require(data.size <= EhlProtocol.MAX_PACKET_LENGTH - EhlProtocol.MIN_PACKET_LENGTH) {
            "Data payload too large"
        }
    }
    
    /**
     * Calculate total packet length (including STX and ETX)
     */
    val packetLength: Int
        get() = EhlProtocol.MIN_PACKET_LENGTH + data.size
    
    /**
     * Calculate checksum (XOR of all bytes from STX to last data byte, excluding checksum and ETX)
     */
    fun calculateChecksum(): Byte {
        var checksum: Byte = EhlProtocol.STX
        checksum = (checksum.toInt() xor packetLength).toByte()
        checksum = (checksum.toInt() xor address).toByte()
        checksum = (checksum.toInt() xor command.code).toByte()
        
        for (byte in data) {
            checksum = (checksum.toInt() xor byte.toInt()).toByte()
        }
        
        return checksum
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EhlPacket
        
        if (address != other.address) return false
        if (command != other.command) return false
        if (!data.contentEquals(other.data)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = address
        result = 31 * result + command.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
    
    override fun toString(): String {
        val dataHex = data.joinToString(" ") { "%02X".format(it) }
        return "EhlPacket(addr=$address, cmd=${command.name}(${command.code}), data=[$dataHex], chksum=${"%02X".format(calculateChecksum())})"
    }
}

/**
 * Result of parsing an EHL packet
 */
sealed class EhlPacketParseResult {
    /** Successfully parsed packet */
    data class Success(val packet: EhlPacket) : EhlPacketParseResult()
    
    /** Invalid packet format */
    data class InvalidFormat(val reason: String) : EhlPacketParseResult()
    
    /** Checksum mismatch */
    data class ChecksumError(val expected: Byte, val actual: Byte) : EhlPacketParseResult()
    
    /** Incomplete packet (need more data) */
    data object Incomplete : EhlPacketParseResult()
}
