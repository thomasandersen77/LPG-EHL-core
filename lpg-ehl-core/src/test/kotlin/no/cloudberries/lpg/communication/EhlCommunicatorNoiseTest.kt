package no.cloudberries.lpg.communication

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Tests for EhlCommunicator noise resilience and corruption handling.
 * Verifies that the hardened protocol can handle real-world RS-485 conditions.
 */
class EhlCommunicatorNoiseTest {

    private lateinit var serialPort: InMemorySerialPort
    private lateinit var communicator: EhlCommunicator
    private val logger = LoggerFactory.getLogger(EhlCommunicatorNoiseTest::class.java)

    @BeforeEach
    fun setup() {
        // Will be created per test with specific input data
    }

    private fun createCommunicator(inputData: ByteArray): EhlCommunicator {
        serialPort = InMemorySerialPort(inputData)
        serialPort.connect()
        return EhlCommunicator(serialPort)
    }
    
    /**
     * In-memory implementation of SerialPortIO for testing.
     */
    private class InMemorySerialPort(private val inputData: ByteArray) : SerialPortIO {
        private var inputStream = ByteArrayInputStream(inputData)
        private val outputStream = ByteArrayOutputStream()
        private var connected = false
        
        override val isConnected: Boolean
            get() = connected
        
        override fun connect(): Boolean {
            connected = true
            return true
        }
        
        override fun disconnect() {
            connected = false
        }
        
        override fun write(data: ByteArray): Int {
            outputStream.write(data)
            return data.size
        }
        
        override fun read(maxBytes: Int): ByteArray {
            val available = inputStream.available()
            if (available <= 0) {
                return ByteArray(0)
            }
            val toRead = minOf(available, maxBytes)
            val buffer = ByteArray(toRead)
            val bytesRead = inputStream.read(buffer)
            return buffer.copyOf(bytesRead)
        }
        
        override fun flush() {
            // No-op for in-memory
        }
    }

    @Test
    fun `The Noise Test - garbage bytes followed by valid packet`() = runBlocking {
        // Create a valid STATE query packet for address 1
        val validPacket = createStateQueryPacket(1)
        val validBytes = EhlCodec.encode(validPacket)
        
        // Prepare garbage + valid packet data
        val garbageBytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xAA.toByte())
        val inputData = garbageBytes + validBytes
        
        logger.info("Input data: ${formatBytes(inputData)}")
        logger.info("Valid packet starts at index ${garbageBytes.size}: ${formatBytes(validBytes)}")
        
        val communicator = createCommunicator(inputData)
        
        // Should skip garbage and find valid packet
        val receivedPacket = communicator.receive()
        
        assertNotNull(receivedPacket, "Should receive valid packet after skipping garbage")
        assertEquals(validPacket.address, receivedPacket!!.address)
        assertEquals(validPacket.command, receivedPacket.command)
        
        logger.info("✅ Noise test passed: Valid packet extracted from noisy data")
    }

    @Test
    fun `The Lying Packet Test - oversized length claim`() = runBlocking {
        // Create packet that claims to be 200 bytes long (way over MAX_PACKET_LENGTH = 64)
        val lyingPacket = byteArrayOf(
            0x02,           // STX
            200.toByte(),   // Length (LYING - claims 200 bytes)
            0x01,           // Address
            0x75,           // Command (STATE)
            0x50,           // Checksum (would be correct if length was 4)
            0x03            // ETX
        )
        
        logger.info("Lying packet: ${formatBytes(lyingPacket)}")
        
        val communicator = createCommunicator(lyingPacket)
        
        // Should immediately reject due to oversized length
        val receivedPacket = communicator.receive()
        
        assertNull(receivedPacket, "Should reject packet with oversized length claim")
        
        logger.info("✅ Lying packet test passed: Oversized length rejected immediately")
    }

    @Test
    fun `Multiple corrupted packets followed by valid packet`() = runBlocking {
        val validPacket = createStateQueryPacket(1)
        val validBytes = EhlCodec.encode(validPacket)
        
        // Multiple types of corruption
        val corruption1 = byteArrayOf(0x02, 0x04, 0x01, 0x75, 0xFF.toByte(), 0x03) // Bad checksum
        val corruption2 = byteArrayOf(0x02, 150.toByte(), 0x01) // Oversized length
        val corruption3 = byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x55.toByte()) // Pure garbage
        
        val inputData = corruption1 + corruption2 + corruption3 + validBytes
        
        logger.info("Complex corruption scenario with ${inputData.count()} total bytes")
        
        val communicator = createCommunicator(inputData)
        
        val receivedPacket = communicator.receive()
        
        assertNotNull(receivedPacket, "Should find valid packet after multiple corruptions")
        assertEquals(validPacket.address, receivedPacket!!.address)
        assertEquals(validPacket.command, receivedPacket.command)
        
        logger.info("✅ Complex corruption test passed")
    }

    @Test
    fun `Back-to-back valid packets with noise between`() = runBlocking {
        val packet1 = createStateQueryPacket(1)
        val packet2 = createStateQueryPacket(2)
        
        val bytes1 = EhlCodec.encode(packet1)
        val bytes2 = EhlCodec.encode(packet2)
        val noise = byteArrayOf(0xFF.toByte(), 0xAA.toByte())
        
        val inputData = bytes1 + noise + bytes2
        
        logger.info("Back-to-back packets with noise: ${inputData.count()} bytes")
        
        val communicator = createCommunicator(inputData)
        
        // Receive first packet
        val received1 = communicator.receive()
        assertNotNull(received1, "Should receive first packet")
        assertEquals(1, received1!!.address)
        
        // Receive second packet (after skipping noise)
        val received2 = communicator.receive()
        assertNotNull(received2, "Should receive second packet after noise")
        assertEquals(2, received2!!.address)
        
        logger.info("✅ Back-to-back packets test passed")
    }

    @Test
    fun `Undersized packet rejection`() = runBlocking {
        // Create packet shorter than MIN_PACKET_LENGTH (6)
        val tinyPacket = byteArrayOf(
            0x02,           // STX
            0x02,           // Length = 2 (too small)
            0x01,           // Address
            0x03            // ETX (missing command and checksum)
        )
        
        logger.info("Undersized packet: ${formatBytes(tinyPacket)}")
        
        val communicator = createCommunicator(tinyPacket)
        
        val receivedPacket = communicator.receive()
        
        assertNull(receivedPacket, "Should reject undersized packet")
        
        logger.info("✅ Undersized packet rejection test passed")
    }

    @Test
    fun `STX not at buffer start - should find next valid packet`() = runBlocking {
        val validPacket = createStateQueryPacket(1)
        val validBytes = EhlCodec.encode(validPacket)
        
        // Start with data that looks like a packet but isn't (missing STX)
        val fakeStart = byteArrayOf(0x04, 0x01, 0x75, 0x50, 0x03)
        val inputData = fakeStart + validBytes
        
        logger.info("Fake packet start + valid packet")
        
        val communicator = createCommunicator(inputData)
        
        val receivedPacket = communicator.receive()
        
        assertNotNull(receivedPacket, "Should find valid packet despite fake start")
        assertEquals(validPacket.address, receivedPacket!!.address)
        
        logger.info("✅ STX search test passed")
    }

    @Test
    fun `Real-world RS485 noise scenario`() = runBlocking {
        // Simulate realistic RS-485 line noise and transmission errors
        val validPacket = createPriceProgramPacket(1, "15.90")
        val validBytes = EhlCodec.encode(validPacket)
        
        // Common RS-485 noise patterns
        val lineNoise = byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0xFF.toByte()) // Power fluctuations
        val partialTransmission = byteArrayOf(0x02, 0x08, 0x01) // Incomplete packet
        val corruptedPacket = byteArrayOf(0x02, 0x04, 0x01, 0x75, 0xAA.toByte(), 0x03) // Bad checksum
        
        val realWorldData = lineNoise + partialTransmission + corruptedPacket + lineNoise + validBytes
        
        logger.info("Real-world RS-485 noise scenario: ${realWorldData.count()} bytes")
        logger.info("Data: ${formatBytes(realWorldData)}")
        
        val communicator = createCommunicator(realWorldData)
        
        val receivedPacket = communicator.receive()
        
        assertNotNull(receivedPacket, "Should extract valid packet from realistic noise")
        assertEquals(validPacket.address, receivedPacket!!.address)
        assertEquals(validPacket.command, receivedPacket.command)
        assertEquals(validPacket.data.count(), receivedPacket.data.count())
        
        logger.info("✅ Real-world RS-485 noise test passed")
    }

    @Test
    fun `Buffer boundary conditions`() = runBlocking {
        // Test packet that spans internal buffer boundaries
        val validPacket = createAmountPresetPacket(1, "50000")  // Longer packet
        val validBytes = EhlCodec.encode(validPacket)
        
        // Add padding to create boundary conditions
        val padding = ByteArray(100) { 0xFF.toByte() }
        val inputData = padding + validBytes
        
        logger.info("Buffer boundary test with ${inputData.count()} bytes of padding + packet")
        
        val communicator = createCommunicator(inputData)
        
        val receivedPacket = communicator.receive()
        
        assertNotNull(receivedPacket, "Should handle buffer boundary conditions")
        assertEquals(validPacket.address, receivedPacket!!.address)
        assertEquals(validPacket.command, receivedPacket.command)
        
        logger.info("✅ Buffer boundary test passed")
    }
    
    // Helper functions to create test packets
    private fun createStateQueryPacket(address: Int): EhlPacket {
        return no.cloudberries.lpg.protocol.EhlPacketBuilder.createStateQuery(address)
    }
    
    private fun createPriceProgramPacket(address: Int, price: String): EhlPacket {
        return no.cloudberries.lpg.protocol.EhlPacketBuilder.createPriceProgram(address, price)
    }
    
    private fun createAmountPresetPacket(address: Int, amount: String): EhlPacket {
        return no.cloudberries.lpg.protocol.EhlPacketBuilder.createAmountPreset(address, amount)
    }
    
    private fun formatBytes(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "0x%02X".format(it) }
    }
}
