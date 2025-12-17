package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Codec Tests")
class EhlCodecTest {
    
    @Test
    @DisplayName("Encode simple STATE packet without data")
    fun testEncodeSimplePacket() {
        val packet = EhlPacket(
            address = 1,
            command = EhlCommand.STATE
        )
        
        val encoded = EhlCodec.encode(packet)
        
        // Expected: STX(0x10) LEN(0x06) ADDR(0x01) CMD(0x4B) CHKSUM ETX(0x36) - Controller to Dispenser
        assertEquals(6, encoded.size)
        assertEquals(0x10.toByte(), encoded[0]) // STX_CONTROLLER
        assertEquals(0x06.toByte(), encoded[1]) // Length
        assertEquals(0x01.toByte(), encoded[2]) // Address
        assertEquals(0x4B.toByte(), encoded[3]) // STATE command (75)
        assertEquals(0x36.toByte(), encoded[5]) // ETX
        
        // Verify checksum (using correct STX value)
        val expectedChecksum = (0x10 xor 0x06 xor 0x01 xor 0x4B).toByte()
        assertEquals(expectedChecksum, encoded[4])
    }
    
    @Test
    @DisplayName("Encode packet with data payload")
    fun testEncodePacketWithData() {
        val packet = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf(0x05)
        )
        
        val encoded = EhlCodec.encode(packet)
        
        assertEquals(7, encoded.size)
        assertEquals(0x10.toByte(), encoded[0]) // STX_CONTROLLER
        assertEquals(0x07.toByte(), encoded[1]) // Length
        assertEquals(0x01.toByte(), encoded[2]) // Address
        assertEquals(0x4B.toByte(), encoded[3]) // Command
        assertEquals(0x05.toByte(), encoded[4]) // Data
        assertEquals(0x36.toByte(), encoded[6]) // ETX
    }
    
    @Test
    @DisplayName("Decode valid packet successfully")
    fun testDecodeValidPacket() {
        // Test controller STX: STX(0x10) LEN(0x07) ADDR(0x01) CMD(0x4B) DATA(0x05) CHKSUM ETX(0x36)
        val checksumController = (0x10 xor 0x07 xor 0x01 xor 0x4B xor 0x05).toByte()
        val dataController = byteArrayOf(0x10, 0x07, 0x01, 0x4B, 0x05, checksumController, 0x36)
        
        val resultController = EhlCodec.decode(dataController)
        assertTrue(resultController is EhlPacketParseResult.Success)
        
        // Test dispenser STX: STX(0x20) LEN(0x07) ADDR(0x01) CMD(0x4B) DATA(0x05) CHKSUM ETX(0x36)
        val checksumDispenser = (0x20 xor 0x07 xor 0x01 xor 0x4B xor 0x05).toByte()
        val data = byteArrayOf(0x20, 0x07, 0x01, 0x4B, 0x05, checksumDispenser, 0x36)
        
        val result = EhlCodec.decode(data)
        
        assertTrue(result is EhlPacketParseResult.Success)
        val packet = (result as EhlPacketParseResult.Success).packet
        
        assertEquals(1, packet.address)
        assertEquals(EhlCommand.STATE, packet.command)
        assertArrayEquals(byteArrayOf(0x05), packet.data)
    }
    
    @Test
    @DisplayName("Decode packet with invalid STX")
    fun testDecodeInvalidSTX() {
        // Use 0x21 which is neither controller (0x10) nor dispenser (0x20) STX
        val data = byteArrayOf(0x21, 0x06, 0x01, 0x4B, 0x00, 0x36)
        
        val result = EhlCodec.decode(data)
        
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        assertTrue((result as EhlPacketParseResult.InvalidFormat).reason.contains("STX"))
    }
    
    @Test
    @DisplayName("Decode packet with invalid ETX")
    fun testDecodeInvalidETX() {
        val data = byteArrayOf(0x10, 0x06, 0x01, 0x4B, 0x00, 0x37) // Use controller STX
        
        val result = EhlCodec.decode(data)
        
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        assertTrue((result as EhlPacketParseResult.InvalidFormat).reason.contains("ETX"))
    }
    
    @Test
    @DisplayName("Decode packet with checksum error")
    fun testDecodeChecksumError() {
        // Intentionally wrong checksum, using controller STX
        val data = byteArrayOf(0x10, 0x06, 0x01, 0x4B, 0xFF.toByte(), 0x36)
        
        val result = EhlCodec.decode(data)
        
        assertTrue(result is EhlPacketParseResult.ChecksumError)
    }
    
    @Test
    @DisplayName("Decode incomplete packet")
    fun testDecodeIncompletePacket() {
        val data = byteArrayOf(0x10, 0x06, 0x01) // Use controller STX
        
        val result = EhlCodec.decode(data)
        
        assertTrue(result is EhlPacketParseResult.Incomplete)
    }
    
    @Test
    @DisplayName("Round-trip encode/decode")
    fun testRoundTrip() {
        val original = EhlPacket(
            address = 5,
            command = EhlCommand.UNBLOCK,
            data = byteArrayOf(0x01, 0x02, 0x03)
        )
        
        val encoded = EhlCodec.encode(original)
        val result = EhlCodec.decode(encoded)
        
        assertTrue(result is EhlPacketParseResult.Success)
        val decoded = (result as EhlPacketParseResult.Success).packet
        
        assertEquals(original, decoded)
    }
    
    @Test
    @DisplayName("Build STATE query packet")
    fun testBuildStateQuery() {
        val packet = EhlPacketBuilder.createStateQuery(1)
        
        assertEquals(1, packet.address)
        assertEquals(EhlCommand.STATE, packet.command)
        assertEquals(0, packet.data.size)
    }
    
    @Test
    @DisplayName("Build UNBLOCK packet")
    fun testBuildUnblock() {
        val packet = EhlPacketBuilder.createUnblock(2)
        
        assertEquals(2, packet.address)
        assertEquals(EhlCommand.UNBLOCK, packet.command)
    }
    
    @Test
    @DisplayName("Build price program packet")
    fun testBuildPriceProgram() {
        val packet = EhlPacketBuilder.createPriceProgram(1, "15.90")
        
        assertEquals(1, packet.address)
        assertEquals(EhlCommand.PROG_PRC, packet.command)
        assertEquals(4, packet.data.size)
        
        // Price "15.90" should encode as ASCII: '0', '9', '5', '1' (LSB-first from VB6)
        assertEquals('0'.code.toByte(), packet.data[0])
        assertEquals('9'.code.toByte(), packet.data[1])
        assertEquals('5'.code.toByte(), packet.data[2])
        assertEquals('1'.code.toByte(), packet.data[3])
    }
    
    @Test
    @DisplayName("Build amount preset packet - VB6 compatible")
    fun testBuildAmountPreset() {
        val packet = EhlPacketBuilder.createAmountPreset(1, "12345")
        
        assertEquals(1, packet.address)
        assertEquals(EhlCommand.PROG_AMOUNT, packet.command)
        assertEquals(5, packet.data.size)
        
        // Amount "12345" should encode as ASCII: '5', '4', '3', '2', '1' (LSB-first from VB6)
        assertEquals('5'.code.toByte(), packet.data[0])
        assertEquals('4'.code.toByte(), packet.data[1])
        assertEquals('3'.code.toByte(), packet.data[2])
        assertEquals('2'.code.toByte(), packet.data[3])
        assertEquals('1'.code.toByte(), packet.data[4])
    }
    
    @Test
    @DisplayName("Build volume preset packet - VB6 compatible")
    fun testBuildVolumePreset() {
        val packet = EhlPacketBuilder.createVolumePreset(2, "123456")
        
        assertEquals(2, packet.address)
        assertEquals(EhlCommand.PROG_VOLUME, packet.command)
        assertEquals(6, packet.data.size)
        
        // Volume "123456" should encode as ASCII: '6', '5', '4', '3', '2', '1' (LSB-first from VB6)
        assertEquals('6'.code.toByte(), packet.data[0])
        assertEquals('5'.code.toByte(), packet.data[1])
        assertEquals('4'.code.toByte(), packet.data[2])
        assertEquals('3'.code.toByte(), packet.data[3])
        assertEquals('2'.code.toByte(), packet.data[4])
        assertEquals('1'.code.toByte(), packet.data[5])
    }
    
    @Test
    @DisplayName("Build ERROR_QUERY packet")
    fun testBuildErrorQuery() {
        val packet = EhlPacketBuilder.createErrorQuery(3)
        
        assertEquals(3, packet.address)
        assertEquals(EhlCommand.ERROR_QUERY, packet.command)
        assertEquals(0, packet.data.size)
    }
    
    @Test
    @DisplayName("Build price program packet with invalid format throws exception")
    fun testBuildPriceProgramInvalidFormat() {
        assertThrows(IllegalArgumentException::class.java) {
            EhlPacketBuilder.createPriceProgram(1, "15.9")
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlPacketBuilder.createPriceProgram(1, "5.90")
        }
    }
}
