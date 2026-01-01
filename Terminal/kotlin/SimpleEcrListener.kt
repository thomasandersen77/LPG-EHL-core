package no.cloudberries.lpg.payment

import java.net.ServerSocket
import java.net.Socket

/**
 * Simple ECR Listener
 * 
 * Listens on port 8009 and logs EVERYTHING the terminal sends.
 * This helps us understand the protocol.
 */
object SimpleEcrListener {
    
    @JvmStatic
    fun main(args: Array<String>) {
        val port = 8009
        
        println("═══════════════════════════════════════════════")
        println("   ECR LISTENER - FANGER TERMINAL-MELDINGER")
        println("═══════════════════════════════════════════════")
        println()
        println("Lytter på port $port...")
        println("Terminalen vil nå prøve å koble til")
        println()
        println("⚠️  VIKTIG:")
        println("- La terminalen stå på")
        println("- Den vil sende meldinger automatisk")
        println("- Logg vil vise ALLE bytes")
        println()
        println("Trykk Ctrl+C for å stoppe")
        println()
        println("═══════════════════════════════════════════════")
        println()
        
        val serverSocket = ServerSocket(port)
        
        while (true) {
            try {
                println("⏳ Venter på tilkobling fra terminal...")
                val socket = serverSocket.accept()
                
                val remoteAddr = socket.inetAddress.hostAddress
                val remotePort = socket.port
                
                println()
                println("✅ TILKOBLET!")
                println("   Fra: $remoteAddr:$remotePort")
                println("   Tid: ${java.time.LocalTime.now()}")
                println()
                println("───────────────────────────────────────────────")
                
                handleConnection(socket)
                
                println("───────────────────────────────────────────────")
                println()
                println("❌ Forbindelse lukket")
                println()
                
            } catch (e: Exception) {
                println("❌ Feil: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun handleConnection(socket: Socket) {
        socket.soTimeout = 120000  // 2 minutes timeout
        
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        
        var messageCount = 0
        
        try {
            val buffer = ByteArray(4096)
            
            while (!socket.isClosed) {
                val bytesRead = input.read(buffer)
                
                if (bytesRead <= 0) {
                    println("   [EOF - forbindelse lukket av terminal]")
                    break
                }
                
                messageCount++
                val data = buffer.copyOf(bytesRead)
                
                println()
                println("📥 MELDING #$messageCount (${bytesRead} bytes)")
                println("   Tid: ${java.time.LocalTime.now()}")
                println()
                
                // Show HEX
                print("   HEX:   ")
                data.forEachIndexed { i, byte ->
                    print("%02X ".format(byte))
                    if ((i + 1) % 16 == 0 && i < data.size - 1) {
                        print("\n          ")
                    }
                }
                println()
                
                // Show ASCII
                print("   ASCII: ")
                data.forEach { byte ->
                    val c = byte.toInt() and 0xFF
                    if (c in 32..126) {
                        print(c.toChar())
                    } else {
                        print('.')
                    }
                }
                println()
                
                // Show decimal
                print("   DEC:   ")
                data.take(16).forEachIndexed { i, byte ->
                    print("%3d ".format(byte.toInt() and 0xFF))
                }
                if (data.size > 16) print("...")
                println()
                
                // Analyze first byte
                val firstByte = data[0].toInt() and 0xFF
                print("   Type:  ")
                when (firstByte) {
                    0x05 -> println("ENQ (Enquiry)")
                    0x06 -> println("ACK (Acknowledge)")
                    0x15 -> println("NAK (Negative Acknowledge)")
                    0x02 -> println("STX (Start of Text)")
                    0x03 -> println("ETX (End of Text)")
                    0x10 -> println("DLE (Data Link Escape)")
                    0x16 -> println("SYN (Synchronous Idle)")
                    0x04 -> println("EOT (End of Transmission)")
                    else -> println("Data (0x%02X = %d)".format(firstByte, firstByte))
                }
                
                println()
                println("   💬 Hva skal vi svare?")
                println("   → Sender ACK (0x06)")
                
                // Send ACK
                output.write(0x06)
                output.flush()
                
                println("   ✓ Sendt ACK")
                println()
                
                Thread.sleep(100)
            }
            
        } catch (e: java.net.SocketTimeoutException) {
            println()
            println("⏱️  Timeout - ingen flere meldinger")
        } catch (e: Exception) {
            println()
            println("❌ Feil under kommunikasjon: ${e.message}")
        }
        
        println()
        println("📊 OPPSUMMERING:")
        println("   Totalt $messageCount meldinger mottatt")
        println()
    }
}
