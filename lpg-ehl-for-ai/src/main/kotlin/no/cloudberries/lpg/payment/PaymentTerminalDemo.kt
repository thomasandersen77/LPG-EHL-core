package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory

/**
 * Payment Terminal Demo
 * 
 * Demonstrates payment terminal integration with both real and simulated terminals.
 */
object PaymentTerminalDemo {
    
    private val logger = LoggerFactory.getLogger(PaymentTerminalDemo::class.java)
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Payment Terminal Demo ===\n")
        
        // Check if we should use real terminal
        val useRealTerminal = args.contains("--real")
        
        if (useRealTerminal) {
            testRealTerminal()
        } else {
            testSimulatedTerminal()
        }
    }
    
    private fun testRealTerminal() {
        println("--- Testing REAL Payment Terminal ---")
        println("Terminal: 192.168.0.4:8009\n")
        
        val terminal = TcpPaymentTerminal(
            host = "192.168.0.4",
            port = 8009,
            timeout = 30000
        )
        
        // Connect
        println("1. Connecting to terminal...")
        val connected = terminal.connect()
        println("   Connected: $connected\n")
        
        if (!connected) {
            println("ERROR: Could not connect to terminal")
            return
        }
        
        // Test payment
        try {
            println("2. Requesting payment: 50.00 kr")
            val result = terminal.requestPayment(5000)
            
            println("\n--- Payment Result ---")
            println("Status:         ${result.status}")
            println("Amount:         ${result.amount / 100.0} kr")
            println("Transaction ID: ${result.transactionId ?: "N/A"}")
            println("Receipt:        ${result.receiptText ?: "N/A"}")
            println("Error:          ${result.errorMessage ?: "N/A"}")
            
        } finally {
            // Disconnect
            println("\n3. Disconnecting...")
            terminal.disconnect()
            println("   Done")
        }
    }
    
    private fun testSimulatedTerminal() {
        println("--- Testing SIMULATED Payment Terminal ---")
        println("(Use --real flag to test with actual terminal)\n")
        
        val terminal = SimulatedPaymentTerminal(
            autoApprove = true,
            simulatedDelay = 1500
        )
        
        // Connect
        println("1. Connecting to simulated terminal...")
        terminal.connect()
        println("   Connected: ${terminal.isConnected()}\n")
        
        // Test payment 1 - Small amount
        println("2. Requesting payment: 50.00 kr")
        var result = terminal.requestPayment(5000)
        printPaymentResult(result)
        
        // Test payment 2 - Larger amount
        println("\n3. Requesting payment: 250.00 kr")
        result = terminal.requestPayment(25000)
        printPaymentResult(result)
        
        // Disconnect
        println("\n4. Disconnecting...")
        terminal.disconnect()
        println("   Done")
    }
    
    private fun printPaymentResult(result: PaymentResult) {
        println("\n--- Payment Result ---")
        println("Status:         ${result.status}")
        println("Amount:         ${result.amount / 100.0} kr")
        println("Transaction ID: ${result.transactionId ?: "N/A"}")
        
        if (result.receiptText != null) {
            println("\nReceipt:")
            println("---")
            println(result.receiptText)
            println("---")
        }
        
        if (result.errorMessage != null) {
            println("Error: ${result.errorMessage}")
        }
    }
}
