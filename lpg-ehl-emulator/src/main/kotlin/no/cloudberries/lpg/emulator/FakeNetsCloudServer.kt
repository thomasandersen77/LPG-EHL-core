package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

/**
 * Fake Nets Cloud Connect Server
 * 
 * Emulates Nets Cloud Connect SSL/TLS endpoint for local testing.
 * Listens on port 6001 (same as real Nets Cloud) and responds with handshake messages.
 * 
 * **Purpose:**
 * - Local development without needing real Nets Cloud access
 * - Integration testing of SSL/TLS connection logic
 * - Prototyping authentication/registration handshake
 * 
 * **Protocol:**
 * 1. Accept SSL/TLS connection
 * 2. Send ConnectCloud handshake: "aCC002000a;NET;ConnectCloud;0.2;fake00001;24203ÿÿ1"
 * 3. Receive Baxi protocol commands
 * 4. Respond with simulated payment responses
 * 
 * **Usage:**
 * ```kotlin
 * val server = FakeNetsCloudServer(port = 6001)
 * server.start()
 * // ... run tests ...
 * server.stop()
 * ```
 */
class FakeNetsCloudServer(
    private val port: Int = 6001,
    private val autoSendHandshake: Boolean = true
) : Closeable {
    
    private val logger = LoggerFactory.getLogger(FakeNetsCloudServer::class.java)
    
    private var serverSocket: SSLServerSocket? = null
    private var isRunning = false
    private var acceptThread: Thread? = null
    
    /**
     * Start the fake server
     */
    fun start() {
        if (isRunning) {
            logger.warn("Server already running on port $port")
            return
        }
        
        logger.info("Starting Fake Nets Cloud Server on port $port...")
        
        try {
            // Create self-signed SSL context
            val sslContext = createSelfSignedSSLContext()
            val serverSocketFactory = sslContext.serverSocketFactory
            
            serverSocket = serverSocketFactory.createServerSocket(port) as SSLServerSocket
            serverSocket!!.needClientAuth = false  // Don't require client certificate
            
            isRunning = true
            
            // Start accept thread
            acceptThread = Thread {
                acceptLoop()
            }.apply {
                name = "FakeNetsCloud-Accept"
                isDaemon = true
                start()
            }
            
            logger.info("✅ Fake Nets Cloud Server listening on port $port")
            logger.info("   Protocol: TLS (self-signed certificate)")
            logger.info("   Auto handshake: $autoSendHandshake")
            
        } catch (e: Exception) {
            isRunning = false
            throw RuntimeException("Failed to start Fake Nets Cloud Server on port $port", e)
        }
    }
    
    /**
     * Accept incoming connections
     */
    private fun acceptLoop() {
        while (isRunning) {
            try {
                val clientSocket = serverSocket!!.accept() as SSLSocket
                logger.info("📥 Client connected: ${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")
                
                // Handle client in separate thread
                Thread {
                    handleClient(clientSocket)
                }.apply {
                    name = "FakeNetsCloud-Client-${clientSocket.port}"
                    isDaemon = true
                    start()
                }
                
            } catch (e: Exception) {
                if (isRunning) {
                    logger.error("Error accepting client connection", e)
                }
            }
        }
    }
    
    /**
     * Handle client connection
     */
    private fun handleClient(socket: SSLSocket) {
        try {
            val input = socket.inputStream
            val output = socket.outputStream
            
            logger.debug("Starting SSL handshake with client...")
            socket.startHandshake()
            logger.debug("SSL handshake completed. Cipher: ${socket.session.cipherSuite}")
            
            // Send ConnectCloud handshake if enabled
            if (autoSendHandshake) {
                sendHandshake(output)
            }
            
            // Read and respond to commands
            val buffer = ByteArray(2048)
            while (socket.isConnected) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) {
                    logger.debug("Client disconnected")
                    break
                }
                
                val command = buffer.copyOf(bytesRead)
                logger.debug("📥 Received ${bytesRead} bytes: ${command.toHexString()}")
                
                // Parse and respond
                val response = handleCommand(command)
                if (response != null) {
                    output.write(response)
                    output.flush()
                    logger.debug("📤 Sent ${response.size} bytes: ${response.toHexString()}")
                }
            }
            
        } catch (e: Exception) {
            logger.debug("Client session ended: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Send ConnectCloud handshake message
     */
    private fun sendHandshake(output: OutputStream) {
        // Real response format from Nets Cloud Connect:
        // aCC002000a;NET;ConnectCloud;0.2;cloud00001;24203ÿÿ1
        val handshake = "aCC002000a;NET;ConnectCloud;0.2;fake00001;24203\u00ff\u00ff1"
        val bytes = handshake.toByteArray(Charsets.ISO_8859_1)
        
        output.write(bytes)
        output.flush()
        
        logger.info("📤 Sent ConnectCloud handshake: $handshake")
    }
    
    /**
     * Handle incoming Baxi protocol command
     */
    private fun handleCommand(command: ByteArray): ByteArray? {
        // Simple command parsing
        val payload = parsePayload(command)
        logger.debug("Command payload: $payload")
        
        return when {
            payload.startsWith("P;10") -> {
                // Purchase command - simulate approval
                val response = "00;TXN-FAKE-001;AUTH-123456"
                buildResponse(response)
            }
            payload.startsWith("P;03") -> {
                // Preauth command - simulate approval
                val response = "00;PREAUTH-FAKE-001;AUTH-789012"
                buildResponse(response)
            }
            payload.startsWith("C") -> {
                // Cancel command - acknowledge
                val response = "00;CANCELLED"
                buildResponse(response)
            }
            payload.startsWith("S") -> {
                // Status command
                val response = "00;READY"
                buildResponse(response)
            }
            else -> {
                logger.warn("Unknown command: $payload")
                null
            }
        }
    }
    
    /**
     * Parse command payload (handle TCP framing)
     */
    private fun parsePayload(command: ByteArray): String {
        return if (command.size >= 2) {
            // Check for 2-byte length header
            val declaredLength = ((command[0].toInt() and 0xFF) shl 8) or (command[1].toInt() and 0xFF)
            if (declaredLength > 0 && declaredLength + 2 <= command.size) {
                // Has length header, extract payload
                String(command.copyOfRange(2, 2 + declaredLength), Charsets.ISO_8859_1)
            } else {
                // No header, treat as raw
                String(command, Charsets.ISO_8859_1)
            }
        } else {
            String(command, Charsets.ISO_8859_1)
        }
    }
    
    /**
     * Build TCP-framed response
     */
    private fun buildResponse(payload: String): ByteArray {
        val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
        val length = payloadBytes.size
        
        // Create 2-byte header (Big Endian)
        val header = ByteArray(2)
        header[0] = ((length shr 8) and 0xFF).toByte()
        header[1] = (length and 0xFF).toByte()
        
        return header + payloadBytes
    }
    
    /**
     * Stop the server
     */
    fun stop() {
        if (!isRunning) {
            return
        }
        
        logger.info("Stopping Fake Nets Cloud Server...")
        isRunning = false
        
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        
        acceptThread?.interrupt()
        acceptThread = null
        serverSocket = null
        
        logger.info("✅ Fake Nets Cloud Server stopped")
    }
    
    override fun close() {
        stop()
    }
    
    companion object {
        /**
         * Create self-signed SSL context for testing
         */
        private fun createSelfSignedSSLContext(): SSLContext {
            // For simplicity, use a trust-all approach
            // In real implementation, generate proper keystore with self-signed cert
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )
            
            // Create a simple KeyManager (no actual certificate)
            val keyManagers = arrayOf<KeyManager>()
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagers, trustAllCerts, SecureRandom())
            
            return sslContext
        }
        
        private fun ByteArray.toHexString(): String = 
            joinToString(" ") { "%02X".format(it) }
    }
}
