package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.Socket
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun main() {
    val ip = "192.168.0.4"
    val port = 8009
    
    println("╔═══════════════════════════════════════════════════════════╗")
    println("║         BAX PROTOCOL ANALYZER & DEBUGGER                  ║")
    println("╚═══════════════════════════════════════════════════════════╝")
    println()
    println("Target: $ip:$port")
    println("Time: ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
    println()
    
    val tests = listOf(
        Test("ENQ (Wake up)", byteArrayOf(0x05)),
        Test("ACK", byteArrayOf(0x06)),
        Test("Status Query", createFrame("S")),
        Test("Purchase P,1,100", createFrame("P,1,100")),
        Test("Purchase P100", createFrame("P100")),
        Test("Purchase 100", createFrame("100")),
        Test("Initialize I", createFrame("I")),
        Test("Reset R", createFrame("R")),
        Test("Version V", createFrame("V")),
        Test("Display Test D,TEST", createFrame("D,TEST")),
    )
    
    tests.forEachIndexed { index, test ->
        println("═".repeat(60))
        println("TEST ${index + 1}/${tests.size}: ${test.name}")
        println("═".repeat(60))
        runTest(ip, port, test)
        println()
        Thread.sleep(500) // Pause mellom tester
    }
    
    println()
    println("╔═══════════════════════════════════════════════════════════╗")
    println("║                    ANALYSIS COMPLETE                      ║")
    println("╚═══════════════════════════════════════════════════════════╝")
}

data class Test(val name: String, val command: ByteArray)

fun createFrame(payload: String): ByteArray {
    val bytes = payload.toByteArray(Charsets.ISO_8859_1)
    val frame = ByteArray(1 + bytes.size + 2) // STX + payload + ETX + LRC
    
    frame[0] = 0x02 // STX
    System.arraycopy(bytes, 0, frame, 1, bytes.size)
    frame[bytes.size + 1] = 0x03 // ETX
    
    // Calculate LRC
    var lrc: Byte = 0
    for (i in 1..bytes.size + 1) {
        lrc = (lrc.toInt() xor frame[i].toInt()).toByte()
    }
    frame[frame.size - 1] = lrc
    
    return frame
}

fun runTest(ip: String, port: Int, test: Test) {
    try {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 3000
            val out = socket.getOutputStream()
            val input = socket.getInputStream()
            
            // Send
            println("→ SENDING:")
            println("  HEX: ${test.command.toHexString()}")
            println("  ASCII: ${test.command.toAsciiString()}")
            println("  Size: ${test.command.size} bytes")
            
            out.write(test.command)
            out.flush()
            
            // Wait and receive
            Thread.sleep(300)
            
            if (input.available() > 0) {
                val buffer = ByteArray(1024)
                val read = input.read(buffer)
                val response = buffer.copyOf(read)
                
                println()
                println("← RECEIVED:")
                println("  HEX: ${response.toHexString()}")
                println("  ASCII: ${response.toAsciiString()}")
                println("  Size: ${response.size} bytes")
                
                // Analysis
                println()
                println("  ANALYSIS:")
                when {
                    response.contains(0x06) -> println("    ✓ ACK (0x06) - Command accepted!")
                    response.contains(0x15) -> {
                        println("    ✗ NAK (0x15) - Command rejected")
                        if (response.size > 1) {
                            println("    Error payload: ${response.drop(1).toByteArray().toHexString()}")
                            decodeError(response)
                        }
                    }
                    response[0] == 0x02.toByte() -> {
                        println("    ℹ Data frame (STX)")
                        parseFrame(response)
                    }
                    else -> println("    ? Unknown response type")
                }
            } else {
                println()
                println("← NO RESPONSE (timeout)")
            }
        }
    } catch (e: Exception) {
        println()
        println("✗ ERROR: ${e.message}")
    }
}

fun decodeError(response: ByteArray) {
    if (response.size >= 7 && response[0] == 0x15.toByte()) {
        val payload = response.slice(1 until response.size)
        println("    Attempting error decode...")
        
        // Common Bax/Nets error codes
        val errorMap = mapOf(
            "03 01 00 02 02" to "No ECR registered / Ingen kasse registrert",
            "03 01 00 03" to "Transaction in progress",
            "03 01 00 04" to "Terminal busy",
            "03 01 00 05" to "Invalid format",
            "03 01 00 06" to "Communication error"
        )
        
        val errorKey = payload.take(5).joinToString(" ") { "%02X".format(it) }
        val meaning = errorMap[errorKey] ?: "Unknown error code"
        println("    → $meaning")
    }
}

fun parseFrame(data: ByteArray) {
    val etxIdx = data.indexOf(0x03)
    if (etxIdx > 0) {
        val payload = data.slice(1 until etxIdx)
        val payloadStr = String(payload.toByteArray(), Charsets.ISO_8859_1)
        println("    Payload: $payloadStr")
    }
}

fun ByteArray.toHexString() = joinToString(" ") { "%02X".format(it) }

fun ByteArray.toAsciiString() = map { byte ->
    val c = byte.toInt() and 0xFF
    when {
        c == 0x02 -> "<STX>"
        c == 0x03 -> "<ETX>"
        c == 0x05 -> "<ENQ>"
        c == 0x06 -> "<ACK>"
        c == 0x15 -> "<NAK>"
        c in 32..126 -> c.toChar().toString()
        else -> "·"
    }
}.joinToString("")
