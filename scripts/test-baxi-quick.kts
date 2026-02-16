#!/usr/bin/env kotlin

/**
 * Quick Connectivity Test for Baxi Terminal
 * 
 * Just opens terminal and checks if it becomes ready.
 * Fast iteration for debugging connectivity issues.
 * 
 * Usage:
 *   kotlin test-baxi-quick.main.kts
 */

@file:DependsOn("no.cloudberries.norgesgass:baxi-kotlin:0.1.0-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.9")

import no.cloudberries.norgesgass.baxi.client.BaxiClient
import no.cloudberries.norgesgass.baxi.client.BaxiClientImpl
import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

println("🔍 Quick Baxi Terminal Connectivity Test")
println("═".repeat(60))

// Configuration
val HOST = System.getenv("NETS_HOST") ?: "connectcloud.aws.nets.eu"
val PORT = System.getenv("NETS_PORT")?.toInt() ?: 443

println("Host: $HOST")
println("Port: $PORT")
println()

val client: BaxiClient = BaxiClientImpl()
val readyLatch = CountDownLatch(1)
var errorMessage: String? = null

client.setEventListener(object : BaxiEventListener {
    override fun onTerminalReady() {
        println("✅ Terminal is READY!")
        readyLatch.countDown()
    }
    
    override fun onError(errorCode: Int, errorString: String?) {
        errorMessage = "Error $errorCode: $errorString"
        println("❌ $errorMessage")
        readyLatch.countDown()
    }
    
    override fun onDisplayText(displayText: String, displayTextSourceId: Int?, displayTextId: Int?) {
        println("   Display: $displayText")
    }
})

try {
    println("🔌 Opening terminal...")
    
    val config = BaxiIniConfig(
        hostIpAddress = HOST,
        hostPort = PORT,
        vendorInfoExtended = "LPG-EHL-QUICK-TEST",
        socketListenerEnabled = false,
        socketListenerPort = null
    )
    
    val result = client.open(config)
    println("   Open() returned: callResult=${result.callResult}")
    
    if (result.callResult != 1) {
        println("❌ Open failed: ${result.methodRejectCode} - ${result.methodRejectInfo}")
        System.exit(1)
    }
    
    println("⏳ Waiting for terminal ready (30s timeout)...")
    
    if (readyLatch.await(30, TimeUnit.SECONDS)) {
        if (errorMessage != null) {
            println("\n❌ Terminal reported error during opening")
            System.exit(1)
        } else {
            println("\n✅ SUCCESS! Terminal is ready and connected!")
            println("═".repeat(60))
        }
    } else {
        println("\n⏱️  TIMEOUT - Terminal did not become ready within 30 seconds")
        println("   This could mean:")
        println("   - Network connectivity issues")
        println("   - Incorrect host/port")
        println("   - Terminal is offline or not responding")
        System.exit(1)
    }
    
} catch (e: Exception) {
    println("\n💥 Exception: ${e.message}")
    e.printStackTrace()
    System.exit(1)
} finally {
    println("\n🔒 Closing...")
    client.close()
}
