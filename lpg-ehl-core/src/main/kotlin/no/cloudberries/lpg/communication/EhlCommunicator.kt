package no.cloudberries.lpg.communication

import kotlinx.coroutines.*
import no.cloudberries.lpg.protocol.*
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Communicates with LPG dispensers using the EHL protocol over RS-485 serial connection.
 * Handles packet transmission, reception, buffering, and timeout management.
 * 
 * Uses SerialTransport interface for serial communication, allowing both real serial ports
 * and in-memory implementations for testing.
 */
class EhlCommunicator(private val transport: SerialTransport) {
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
        if (!transport.isConnected) {
            throw IOException("Serial port not connected")
        }

        val bytes = EhlCodec.encode(packet)
        
        // RAW HEX logging for observability (INFO level to ensure visibility)
        logger.info("📤 TX HEX: [${bytes.toHexString()}] -> ${packet.command}")
        
        // Detailed packet info at DEBUG level
        if (logger.isDebugEnabled) {
            logger.debug(EhlPacketFormatter.formatPacketForLogging(packet, EhlPacketFormatter.Direction.SENDING))
        }
        
        transport.write(bytes)
        transport.flush()
    }

    /**
     * Receive an EHL packet from the serial port.
     * Reads available data and attempts to parse a complete packet.
     *
     * @param timeoutMs Maximum time to wait for a complete packet (default: 5000ms)
     * @return Parsed EHL packet
     * @throws IOException if receive fails
     * @throws TimeoutException if no complete packet received within timeout
     */
    @Suppress("UNREACHABLE_CODE")
    suspend fun receive(timeoutMs: Long = 5000): EhlPacket = withTimeout(timeoutMs) {
        withContext(Dispatchers.IO) {
            var invalidByteStreak = 0
            val maxInvalidStreak = 10  // Maximum consecutive invalid bytes before clearing buffer
            
            while (true) {
                // Check for complete packet in buffer
                val packet = tryParseBuffer()
                if (packet != null) {
                    invalidByteStreak = 0
                    return@withContext packet
                }

                // Read more data from serial port
                val newData = transport.readAvailable()
                if (newData.isNotEmpty()) {
                    // RAW HEX logging for observability (INFO level to ensure visibility)
                    logger.info("📥 RX HEX: [${newData.toHexString()}]")
                    
                    synchronized(bufferLock) {
                        receiveBuffer.addAll(newData.toList())
                        if (logger.isDebugEnabled) {
                            logger.debug(EhlPacketFormatter.formatBufferStatus(receiveBuffer.size, "received data"))
                        }
                    }
                    invalidByteStreak = 0
                } else {
                    // No data available, wait a bit
                    delay(10)
                }

                // Prevent buffer overflow
                synchronized(bufferLock) {
                    if (receiveBuffer.size > MAX_BUFFER_SIZE) {
                        logger.warn(EhlPacketFormatter.formatError(
                            "Buffer Overflow",
                            "Buffer exceeded ${MAX_BUFFER_SIZE} bytes (${receiveBuffer.size}), clearing oldest data"
                        ))
                        receiveBuffer.subList(0, receiveBuffer.size - MAX_BUFFER_SIZE).clear()
                    }
                    
                    // If we keep receiving invalid data, something is wrong - clear buffer and reset
                    if (invalidByteStreak >= maxInvalidStreak) {
                        logger.error(EhlPacketFormatter.formatError(
                            "Too Many Invalid Bytes",
                            "$invalidByteStreak consecutive invalid bytes, clearing buffer to recover"
                        ))
                        receiveBuffer.clear()
                        invalidByteStreak = 0
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("Unreachable")
        }
    }

    /**
     * Try to parse a complete EHL packet from the receive buffer.
     * Handles edge cases like garbage data, partial packets, and multiple packets in buffer.
     *
     * @return Parsed packet if successful, null if incomplete or invalid
     */
    private fun tryParseBuffer(): EhlPacket? {
        synchronized(bufferLock) {
            if (receiveBuffer.isEmpty()) {
                return null
            }

            // Look for STX byte to synchronize (accept both controller and dispenser STX)
            val stxIndex = receiveBuffer.indexOfFirst { it == EhlProtocol.STX_CONTROLLER || it == EhlProtocol.STX_DISPENSER }
            if (stxIndex == -1) {
                // No STX in buffer - clear garbage data
                if (receiveBuffer.size > 10) {
                    logger.warn(EhlPacketFormatter.formatError(
                        "Synchronization Lost",
                        "No STX byte found in ${receiveBuffer.size} bytes, clearing garbage data"
                    ))
                    receiveBuffer.clear()
                }
                return null
            } else if (stxIndex > 0) {
                // Found STX but not at start - remove garbage before it
                if (logger.isDebugEnabled) {
                    logger.debug("🔧 Synchronization: Removed $stxIndex garbage bytes before STX")
                }
                receiveBuffer.subList(0, stxIndex).clear()
            }

            val bufferArray = receiveBuffer.toByteArray()
            
            when (val result = EhlCodec.decode(bufferArray)) {
                is EhlPacketParseResult.Success -> {
                    // Log parsed packet at INFO level for observability
                    logger.info("📥 RX PARSED: ${result.packet.command} from addr ${result.packet.address}")
                    
                    if (logger.isDebugEnabled) {
                        logger.debug(EhlPacketFormatter.formatPacketForLogging(
                            result.packet,
                            EhlPacketFormatter.Direction.RECEIVING
                        ))
                    }
                    
                    // Clear the parsed bytes from buffer
                    val packetLength = result.packet.packetLength
                    if (packetLength <= receiveBuffer.size) {
                        receiveBuffer.subList(0, packetLength).clear()
                        if (logger.isTraceEnabled) {
                            logger.trace(EhlPacketFormatter.formatBufferStatus(
                                receiveBuffer.size,
                                "after parsing packet"
                            ))
                        }
                        
                        // PRODUCTION OPTIMIZATION: After successful parse, check for additional STX bytes
                        // This handles back-to-back packets where the first might be corrupted noise
                        tryParseAdditionalPackets()
                    } else {
                        logger.error(EhlPacketFormatter.formatError(
                            "Packet Length Mismatch",
                            "Expected $packetLength bytes but buffer has ${receiveBuffer.size}"
                        ))
                        receiveBuffer.clear()
                    }
                    return result.packet
                }
                
                is EhlPacketParseResult.Incomplete -> {
                    if (logger.isDebugEnabled) {
                        logger.debug("⏳ Incomplete packet: ${receiveBuffer.size} bytes received, waiting for more data")
                    }
                    // Edge case: if buffer has been incomplete for too long, it might be corrupt
                    if (receiveBuffer.size >= EhlProtocol.MIN_PACKET_LENGTH + 10) {
                        logger.warn(EhlPacketFormatter.formatError(
                            "Possibly Corrupt Data",
                            "Buffer has ${receiveBuffer.size} bytes but packet still incomplete"
                        ))
                    }
                    return null
                }
                
                is EhlPacketParseResult.ChecksumError -> {
                    logger.warn("RS-485 transmission error: checksum mismatch (expected 0x%02X, got 0x%02X)".format(
                        result.expected, result.actual
                    ))
                    // ENHANCED RECOVERY: Look for next valid STX more intelligently
                    return handleCorruptedPacketRecovery()
                }
                
                is EhlPacketParseResult.InvalidFormat -> {
                    logger.warn("Invalid packet format: ${result.reason}")
                    // ENHANCED RECOVERY: Use same intelligent recovery as checksum errors
                    return handleCorruptedPacketRecovery()
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

    /**
     * PRODUCTION HELPER: Enhanced recovery from corrupted packets.
     * Intelligently searches for the next valid STX to minimize data loss.
     */
    private fun handleCorruptedPacketRecovery(): EhlPacket? {
        // Look for the next STX byte in the buffer, starting from position 1
        val remainingBuffer = receiveBuffer.drop(1)
        val nextStxIndex = remainingBuffer.indexOfFirst { 
            it == EhlProtocol.STX_CONTROLLER || it == EhlProtocol.STX_DISPENSER 
        }
        
        if (nextStxIndex >= 0) {
            // Found next STX - remove corrupted data up to (but not including) the new STX
            val bytesToRemove = nextStxIndex + 1
            receiveBuffer.subList(0, bytesToRemove).clear()
            if (logger.isDebugEnabled) {
                logger.debug("🔧 RS-485 Recovery: Found next STX, removed $bytesToRemove corrupted bytes")
            }
            
            // Try to parse the packet starting at the new STX position
            // This handles back-to-back packets where noise corrupted the first one
            return tryParseAtCurrentPosition()
        } else {
            // No next STX found - remove just the first byte (minimal data loss)
            receiveBuffer.removeAt(0)
            if (logger.isDebugEnabled) {
                logger.debug("🔧 RS-485 Recovery: No next STX found, removed 1 byte")
            }
        }
        
        return null
    }
    
    /**
     * PRODUCTION HELPER: Try to parse additional packets if buffer contains more STX bytes.
     * Optimizes back-to-back packet handling.
     */
    private fun tryParseAdditionalPackets() {
        // Only process if we have remaining data that might contain another packet
        if (receiveBuffer.size >= EhlProtocol.MIN_PACKET_LENGTH) {
            val hasAdditionalStx = receiveBuffer.any { 
                it == EhlProtocol.STX_CONTROLLER || it == EhlProtocol.STX_DISPENSER 
            }
            if (hasAdditionalStx && logger.isDebugEnabled) {
                logger.debug("🔄 Buffer contains additional STX bytes - will be processed on next iteration")
            }
        }
    }
    
    /**
     * PRODUCTION HELPER: Try to parse a packet at the current buffer position.
     * Returns packet if successful, null otherwise.
     */
    private fun tryParseAtCurrentPosition(): EhlPacket? {
        val bufferArray = receiveBuffer.toByteArray()
        return when (val result = EhlCodec.decode(bufferArray)) {
            is EhlPacketParseResult.Success -> {
                val packetLength = result.packet.packetLength
                if (packetLength <= receiveBuffer.size) {
                    receiveBuffer.subList(0, packetLength).clear()
                    result.packet
                } else {
                    null
                }
            }
            else -> null // Don't recursively handle errors here
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
