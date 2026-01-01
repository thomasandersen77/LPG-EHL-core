package no.cloudberries.lpg

import no.cloudberries.lpg.payment.NetsBaxProtocol
import java.net.Socket

fun main() {
    val ip = "192.168.0.4"
    val port = 8009

    println("🚀 TEST MED ENQ HANDSHAKE")
    println("   Mål: $ip:$port")
    println("--------------------------------------------------")

    try {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 5000
            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            // Steg 1: Send ENQ for å initiere kommunikasjon
            println("1. Sender ENQ (0x05) for å vekke terminalen...")
            out.write(0x05)
            out.flush()
            Thread.sleep(500)

            // Les eventuelt svar på ENQ
            if (input.available() > 0) {
                val buffer = ByteArray(256)
                val read = input.read(buffer)
                val response = buffer.copyOf(read)
                println("   Svar på ENQ: ${response.joinToString(" ") { "%02X".format(it) }}")
                
                when {
                    response.contains(0x06) -> println("   ✓ ACK mottatt")
                    response.contains(0x15) -> println("   ✗ NAK mottatt")
                }
            } else {
                println("   (Ingen respons på ENQ)")
            }

            // Steg 2: Send Purchase
            println("\n2. Sender Purchase (1 krone)...")
            val cmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
            println("   Kommando: ${cmd.joinToString(" ") { "%02X".format(it) }}")
            out.write(cmd)
            out.flush()

            // Steg 3: Les respons
            println("\n3. Venter på respons...")
            Thread.sleep(300)
            
            if (input.available() > 0) {
                val buffer = ByteArray(256)
                val read = input.read(buffer)
                val response = buffer.copyOf(read)
                println("   Respons: ${response.joinToString(" ") { "%02X".format(it) }}")

                when {
                    response.contains(0x06) -> {
                        println("\n🎉 SUCCESS! ACK MOTTATT!")
                        println("   SJEKK TERMINALEN - DEN SKAL BE OM KORT!")
                    }
                    response.contains(0x15) -> {
                        println("\n⚠️  NAK mottatt")
                        // Dekode feilmeldingen
                        if (response.size >= 7) {
                            val errorCode = response.slice(3..6)
                            println("   Feilkode: ${errorCode.joinToString(" ") { "%02X".format(it) }}")
                        }
                    }
                    else -> println("   Ukjent respons")
                }
            } else {
                println("   Ingen respons (timeout)")
            }
        }
    } catch (e: Exception) {
        println("🔥 Feil: ${e.message}")
        e.printStackTrace()
    }
}
