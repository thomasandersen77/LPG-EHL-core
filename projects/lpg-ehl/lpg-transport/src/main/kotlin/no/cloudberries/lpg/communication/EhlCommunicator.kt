package no.cloudberries.lpg.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.cloudberries.lpg.protocol.*
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlin.math.min
import kotlin.math.pow

/**
 * Configuration for serial communication retry behavior.
 * 
 * @property maxRetries Maximum number of retry attempts (0 = no retry)
 * @property initialDelayMs Initial delay before first retry in milliseconds
 * @property maxDelayMs Maximum delay between retries (caps exponential backoff)
 * @property backoffMultiplier Multiplier for exponential backoff (e.g., 2.0 doubles delay each retry)
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 100,
    val maxDelayMs: Long = 2000,
    val backoffMultiplier: Double = 2.0
) {
    companion object {
        /** No retry - fail immediately on first error */
        val NO_RETRY = RetryConfig(maxRetries = 0)
        
        /** Default retry config for edge/industrial RS-485 environments */
        val DEFAULT = RetryConfig(
            maxRetries = 3,
            initialDelayMs = 100,
            maxDelayMs = 2000,
            backoffMultiplier = 2.0
        )
        
        /** Aggressive retry for unstable connections */
        val AGGRESSIVE = RetryConfig(
            maxRetries = 5,
            initialDelayMs = 50,
            maxDelayMs = 1000,
            backoffMultiplier = 1.5
        )
    }
}

/**
 * Communicates with LPG dispensers using the EHL protocol over RS-485 serial connection.
 * Handles packet transmission, reception, buffering, timeout management, and automatic retry.
 * 
 * Uses SerialTransport interface for serial communication, allowing both real serial ports
 * and in-memory implementations for testing.
 * 
 * Thread-safety: Uses Mutex to ensure single-flight request/response pattern.
 * 
 * **Retry Behavior:**
 * - Timeouts trigger automatic retry with exponential backoff
 * - Buffer is cleared between retries to ensure clean state
 * - IOException (port disconnected) does NOT retry - fails immediately
 * - Retry statistics are logged for monitoring
 * 
 * @property transport Serial transport implementation
 * @property enableRawLogging If true, logs raw TX/RX bytes at DEBUG level. If false, uses TRACE.
 * @property retryConfig Configuration for retry behavior (default: 3 retries with exponential backoff)
 */
class EhlCommunicator(
    private val transport: SerialTransport,
    private val enableRawLogging: Boolean = true,
    private val retryConfig: RetryConfig = RetryConfig.DEFAULT
) {
    private val logger = LoggerFactory.getLogger(EhlCommunicator::class.java)
    private val receiveBuffer = mutableListOf<Byte>()
    private val bufferLock = Any()
    private val txMutex = Mutex()  // Ensures only one request/response at a time
    private val attemptTracker: CommandAttemptTracker? = transport as? CommandAttemptTracker
    
    // Retry statistics for monitoring
    @Volatile private var totalRetries: Long = 0
    @Volatile private var successfulRetries: Long = 0
    @Volatile private var failedAfterRetries: Long = 0

    /**
     * Send an EHL packet and wait for a response with automatic retry on timeout.
     * Uses Mutex to ensure single-flight pattern (no concurrent requests).
     * 
     * Since Mutex guarantees exclusive ownership of the line, the first valid
     * response received is always intended for us - no filtering needed.
     * 
     * **Retry behavior:**
     * - TimeoutCancellationException triggers retry with exponential backoff
     * - IOException (serial port error) fails immediately - no retry
     * - Buffer is cleared between retries
     *
     * @param packet The EHL packet to send
     * @param timeoutMs Maximum time to wait for response in milliseconds
     * @return Response packet if successful
     * @throws IOException if communication fails (no retry)
     * @throws TimeoutCancellationException if no response after all retries exhausted
     */
    suspend fun sendAndReceive(
        packet: EhlPacket, 
        timeoutMs: Long = 3000
    ): EhlPacket {
        return txMutex.withLock {
            sendAndReceiveWithRetry(packet, timeoutMs)
        }
    }
    
    /**
     * Internal method that handles retry logic.
     * Must be called while holding txMutex.
     */
    private suspend fun sendAndReceiveWithRetry(
        packet: EhlPacket,
        timeoutMs: Long
    ): EhlPacket {
        var lastException: Exception? = null
        var attempt = 0
        val maxAttempts = retryConfig.maxRetries + 1  // +1 for initial attempt

        attemptTracker?.recordAttempt()
        
        while (attempt < maxAttempts) {
            try {
                return withTimeout(timeoutMs) {
                    // Clear buffer on retry to ensure clean state
                    if (attempt > 0) {
                        clearBuffer()
                        logger.debug("🔄 RETRY attempt $attempt/${retryConfig.maxRetries} for ${packet.command} to addr ${packet.address}")
                    }
                    
                    // Send packet
                    send(packet)
                    
                    // Since we own the mutex, first valid response is ours
                    val response = receive(timeoutMs)
                    attemptTracker?.recordSuccess()
                    response
                }
            } catch (e: TimeoutCancellationException) {
                lastException = e
                attempt++
                totalRetries++
                
                if (attempt < maxAttempts) {
                    val delayMs = calculateBackoffDelay(attempt)
                    logger.warn("⏱️ Timeout on ${packet.command} to addr ${packet.address} " +
                            "(attempt $attempt/$maxAttempts), retrying in ${delayMs}ms...")
                    delay(delayMs)
                } else {
                    failedAfterRetries++
                    logger.error("❌ All ${retryConfig.maxRetries} retries exhausted for ${packet.command} " +
                            "to addr ${packet.address}. Total timeout: ${timeoutMs * maxAttempts}ms")
                }
            } catch (e: IOException) {
                // Serial port error - don't retry, fail immediately
                logger.error("🔌 Serial port error (no retry): ${e.message}")
                attemptTracker?.recordFailure()
                attemptImmediateReconnect()
                throw e
            }
        }
        
        // All retries exhausted - lastException is guaranteed to be set since we only get here
        // after failing maxAttempts times, each of which sets lastException
        attemptTracker?.recordFailure()
        throw lastException!!
    }

    private fun attemptImmediateReconnect() {
        val watchdogCapable = transport as? HardwareWatchdogCapable ?: return
        try {
            logger.warn("🔧 Immediate reconnect triggered due to serial I/O error")
            val success = watchdogCapable.reconnect()
            if (success) {
                logger.info("🎉 Immediate reconnect succeeded after I/O error")
            } else {
                logger.error("💥 Immediate reconnect failed after I/O error")
            }
        } catch (e: Exception) {
            logger.error("Immediate reconnect failed with exception: ${e.message}", e)
        }
    }
    
    /**
     * Calculate backoff delay for a given retry attempt using exponential backoff.
     * 
     * @param attempt Retry attempt number (1-based)
     * @return Delay in milliseconds
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = (retryConfig.initialDelayMs * 
            retryConfig.backoffMultiplier.pow(attempt - 1)).toLong()
        return min(exponentialDelay, retryConfig.maxDelayMs)
    }
    
    /**
     * Get retry statistics for monitoring.
     * 
     * @return Map with retry statistics
     */
    fun getRetryStatistics(): Map<String, Any> = mapOf(
        "totalRetries" to totalRetries,
        "successfulRetries" to successfulRetries,
        "failedAfterRetries" to failedAfterRetries,
        "retryConfig" to mapOf(
            "maxRetries" to retryConfig.maxRetries,
            "initialDelayMs" to retryConfig.initialDelayMs,
            "maxDelayMs" to retryConfig.maxDelayMs,
            "backoffMultiplier" to retryConfig.backoffMultiplier
        )
    )
    
    /**
     * Reset retry statistics (useful for testing or monitoring reset).
     */
    fun resetRetryStatistics() {
        totalRetries = 0
        successfulRetries = 0
        failedAfterRetries = 0
    }

    /**
     * Execute a block while holding the RS-485 line lock (half-duplex).
     * Use this for multi-step sequences: drain → send → receiveUntil.
     *
     * @param block Suspending block that runs with exclusive line access
     * @return Result of the block
     */
    suspend fun <T> withExclusive(block: suspend EhlCommunicator.() -> T): T {
        return txMutex.withLock { block(this) }
    }

    /**
     * Execute a block with exclusive line access AND attempt tracking.
     *
     * Use this for multi-step command flows that manually call send()/receiveUntil().
     * This records a single attempt for the whole block and marks success/failure accordingly.
     *
     * IMPORTANT: Do not call sendAndReceive() inside this block, or you'll double-count attempts.
     */
    suspend fun <T> withExclusiveAttempt(block: suspend EhlCommunicator.() -> T): T {
        return txMutex.withLock {
            attemptTracker?.recordAttempt()
            try {
                val result = block(this)
                attemptTracker?.recordSuccess()
                result
            } catch (e: IOException) {
                attemptTracker?.recordFailure()
                attemptImmediateReconnect()
                throw e
            } catch (e: Exception) {
                attemptTracker?.recordFailure()
                throw e
            }
        }
    }

    /**
     * Drain the receive buffer and serial port for the given duration.
     * Reads and discards all incoming bytes. Use before sending to clear old traffic.
     *
     * MUST be called from within [withExclusive] for correct half-duplex behavior.
     *
     * @param durationMs How long to read and discard (e.g. 50–150 ms)
     */
    suspend fun drain(durationMs: Long) {
        require(durationMs in 0..10_000) { "drain duration must be 0–10000 ms, got $durationMs" }
        val deadline = System.currentTimeMillis() + durationMs
        var totalDiscarded = 0
        while (System.currentTimeMillis() < deadline) {
            val chunk = transport.readAvailable()
            if (chunk.isNotEmpty()) {
                totalDiscarded += chunk.size
                if (enableRawLogging) logger.debug("Drain: discarding ${chunk.size} bytes")
            }
            synchronized(bufferLock) { receiveBuffer.clear() }
            delay(10)
        }
        if (totalDiscarded > 0) logger.debug("Drained for ${durationMs}ms, discarded $totalDiscarded bytes")
    }

    /**
     * Receive packets until one matches the predicate or timeout.
     * Ignores non-matching packets (logs at DEBUG: "ignored while awaiting ...").
     *
     * MUST be called from within [withExclusive] for correct half-duplex behavior.
     *
     * @param timeoutMs Maximum time to wait
     * @param predicate Return true to accept the packet
     * @param awaitingLabel Label for log when ignoring packets (e.g. "STATE(open bit 0x02)")
     * @return Matching packet, or null on timeout
     */
    suspend fun receiveUntil(
        timeoutMs: Long,
        predicate: (EhlPacket) -> Boolean,
        awaitingLabel: String = ""
    ): EhlPacket? = withContext(Dispatchers.IO) {
        val label = awaitingLabel.ifEmpty { "predicate" }
        var invalidByteStreak = 0
        val maxInvalidStreak = 10
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            when (val result = tryParseBufferWithStatus()) {
                is ParseStatus.Success -> {
                    invalidByteStreak = 0
                    val p = result.packet
                    if (predicate(p)) {
                        logger.debug("📥 Matched: ${p.command} addr=${p.address}")
                        return@withContext p
                    }
                    logger.debug("Ignored ${p.command} addr=${p.address} while awaiting $label")
                }
                is ParseStatus.Incomplete -> {
                    if (synchronized(bufferLock) { receiveBuffer.size } >= EhlProtocol.MIN_PACKET_LENGTH) {
                        invalidByteStreak++
                    }
                }
                is ParseStatus.ParseError -> invalidByteStreak++
            }

            val newData = transport.readAvailable()
            if (newData.isNotEmpty()) {
                if (enableRawLogging) logger.debug("📥 RX HEX: [${newData.toHexString()}]")
                synchronized(bufferLock) { receiveBuffer.addAll(newData.toList()) }
            }

            synchronized(bufferLock) {
                if (receiveBuffer.size > MAX_BUFFER_SIZE) {
                    receiveBuffer.subList(0, receiveBuffer.size - MAX_BUFFER_SIZE).clear()
                }
                if (invalidByteStreak >= maxInvalidStreak) {
                    receiveBuffer.clear()
                    invalidByteStreak = 0
                }
            }
            delay(10)
        }
        null
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
        
        // RAW HEX logging for protocol debugging (configurable level)
        if (enableRawLogging) {
            logger.debug("📤 TX HEX: [${bytes.toHexString()}] -> ${packet.command}")
        } else {
            logger.trace("📤 TX HEX: [${bytes.toHexString()}] -> ${packet.command}")
        }
        
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
            val maxInvalidStreak = 10  // Maximum consecutive parse errors before clearing buffer
            
            while (true) {
                // Check for complete packet in buffer
                val bufferSizeBeforeParse = synchronized(bufferLock) { receiveBuffer.size }
                
                when (val result = tryParseBufferWithStatus()) {
                    is ParseStatus.Success -> {
                        invalidByteStreak = 0
                        return@withContext result.packet
                    }
                    is ParseStatus.Incomplete -> {
                        // If buffer has enough data for a packet but still incomplete,
                        // it might be noise/garbage - increment streak
                        if (bufferSizeBeforeParse >= EhlProtocol.MIN_PACKET_LENGTH) {
                            invalidByteStreak++
                            if (logger.isDebugEnabled) {
                                logger.debug("Buffer has $bufferSizeBeforeParse bytes but incomplete (streak=$invalidByteStreak) - possible noise")
                            }
                        }
                    }
                    is ParseStatus.ParseError -> {
                        // Checksum or format error - increment streak
                        invalidByteStreak++
                        logger.debug("Parse error (streak=$invalidByteStreak): ${result.reason}")
                    }
                }

                // Read more data from serial port
                val newData = transport.readAvailable()
                if (newData.isNotEmpty()) {
                    // RAW HEX logging for protocol debugging (configurable level)
                    if (enableRawLogging) {
                        logger.debug("📥 RX HEX: [${newData.toHexString()}]")
                    } else {
                        logger.trace("📥 RX HEX: [${newData.toHexString()}]")
                    }
                    
                    synchronized(bufferLock) {
                        receiveBuffer.addAll(newData.toList())
                        if (logger.isDebugEnabled) {
                            logger.debug(EhlPacketFormatter.formatBufferStatus(receiveBuffer.size, "received data"))
                        }
                    }
                } else {
                    // No data available, wait a bit
                    delay(10)
                }

                // Prevent buffer overflow and handle persistent errors
                synchronized(bufferLock) {
                    if (receiveBuffer.size > MAX_BUFFER_SIZE) {
                        logger.warn(EhlPacketFormatter.formatError(
                            "Buffer Overflow",
                            "Buffer exceeded ${MAX_BUFFER_SIZE} bytes (${receiveBuffer.size}), clearing oldest data"
                        ))
                        receiveBuffer.subList(0, receiveBuffer.size - MAX_BUFFER_SIZE).clear()
                    }
                    
                    // If we keep getting parse errors, something is wrong - clear buffer and reset
                    if (invalidByteStreak >= maxInvalidStreak) {
                        logger.error(EhlPacketFormatter.formatError(
                            "Too Many Parse Errors",
                            "$invalidByteStreak consecutive parse errors, clearing buffer to recover"
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
     * Internal status for buffer parsing
     */
    private sealed class ParseStatus {
        data class Success(val packet: EhlPacket) : ParseStatus()
        data object Incomplete : ParseStatus()
        data class ParseError(val reason: String) : ParseStatus()
    }

    /**
     * Try to parse a complete EHL packet from the receive buffer.
     * Returns status indicating success, incomplete, or error.
     */
    private fun tryParseBufferWithStatus(): ParseStatus {
        synchronized(bufferLock) {
            if (receiveBuffer.isEmpty()) {
                return ParseStatus.Incomplete
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
                return ParseStatus.Incomplete
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
                    // Log parsed packet at DEBUG level (routine protocol traffic)
                    logger.debug("📥 RX PARSED: ${result.packet.command} from addr ${result.packet.address}")
                    
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
                        tryParseAdditionalPackets()
                    } else {
                        logger.error(EhlPacketFormatter.formatError(
                            "Packet Length Mismatch",
                            "Expected $packetLength bytes but buffer has ${receiveBuffer.size}"
                        ))
                        receiveBuffer.clear()
                    }
                    return ParseStatus.Success(result.packet)
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
                    return ParseStatus.Incomplete
                }
                
                is EhlPacketParseResult.ChecksumError -> {
                    val reason = "Checksum mismatch (expected 0x%02X, got 0x%02X)".format(
                        result.expected, result.actual
                    )
                    logger.warn("RS-485 transmission error: $reason")
                    // ENHANCED RECOVERY: Look for next valid STX more intelligently
                    handleCorruptedPacketRecovery()
                    return ParseStatus.ParseError(reason)
                }
                
                is EhlPacketParseResult.InvalidFormat -> {
                    logger.warn("Invalid packet format: ${result.reason}")
                    // ENHANCED RECOVERY: Use same intelligent recovery as checksum errors
                    handleCorruptedPacketRecovery()
                    return ParseStatus.ParseError(result.reason)
                }
            }
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * @return Parsed packet if successful, null if incomplete or invalid
     */
    private fun tryParseBuffer(): EhlPacket? {
        return when (val status = tryParseBufferWithStatus()) {
            is ParseStatus.Success -> status.packet
            else -> null
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
    private fun handleCorruptedPacketRecovery() {
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
        } else {
            // No next STX found - remove just the first byte (minimal data loss)
            receiveBuffer.removeAt(0)
            if (logger.isDebugEnabled) {
                logger.debug("🔧 RS-485 Recovery: No next STX found, removed 1 byte")
            }
        }
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
