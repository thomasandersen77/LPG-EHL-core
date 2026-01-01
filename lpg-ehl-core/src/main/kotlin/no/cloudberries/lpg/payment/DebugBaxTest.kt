package no.cloudberries.lpg.payment

import java.net.Socket
import java.io.InputStream
import java.io.OutputStream
import kotlin.system.exitProcess

/**
 * Debug utility for testing Bax terminal connectivity
 * 
 * Usage: java -cp ... no.cloudberries.lpg.payment.DebugBaxTest [host] [port]
 * Default: 192.168.0.4:8009
 */
object DebugBaxTest {
    
    @JvmStatic
    fun main(args: Array<String>) {
        val host = args.getOrNull(0) ?: "192.168.0.4"
        val port = args.getOrNull(1)?.toIntOrNull() ?: 8009
        
        println("=".repeat(60))
        println("Bax Terminal Debug Test")
        println("=".repeat(60))
        println("Target: $host:$port")
        println()
        
        try {
            println("1. Connecting...")
            val socket = Socket(host, port).apply {
                soTimeout = 5000 // 5 second timeout
            }
            println("   ✓ Connected to $host:$port")
            println()
            
            val input: InputStream = socket.getInputStream()
            val output: OutputStream = socket.getOutputStream()
            
            // ECR Mode: Wake up terminal with ENQ
            println("2. Waking up terminal (ENQ)...")
            output.write(NetsBaxProtocol.ENQ.toInt())
            output.flush()
            println("   Sent ENQ (0x05)")
            Thread.sleep(500) // Give terminal time to wake up
            
            // Check for any initial response
            if (input.available() > 0) {
                val wakeResponse = readAvailableBytes(input)
                println("   Wake response: ${wakeResponse.toHexString()}")
                println("   Debug: ${wakeResponse.toDebugString()}")
            } else {
                println("   No immediate response (OK for ECR mode)")
            }
            println()
            
            // Test 1: Status Query
            println("3. Sending Status Query (S)...")
            val statusCmd = NetsBaxProtocol.createStatusCommand()
            println("   Command: ${statusCmd.toHexString()}")
            println("   Debug: ${statusCmd.toDebugString()}")
            output.write(statusCmd)
            output.flush()
            println("   ✓ Sent")
            println()
            
            println("4. Reading response...")
            val response = readResponse(input, timeoutMs = 3000)
            println("   Raw: ${response.toHexString()}")
            println("   Debug: ${response.toDebugString()}")
            
            val parsed = NetsBaxProtocol.parseResponse(response)
            println("   Parsed: $parsed")
            println()
            
            // Test 2: Small purchase (1.00 NOK)
            println("5. Sending Purchase Request (1.00 NOK)...")
            val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
            println("   Command: ${purchaseCmd.toHexString()}")
            println("   Debug: ${purchaseCmd.toDebugString()}")
            output.write(purchaseCmd)
            output.flush()
            println("   ✓ Sent")
            println()
            
            println("6. Reading purchase response...")
            val purchaseResponse = readResponse(input)
            println("   Raw: ${purchaseResponse.toHexString()}")
            println("   Debug: ${purchaseResponse.toDebugString()}")
            
            val parsedPurchase = NetsBaxProtocol.parseResponse(purchaseResponse)
            println("   Parsed: $parsedPurchase")
            println()
            
            socket.close()
            println("=".repeat(60))
            println("Test completed successfully!")
            println("=".repeat(60))
            
        } catch (e: Exception) {
            System.err.println()
            System.err.println("ERROR: ${e.message}")
            System.err.println()
            System.err.println("Details:")
            e.printStackTrace()
            exitProcess(1)
        }
    }
    
    private fun readAvailableBytes(input: InputStream, maxBytes: Int = 1024): ByteArray {
        val buffer = ByteArray(maxBytes)
        val bytesRead = input.read(buffer, 0, minOf(input.available(), maxBytes))
        return if (bytesRead > 0) buffer.copyOf(bytesRead) else ByteArray(0)
    }
    
    private fun readResponse(input: InputStream, maxBytes: Int = 1024, timeoutMs: Long = 10000): ByteArray {
        val buffer = ByteArray(maxBytes)
        var bytesRead = 0
        val startTime = System.currentTimeMillis()
        
        // Read until we have a complete frame or timeout
        while (bytesRead < maxBytes) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw RuntimeException("Timeout waiting for response after ${timeoutMs}ms")
            }
            
            if (input.available() > 0) {
                val read = input.read(buffer, bytesRead, maxBytes - bytesRead)
                if (read == -1) break
                bytesRead += read
                
                // Check if we have a complete frame
                val accumulated = buffer.copyOf(bytesRead)
                when (val status = NetsBaxProtocol.checkFrameComplete(accumulated)) {
                    is FrameStatus.Complete -> {
                        return accumulated.copyOf(status.frameEnd)
                    }
                    is FrameStatus.NeedMoreData -> {
                        // Keep reading
                        Thread.sleep(50)
                    }
                    else -> {
                        Thread.sleep(50)
                    }
                }
            } else {
                Thread.sleep(50)
            }
        }
        
        return buffer.copyOf(bytesRead)
    }
    
    private fun ByteArray.toHexString(): String = 
        NetsBaxProtocol.run { this@toHexString.toHexString() }
    
    private fun ByteArray.toDebugString(): String = 
        NetsBaxProtocol.run { this@toDebugString.toDebugString() }
}
