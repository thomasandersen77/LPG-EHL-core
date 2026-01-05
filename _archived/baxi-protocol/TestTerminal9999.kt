package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

fun main() {
    val ip = "192.168.0.4"
    val port = 8009 // Using current active port

    println("🚀 STARTER TEST MOT PORT $port")
    println("--------------------------------------------------")
    println("mål: Betale 1 krone (Purchase)")
    println("Terminal IP: $ip")
    println("ECR Port:    $port")
    println("--------------------------------------------------")

    try {
        val socket = Socket()
        socket.connect(InetSocketAddress(ip, port), 5000)
        socket.soTimeout = 15000 // 15 sekunders tålmodighet

        socket.use { s ->
            println("✅ TILKOBLET! (TCP Connection Established)")
            
            // Bygg Bax-pakken: Purchase, Operator 1, 100 øre (1 kr)
            val cmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
            
            // Send
            println("📤 Sender kommando (HEX): ${cmd.joinToString(" ") { "%02X".format(it) }}")
            s.getOutputStream().write(cmd)
            s.getOutputStream().flush()

            // Lytt etter svar
            println("⏳ Venter på svar fra terminalen...")
            val buffer = ByteArray(1024)
            val inputStream = s.getInputStream()
            
            val accumulated = ArrayList<Byte>()
            val start = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - start < 15000) {
                if (inputStream.available() > 0) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    for (i in 0 until read) accumulated.add(buffer[i])
                    
                    // Sjekk om vi har fått ACK (06)
                    if (accumulated.contains(0x06.toByte())) {
                        println("🎉 HURRA! Fikk ACK (0x06). Terminalen har godkjent forespørselen!")
                        println("   SE PÅ TERMINALSKJERMEN NÅ!")
                        break
                    }
                    // Sjekk om vi fikk NAK (15)
                    if (accumulated.contains(0x15.toByte())) {
                        println("⚠️  Fikk NAK (0x15). Forbindelse ok, men terminalen sa nei til innholdet.")
                        break
                    }
                }
                Thread.sleep(50)
            }
            
            println("--------------------------------------------------")
            println("Mottatt totalt: ${accumulated.size} bytes")
            println("HEX DUMP: ${accumulated.joinToString(" ") { "%02X".format(it) }}")
        }
    } catch (e: Exception) {
        println("🔥 FEIL: ${e.message}")
        e.printStackTrace()
    }
}
