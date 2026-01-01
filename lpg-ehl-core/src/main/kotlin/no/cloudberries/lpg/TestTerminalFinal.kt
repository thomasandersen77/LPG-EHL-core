package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.InetSocketAddress
import java.net.Socket

fun main() {
    // KONFIGURASJON
    val terminalIp = "192.168.0.4"  // Terminalens IP
    val terminalPort = 8009       // Terminalens Port

    println("🚀 STARTER ENDELIG TEST")
    println("   Mål:           $terminalIp:$terminalPort")
    println("   Handling:      Kjøp 1 krone (Purchase)")
    println("--------------------------------------------------")

    try {
        val socket = Socket()
        socket.connect(InetSocketAddress(terminalIp, terminalPort), 5000)
        socket.soTimeout = 10000

        socket.use { s ->
            println("✅ TILKOBLET! Sender kommando...")
            
            // Bax format: Purchase, Operator 1, 100 øre
            val cmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
            
            // Send
            s.getOutputStream().write(cmd)
            s.getOutputStream().flush()

            // Les svar
            val buffer = ByteArray(1024)
            val inputStream = s.getInputStream()
            val start = System.currentTimeMillis()
            
            println("⏳ Venter på svar fra terminalen...")
            
            while (System.currentTimeMillis() - start < 10000) {
                if (inputStream.available() > 0) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    
                    val data = buffer.copyOf(read)
                    // Sjekk etter ACK (0x06)
                    if (data.contains(0x06.toByte())) {
                        println("\n🎉 SUKSESS! MOTTATT ACK (0x06)!")
                        println("   Terminalen lyser nå opp og ber om kort.")
                        println("   GRATULERER! Du kan ta kveld.")
                        return
                    }
                    // Sjekk etter NAK (0x15)
                    if (data.contains(0x15.toByte())) {
                        println("\n⚠️  Mottok NAK (0x15). Samband OK, men terminalen nektet (sjekk skjermen).")
                        return
                    }
                }
                Thread.sleep(50)
            }
            println("❌ Ingen svar (Timeout). Sjekk ECR IP Whitelist en siste gang.")
        }
    } catch (e: Exception) {
        println("🔥 FEIL: ${e.message}")
    }
}
