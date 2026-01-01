package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ECR Server
 * 
 * Acts as a cash register (ECR = Electronic Cash Register) server that responds
 * to payment terminal requests. This allows the terminal to establish connection
 * and receive commands.
 * 
 * @property port Port to listen on (default 8009 for ECR protocol)
 */
class EcrServer(
    private val port: Int = 8009
) {
    private val logger = LoggerFactory.getLogger(EcrServer::class.java)
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null
    
    /**
     * Start the ECR server
     */
    fun start() {
        if (running.get()) {
            logger.warn("ECR server already running")
            return
        }
        
        try {
            serverSocket = ServerSocket(port)
            running.set(true)
            
            logger.info("ECR Server started on port $port")
            logger.info("Payment terminal can now connect...")
            
            // Accept connections in background thread
            executor.submit {
                acceptConnections()
            }
            
        } catch (e: IOException) {
            logger.error("Failed to start ECR server: ${e.message}")
            running.set(false)
        }
    }
    
    /**
     * Stop the ECR server
     */
    fun stop() {
        running.set(false)
        
        try {
            serverSocket?.close()
            executor.shutdown()
            logger.info("ECR Server stopped")
        } catch (e: IOException) {
            logger.error("Error stopping server: ${e.message}")
        }
    }
    
    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = running.get()
    
    /**
     * Accept incoming connections from payment terminal
     */
    private fun acceptConnections() {
        while (running.get()) {
            try {
                val socket = serverSocket?.accept() ?: break
                logger.info("Payment terminal connected from ${socket.inetAddress.hostAddress}:${socket.port}")
                
                // Handle connection in separate thread
                executor.submit {
                    handleTerminalConnection(socket)
                }
                
            } catch (e: IOException) {
                if (running.get()) {
                    logger.error("Error accepting connection: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Handle communication with connected payment terminal
     */
    private fun handleTerminalConnection(socket: Socket) {
        try {
            socket.soTimeout = 60000 // 60 second timeout
            
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            
            logger.info("Terminal session started")
            
            // Send initial ACK to establish connection
            sendAck(output)
            
            val buffer = ByteArray(4096)
            
            while (running.get() && !socket.isClosed) {
                val bytesRead = input.read(buffer)
                
                if (bytesRead <= 0) {
                    logger.info("Terminal disconnected")
                    break
                }
                
                val data = buffer.copyOf(bytesRead)
                logger.debug("Received ${bytesRead} bytes from terminal")
                logHexDump(data)
                
                // Parse and respond to terminal message
                val response = processTerminalMessage(data)
                if (response != null) {
                    output.write(response)
                    output.flush()
                    logger.debug("Sent ${response.size} bytes to terminal")
                    logHexDump(response)
                }
            }
            
        } catch (e: IOException) {
            logger.error("Terminal communication error: ${e.message}")
        } finally {
            try {
                socket.close()
                logger.info("Terminal session ended")
            } catch (e: IOException) {
                // Ignore
            }
        }
    }
    
    /**
     * Send initial ACK to terminal
     */
    private fun sendAck(output: java.io.OutputStream) {
        // Simple ACK response - this may need adjustment based on actual protocol
        val ack = byteArrayOf(0x06) // ACK character
        output.write(ack)
        output.flush()
        logger.debug("Sent ACK to terminal")
    }
    
    /**
     * Process message from terminal and generate response
     */
    private fun processTerminalMessage(data: ByteArray): ByteArray? {
        // Try to identify message type
        
        // Check if it's a text-based message
        val text = String(data, StandardCharsets.UTF_8).trim()
        if (text.isNotEmpty() && text.all { it.isLetterOrDigit() || it.isWhitespace() || it in ":-_." }) {
            logger.info("Terminal message (text): $text")
            
            // Respond with simple OK
            return "OK\n".toByteArray(StandardCharsets.UTF_8)
        }
        
        // Binary protocol - analyze first byte
        when (data[0].toInt() and 0xFF) {
            0x06 -> {
                // ENQ (enquiry) - respond with ACK
                logger.info("Terminal sent ENQ")
                return byteArrayOf(0x06)
            }
            0x05 -> {
                // ENQ (enquiry) - respond with ACK
                logger.info("Terminal sent ENQ")
                return byteArrayOf(0x06)
            }
            0x02 -> {
                // STX (start of text) - ZVT/ECR protocol message
                logger.info("Terminal sent STX message (possible ZVT protocol)")
                // Send ACK + simple status
                return byteArrayOf(0x06)
            }
            0x10 -> {
                // DLE (data link escape)
                logger.info("Terminal sent DLE")
                return byteArrayOf(0x06)
            }
            else -> {
                // Unknown message - send ACK anyway
                logger.info("Terminal sent unknown message (first byte: 0x${"%02X".format(data[0])})")
                return byteArrayOf(0x06)
            }
        }
    }
    
    /**
     * Log hex dump of data
     */
    private fun logHexDump(data: ByteArray) {
        if (data.size <= 64) {
            val hex = data.joinToString(" ") { "%02X".format(it) }
            val ascii = data.map { 
                val c = it.toInt() and 0xFF
                if (c in 32..126) c.toChar() else '.'
            }.joinToString("")
            
            logger.debug("  HEX:   $hex")
            logger.debug("  ASCII: $ascii")
        } else {
            logger.debug("  (${data.size} bytes - too long to display)")
        }
    }
}

/**
 * Main application for running ECR server
 */
object EcrServerApp {
    
    @JvmStatic
    fun main(args: Array<String>) {
        val logger = LoggerFactory.getLogger(EcrServerApp::class.java)
        
        println("=== ECR Server for Payment Terminal ===")
        println()
        println("This server acts as a cash register (ECR) that the payment")
        println("terminal can connect to. It will respond to terminal requests.")
        println()
        
        val port = args.firstOrNull()?.toIntOrNull() ?: 8009
        
        println("Starting ECR server on port $port...")
        println("Press Ctrl+C to stop")
        println()
        
        val server = EcrServer(port)
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nShutting down ECR server...")
            server.stop()
        })
        
        // Start server
        server.start()
        
        if (server.isRunning()) {
            println("✓ ECR Server is running")
            println("  Payment terminal should now be able to connect")
            println("  Waiting for terminal connection on port $port...")
            println()
            
            // Keep main thread alive
            try {
                Thread.sleep(Long.MAX_VALUE)
            } catch (e: InterruptedException) {
                logger.info("Server interrupted")
            }
        } else {
            println("✗ Failed to start ECR server")
            System.exit(1)
        }
    }
}
