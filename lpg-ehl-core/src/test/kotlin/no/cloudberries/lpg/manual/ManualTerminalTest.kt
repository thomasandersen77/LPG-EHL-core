package no.cloudberries.lpg.manual

import no.cloudberries.lpg.payment.BaxResponse
import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manual test for Ingenico Self/4000 terminal communication
 * 
 * Usage:
 * 1. Ensure terminal is configured to connect to this machine's IP on port 8009
 * 2. Run this main function from IntelliJ (Right-click -> Run)
 * 3. Terminal should connect automatically
 * 4. Test will send a purchase command and display responses
 * 
 * Expected Terminal Config:
 * - Komm type: IP ETHERNET
 * - ECR IP: <this machine's IP> (e.g., 192.168.0.41)
 * - ECR IP PORT: 8009
 * - ECR/TLS: Nei
 */
object ManualTerminalTest {
    
    private const val PORT = 8009
    private const val TEST_AMOUNT = 200 // 2.00 NOK
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("═══════════════════════════════════════════════════════")
        println("  Manual Ingenico Self/4000 Terminal Test")
        println("  TCP/Ethernet Mode - Port $PORT")
        println("═══════════════════════════════════════════════════════")
        println()
        
        // Set TCP mode
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
        log("✓ Set framing mode: TCP_ETHERNET")
        
        try {
            ServerSocket(PORT).use { serverSocket ->
                log("✓ Server listening on port $PORT")
                log("⏳ Waiting for terminal to connect...")
                println("   (Make sure terminal is powered on and configured)")
                println()
                
                val clientSocket = serverSocket.accept()
                log("✓ Terminal connected from: ${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")
                println()
                
                handleTerminalSession(clientSocket)
            }
        } catch (e: Exception) {
            error("✗ Error: ${e.message}", e)
        }
        
        println()
        log("Session ended. Press Enter to exit...")
        readLine()
    }
    
    private fun handleTerminalSession(socket: Socket) {
        socket.use { client ->
            val input = client.getInputStream()
            val output = client.getOutputStream()
            
            try {
                // Start heartbeat responder thread
                val heartbeatThread = startHeartbeatResponder(input, output)
                
                // Wait a moment for initial connection to stabilize
                Thread.sleep(500)
                
                // Read initial message (usually I1 identification)
                log("📥 Listening for initial message from terminal...")
                val initialMsg = readMessage(input)
                if (initialMsg != null) {
                    logReceived(initialMsg)
                    val parsed = NetsBaxProtocol.parseResponse(initialMsg)
                    log("   Parsed: $parsed")
                    println()
                }
                
                // Wait a moment
                Thread.sleep(1000)
                
                // Send purchase command
                log("💳 Sending Purchase command: $TEST_AMOUNT øre (${TEST_AMOUNT / 100.0} NOK)")
                val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(
                    amountCents = TEST_AMOUNT,
                    operatorId = "1"
                )
                logSent(purchaseCmd)
                output.write(purchaseCmd)
                output.flush()
                println()
                
                // Listen for responses
                log("📥 Listening for terminal responses...")
                log("   (Waiting for transaction result or timeout...)")
                println()
                
                var responseCount = 0
                val timeout = System.currentTimeMillis() + 60_000 // 60 seconds
                
                while (System.currentTimeMillis() < timeout) {
                    if (input.available() > 0) {
                        val response = readMessage(input)
                        if (response != null && response.isNotEmpty()) {
                            // Filter out heartbeats from display
                            if (!(response.size == 2 && response[0] == 0.toByte() && response[1] == 0.toByte())) {
                                responseCount++
                                logReceived(response, responseCount)
                                
                                val parsed = NetsBaxProtocol.parseResponse(response)
                                log("   Parsed: $parsed")
                                
                                when (parsed) {
                                    is BaxResponse.Success -> {
                                        log("✅ TRANSACTION APPROVED!")
                                        log("   Transaction ID: ${parsed.transactionId}")
                                        log("   Auth Code: ${parsed.authCode}")
                                        break
                                    }
                                    is BaxResponse.Error -> {
                                        log("❌ TRANSACTION ERROR: ${parsed.message}")
                                        break
                                    }
                                    is BaxResponse.Data -> {
                                        log("   Status: ${parsed.payload}")
                                        if (parsed.payload.contains("ECR Timeout", ignoreCase = true)) {
                                            log("⚠️  Transaction timed out (no card inserted)")
                                        }
                                    }
                                    else -> {
                                        log("   Response type: ${parsed::class.simpleName}")
                                    }
                                }
                                println()
                            }
                        }
                    }
                    Thread.sleep(100)
                }
                
                if (responseCount == 0) {
                    log("⚠️  No responses received within timeout period")
                }
                
                heartbeatThread.interrupt()
                
            } catch (e: Exception) {
                error("Session error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Start background thread to respond to heartbeats
     */
    private fun startHeartbeatResponder(input: InputStream, output: OutputStream): Thread {
        val thread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (input.available() >= 2) {
                        val peek = ByteArray(2)
                        input.mark(2)
                        val read = input.read(peek)
                        
                        if (read == 2 && peek[0] == 0.toByte() && peek[1] == 0.toByte()) {
                            // Heartbeat detected - consume it and respond
                            output.write(byteArrayOf(0, 0))
                            output.flush()
                            // Silent - don't log heartbeats
                        } else {
                            // Not a heartbeat, reset stream
                            input.reset()
                        }
                    }
                    Thread.sleep(50)
                }
            } catch (e: InterruptedException) {
                // Thread stopped
            } catch (e: Exception) {
                // Ignore - main thread will handle errors
            }
        }
        thread.isDaemon = true
        thread.start()
        return thread
    }
    
    /**
     * Read a message from the terminal
     * TCP mode: [2-byte length] + [payload]
     * 
     * FIXED: Now properly reads ONE message at a time,
     * handling multiple messages in sequence correctly.
     */
    private fun readMessage(input: InputStream): ByteArray? {
        // Wait for at least the header (2 bytes)
        if (input.available() < 2) return null
        
        // Read length header (blocking read for exactly 2 bytes)
        val header = ByteArray(2)
        var headerRead = 0
        while (headerRead < 2) {
            val read = input.read(header, headerRead, 2 - headerRead)
            if (read == -1) return null // Connection closed
            headerRead += read
        }
        
        val length = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
        
        // Zero-length = heartbeat
        if (length == 0) {
            return byteArrayOf(0, 0)
        }
        
        // Read payload (blocking read for exact length)
        val payload = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(payload, totalRead, length - totalRead)
            if (read == -1) return null // Connection closed
            totalRead += read
        }
        
        // Return full message (header + payload)
        return header + payload
    }
    
    private fun log(message: String) {
        val time = LocalDateTime.now().format(timeFormatter)
        println("[$time] $message")
    }
    
    private fun error(message: String, throwable: Throwable? = null) {
        val time = LocalDateTime.now().format(timeFormatter)
        System.err.println("[$time] $message")
        throwable?.printStackTrace()
    }
    
    private fun logSent(data: ByteArray) {
        log("📤 SENT (${data.size} bytes):")
        log("   HEX: ${data.toHexString()}")
        
        // Try to parse payload
        if (data.size > 2) {
            val payloadStart = 2
            val payload = String(data.copyOfRange(payloadStart, data.size), StandardCharsets.ISO_8859_1)
            log("   CMD: $payload")
        }
    }
    
    private fun logReceived(data: ByteArray, count: Int = 0) {
        val prefix = if (count > 0) "📥 RESPONSE #$count" else "📥 RECEIVED"
        log("$prefix (${data.size} bytes):")
        log("   HEX: ${data.toHexString()}")
        
        // Try to parse payload
        if (data.size > 2) {
            val length = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            if (length > 0 && length + 2 == data.size) {
                val payload = String(data.copyOfRange(2, data.size), StandardCharsets.ISO_8859_1)
                log("   TXT: $payload")
            } else if (data.size == 2 && data[0] == 0.toByte() && data[1] == 0.toByte()) {
                log("   TXT: [HEARTBEAT]")
            }
        }
    }
    
    private fun ByteArray.toHexString(): String = 
        joinToString(" ") { "%02X".format(it) }
}
