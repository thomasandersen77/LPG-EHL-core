package no.cloudberries.lpg.communication

import kotlinx.coroutines.*
import no.cloudberries.lpg.protocol.EhlCodec
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketParseResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Communicates with LPG dispensers using the EHL protocol over RS-485 serial connection.
 * Handles packet transmission, reception, buffering, and timeout management.
 */
class EhlCommunicator(private val serialPortManager: SerialPortManager) {
    private val logger = LoggerFactory.getLogger(EhlCommunicator::class.java)
    private val receiveBuffer = mutableListOf<Byte>()
    private val bufferLock = Any()

    /**
     * Send an EHL packet and wait for a response.
     *
     * @param packet The EHL packet to send
     * @param timeoutMs Maximum time to wait for response in milliseconds
     * @return Response packet if successful
     * @throws IOException if communication fails
     * @throws TimeoutException if no response within timeout
     */
    suspend fun sendAndReceive(packet: EhlPacket, timeoutMs: Long = 2000): EhlPacket {
        return withTimeout(timeoutMs) {
            // Send packet
            send(packet)
            
            // Wait for response
            receive()
        }
    }

    /**
     * Send an EHL packet without waiting for response.
     *
     * @param packet The EHL packet to send
     * @throws IOException if send fails
     */
    fun send(packet: EhlPacket) {
        if (!serialPortManager.isConnected) {
            throw IOException("Serial port not connected")
        }

        val bytes = EhlCodec.encode(packet)
        logger.debug("Sending EHL packet to address ${packet.address}: $packet")
        
        serialPortManager.write(bytes)
        serialPortManager.flush()
        
        logger.debug("Sent ${bytes.size} bytes: ${bytes.toHexString()}")
    }

    /**
     * Receive an EHL packet from the serial port.
     * Reads available data and attempts to parse a complete packet.
     *
     * @return Parsed EHL packet
     * @throws IOException if receive fails
     * @throws IllegalStateException if unable to parse packet
     */
    @Suppress("UNREACHABLE_CODE")
    suspend fun receive(): EhlPacket = withContext(Dispatchers.IO) {
        while (true) {
            // Check for complete packet in buffer
            val packet = tryParseBuffer()
            if (packet != null) {
                return@withContext packet
            }

            // Read more data from serial port
            val newData = serialPortManager.read()
            if (newData.isNotEmpty()) {
                synchronized(bufferLock) {
                    receiveBuffer.addAll(newData.toList())
                    logger.debug("Buffer now has ${receiveBuffer.size} bytes")
                }
            } else {
                // No data available, wait a bit
                delay(10)
            }

            // Prevent buffer overflow
            synchronized(bufferLock) {
                if (receiveBuffer.size > MAX_BUFFER_SIZE) {
                    logger.warn("Receive buffer overflow, clearing oldest bytes")
                    receiveBuffer.subList(0, receiveBuffer.size - MAX_BUFFER_SIZE).clear()
                }
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }

    /**
     * Try to parse a complete EHL packet from the receive buffer.
     *
     * @return Parsed packet if successful, null if incomplete or invalid
     */
    private fun tryParseBuffer(): EhlPacket? {
        synchronized(bufferLock) {
            if (receiveBuffer.isEmpty()) {
                return null
            }

            val bufferArray = receiveBuffer.toByteArray()
            
            when (val result = EhlCodec.decode(bufferArray)) {
                is EhlPacketParseResult.Success -> {
                    logger.debug("Successfully parsed EHL packet: ${result.packet}")
                    // Clear the parsed bytes from buffer
                    val packetLength = EhlCodec.encode(result.packet).size
                    receiveBuffer.subList(0, packetLength).clear()
                    return result.packet
                }
                
                is EhlPacketParseResult.Incomplete -> {
                    logger.debug("Incomplete packet in buffer, waiting for more data")
                    return null
                }
                
                is EhlPacketParseResult.ChecksumError -> {
                    logger.error("Checksum error: expected ${result.expected}, got ${result.actual}")
                    // Remove invalid data from buffer
                    if (receiveBuffer.isNotEmpty()) {
                        receiveBuffer.removeAt(0)
                    }
                    return null
                }
                
                is EhlPacketParseResult.InvalidFormat -> {
                    logger.error("Invalid packet format: ${result.reason}")
                    // Remove invalid byte and try again
                    if (receiveBuffer.isNotEmpty()) {
                        receiveBuffer.removeAt(0)
                    }
                    return null
                }
            }
        }
    }

    /**
     * Clear the receive buffer.
     */
    fun clearBuffer() {
        synchronized(bufferLock) {
            receiveBuffer.clear()
            logger.debug("Receive buffer cleared")
        }
    }

    /**
     * Get the current buffer size.
     */
    fun getBufferSize(): Int {
        synchronized(bufferLock) {
            return receiveBuffer.size
        }
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 1024 // Maximum buffer size before overflow protection kicks in
    }
}

/**
 * Extension function to convert ByteArray to hex string for logging.
 */
private fun ByteArray.toHexString(): String {
    return joinToString(" ") { "%02X".format(it) }
}
