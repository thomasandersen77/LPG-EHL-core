@file:Repository("file:///Users/alejandrosaksida/.m2/repository")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("no.cloudberries.lpg:lpg-ehl-core:0.0.1-SNAPSHOT")
@file:DependsOn("no.cloudberries.lpg:lpg-transport:0.0.1-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:1.7.36")
@file:DependsOn("com.fazecast:jSerialComm:2.9.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

// Configure logging
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info")

val portName = "/dev/ttyS3"
val baudRate = 9600
val address = 1 // Default dispenser address

fun main() {
    println("Starting Pump Unlock Script")
    println("Target: $portName @ $baudRate baud, Address: $address")
    
    try {
        // 1. Configure Serial Port
        val config = SerialPortConfig(
            portName = portName,
            baudRate = baudRate,
            dataBits = 8,
            stopBits = 1,
            parity = 0 // 0 = NO_PARITY in jSerialComm
        )
        
        println("Configuration: $config")
        
        // 2. Initialize Transport via SerialPortManager
        val serialManager = SerialPortManager(config)
        println("Connecting to serial port...")
        if (!serialManager.connect()) {
            println("Failed to connect to $portName. Check permissions and if port exists.")
            return
        }
        println("Connected.")
        
        // 3. Initialize Communicator
        val communicator = EhlCommunicator(serialManager)
        
        runBlocking {
            try {
                // 4. Send UNBLOCK (Unlock) command
                println("Sending UNBLOCK (Unlock) command to address $address...")
                
                // Manually create packet since Builder is missing/hidden
                val packet = EhlPacket(address, EhlCommand.UNBLOCK)
                
                println("sending packet: $packet")
                val response = communicator.sendAndReceive(packet, timeoutMs = 2000)
                
                println("Response received!")
                println("Command: ${response.command}")
                println("Status: ${response.data.joinToString(" ") { "%02X".format(it) }}")
                
                if (response.command == EhlCommand.OK) {
                    println("SUCCESS: Pump unlocked (ACK received)")
                } else {
                    println("WARNING: Received response ${response.command} instead of OK")
                }
                
            } catch (e: Exception) {
                println("Error during communication: ${e.message}")
                e.printStackTrace()
            }
        }
        
        serialManager.disconnect()
        println("Disconnected.")
        
    } catch (e: Exception) {
        println("Fatal error: ${e.message}")
        e.printStackTrace()
    }
}

main()
