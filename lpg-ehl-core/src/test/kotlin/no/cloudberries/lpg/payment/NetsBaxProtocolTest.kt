package no.cloudberries.lpg.payment

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * Tests for NetsBaxProtocol implementation
 * 
 * Covers both TCP/Ethernet and RS232/Serial framing modes
 */
class NetsBaxProtocolTest {
    
    @BeforeEach
    fun setup() {
        // Default to TCP mode for most tests
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
    }
    
    @AfterEach
    fun cleanup() {
        // Reset to default
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
    }
    
    // ===== TCP/ETHERNET MODE TESTS =====
    
    @Test
    fun `TCP mode - createPurchaseCommand generates correct format`() {
        val command = NetsBaxProtocol.createPurchaseCommand(amountCents = 200, operatorId = "1")
        
        // Expected: 2-byte length header + "P;10;1;200;0" (12 bytes)
        val payload = "P;10;1;200;0"
        val expectedLength = payload.length  // 12 bytes
        
        assertAll(
            { assertEquals(expectedLength + 2, command.size, "Total length should be payload + 2-byte header") },
            { assertEquals(0x00, command[0].toInt(), "High byte of length") },
            { assertEquals(0x0C, command[1].toInt(), "Low byte of length (12 decimal)") },
            { assertEquals(payload, String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)) }
        )
    }
    
    @Test
    fun `TCP mode - createPreauthCommand generates correct format`() {
        val command = NetsBaxProtocol.createPreauthCommand(amountCents = 500, operatorId = "2")
        
        val payload = "P;03;2;500;0"
        val expectedLength = payload.length
        
        assertAll(
            { assertEquals(expectedLength + 2, command.size) },
            { assertEquals(payload, String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)) }
        )
    }
    
    @Test
    fun `TCP mode - buildTcpFrame with short payload`() {
        val payload = "TEST"
        val frame = NetsBaxProtocol.buildTcpFrame(payload)
        
        assertAll(
            { assertEquals(6, frame.size, "4 bytes payload + 2 byte header") },
            { assertEquals(0x00, frame[0].toInt(), "High byte") },
            { assertEquals(0x04, frame[1].toInt(), "Low byte (4 decimal)") },
            { assertEquals("TEST", String(frame.copyOfRange(2, frame.size), Charsets.ISO_8859_1)) }
        )
    }
    
    @Test
    fun `TCP mode - buildTcpFrame with longer payload`() {
        val payload = "P;10;1;123456;0" // 15 bytes
        val frame = NetsBaxProtocol.buildTcpFrame(payload)
        
        assertAll(
            { assertEquals(17, frame.size, "15 bytes payload + 2 byte header") },
            { assertEquals(0x00, frame[0].toInt(), "High byte") },
            { assertEquals(0x0F, frame[1].toInt(), "Low byte (15 decimal)") }
        )
    }
    
    @Test
    fun `TCP mode - parseTcpResponse with length header`() {
        // Simulate response: length(13) + "A000ECR Timeout"
        val payload = "A000ECR Timeout"
        val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
        val length = payloadBytes.size
        
        val response = byteArrayOf(
            ((length shr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte()
        ) + payloadBytes
        
        val parsed = NetsBaxProtocol.parseResponse(response)
        
        assertTrue(parsed is BaxResponse.Data)
        assertEquals(payload.trim(), (parsed as BaxResponse.Data).payload)
    }
    
    @Test
    fun `TCP mode - parseTcpResponse without length header`() {
        // Some terminals may send raw payload without length header
        val payload = "D!000"
        val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
        
        val parsed = NetsBaxProtocol.parseResponse(payloadBytes)
        
        assertTrue(parsed is BaxResponse.Data)
        assertEquals(payload, (parsed as BaxResponse.Data).payload)
    }
    
    @Test
    fun `TCP mode - parseResponse handles Ingenico A000 format`() {
        val payload = "A000FEIL I"
        val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
        
        val parsed = NetsBaxProtocol.parseResponse(payloadBytes)
        
        assertTrue(parsed is BaxResponse.Data)
        assertEquals(payload, (parsed as BaxResponse.Data).payload)
    }
    
    @Test
    fun `TCP mode - parseResponse handles bracket format`() {
        val payload = "[00]"
        val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
        
        val parsed = NetsBaxProtocol.parseResponse(payloadBytes)
        
        assertTrue(parsed is BaxResponse.Data)
    }
    
    // ===== SERIAL MODE TESTS =====
    
    @Test
    fun `Serial mode - createPurchaseCommand generates correct format`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val command = NetsBaxProtocol.createPurchaseCommand(amountCents = 200, operatorId = "1")
        
        // Expected format: <STX>P,1,200<ETX><LRC>
        assertAll(
            { assertEquals(NetsBaxProtocol.STX, command[0], "Should start with STX") },
            { assertEquals(NetsBaxProtocol.ETX, command[command.size - 2], "Should have ETX before LRC") },
            { assertTrue(command.size > 4, "Should have STX + payload + ETX + LRC") }
        )
        
        // Verify payload uses comma delimiter
        val payload = String(command.copyOfRange(1, command.size - 2), Charsets.ISO_8859_1)
        assertEquals("P,1,200", payload)
    }
    
    @Test
    fun `Serial mode - buildSerialFrame with STX-ETX-LRC`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val payload = "TEST"
        val frame = NetsBaxProtocol.buildSerialFrame(payload)
        
        assertAll(
            { assertEquals(7, frame.size, "STX + 4 bytes + ETX + LRC") },
            { assertEquals(NetsBaxProtocol.STX, frame[0]) },
            { assertEquals(NetsBaxProtocol.ETX, frame[5]) },
            { assertEquals("TEST", String(frame.copyOfRange(1, 5), Charsets.ISO_8859_1)) }
        )
    }
    
    @Test
    fun `Serial mode - verifyLrc validates correct checksum`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val frame = NetsBaxProtocol.buildSerialFrame("TEST")
        
        assertTrue(NetsBaxProtocol.verifyLrc(frame), "LRC should be valid")
    }
    
    @Test
    fun `Serial mode - verifyLrc rejects corrupted checksum`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val frame = NetsBaxProtocol.buildSerialFrame("TEST")
        val corrupted = frame.clone()
        corrupted[corrupted.size - 1] = (corrupted.last().toInt() xor 0xFF).toByte() // Flip all bits
        
        assertFalse(NetsBaxProtocol.verifyLrc(corrupted), "LRC should be invalid")
    }
    
    @Test
    fun `Serial mode - parseSerialResponse with ACK`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val ackByte = byteArrayOf(NetsBaxProtocol.ACK)
        val parsed = NetsBaxProtocol.parseResponse(ackByte)
        
        assertTrue(parsed is BaxResponse.Ack)
    }
    
    @Test
    fun `Serial mode - parseSerialResponse with NAK`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val nakByte = byteArrayOf(NetsBaxProtocol.NAK)
        val parsed = NetsBaxProtocol.parseResponse(nakByte)
        
        assertTrue(parsed is BaxResponse.Nak)
    }
    
    // ===== EDGE CASES =====
    
    @Test
    fun `createPurchaseCommand rejects negative amount`() {
        assertThrows(IllegalArgumentException::class.java) {
            NetsBaxProtocol.createPurchaseCommand(amountCents = -100)
        }
    }
    
    @Test
    fun `createPurchaseCommand rejects zero amount`() {
        assertThrows(IllegalArgumentException::class.java) {
            NetsBaxProtocol.createPurchaseCommand(amountCents = 0)
        }
    }
    
    @Test
    fun `parseResponse handles empty response`() {
        val parsed = NetsBaxProtocol.parseResponse(byteArrayOf())
        
        assertTrue(parsed is BaxResponse.Error)
    }
    
    @Test
    fun `TCP mode handles large payloads with correct length header`() {
        val payload = "P;10;1;999999;0" + "X".repeat(200) // Large payload
        val frame = NetsBaxProtocol.buildTcpFrame(payload)
        
        val expectedLength = payload.length
        val actualLength = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        
        assertEquals(expectedLength, actualLength)
    }
    
    // ===== REFUND/REVERSAL TESTS =====
    
    @Test
    fun `TCP mode - createRefundCommand generates correct format`() {
        val command = NetsBaxProtocol.createRefundCommand(amountCents = 150, operatorId = "2")
        
        val payload = "P;20;2;150;0"
        val expectedLength = payload.length
        
        assertAll(
            { assertEquals(expectedLength + 2, command.size) },
            { assertEquals(payload, String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)) }
        )
    }
    
    @Test
    fun `TCP mode - createRefundCommand with transaction ID`() {
        val command = NetsBaxProtocol.createRefundCommand(
            amountCents = 150, 
            operatorId = "2",
            transactionId = "TX123"
        )
        
        val payloadString = String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)
        assertTrue(payloadString.contains("P;20;2;150;0;TX123"))
    }
    
    @Test
    fun `Serial mode - createRefundCommand generates correct format`() {
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.SERIAL
        
        val command = NetsBaxProtocol.createRefundCommand(amountCents = 150, operatorId = "2")
        
        // Verify payload uses comma delimiter and R command
        val payload = String(command.copyOfRange(1, command.size - 2), Charsets.ISO_8859_1)
        assertEquals("R,2,150", payload)
    }
    
    // ===== DETAILED STATUS TESTS =====
    
    @Test
    fun `TCP mode - createStatusCommand with specific type`() {
        val command = NetsBaxProtocol.createStatusCommand(statusType = "PRINTER")
        
        val payloadString = String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)
        assertEquals("P;90;PRINTER", payloadString)
    }
    
    @Test
    fun `TCP mode - createStatusCommand without type`() {
        val command = NetsBaxProtocol.createStatusCommand()
        
        val payloadString = String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1)
        assertEquals("S", payloadString)
    }
    
    // ===== EXACT FRAME VERIFICATION (Gemini Requirement) =====
    
    @Test
    fun `testTcpFramingPurchaseCommand - verifies exact TCP frame structure`() {
        // Set TCP mode explicitly
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
        
        // Create purchase command for 2.00 NOK
        val command = NetsBaxProtocol.createPurchaseCommand(amountCents = 200, operatorId = "1")
        
        // Expected payload: "P;10;1;200;0" (12 bytes)
        val expectedPayload = "P;10;1;200;0"
        val expectedLength = expectedPayload.length // 12
        
        assertAll(
            "TCP frame structure verification",
            // Verify frame starts with length header, not STX
            { assertEquals(0x00, command[0].toInt(), "High byte of length header") },
            { assertEquals(0x0C, command[1].toInt(), "Low byte of length (0x0C = 12 decimal)") },
            
            // Verify total length
            { assertEquals(expectedLength + 2, command.size, "Total frame size (header + payload)") },
            
            // Verify payload content
            { assertEquals(expectedPayload, String(command.copyOfRange(2, command.size), Charsets.ISO_8859_1), "Payload content") },
            
            // Verify NO STX/ETX/LRC in frame (critical for TCP mode)
            { assertFalse(command.contains(NetsBaxProtocol.STX), "Frame must NOT contain STX (0x02) in TCP mode") },
            { assertFalse(command.contains(NetsBaxProtocol.ETX), "Frame must NOT contain ETX (0x03) in TCP mode") },
            
            // Verify uses semicolon delimiter (TCP), not comma (Serial)
            { assertTrue(expectedPayload.contains(";"), "TCP mode uses semicolon delimiter") },
            { assertFalse(expectedPayload.contains(","), "TCP mode should not use comma delimiter") }
        )
    }
}
