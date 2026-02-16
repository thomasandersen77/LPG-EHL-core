package no.cloudberries.lpg

import java.net.Socket

fun main() {
    val ip = "192.168.0.4"
    val port = 8009
    
    println("Quick ECR Test - $ip:$port")
    println("=" .repeat(50))
    
    // Test 1: Just connect and see if terminal sends anything
    println("\n1. Opening connection and listening...")
    try {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 2000
            val input = socket.getInputStream()
            Thread.sleep(500)
            
            if (input.available() > 0) {
                val buf = ByteArray(256)
                val read = input.read(buf)
                println("   Terminal sent on connect: ${buf.copyOf(read).hex()}")
            } else {
                println("   No data on connect")
            }
            
            // Test 2: Send Status
            println("\n2. Sending Status (S)...")
            val status = byteArrayOf(0x02, 0x53, 0x03, 0x50)
            socket.getOutputStream().write(status)
            socket.getOutputStream().flush()
            Thread.sleep(500)
            
            if (input.available() > 0) {
                val buf = ByteArray(256)
                val read = input.read(buf)
                println("   Response: ${buf.copyOf(read).hex()}")
            } else {
                println("   No response")
            }
            
            // Test 3: Try to register/initialize
            println("\n3. Sending Registration attempt (R)...")
            val reg = byteArrayOf(0x02, 0x52, 0x03, 0x51)
            socket.getOutputStream().write(reg)
            socket.getOutputStream().flush()
            Thread.sleep(500)
            
            if (input.available() > 0) {
                val buf = ByteArray(256)
                val read = input.read(buf)
                println("   Response: ${buf.copyOf(read).hex()}")
            } else {
                println("   No response")
            }
            
            // Test 4: Purchase
            println("\n4. Sending Purchase (P,1,100)...")
            val purchase = byteArrayOf(0x02, 0x50, 0x2C, 0x31, 0x2C, 0x31, 0x30, 0x30, 0x03, 0x53)
            socket.getOutputStream().write(purchase)
            socket.getOutputStream().flush()
            Thread.sleep(500)
            
            if (input.available() > 0) {
                val buf = ByteArray(256)
                val read = input.read(buf)
                val response = buf.copyOf(read)
                println("   Response: ${response.hex()}")
                
                if (response[0] == 0x06.toByte()) {
                    println("   ✓✓✓ ACK! SUCCESS! ✓✓✓")
                } else if (response[0] == 0x15.toByte()) {
                    println("   ✗ NAK - still rejected")
                }
            } else {
                println("   No response")
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
    
    println("\n" + "=".repeat(50))
}

fun ByteArray.hex() = joinToString(" ") { "%02X".format(it) }
