package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Payment Terminal Client
 * 
 * Low-level TCP client for communicating with payment terminals using BAX protocol.
 * Handles connection management, streaming responses, and proper timeout handling.
 * 
 * Design principles:
 * - Single Responsibility: Only handles TCP communication
 * - Robust streaming: Handles fragmented TCP responses
 * - Explicit timeout handling: Both connect and read timeouts
 * - Diagnostic logging: Logs local/remote addresses for troubleshooting
 * 
 * Usage:
 * ```kotlin
 * PaymentTerminalClient("192.168.0.4", 8009).use { client ->
 *     client.connect()
 *     val response = client.sendCommand(NetsBaxProtocol.createPurchaseCommand(100))
 * }
 * ```
 * 
 * @property host Terminal IP address
 * @property port Terminal port (default 8009 for ECR)
 * @property connectTimeoutMs Connection timeout in milliseconds
 * @property readTimeoutMs Read timeout for complete response
 */
class PaymentTerminalClient(
    private val host: String,
    private val port: Int = 8009,
    private val connectTimeoutMs: Int = 5000,
    private val readTimeoutMs: Int = 15000
) : Closeable {
    
    private val logger = LoggerFactory.getLogger(PaymentTerminalClient::class.java)
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    /**
     * Connection state
     */
    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false
    
    /**
     * Local address (for ECR whitelist verification)
     */
    val localAddress: String?
        get() = socket?.localAddress?.hostAddress
    
    /**
     * Local port
     */
    val localPort: Int?
        get() = socket?.localPort
    
    /**
     * Connect to the payment terminal
     * 
     * @throws IOException if connection fails
     */
    fun connect() {
        if (isConnected) {
            logger.debug("Already connected to $host:$port")
            return
        }
        
        logger.info("Connecting to terminal at $host:$port...")
        
        try {
            socket = Socket().apply {
                // Set socket options before connecting
                tcpNoDelay = true
                soTimeout = readTimeoutMs
            }
            
            socket!!.connect(InetSocketAddress(host, port), connectTimeoutMs)
            
            inputStream = socket!!.getInputStream()
            outputStream = socket!!.getOutputStream()
            
            logger.info("✅ Connected to terminal")
            logger.info("   Remote: $host:$port")
            logger.info("   Local:  $localAddress:$localPort")
            logger.info("   ⚠️  Verify that $localAddress is whitelisted in terminal ECR settings!")
            
        } catch (e: SocketTimeoutException) {
            cleanup()
            throw IOException("Connection timeout after ${connectTimeoutMs}ms to $host:$port", e)
        } catch (e: IOException) {
            cleanup()
            throw IOException("Failed to connect to $host:$port: ${e.message}", e)
        }
    }
    
    /**
     * Send a command and wait for complete response
     * 
     * @param command Complete BAX frame to send
     * @return TerminalResponse with all accumulated data
     */
    fun sendCommand(command: ByteArray): TerminalResponse {
        requireConnected()
        
        logger.debug("📤 Sending ${command.size} bytes: ${command.toHexString()}")
        
        try {
            outputStream!!.write(command)
            outputStream!!.flush()
            
            return readResponse()
            
        } catch (e: IOException) {
            logger.error("Failed to send command: ${e.message}")
            throw e
        }
    }
    
    /**
     * Read complete response from terminal
     * 
     * Handles:
     * - ACK/NAK followed by data
     * - Fragmented TCP packets
     * - Complete frame detection (STX...ETX+LRC)
     */
    private fun readResponse(): TerminalResponse {
        val accumulated = ArrayList<Byte>()
        val buffer = ByteArray(2048)
        val startTime = System.currentTimeMillis()
        
        var hasAck = false
        var hasNak = false
        var hasCompleteFrame = false
        
        logger.debug("⏳ Waiting for response (timeout: ${readTimeoutMs}ms)...")
        
        while (!hasCompleteFrame && (System.currentTimeMillis() - startTime) < readTimeoutMs) {
            try {
                val bytesRead = inputStream!!.read(buffer)
                
                if (bytesRead == -1) {
                    logger.warn("Socket closed by terminal")
                    break
                }
                
                // Accumulate bytes
                for (i in 0 until bytesRead) {
                    accumulated.add(buffer[i])
                }
                
                val current = accumulated.toByteArray()
                logger.debug("📥 Received $bytesRead bytes, total: ${current.size}")
                logger.debug("   HEX: ${current.toHexString()}")
                
                // Check for ACK/NAK
                if (current.contains(NetsBaxProtocol.ACK)) {
                    hasAck = true
                    logger.debug("   ✅ ACK detected")
                }
                if (current.contains(NetsBaxProtocol.NAK)) {
                    hasNak = true
                    logger.debug("   ❌ NAK detected")
                }
                
                // Check for complete frame
                when (val status = NetsBaxProtocol.checkFrameComplete(current)) {
                    is FrameStatus.Complete -> {
                        hasCompleteFrame = true
                        logger.debug("   🎉 Complete frame received")
                    }
                    is FrameStatus.NeedMoreData -> {
                        logger.debug("   ... ${status.reason}")
                    }
                    is FrameStatus.NoFrame -> {
                        // Just ACK/NAK, might get more data
                        if (hasAck || hasNak) {
                            // Wait a bit more for potential frame
                            Thread.sleep(100)
                        }
                    }
                    is FrameStatus.Empty -> {
                        // Should not happen after read
                    }
                }
                
                // If we only got ACK/NAK and no frame after brief wait, that's the complete response
                if ((hasAck || hasNak) && current.size == 1) {
                    break
                }
                
            } catch (e: SocketTimeoutException) {
                logger.debug("⏰ Read timeout - no more data")
                break
            }
        }
        
        val data = accumulated.toByteArray()
        val elapsedMs = System.currentTimeMillis() - startTime
        
        logger.info("Response received: ${data.size} bytes in ${elapsedMs}ms")
        
        return TerminalResponse(
            rawData = data,
            hasAck = hasAck,
            hasNak = hasNak,
            hasCompleteFrame = hasCompleteFrame,
            elapsedMs = elapsedMs
        )
    }
    
    /**
     * Send ACK to terminal
     */
    fun sendAck() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.ACK))
        outputStream!!.flush()
        logger.debug("📤 Sent ACK")
    }
    
    /**
     * Send NAK to terminal
     */
    fun sendNak() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.NAK))
        outputStream!!.flush()
        logger.debug("📤 Sent NAK")
    }
    
    private fun requireConnected() {
        if (!isConnected) {
            throw IllegalStateException("Not connected to terminal")
        }
    }
    
    private fun cleanup() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            // Ignore cleanup errors
        }
        inputStream = null
        outputStream = null
        socket = null
    }
    
    override fun close() {
        if (isConnected) {
            logger.info("Closing connection to $host:$port")
        }
        cleanup()
    }
    
    private fun ByteArray.toHexString(): String = 
        joinToString(" ") { "%02X".format(it) }
}

/**
 * Response from payment terminal
 */
data class TerminalResponse(
    val rawData: ByteArray,
    val hasAck: Boolean,
    val hasNak: Boolean,
    val hasCompleteFrame: Boolean,
    val elapsedMs: Long
) {
    /**
     * Parse the raw data into a BaxResponse
     */
    fun parse(): BaxResponse = NetsBaxProtocol.parseResponse(rawData)
    
    /**
     * Check if response indicates success (ACK or data received)
     */
    val isSuccess: Boolean
        get() = hasAck && !hasNak && rawData.isNotEmpty()
    
    /**
     * Check if response indicates failure
     */
    val isFailure: Boolean
        get() = hasNak || rawData.isEmpty()
    
    /**
     * Get hex representation of raw data
     */
    fun toHexString(): String = rawData.joinToString(" ") { "%02X".format(it) }
    
    /**
     * Get ASCII representation (printable chars only)
     */
    fun toAsciiString(): String = rawData.map { 
        val c = it.toInt() and 0xFF
        if (c in 32..126) c.toChar() else '.'
    }.joinToString("")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalResponse) return false
        return rawData.contentEquals(other.rawData) &&
               hasAck == other.hasAck &&
               hasNak == other.hasNak &&
               hasCompleteFrame == other.hasCompleteFrame
    }
    
    override fun hashCode(): Int {
        var result = rawData.contentHashCode()
        result = 31 * result + hasAck.hashCode()
        result = 31 * result + hasNak.hashCode()
        result = 31 * result + hasCompleteFrame.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "TerminalResponse(bytes=${rawData.size}, ack=$hasAck, nak=$hasNak, frame=$hasCompleteFrame, ms=$elapsedMs)"
    }
}
