package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Socket

/**
 * Interactive Payment Demo
 * 
 * Connects to REAL payment terminal and initiates a payment.
 * Waits for user to tap card before completing.
 */
object InteractivePaymentDemo {
    
    private val logger = LoggerFactory.getLogger(InteractivePaymentDemo::class.java)
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("═══════════════════════════════════════════════")
        println("   INTERAKTIV BETALINGSDEMO MED EKTE TERMINAL")
        println("═══════════════════════════════════════════════")
        println()
        
        val amountOre = 3  // 3 øre = 0.03 kr
        
        println("Terminal: 192.168.0.4:8009")
        println("Beløp: $amountOre øre (${amountOre / 100.0} kr)")
        println()
        println("⚠️  Du må tappe kortet når terminalen ber om det!")
        println()
        
        var socket: Socket? = null
        
        try {
            // 1. Connect to terminal
            println("1. Kobler til terminal...")
            socket = Socket("192.168.0.4", 8009)
            socket.soTimeout = 60000  // 60 seconds timeout
            println("   ✓ Tilkoblet!")
            println()
            
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            
            // 2. Wait for initial response from terminal
            println("2. Venter på terminal...")
            val initialBuffer = ByteArray(1024)
            val initialBytes = input.read(initialBuffer)
            if (initialBytes > 0) {
                val initialData = initialBuffer.copyOf(initialBytes)
                println("   Terminal sendte: ${initialData.size} bytes")
                printHexDump("   ", initialData)
            }
            println()
            
            // 3. Send payment request
            println("3. Sender betalingsforespørsel: $amountOre øre")
            
            // Try different protocol approaches
            
            // Approach 1: Simple text
            val textRequest = "PAYMENT:$amountOre\n"
            output.write(textRequest.toByteArray())
            output.flush()
            println("   → Sendt tekstmelding: $textRequest")
            
            Thread.sleep(500)
            
            // Approach 2: Binary amount (BCD format)
            val binaryRequest = buildBinaryPaymentRequest(amountOre)
            output.write(binaryRequest)
            output.flush()
            println("   → Sendt binær melding:")
            printHexDump("      ", binaryRequest)
            println()
            
            // 4. Wait for response and card tap
            println("4. Venter på respons fra terminal...")
            println()
            println("╔═══════════════════════════════════════════════╗")
            println("║                                               ║")
            println("║   🔔 TAPPE KORTET NÅ!                         ║")
            println("║                                               ║")
            println("║   Terminalen skal be om betaling             ║")
            println("║   Hold kortet mot terminalen                 ║")
            println("║                                               ║")
            println("║   (Venter i maks 60 sekunder...)             ║")
            println("║                                               ║")
            println("╚═══════════════════════════════════════════════╝")
            println()
            
            // Read responses until timeout or completion
            val buffer = ByteArray(4096)
            var totalBytesRead = 0
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < 60000) {
                if (input.available() > 0) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        totalBytesRead += bytesRead
                        val data = buffer.copyOf(bytesRead)
                        
                        println("📥 Mottok ${bytesRead} bytes fra terminal:")
                        printHexDump("   ", data)
                        
                        // Try to parse as text
                        val text = String(data, Charsets.UTF_8).trim()
                        if (text.isNotEmpty() && text.all { it.isLetterOrDigit() || it.isWhitespace() || it in ":-_." }) {
                            println("   Tekst: \"$text\"")
                        }
                        println()
                        
                        // Check for approval indicators
                        if (text.contains("OK", ignoreCase = true) || 
                            text.contains("APPROVED", ignoreCase = true) ||
                            data.contains(0x06.toByte())) {
                            println("✅ BETALING GODKJENT!")
                            break
                        }
                        
                        // Check for decline indicators
                        if (text.contains("DECLINED", ignoreCase = true) ||
                            text.contains("ERROR", ignoreCase = true)) {
                            println("❌ BETALING AVVIST!")
                            break
                        }
                    }
                }
                
                Thread.sleep(100)
            }
            
            if (totalBytesRead == 0) {
                println("⏱️  Timeout - ingen respons fra terminal")
                println()
                println("Mulige årsaker:")
                println("- Terminalen krever spesiell initialisering")
                println("- Feil protokoll-format")
                println("- Terminalen venter på annet kommando")
            }
            
        } catch (e: IOException) {
            println("❌ Feil: ${e.message}")
            e.printStackTrace()
        } finally {
            socket?.close()
            println()
            println("═══════════════════════════════════════════════")
            println("   Demo avsluttet")
            println("═══════════════════════════════════════════════")
        }
    }
    
    /**
     * Build a binary payment request
     * Tries common ECR/ZVT formats
     */
    private fun buildBinaryPaymentRequest(amountOre: Int): ByteArray {
        // Format 1: ZVT-style (simplified)
        // 06 01 [amount in BCD]
        
        val bcd = amountToBCD(amountOre)
        
        return byteArrayOf(
            0x06,  // ENQ or Command code
            0x01,  // Payment command
            *bcd   // Amount
        )
    }
    
    /**
     * Convert amount to BCD (Binary Coded Decimal)
     */
    private fun amountToBCD(amount: Int): ByteArray {
        val str = String.format("%012d", amount)  // 12 digits
        val bytes = ByteArray(6)
        
        for (i in 0..5) {
            val high = str[i * 2].digitToInt()
            val low = str[i * 2 + 1].digitToInt()
            bytes[i] = ((high shl 4) or low).toByte()
        }
        
        return bytes
    }
    
    /**
     * Print hex dump with ASCII
     */
    private fun printHexDump(prefix: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        val ascii = data.map { 
            val c = it.toInt() and 0xFF
            if (c in 32..126) c.toChar() else '.'
        }.joinToString("")
        
        println("${prefix}HEX:   $hex")
        println("${prefix}ASCII: $ascii")
    }
}
