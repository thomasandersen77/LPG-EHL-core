#!/usr/bin/env kotlin

/**
 * Test Script for Baxi Terminal Integration with Nets Connect@Cloud
 * 
 * Usage:
 *   kotlin test-baxi-terminal.main.kts
 * 
 * Environment variables (optional overrides):
 *   NETS_HOST=connectcloud.aws.nets.eu (or IP address)
 *   NETS_PORT=443 (or custom port)
 *   TERMINAL_ID=42696609
 */

@file:DependsOn("no.cloudberries.norgesgass:baxi-kotlin:0.1.0-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.9")

import no.cloudberries.norgesgass.baxi.client.BaxiClient
import no.cloudberries.norgesgass.baxi.client.BaxiClientImpl
import no.cloudberries.norgesgass.baxi.client.TransferAmountArgs
import no.cloudberries.norgesgass.baxi.client.AdministrationArgs
import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import no.cloudberries.norgesgass.baxi.events.LocalModeEvent
import no.cloudberries.norgesgass.baxi.events.LastFinancialResultEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// ============================================================================
// CONFIGURATION - Update these values
// ============================================================================

val NETS_CLOUD_CONFIG = mapOf(
    // Nets Connect@Cloud endpoint
    "host" to (System.getenv("NETS_HOST") ?: "connectcloud.aws.nets.eu"),
    "port" to (System.getenv("NETS_PORT") ?: "443").toInt(),
    
    // Credentials from server.json
    "username" to "cloudberries_shared",
    "password" to "B8PnVjmVq-SMM9QD",
    "terminalId" to (System.getenv("TERMINAL_ID") ?: "42696609"),
    
    // Environment: PROD or QA
    "environment" to "PROD"
)

println("━".repeat(80))
println("🧪 Baxi Terminal Test Script")
println("━".repeat(80))
println("Host:        ${NETS_CLOUD_CONFIG["host"]}")
println("Port:        ${NETS_CLOUD_CONFIG["port"]}")
println("Terminal ID: ${NETS_CLOUD_CONFIG["terminalId"]}")
println("Environment: ${NETS_CLOUD_CONFIG["environment"]}")
println("━".repeat(80))
println()

// ============================================================================
// EVENT LISTENER
// ============================================================================

class TestEventListener : BaxiEventListener {
    private var terminalReadyLatch: CountDownLatch? = null
    private var operationCompleteLatch: CountDownLatch? = null
    
    var lastLocalMode: LocalModeEvent? = null
    var lastFinancialResult: LastFinancialResultEvent? = null
    var lastDisplayText: String? = null
    private val printTextBuilder = StringBuilder()
    
    fun waitForReady(timeoutSeconds: Long = 30): Boolean {
        terminalReadyLatch = CountDownLatch(1)
        return terminalReadyLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: false
    }
    
    fun waitForOperation(timeoutSeconds: Long = 60): Boolean {
        operationCompleteLatch = CountDownLatch(1)
        lastLocalMode = null
        lastFinancialResult = null
        printTextBuilder.clear()
        return operationCompleteLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: false
    }
    
    override fun onTerminalReady() {
        println("✅ Terminal Ready!")
        terminalReadyLatch?.countDown()
    }
    
    override fun onDisplayText(displayText: String, displayTextSourceId: Int?, displayTextId: Int?) {
        println("📺 Display: $displayText")
        lastDisplayText = displayText
    }
    
    override fun onPrintText(printText: String) {
        println("🖨️  Print: $printText")
        printTextBuilder.append(printText).append("\n")
    }
    
    override fun onError(errorCode: Int, errorString: String?) {
        println("❌ Error: code=$errorCode, message=$errorString")
    }
    
    override fun onLocalMode(event: LocalModeEvent) {
        println("💳 LocalMode: result=${event.result}, responseCode=${event.responseCode}")
        println("   Fields: ${event.fields}")
        lastLocalMode = event
        checkComplete()
    }
    
    override fun onLastFinancialResult(event: LastFinancialResultEvent) {
        println("💰 FinancialResult: result=${event.result}, data=${event.resultData}")
        lastFinancialResult = event
        checkComplete()
    }
    
    private fun checkComplete() {
        if (lastLocalMode != null && lastFinancialResult != null) {
            operationCompleteLatch?.countDown()
        }
    }
    
    fun getPrintedReceipt(): String = printTextBuilder.toString()
}

// ============================================================================
// MAIN TEST FLOW
// ============================================================================

val client: BaxiClient = BaxiClientImpl()
val listener = TestEventListener()

try {
    client.setEventListener(listener)
    
    // Step 1: Open terminal
    println("\n🔌 Step 1: Opening terminal...")
    val config = BaxiIniConfig(
        hostIpAddress = NETS_CLOUD_CONFIG["host"] as String,
        hostPort = NETS_CLOUD_CONFIG["port"] as Int,
        vendorInfoExtended = "LPG-EHL-TEST",
        socketListenerEnabled = false,
        socketListenerPort = null
    )
    
    val openResult = client.open(config)
    println("Open call result: ${openResult.callResult}")
    
    if (openResult.callResult != 1) {
        println("❌ Failed to open terminal: ${openResult.methodRejectCode} - ${openResult.methodRejectInfo}")
        System.exit(1)
    }
    
    println("⏳ Waiting for terminal ready...")
    if (!listener.waitForReady(30)) {
        println("❌ Timeout waiting for terminal ready")
        System.exit(1)
    }
    
    // Step 2: Get terminal status
    println("\n📊 Step 2: Terminal is ready!")
    Thread.sleep(1000)
    
    // Step 3: Test purchase (MAX 1 KRONE FOR TESTING!)
    println("\n💳 Step 3: Testing purchase (1.00 NOK - TEST LIMIT)...")
    val purchaseArgs = TransferAmountArgs(
        operId = "0000",
        type1 = 10, // Purchase
        amount1 = 100, // 1.00 NOK (100 øre) - MAX FOR TESTING!
        type2 = 0,
        amount2 = 0,
        type3 = 0,
        amount3 = 0,
        optionalData = "LPG Test"
    )
    
    val transferResult = client.transferAmount(purchaseArgs)
    println("TransferAmount call result: ${transferResult.callResult}")
    
    if (transferResult.callResult != 1) {
        println("❌ Failed to initiate purchase: ${transferResult.methodRejectCode} - ${transferResult.methodRejectInfo}")
    } else {
        println("⏳ Waiting for operation to complete...")
        if (listener.waitForOperation(180)) {
            println("\n✅ Purchase completed!")
            println("   LocalMode result: ${listener.lastLocalMode?.result}")
            println("   Response code: ${listener.lastLocalMode?.responseCode}")
            println("   Financial result: ${listener.lastFinancialResult?.result}")
            println("\n📄 Receipt:")
            println(listener.getPrintedReceipt())
        } else {
            println("❌ Timeout waiting for operation to complete")
        }
    }
    
    // Step 4: Close terminal
    println("\n🔒 Step 4: Closing terminal...")
    val closeResult = client.closeTerminal()
    println("Close result: ${closeResult.callResult}")
    
    println("\n━".repeat(80))
    println("✅ Test completed successfully!")
    println("━".repeat(80))
    
} catch (e: Exception) {
    println("\n❌ Exception: ${e.message}")
    e.printStackTrace()
} finally {
    client.close()
}
