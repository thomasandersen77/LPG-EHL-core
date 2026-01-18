package no.cloudberries.lpg.service.integration

import no.cloudberries.lpg.payment.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Nets Cloud Socket Client
 * 
 * SSL/TLS socket client for Nets Cloud Connect.
 * Implements TerminalConnection interface from core, following Clean Architecture principles.
 * 
 * **Architecture:**
 * ```
 * [LPG-EHL API] <--SSL/TLS--> [Nets Cloud 3.33.230.243:6001] <--ECR--> [Terminal]
 * ```
 * 
 * **Usage:**
 * ```kotlin
 * val client = NetsCloudSocketClient(
 *     host = "3.33.230.243",  // Production
 *     port = 6001
 * )
 * // Or for local testing:
 * val client = NetsCloudSocketClient(
 *     host = "localhost",     // FakeNetsCloudServer
 *     port = 6001
 * )
 * 
 * client.use { terminal ->
 *     terminal.connect()
 *     
 *     val command = NetsBaxProtocol.createPurchaseCommand(10000)
 *     val response = terminal.sendCommand(command)
 *     
 *     when (val result = response.parse()) {
 *         is BaxResponse.Success -> println("Approved!")
 *         is BaxResponse.Error -> println("Declined: ${result.message}")
 *         else -> println("Unexpected: $result")
 *     }
 * }
 * ```
 * 
 * @property host Nets Cloud Connect host (default: 3.33.230.243)
 * @property port Nets Cloud Connect port (default: 6001)
 * @property connectTimeoutMs Connection timeout in milliseconds
 * @property readTimeoutMs Read timeout for complete response
 * @property trustAllCertificates For development - bypasses certificate validation
 */
class NetsCloudSocketClient(
    private val host: String = "3.33.230.243",
    private val port: Int = 6001,
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 30000,
    private val trustAllCertificates: Boolean = true  // For dev/testing
) : TerminalConnection {
    
    private val logger = LoggerFactory.getLogger(NetsCloudSocketClient::class.java)
    
    private var socket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    override val isConnected: Boolean
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
    
    override fun connect() {
        if (isConnected) {
            logger.debug("Already connected to Nets Cloud Connect at $host:$port")
            return
        }
        
        logger.info("Connecting to Nets Cloud Connect at $host:$port...")
        
        try {
            // Create SSL socket factory
            val factory = if (trustAllCertificates) {
                logger.debug("Using TrustAll SSL context (development mode)")
                createTrustAllSSLSocketFactory()
            } else {
                logger.debug("Using default SSL context (production mode)")
                SSLSocketFactory.getDefault() as SSLSocketFactory
            }
            
            socket = factory.createSocket() as SSLSocket
            
            // Set socket options
            socket!!.tcpNoDelay = true
            socket!!.soTimeout = readTimeoutMs
            
            // Enable TLS 1.2/1.3
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
            throw TerminalConnectionException(
                "Connection timeout after ${connectTimeoutMs}ms to $host:$port",
                e
            )
        } catch (e: IOException) {
            cleanup()
            throw TerminalConnectionException(
                "Failed to connect to Nets Cloud Connect at $host:$port: ${e.message}",
                e
            )
        }
    }
    
    override fun sendCommand(command: ByteArray): TerminalResponse {
        requireConnected()
        
        logger.debug("📤 Sending ${command.size} bytes: ${command.toHexString()}")
        
        try {
            outputStream!!.write(command)
            outputStream!!.flush()
            
            return readResponse()
            
        } catch (e: IOException) {
            logger.error("Failed to send command: ${e.message}")
            throw TerminalConnectionException("Failed to send command", e)
        }
    }
    
    /**
     * Read complete response from Nets Cloud Connect / terminal
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
                            Thread.sleep(100)
                        }
                    }
                    is FrameStatus.Empty -> {
                        // Should not happen after read
                    }
                }
                
                // If we only got ACK/NAK and no frame after brief wait, that's complete
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
    
    override fun sendAck() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.ACK))
        outputStream!!.flush()
        logger.debug("📤 Sent ACK")
    }
    
    override fun sendNak() {
        requireConnected()
        outputStream!!.write(byteArrayOf(NetsBaxProtocol.NAK))
        outputStream!!.flush()
        logger.debug("📤 Sent NAK")
    }
    
    private fun requireConnected() {
        if (!isConnected) {
            throw TerminalConnectionException("Not connected to Nets Cloud Connect")
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
    
    companion object {
        /**
         * Create SSL socket factory that trusts all certificates
         * 
         * WARNING: Only for development/testing! In production, use proper certificate validation.
         */
        private fun createTrustAllSSLSocketFactory(): SSLSocketFactory {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            
            return sslContext.socketFactory
        }
    }
}
