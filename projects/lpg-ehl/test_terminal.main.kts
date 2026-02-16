#!/usr/bin/env kotlin

import java.net.Socket
import java.net.InetSocketAddress

// Nets/BAX Protocol constants
const val STX: Byte = 0x02
const val ETX: Byte = 0x03
const val ACK: Byte = 0x06
const val NAK: Byte = 0x15

// Terminal connection
val HOST = System.getenv("TERMINAL_HOST") ?: "192.168.0.41"
val PORT = System.getenv("TERMINAL_PORT")?.toInt() ?: 8009
val TIMEOUT_MS = 15000 // 15 seconds for payment

fun hexdump(b: ByteArray): String = b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

fun calculateLrc(data: ByteArray, startIndex: Int = 0, endIndex: Int = data.size): Byte {
    var lrc: Byte = 0
    for (i in startIndex until endIndex) {
        lrc = (lrc.toInt() xor data[i].toInt()).toByte()
    }
    return lrc
}

fun buildFrame(payload: String): ByteArray {
    val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
    val frame = ByteArray(1 + payloadBytes.size + 1 + 1)
    
    frame[0] = STX
    System.arraycopy(payloadBytes, 0, frame, 1, payloadBytes.size)
    frame[payloadBytes.size + 1] = ETX
    
    // Calculate LRC: XOR of payload + ETX
    val lrc = calculateLrc(frame, startIndex = 1, endIndex = frame.size - 1)
    frame[frame.size - 1] = lrc
    
    return frame
}

fun readResponse(socket: Socket, timeoutMs: Int = 5000): ByteArray {
    val start = System.currentTimeMillis()
    val buffer = mutableListOf<Byte>()
    val input = socket.getInputStream()
    
    socket.soTimeout = 500 // Short reads
    
    while (System.currentTimeMillis() - start < timeoutMs) {
        try {
            val available = input.available()
            if (available > 0) {
                val chunk = ByteArray(available)
                val read = input.read(chunk)
                for (i in 0 until read) {
                    buffer.add(chunk[i])
                }
                
                // Check if we have complete response
                val hasStx = buffer.contains(STX)
                val hasEtx = buffer.contains(ETX)
                if (hasStx && hasEtx) {
                    val etxIndex = buffer.indexOf(ETX)
                    if (etxIndex + 1 < buffer.size) {
                        // We have LRC too
                        break
                    }
                }
            }
            Thread.sleep(50)
        } catch (e: Exception) {
            // Timeout on read is OK, continue
        }
    }
    
    return buffer.toByteArray()
}

fun parseResponse(data: ByteArray): String? {
    if (data.isEmpty()) return null
    
    // Find STX and ETX
    val stxIndex = data.indexOf(STX)
    val etxIndex = data.indexOf(ETX)
    
    if (stxIndex >= 0 && etxIndex > stxIndex) {
        val payload = data.copyOfRange(stxIndex + 1, etxIndex)
        return String(payload, Charsets.ISO_8859_1)
    }
    
    // Single byte responses
    return when {
        data.size == 1 && data[0] == ACK -> "ACK"
        data.size == 1 && data[0] == NAK -> "NAK"
        else -> null
    }
}

println("=" .repeat(60))
println("🔧 NETS/BAX TERMINAL TEST")
println("=" .repeat(60))
println("Terminal: $HOST:$PORT")
println("Amount: 1.00 NOK (100 øre)")
println()

try {
    val socket = Socket()
    socket.connect(InetSocketAddress(HOST, PORT), 5000)
    println("✅ Connected to terminal")
    
    val output = socket.getOutputStream()
    val input = socket.getInputStream()
    
    // Create Purchase command: P,<OperatorID>,<AmountCents>
    val purchaseCommand = "P,1,100"  // 1 NOK = 100 øre
    val frame = buildFrame(purchaseCommand)
    
    println()
    println("📤 Sending Purchase command:")
    println("   Command: $purchaseCommand")
    println("   Frame: ${hexdump(frame)}")
    println()
    
    output.write(frame)
    output.flush()
    
    println("⏳ Waiting for terminal response (timeout: ${TIMEOUT_MS/1000}s)...")
    println("   (Terminal should display: 'Beløp: 1.00 kr')")
    println()
    
    // Read response with longer timeout for user interaction
    val response = readResponse(socket, TIMEOUT_MS)
    
    if (response.isEmpty()) {
        println("❌ No response from terminal")
        println("   Check:")
        println("   - Is terminal powered on?")
        println("   - Is ECR mode enabled?")
        println("   - Is correct IP/port configured?")
    } else {
        println("📥 Received response (${response.size} bytes):")
        println("   Hex: ${hexdump(response)}")
        
        val parsed = parseResponse(response)
        if (parsed != null) {
            println("   Parsed: $parsed")
            
            when {
                parsed == "ACK" -> println("   ✅ Terminal acknowledged")
                parsed == "NAK" -> println("   ❌ Terminal rejected (not ready or ECR not registered)")
                parsed.startsWith("APPROVED") -> println("   ✅ PAYMENT APPROVED!")
                parsed.startsWith("DECLINED") -> println("   ❌ Payment declined")
                parsed.contains("NO ECR") -> println("   ⚠️ ECR not registered - check terminal settings")
                else -> println("   ℹ️ Response: $parsed")
            }
        }
    }
    
    socket.close()
    
} catch (e: Exception) {
    println("❌ Error: ${e.message}")
    println()
    println("Troubleshooting:")
    println("- Verify terminal IP address: $HOST")
    println("- Verify ECR port: $PORT (should be 8009)")
    println("- Check network connectivity: ping $HOST")
    println("- Ensure terminal is in ECR mode")
}

println()
println("=" .repeat(60))
