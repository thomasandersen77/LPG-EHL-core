package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Cloud Terminal Client
 * 
 * SSL/TLS client for communicating with payment terminals via Nets Cloud Connect.
 * 
 * This client establishes an encrypted SSL/TLS connection to Nets Cloud Connect infrastructure
 * (IP: 3.33.230.243, Port: 6001) and uses the standard BAX protocol for commands/responses.
 * 
 * **Architecture:**
 * ```
 * [LPG Edge] <--SSL/TLS--> [Nets Cloud 3.33.230.243:6001] <--ECR Protocol--> [Terminal]
 * ```
 * 
 * **Key Differences from Direct TCP:**
 * - Uses SSLSocket instead of plain Socket
 * - Connects to Nets Cloud infrastructure, not directly to terminal
 * - Encrypted communication via TLS
 * - Same BAX protocol payload format
 * 
 * **Usage:**
 * ```kotlin
 * CloudTerminalClient("3.33.230.243", 6001).use { client ->
 *     client.connect()
 *     val response = client.sendCommand(NetsBaxProtocol.createPurchaseCommand(100))
 * }
 * ```
 * 
 * @property host Nets Cloud Connect host (default: 3.33.230.243)
 * @property port Nets Cloud Connect port (default: 6001)
 * @property connectTimeoutMs Connection timeout in milliseconds
 * @property readTimeoutMs Read timeout for complete response
 * 
 * @see NetsBaxProtocol for command creation and response parsing
 */
class CloudTerminalClient(
    private val host: String = "3.33.230.243",
    private val port: Int = 6001,
    private val connectTimeoutMs: Int = 10000,  // Longer timeout for SSL handshake
    private val readTimeoutMs: Int = 30000      // Longer timeout for cloud latency
) : Closeable {
    
    private val logger = LoggerFactory.getLogger(CloudTerminalClient::class.java)
    
    private var socket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    /**
     * Connection state
     */
    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false
    
    /**
     * Local address
     */
    val localAddress: String?
        get() = socket?.localAddress?.hostAddress
    
    /**
     * Local port
     */
    val localPort: Int?
        get() = socket?.localPort
    
    /**
     * Connect to Nets Cloud Connect and establish SSL/TLS session
     * 
     * @throws IOException if connection or SSL handshake fails
     */
    fun connect() {
        if (isConnected) {
            logger.debug("Already connected to Nets Cloud Connect at $host:$port")
            return
        }
        
        logger.info("Connecting to Nets Cloud Connect at $host:$port...")
        
        try {
            // Create SSL socket
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = factory.createSocket() as SSLSocket
            
            // Set socket options
            socket!!.tcpNoDelay = true
            socket!!.soTimeout = readTimeoutMs
            
            // Enable all standard TLS protocols
            socket!!.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            
            // Connect
            socket!!.connect(InetSocketAddress(host, port), connectTimeoutMs)
            
            // Perform SSL handshake
            logger.debug("Performing SSL handshake...")
            socket!!.startHandshake()
            
            // Get streams after successful handshake
            inputStream = socket!!.inputStream
            outputStream = socket!!.outputStream
            
            val session = socket!!.session
            logger.info("✅ Connected to Nets Cloud Connect")
            logger.info("   Remote: $host:$port")
            logger.info("   Local:  $localAddress:$localPort")
            logger.info("   TLS Protocol: ${session.protocol}")
            logger.info("   Cipher Suite: ${session.cipherSuite}")
            
        } catch (e: SocketTimeoutException) {
            cleanup()
            throw IOException("Connection timeout after ${connectTimeoutMs}ms to $host:$port", e)
        } catch (e: IOException) {
            cleanup()
            throw IOException("Failed to connect to Nets Cloud Connect at $host:$port: ${e.message}", e)
        }
    }
    
    /**
     * Send a command and wait for complete response
     * 
     * Uses NetsBaxProtocol.framingMode to determine how to frame the command.
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
     * Read complete response from Nets Cloud Connect / terminal
     * 
     * Handles:
     * - ACK/NAK followed by data
     * - Fragmented TCP packets
     * - Complete frame detection based on NetsBaxProtocol.framingMode
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
                    logger.warn("SSL socket closed by Nets Cloud")
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
     * Send ACK to terminal (via Cloud Connect)
     */
    fun sendAck() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.ACK))
        outputStream!!.flush()
        logger.debug("📤 Sent ACK")
    }
    
    /**
     * Send NAK to terminal (via Cloud Connect)
     */
    fun sendNak() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.NAK))
        outputStream!!.flush()
        logger.debug("📤 Sent NAK")
    }
    
    private fun requireConnected() {
        if (!isConnected) {
            throw IllegalStateException("Not connected to Nets Cloud Connect")
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
            logger.info("Closing SSL connection to Nets Cloud Connect at $host:$port")
        }
        cleanup()
    }
    
    private fun ByteArray.toHexString(): String = 
        joinToString(" ") { "%02X".format(it) }
}

/**
 * Response from payment terminal (via Nets Cloud Connect)
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
