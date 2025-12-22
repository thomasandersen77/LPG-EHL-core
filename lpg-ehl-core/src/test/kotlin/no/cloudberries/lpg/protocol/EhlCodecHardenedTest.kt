package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
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
    
    // ============================================================
    // ADDITIONAL MALFORMED PACKET TESTS
    // ============================================================
    
    @Test
    fun `decode should reject extremely large length claims without memory exhaustion`() {
        // Create packet claiming to be 255 bytes (maximum byte value)
        val extremePacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX
            255.toByte(),                   // Claims 255 bytes (impossible/dangerous)
            1,                              // Address
            EhlCommand.STATE.code.toByte(), // Command
            0x00,                           // Checksum
            EhlProtocol.ETX                 // ETX
        )
        
        val result = EhlCodec.decode(extremePacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("exceeds maximum"))
        }
    }
    
    @Test
    fun `decode should reject zero length packets`() {
        val zeroLengthPacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX
            0,                              // Length = 0 (invalid)
            1,                              // Address
            EhlCommand.STATE.code.toByte(), // Command
            0x00,                           // Checksum
            EhlProtocol.ETX                 // ETX
        )
        
        val result = EhlCodec.decode(zeroLengthPacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("below minimum"))
        }
    }
    
    @Test
    fun `decode should reject packets with length = 1 (too small for any valid packet)`() {
        val tinyPacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX
            1,                              // Length = 1 (impossible - minimum is 4)
            1,                              // Address
            EhlCommand.STATE.code.toByte(), // Command
            0x00,                           // Checksum
            EhlProtocol.ETX                 // ETX
        )
        
        val result = EhlCodec.decode(tinyPacket)
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("below minimum"))
        }
    }
    
    @Test
    fun `decode should handle various invalid STX values robustly`() {
        val invalidStxValues = listOf(
            0x00.toByte(), 0x01.toByte(), 0x0F.toByte(), // Too low
            0x30.toByte(), 0x40.toByte(), 0xFF.toByte()  // Too high or random
        )
        
        for (invalidStx in invalidStxValues) {
            val invalidPacket = byteArrayOf(
                invalidStx,                         // Invalid STX
                6,                                  // Valid length
                1,                                  // Address
                EhlCommand.STATE.code.toByte(),     // Command
                0x00,                               // Checksum
                EhlProtocol.ETX                     // ETX
            )
            
            val result = EhlCodec.decode(invalidPacket)
            assertTrue(result is EhlPacketParseResult.InvalidFormat, 
                "STX value 0x${String.format("%02X", invalidStx)} should be rejected")
            if (result is EhlPacketParseResult.InvalidFormat) {
                assertTrue(result.reason.contains("Invalid STX"))
            }
        }
    }
    
    @Test
    fun `decode should detect when actual packet size doesn't match length claim`() {
        // Create packet claiming 8 bytes but providing only 6 total bytes
        val lyingPacket = byteArrayOf(
            EhlProtocol.STX_CONTROLLER,     // STX     (1 byte)
            8,                              // Length  (1 byte) - CLAIMS 8 bytes total
            1,                              // Address (1 byte)
            EhlCommand.STATE.code.toByte(), // Command (1 byte) 
            0x00,                           // Checksum(1 byte)
            EhlProtocol.ETX                 // ETX     (1 byte) = 6 bytes actual
        )
        
        val result = EhlCodec.decode(lyingPacket)
        assertTrue(result is EhlPacketParseResult.Incomplete, 
            "Packet claiming 8 bytes but only providing 6 should be incomplete")
    }
    
    @Test
    fun `decode should handle corrupted ETX bytes`() {
        // Create valid packet but corrupt the ETX
        val validPacket = EhlPacket(1, EhlCommand.STATE)
        val encoded = EhlCodec.encode(validPacket, fromController = true)
        val corruptedEtx = encoded.clone()
        corruptedEtx[corruptedEtx.size - 1] = 0x99.toByte() // Wrong ETX
        
        val result = EhlCodec.decode(corruptedEtx)
        // Should be invalid format due to missing proper ETX
        assertTrue(result is EhlPacketParseResult.InvalidFormat)
        if (result is EhlPacketParseResult.InvalidFormat) {
            assertTrue(result.reason.contains("ETX") || result.reason.contains("format"))
        }
    }
    
    @Test
    fun `decode should be safe from buffer overruns with crafted packets`() {
        // Test boundary conditions that could cause buffer overruns
        val boundaryTests = listOf(
            // Packet claiming exactly maximum length
            byteArrayOf(EhlProtocol.STX_CONTROLLER, 64, 1, 0x75.toByte()) + ByteArray(60) + byteArrayOf(0x00, EhlProtocol.ETX),
            // Empty data array
            byteArrayOf(),
            // Single byte
            byteArrayOf(EhlProtocol.STX_CONTROLLER),
            // Just STX and length
            byteArrayOf(EhlProtocol.STX_CONTROLLER, 6)
        )
        
        for ((index, testPacket) in boundaryTests.withIndex()) {
            assertDoesNotThrow("Boundary test $index should not crash") {
                val result = EhlCodec.decode(testPacket)
                // Should return either Incomplete or InvalidFormat, never crash
                assertTrue(
                    result is EhlPacketParseResult.Incomplete || 
                    result is EhlPacketParseResult.InvalidFormat,
                    "Boundary test $index should return safe error type"
                )
            }
        }
    }
    
    @Test
    fun `decode should handle mixed controller and dispenser STX in sequence`() {
        // Test that both STX types are accepted but invalid ones rejected
        val controllerPacket = EhlPacket(1, EhlCommand.STATE)
        val dispenserPacket = EhlPacket(2, EhlCommand.STATE)
        
        val controllerBytes = EhlCodec.encode(controllerPacket, fromController = true)
        val dispenserBytes = EhlCodec.encode(dispenserPacket, fromController = false)
        
        // Both should decode successfully
        val controllerResult = EhlCodec.decode(controllerBytes)
        val dispenserResult = EhlCodec.decode(dispenserBytes)
        
        assertTrue(controllerResult is EhlPacketParseResult.Success)
        assertTrue(dispenserResult is EhlPacketParseResult.Success)
        
        // Verify STX bytes are as expected
        assertEquals(EhlProtocol.STX_CONTROLLER, controllerBytes[0])
        assertEquals(EhlProtocol.STX_DISPENSER, dispenserBytes[0])
    }
    
    @Test
    fun `decode should provide meaningful error messages for debugging`() {
        val testCases = listOf(
            Pair(byteArrayOf(0xFF.toByte(), 6, 1, 0x75.toByte(), 0x00, EhlProtocol.ETX), "Invalid STX byte"),
            Pair(byteArrayOf(EhlProtocol.STX_CONTROLLER, 200.toByte(), 1, 0x75.toByte(), 0x00, EhlProtocol.ETX), "exceeds maximum"),
            Pair(byteArrayOf(EhlProtocol.STX_CONTROLLER, 1, 1, 0x75.toByte(), 0x00, EhlProtocol.ETX), "below minimum")
        )
        
        for ((packet, expectedErrorKeyword) in testCases) {
            val result = EhlCodec.decode(packet)
            assertTrue(result is EhlPacketParseResult.InvalidFormat)
            assertTrue(
                result.reason.contains(expectedErrorKeyword, ignoreCase = true),
                "Error message '${result.reason}' should contain '$expectedErrorKeyword'"
            )
        }
    }
    
    @Test
    fun `decode should never throw exceptions for any malformed input`() {
        // Stress test with completely random data
        val randomInputs = listOf(
            ByteArray(0),                           // Empty
            ByteArray(1) { 0xFF.toByte() },        // Single 0xFF
            ByteArray(100) { 0xAA.toByte() },      // Lots of 0xAA
            ByteArray(10) { it.toByte() },         // Sequential bytes
            ByteArray(50) { (-it).toByte() }       // Negative bytes
        )
        
        for ((index, randomInput) in randomInputs.withIndex()) {
            assertDoesNotThrow("Random input test $index should not throw") {
                val result = EhlCodec.decode(randomInput)
                // Should always return a proper result type, never throw
                assertTrue(
                    result is EhlPacketParseResult.Success ||
                    result is EhlPacketParseResult.InvalidFormat ||
                    result is EhlPacketParseResult.Incomplete ||
                    result is EhlPacketParseResult.ChecksumError,
                    "Random input $index should return valid result type"
                )
            }
        }
    }
}
