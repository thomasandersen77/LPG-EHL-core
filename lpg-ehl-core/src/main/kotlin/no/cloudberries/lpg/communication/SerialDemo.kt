package no.cloudberries.lpg.communication

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacketBuilder
import org.slf4j.LoggerFactory

/**
 * Demo application for testing serial communication with LPG dispensers.
 * 
 * Usage:
 *   mvn -q exec:java -Dexec.mainClass="no.cloudberries.lpg.communication.SerialDemoKt"
 * 
 * Or with specific port:
 *   mvn -q exec:java -Dexec.mainClass="no.cloudberries.lpg.communication.SerialDemoKt" -Dexec.args="/dev/ttyUSB0"
 */

private val logger = LoggerFactory.getLogger("SerialDemo")

fun main(args: Array<String>) {
    println("\n=== LPG-EHL Serial Communication Demo ===\n")

    // List available ports
    println("Available serial ports:")
    val ports = SerialPortManager.listAvailablePorts()
    if (ports.isEmpty()) {
        println("  No serial ports found!")
        println("\nDemo cannot proceed without a serial port.")
        println("On Linux, make sure you have permissions: sudo chmod 666 /dev/ttyUSB0")
        return
    }
    
    ports.forEachIndexed { index, port ->
        println("  ${index + 1}. $port")
    }
    println()

    // Select port
    val portName = when {
        args.isNotEmpty() -> args[0]
        ports.size == 1 -> ports[0]
        else -> {
            println("Multiple ports available. Using first port: ${ports[0]}")
            println("To specify a port, run: mvn -q exec:java -Dexec.mainClass=\"no.cloudberries.lpg.communication.SerialDemoKt\" -Dexec.args=\"<port>\"")
            ports[0]
        }
    }

    println("Using serial port: $portName")
    println()

    // Create configuration
    val config = SerialPortConfig.forEhlProtocol(portName)
    println("Configuration: $config")
    println()

    // Create serial port manager
    val serialManager = SerialPortManager(config)

    try {
        // Connect to serial port
        println("Connecting to serial port...")
        serialManager.connect()
        println("✓ Connected successfully!\n")

        // Create communicator
        val communicator = EhlCommunicator(serialManager)
        
        // Demo: Query dispenser state
        println("--- Demo 1: Query Dispenser State ---")
        println("Sending STATE query to dispenser address 1...")
        
        val statePacket = EhlPacketBuilder.createStateQuery(address = 1)
        println("Packet to send: $statePacket")
        
        runBlocking {
            try {
                val response = communicator.sendAndReceive(statePacket, timeoutMs = 3000)
                println("✓ Received response: $response")
                println("  Response command: ${response.command}")
                println("  Response data: ${response.data.joinToString(" ") { "%02X".format(it) }}")
            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
                println("  This is expected if no dispenser is connected or in loopback mode")
            }
        }
        
        println()

        // Demo: Using DispenserConnection
        println("--- Demo 2: DispenserConnection ---")
        val connection = DispenserConnection(address = 1, communicator = communicator)
        
        println("Created connection to dispenser 1")
        println("Connection state: ${connection.state}")
        
        runBlocking {
            try {
                println("Querying state via DispenserConnection...")
                val response = connection.queryState(timeoutMs = 3000)
                println("✓ Response: $response")
                println("  Last communication: ${connection.lastCommunication}")
                println("  Is responsive: ${connection.isResponsive()}")
            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }
        
        connection.close()
        println()

        // Demo: List EHL commands
        println("--- Demo 3: Available EHL Commands ---")
        println("Commands that can be sent to dispensers:")
        EhlCommand.entries.forEach { cmd ->
            println("  ${cmd.name.padEnd(15)} (code ${cmd.code.toString().padStart(3)}): ${cmd.description}")
        }
        println()

        println("✓ Demo completed successfully!")

    } catch (e: Exception) {
        logger.error("Demo failed: ${e.message}", e)
        println("✗ Demo failed: ${e.message}")
    } finally {
        println("\nDisconnecting...")
        serialManager.disconnect()
        println("✓ Disconnected")
    }

    println("\n=== Demo Complete ===\n")
}
