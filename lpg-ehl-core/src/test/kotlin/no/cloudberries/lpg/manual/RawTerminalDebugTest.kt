package no.cloudberries.lpg.manual

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * RAW DEBUG MODE - Reads terminal responses byte-by-byte without parsing
 * 
 * Use this to see EXACTLY what the terminal is sending without
 * any length-header interpretation or protocol parsing.
 * 
 * This helps identify if terminal is sending:
 * - Raw ASCII text (e.g., "[00]", "A000ECR Timeout")
 * - Binary with length headers
 * - Hybrid format
 */
object RawTerminalDebugTest {
    
    private const val PORT = 8009
    private const val TEST_AMOUNT = 200 // 2.00 NOK
    private const val MAX_BYTES_TO_READ = 500 // Read max 500 bytes to avoid infinite loops
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("═══════════════════════════════════════════════════════")
        println("  RAW DEBUG MODE - Terminal Response Analyzer")
        println("  Port $PORT - NO PROTOCOL PARSING")
        println("═══════════════════════════════════════════════════════")
        println()
        println("This test reads RAW BYTES from the terminal without")
        println("interpreting them as length headers or protocol frames.")
        println()
        
        // Set TCP mode for sending
        NetsBaxProtocol.framingMode = NetsBaxProtocol.FramingMode.TCP_ETHERNET
        log("✓ Set framing mode: TCP_ETHERNET (for sending)")
        
        try {
            ServerSocket(PORT).use { serverSocket ->
                log("✓ Server listening on port $PORT")
                log("⏳ Waiting for terminal to connect...")
                println()
                
                val clientSocket = serverSocket.accept()
                log("✓ Terminal connected from: ${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")
                println()
                
                val input = clientSocket.getInputStream()
                val output = clientSocket.getOutputStream()
                
                // Wait for any initial message
                Thread.sleep(1000)
                
                // Send purchase command
                log("💳 Sending Purchase command: $TEST_AMOUNT øre")
                val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(
                    amountCents = TEST_AMOUNT,
                    operatorId = "1"
                )
                logSent(purchaseCmd)
                output.write(purchaseCmd)
                output.flush()
                println()
                
                // RAW READING MODE - Read bytes without protocol interpretation
                log("📥 RAW READING MODE - Monitoring all bytes for 30 seconds...")
                log("   (Reading byte-by-byte, no length-header interpretation)")
                println()
                
                val startTime = System.currentTimeMillis()
                val timeout = 30_000L // 30 seconds
                var totalBytesRead = 0
                val buffer = mutableListOf<Byte>()
                
                while (System.currentTimeMillis() - startTime < timeout && totalBytesRead < MAX_BYTES_TO_READ) {
                    if (input.available() > 0) {
                        val byte = input.read()
                        if (byte == -1) break // End of stream
                        
                        val b = byte.toByte()
                        buffer.add(b)
                        totalBytesRead++
                        
                        // Print byte immediately (every 10 bytes on same line for readability)
                        if (totalBytesRead % 10 == 1) {
                            print("[${String.format("%04d", totalBytesRead)}] ")
                        }
                        print(String.format("%02X ", b))
                        if (totalBytesRead % 10 == 0) {
                            println()
                        }
                        
                        // Check if we got a complete "message" (heuristic: pause in data)
                        // Wait a bit to see if more data is coming
                        Thread.sleep(50)
                        if (input.available() == 0) {
                            // No more data immediately available - analyze what we got
                            println()
                            println()
                            analyzeBuffer(buffer.toByteArray())
                            buffer.clear()
                            println()
                            log("   Continuing to monitor...")
                        }
                    } else {
                        Thread.sleep(100)
                    }
                }
                
                if (totalBytesRead >= MAX_BYTES_TO_READ) {
                    println()
                    log("⚠️  Stopped reading after $MAX_BYTES_TO_READ bytes (safety limit)")
                }
                
                println()
                log("✓ Total bytes read: $totalBytesRead")
                
            }
        } catch (e: Exception) {
            error("✗ Error: ${e.message}", e)
        }
        
        println()
        log("Test complete. Press Enter to exit...")
        readLine()
    }
    
    private fun analyzeBuffer(data: ByteArray) {
        if (data.isEmpty()) return
        
        log("━━━ MESSAGE RECEIVED (${data.size} bytes) ━━━")
        
        // Show HEX
        log("HEX: ${data.toHexString()}")
        
        // Try to interpret as ASCII text
        val asciiText = String(data, StandardCharsets.ISO_8859_1)
        val printableText = asciiText.map { c ->
            if (c.code in 32..126) c else '·'
        }.joinToString("")
        log("ASCII: $printableText")
        
        // Check for common patterns
        when {
            data.contentEquals(byteArrayOf(0x00, 0x00)) -> {
                log("✓ IDENTIFIED: Heartbeat (00 00)")
            }
            asciiText.startsWith("[") && asciiText.contains("]") -> {
                log("✓ IDENTIFIED: Bracket format response: $asciiText")
            }
            asciiText.startsWith("A000") -> {
                log("✓ IDENTIFIED: Ingenico A000 response: $asciiText")
            }
            asciiText.startsWith("D!") -> {
                log("✓ IDENTIFIED: Ingenico D! response: $asciiText")
            }
            data[0] == 0x02.toByte() -> {
                log("? Starts with STX (0x02) - might be serial protocol?")
            }
            data.size >= 2 && data[0] == 0x00.toByte() -> {
                val potentialLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                if (potentialLength + 2 == data.size) {
                    log("✓ IDENTIFIED: Valid TCP length header (length=$potentialLength)")
                    val payload = data.copyOfRange(2, data.size)
                    log("   Payload: ${String(payload, StandardCharsets.ISO_8859_1)}")
                } else {
                    log("⚠️  First two bytes (${String.format("%02X %02X", data[0], data[1])}) " +
                        "could be length header ($potentialLength) but size mismatch!")
                    log("   If interpreted as ASCII: '${String(data.take(2).toByteArray(), StandardCharsets.ISO_8859_1)}'")
                }
            }
            else -> {
                log("? Unknown format - see hex/ascii above")
            }
        }
        
        // Show character breakdown for first 20 bytes
        if (data.size <= 20) {
            println("   Byte breakdown:")
            data.forEachIndexed { index, byte ->
                val hex = String.format("%02X", byte)
                val decimal = byte.toInt() and 0xFF
                val char = if (decimal in 32..126) "'${byte.toInt().toChar()}'" else "·"
                println("      [$index] 0x$hex = $decimal = $char")
            }
        }
        
        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    private fun logSent(data: ByteArray) {
        log("📤 SENT (${data.size} bytes):")
        log("   HEX: ${data.toHexString()}")
        if (data.size > 2) {
            val payload = String(data.copyOfRange(2, data.size), StandardCharsets.ISO_8859_1)
            log("   CMD: $payload")
        }
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
    
    private fun ByteArray.toHexString(): String = 
        joinToString(" ") { "%02X".format(it) }
}
