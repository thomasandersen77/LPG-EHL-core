package no.cloudberries.lpg

import no.cloudberries.lpg.payment.CloudTerminalClient
import no.cloudberries.lpg.payment.NetsBaxProtocol
import no.cloudberries.lpg.protocol.*
import no.cloudberries.lpg.transaction.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    // Check if Cloud Connect demo mode is requested
    if (args.contains("--cloud-connect")) {
        cloudConnectDemo()
        return
    }
    val logger = LoggerFactory.getLogger("Main")
    
    println("=== LPG/EHL Core - Protocol Implementation ===")
    println()
    
    // Demonstrate EHL packet encoding/decoding
    println("1. EHL Protocol Packet Demo:")
    println("   Creating STATE query for dispenser address 1...")
    
    val stateQuery = EhlPacketBuilder.createStateQuery(1)
    val encoded = EhlCodec.encode(stateQuery)
    
    println("   Encoded packet: ${encoded.joinToString(" ") { "%02X".format(it) }}")
    println("   Packet details: $stateQuery")
    println()
    
    // Demonstrate packet decoding
    println("2. Decoding EHL Packet:")
    when (val result = EhlCodec.decode(encoded)) {
        is EhlPacketParseResult.Success -> {
            println("   ✓ Successfully decoded: ${result.packet}")
        }
        is EhlPacketParseResult.InvalidFormat -> {
            println("   ✗ Invalid format: ${result.reason}")
        }
        is EhlPacketParseResult.ChecksumError -> {
            println("   ✗ Checksum error")
        }
        is EhlPacketParseResult.Incomplete -> {
            println("   ⏳ Incomplete packet")
        }
    }
    println()
    
    // Demonstrate transaction management
    println("3. Transaction State Machine Demo:")
    val manager = TransactionManager()
    val transaction = manager.startTransaction(dispenserAddress = 1)
    
    println("   Transaction created: ${transaction.id}")
    println("   Initial state: ${transaction.state.description}")
    
    // Simulate transaction lifecycle
    transaction.transitionTo(TransactionState.ACTIVE)
    println("   → Transitioned to: ${transaction.state.description}")
    
    // Simulate delivery
    transaction.deliveredVolume = 45.5f
    transaction.deliveredAmount = 72950 // 729.50 kr
    transaction.unitPrice = 16.04f
    transaction.paymentType = PaymentType.BANK_CARD
    
    transaction.transitionTo(TransactionState.FINISHED)
    println("   → Transitioned to: ${transaction.state.description}")
    println("   Delivered: ${transaction.deliveredVolume} L @ ${transaction.unitPrice} kr/L")
    println("   Total: ${transaction.deliveredAmount / 100.0} kr (${transaction.paymentType.description})")
    println()
    
    // Show available EHL commands
    println("4. Available EHL Commands:")
    EhlCommand.entries.filter { it != EhlCommand.UNKNOWN }.take(8).forEach { cmd ->
        println("   - ${cmd.name.padEnd(12)} (code ${cmd.code.toString().padStart(3)}): ${cmd.description}")
    }
    println()
    
    println("✓ LPG/EHL core initialization complete!")
    println("  Ready for RS-485 communication and transaction processing.")
}

/**
 * Demonstrate Nets Cloud Connect SSL/TLS communication
 */
fun cloudConnectDemo() {
    println("=== Nets Cloud Connect Demo ===")
    println()
    println("Connecting to Nets Cloud Connect at 3.33.230.243:6001...")
    println()
    
    try {
        CloudTerminalClient().use { client ->
            client.connect()
            println("✓ Connected successfully!")
            println()
            
            // Create a test purchase command (10.00 NOK)
            println("Creating purchase command for 10.00 NOK...")
            val command = NetsBaxProtocol.createPurchaseCommand(
                amountCents = 1000,
                operatorId = "1"
            )
            println("Command: ${command.joinToString(" ") { "%02X".format(it) }}")
            println()
            
            println("Sending command to terminal...")
            println("(Terminal should now display payment request)")
            println()
            
            val response = client.sendCommand(command)
            println("Response received: ${response.toHexString()}")
            
            val result = response.parse()
            println("Parsed result: $result")
            println()
            
            when (result) {
                is no.cloudberries.lpg.payment.BaxResponse.Success -> {
                    println("✓ Payment approved!")
                    println("  Transaction ID: ${result.transactionId}")
                    println("  Auth Code: ${result.authCode}")
                }
                is no.cloudberries.lpg.payment.BaxResponse.Error -> {
                    println("✗ Payment failed: ${result.message}")
                }
                else -> {
                    println("⚠ Unexpected response: $result")
                }
            }
        }
    } catch (e: Exception) {
        println("✗ Error: ${e.message}")
        println()
        println("Note: This demo requires:")
        println("  1. Network access to 3.33.230.243:6001")
        println("  2. Terminal configured with ECR IP = 3.33.230.243")
        println("  3. Terminal configured with ECR Port = 6001")
        println("  4. Terminal powered on and connected")
    }
    
    println()
    println("Demo complete.")
}
