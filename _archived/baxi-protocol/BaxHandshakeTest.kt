package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.Socket

fun main() {
    val ip = "192.168.0.4"
    val port = 8009
    
    println("╔════════════════════════════════════════════════════════╗")
    println("║        BAX PROTOCOL - HANDSHAKE & PURCHASE TEST        ║")
    println("╚════════════════════════════════════════════════════════╝")
    println()
    println("Terminal: $ip:$port")
    println("Whitelist: 0.0.0.0 (accepts all)")
    println()
    
    try {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 5000
            val out = socket.getOutputStream()
            val input = socket.getInputStream()
            
            println("✓ TCP Connection established")
            println()
            
            // Step 1: Try admin login/open command
            println("═══════════════════════════════════════════════════════")
            println("STEP 1: Administrative Login/Open")
            println("═══════════════════════════════════════════════════════")
            
            // Try different admin commands
            val adminCommands = listOf(
                "L" to "Login",
                "O" to "Open",
                "A" to "Admin",
                "I" to "Initialize"
            )
            
            var gotAck = false
            
            for ((cmd, desc) in adminCommands) {
                println("\nTrying: $desc ($cmd)")
                val frame = buildBaxFrame(cmd)
                println("  → Sending: ${frame.toHex()}")
                
                out.write(frame)
                out.flush()
                Thread.sleep(500)
                
                if (input.available() > 0) {
                    val buf = ByteArray(256)
                    val read = input.read(buf)
                    val response = buf.copyOf(read)
                    println("  ← Response: ${response.toHex()}")
                    
                    when {
                        response.contains(0x06) -> {
                            println("  ✓✓✓ ACK (0x06) - LOGIN SUCCESS!")
                            gotAck = true
                            break
                        }
                        response.contains(0x15) -> {
                            println("  ✗ NAK (0x15) - Rejected")
                        }
                        else -> {
                            println("  ? Unknown response")
                        }
                    }
                } else {
                    println("  ○ No response")
                }
            }
            
            if (!gotAck) {
                println("\n⚠ No ACK on admin commands. Trying direct purchase...")
            }
            
            println()
            println("═══════════════════════════════════════════════════════")
            println("STEP 2: Purchase Request (1.00 NOK)")
            println("═══════════════════════════════════════════════════════")
            
            val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
            println("\n→ Sending Purchase: ${purchaseCmd.joinToString(" ") { "%02X".format(it) }}")
            
            out.write(purchaseCmd)
            out.flush()
            
            println("⏳ Waiting for terminal response...")
            Thread.sleep(1000)
            
            if (input.available() > 0) {
                val buf = ByteArray(1024)
                val read = input.read(buf)
                val response = buf.copyOf(read)
                
                println("← Response: ${response.toHex()}")
                println()
                
                when {
                    response.contains(0x06) -> {
                        println("╔════════════════════════════════════════════════════════╗")
                        println("║              🎉 SUCCESS! ACK RECEIVED! 🎉              ║")
                        println("╚════════════════════════════════════════════════════════╝")
                        println()
                        println("✓ Terminal accepted purchase request!")
                        println("✓ Check terminal screen - should prompt for card")
                        println()
                    }
                    response.contains(0x15) -> {
                        println("✗ NAK - Purchase rejected")
                        println("  Error code: ${response.toHex()}")
                        if (response.size >= 7) {
                            analyzeError(response)
                        }
                    }
                    response[0] == 0x02.toByte() -> {
                        println("ℹ Data frame received")
                        parseDataFrame(response)
                    }
                    else -> {
                        println("? Unknown response type")
                    }
                }
            } else {
                println("✗ No response (timeout)")
            }
        }
    } catch (e: Exception) {
        println()
        println("✗ ERROR: ${e.message}")
        e.printStackTrace()
    }
    
    println()
    println("═".repeat(58))
    println("Test complete")
}

fun buildBaxFrame(payload: String): ByteArray {
    val bytes = payload.toByteArray(Charsets.ISO_8859_1)
    val frame = ByteArray(1 + bytes.size + 2)
    
    frame[0] = 0x02 // STX
    System.arraycopy(bytes, 0, frame, 1, bytes.size)
    frame[bytes.size + 1] = 0x03 // ETX
    
    // LRC
    var lrc: Byte = 0
    for (i in 1..bytes.size + 1) {
        lrc = (lrc.toInt() xor frame[i].toInt()).toByte()
    }
    frame[frame.size - 1] = lrc
    
    return frame
}

fun analyzeError(response: ByteArray) {
    val errors = mapOf(
        "03 01 00 02 02" to "No ECR registered",
        "03 01 00 03" to "Transaction in progress",
        "03 01 00 04" to "Terminal busy"
    )
    
    val key = response.drop(1).take(5).joinToString(" ") { "%02X".format(it) }
    val meaning = errors[key] ?: "Unknown error"
    println("  → $meaning")
}

fun parseDataFrame(data: ByteArray) {
    val etxIdx = data.indexOf(0x03)
    if (etxIdx > 0) {
        val payload = data.slice(1 until etxIdx)
        val str = String(payload.toByteArray(), Charsets.ISO_8859_1)
        println("  Payload: $str")
    }
}

fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }
