package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for hardened EHL protocol implementation
 * Validates safety checks and robust error handling for production RS-485 environments
 */
class EhlCodecHardenedTest {

    @Test
    fun `decode should reject packets with excessive length to prevent buffer abuse`() {
        // Create packet with enough bytes to pass minimum length check, but length header claims 200 bytes
        val oversizedPacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX
            200.toByte(),                   // Invalid large length (claims 200 bytes)
            1,                              // Address  
            EhlCommand.STATE.code.toByte(), // Command
            0x00,                           // Checksum (dummy)
            EhlProtocol.ETX                 // ETX
        )

        val result = EhlCodec.decode(oversizedPacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("exceeds maximum"))
        }
    }

    @Test
    fun `decode should reject packets with length below minimum`() {
        // Create packet with enough bytes to pass minimum check, but length header claims 2 bytes
        val undersizedPacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX
            2,                              // Invalid small length (claims only 2 bytes)
            1,                              // Address
            EhlCommand.STATE.code.toByte(), // Command  
            0x00,                           // Checksum (dummy)
            EhlProtocol.ETX                 // ETX
        )

        val result = EhlCodec.decode(undersizedPacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("below minimum"))
        }
    }

    @Test
    fun `decode should handle valid length within bounds correctly`() {
        // Create a valid minimal packet - let's calculate the correct checksum
        val packet = EhlPacket(1, EhlCommand.STATE)
        val encoded = EhlCodec.encode(packet, fromController = true)
        
        val result = EhlCodec.decode(encoded)
        assertTrue(result is EhlPacketParseResult.Success)
        assertEquals(1, result.packet.address)
        assertEquals(EhlCommand.STATE, result.packet.command)
    }

    @Test
    fun `decode should provide detailed checksum failure information`() {
        // First create a valid packet to get the correct checksum
        val validPacket = EhlPacket(1, EhlCommand.STATE)
        val encoded = EhlCodec.encode(validPacket, fromController = true)
        val correctChecksum = encoded[encoded.size - 2]  // Checksum is second to last byte
        
        // Now create a packet with intentionally wrong checksum
        val corruptedPacket = encoded.clone()
        corruptedPacket[corruptedPacket.size - 2] = 0xFF.toByte()  // Wrong checksum

        val result = EhlCodec.decode(corruptedPacket)
        assertTrue(result is EhlPacketParseResult.ChecksumError)
        // Verify we get the correct expected vs actual
        assertEquals(correctChecksum.toInt() and 0xFF, result.expected.toInt() and 0xFF)
        assertEquals(0xFF, result.actual.toInt() and 0xFF)
    }

    @Test
    fun `encode should create valid packets that pass decode validation`() {
        // Test round-trip encoding/decoding
        val originalPacket = EhlPacket(
            address = 1,
            command = EhlCommand.VOLUME,
            data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        )

        val encoded = EhlCodec.encode(originalPacket, fromController = true)
        val result = EhlCodec.decode(encoded)

        assertTrue(result is EhlPacketParseResult.Success)
        val decodedPacket = result.packet
        
        assertEquals(originalPacket.address, decodedPacket.address)
        assertEquals(originalPacket.command, decodedPacket.command)
        assertTrue(originalPacket.data.contentEquals(decodedPacket.data))
    }

    @Test
    fun `decode should handle both controller and dispenser STX values`() {
        // Test controller STX (0x10) - use proper encoding
        val controllerPacket = EhlPacket(1, EhlCommand.STATE)
        val controllerEncoded = EhlCodec.encode(controllerPacket, fromController = true)
        val controllerResult = EhlCodec.decode(controllerEncoded)
        assertTrue(controllerResult is EhlPacketParseResult.Success)

        // Test dispenser STX (0x20) - use proper encoding
        val dispenserPacket = EhlPacket(1, EhlCommand.STATE)
        val dispenserEncoded = EhlCodec.encode(dispenserPacket, fromController = false)
        val dispenserResult = EhlCodec.decode(dispenserEncoded)
        assertTrue(dispenserResult is EhlPacketParseResult.Success)
    }

    @Test
    fun `decode should reject packets with invalid STX values`() {
        val invalidStxPacket = byteArrayOf(
            0x99.toByte(),                  // Invalid STX
            6,
            1,
            EhlCommand.STATE.code.toByte(),
            0x00,
            EhlProtocol.ETX
        )

        val result = EhlCodec.decode(invalidStxPacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        assertTrue(result.reason.contains("Invalid STX"))
    }

    @Test
    fun `decode should handle maximum valid packet size correctly`() {
        // Create a packet at the maximum allowed size (64 bytes)
        val maxSizeData = ByteArray(64 - EhlProtocol.MIN_PACKET_LENGTH) { it.toByte() }
        val packet = EhlPacket(1, EhlCommand.PROG_PRC, maxSizeData)
        val encoded = EhlCodec.encode(packet)

        val result = EhlCodec.decode(encoded)
        assertTrue(result is EhlPacketParseResult.Success)
        assertEquals(64, result.packet.packetLength)
    }

    @Test
    fun `decode should handle incomplete packets gracefully`() {
        // Packet that claims to be 10 bytes but we only provide 5
        val incompletePacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,
            10,  // Claims to be 10 bytes
            1,
            EhlCommand.STATE.code.toByte(),
            0x00  // Only 5 bytes total, need 5 more
        )

        val result = EhlCodec.decode(incompletePacket)
        assertTrue(result is EhlPacketParseResult.Incomplete)
    }
}