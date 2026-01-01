package no.cloudberries.lpg.payment

object ShowBaxCommand {
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Bax Command Builder ===\n")
        
        val purchaseCmd = NetsBaxProtocol.createPurchaseCommand(100, "1")
        println("Purchase 100 øre (1.00 NOK):")
        println("  Hex: ${purchaseCmd.toHexString()}")
        println("  Debug: ${purchaseCmd.toDebugString()}")
        println()
        
        val statusCmd = NetsBaxProtocol.createStatusCommand()
        println("Status Query:")
        println("  Hex: ${statusCmd.toHexString()}")
        println("  Debug: ${statusCmd.toDebugString()}")
        println()
        
        // Show manual calculation for verification
        val payload = "P,1,100"
        val bytes = payload.toByteArray(Charsets.ISO_8859_1)
        println("Manual LRC calculation for '$payload':")
        println("  Payload bytes: ${bytes.joinToString(" ") { "%02X".format(it) }}")
        
        var lrc: Byte = 0
        for (b in bytes) {
            lrc = (lrc.toInt() xor b.toInt()).toByte()
        }
        lrc = (lrc.toInt() xor 0x03).toByte()
        
        println("  LRC (payload XOR ETX): %02X".format(lrc))
        println()
        
        println("Expected frame:")
        println("  02 ${bytes.joinToString(" ") { "%02X".format(it) }} 03 %02X".format(lrc))
    }
    
    private fun ByteArray.toHexString(): String = 
        NetsBaxProtocol.run { this@toHexString.toHexString() }
    
    private fun ByteArray.toDebugString(): String = 
        NetsBaxProtocol.run { this@toDebugString.toDebugString() }
}
