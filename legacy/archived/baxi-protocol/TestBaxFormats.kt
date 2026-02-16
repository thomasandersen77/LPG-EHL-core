package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.Socket

fun main() {
    val ip = "192.168.0.4"
    val port = 8009
    
    println("Testing different Bax command formats...")
    println("=" .repeat(60))
    
    // Format 1: Standard with operator: P,1,100
    testCommand(ip, port, "Standard P,1,100", createBaxFrame("P,1,100"))
    
    // Format 2: No operator: P100
    testCommand(ip, port, "Simple P100", createBaxFrame("P100"))
    
    // Format 3: With leading zeros: P00000100
    testCommand(ip, port, "P00000100", createBaxFrame("P00000100"))
    
    // Format 4: Amount only: 100
    testCommand(ip, port, "Amount only: 100", createBaxFrame("100"))
    
    // Format 5: Purchase with different separator: P:100
    testCommand(ip, port, "P:100", createBaxFrame("P:100"))
    
    // Format 6: Status command
    testCommand(ip, port, "Status: S", createBaxFrame("S"))
    
    // Format 7: Just ENQ
    testCommand(ip, port, "ENQ only", byteArrayOf(0x05))
}

fun createBaxFrame(payload: String): ByteArray {
    val payloadBytes = payload.toByteArray(Charsets.ISO_8859_1)
    val frame = ByteArray(1 + payloadBytes.size + 1 + 1)
    
    frame[0] = 0x02 // STX
    System.arraycopy(payloadBytes, 0, frame, 1, payloadBytes.size)
    frame[payloadBytes.size + 1] = 0x03 // ETX
    
    // Calculate LRC
    var lrc: Byte = 0
    for (i in 1 until frame.size - 1) {
        lrc = (lrc.toInt() xor frame[i].toInt()).toByte()
    }
    frame[frame.size - 1] = lrc
    
    return frame
}

fun testCommand(ip: String, port: Int, description: String, cmd: ByteArray) {
    println("\nTesting: $description")
    println("Command: ${cmd.joinToString(" ") { "%02X".format(it) }}")
    
    try {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 2000
            socket.getOutputStream().write(cmd)
            socket.getOutputStream().flush()
            
            Thread.sleep(300)
            
            val input = socket.getInputStream()
            if (input.available() > 0) {
                val buffer = ByteArray(256)
                val read = input.read(buffer)
                val response = buffer.copyOf(read)
                
                println("Response: ${response.joinToString(" ") { "%02X".format(it) }}")
                
                when {
                    response.contains(0x06) -> println("✅ ACK - SUCCESS!")
                    response.contains(0x15) -> println("⚠️  NAK - Rejected")
                    else -> println("❓ Unknown response")
                }
            } else {
                println("No response")
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
