@file:DependsOn("com.microsoft.azure.sdk.iot:iot-device-client:2.1.4")
@file:DependsOn("org.slf4j:slf4j-simple:1.7.36")

import com.microsoft.azure.sdk.iot.device.*
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException
import com.microsoft.azure.sdk.iot.device.twin.DirectMethodPayload
import com.microsoft.azure.sdk.iot.device.twin.DirectMethodResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

val connString = if (args.size >= 2) args[1] else null
val mode = if (args.size >= 1) args[0] else "help"

if (connString == null && mode != "help") {
    println("Usage: kotlin iot_demo.main.kts <mode> <connection_string>")
    println("Modes: send, listen")
    System.exit(1)
}

fun runSendTest(connString: String) {
    println("Starting Test 1: Connect and Send Data")
    val client = DeviceClient(connString, IotHubClientProtocol.MQTT)
    
    try {
        client.open(false)
        val msgStr = "{\"test\": \"data\", \"timestamp\": ${System.currentTimeMillis()}}"
        val msg = Message(msgStr)
        msg.contentType = "application/json"
        msg.contentEncoding = "utf-8"
        
        val latch = CountDownLatch(1)
        
        println("Sending message: $msgStr")
        // Callback signature for sendEventAsync in 2.x
        client.sendEventAsync(msg, { _, _, _ ->
            println("Message callback triggered.")
            latch.countDown()
        }, null)
        
        if (!latch.await(15, TimeUnit.SECONDS)) {
            println("Timed out waiting for message confirmation.")
        }
        
        client.close()
        println("Test 1 completed.")
    } catch (e: Exception) {
        println("Error in Test 1: ${e.message}")
    }
}

fun runListenTest(connString: String) {
    println("Starting Test 2: Listen for Cloud-to-Device messages and Direct Methods")
    val client = DeviceClient(connString, IotHubClientProtocol.MQTT)
    
    try {
        client.open(false)
        
        val latch = CountDownLatch(1)
        
        // 1. Listen for Cloud-to-Device (C2D) Messages
        // Expected: Function2<Message!, Any!, IotHubMessageResult!>
        client.setMessageCallback({ message, _ ->
            val body = String(message!!.bytes, Message.DEFAULT_IOTHUB_MESSAGE_CHARSET)
            println("\n[C2D] Received message: $body")
            
            // Respond back by sending a telemetry message
            val ackMsg = Message("{\"status\": \"received\", \"original_msg\": \"$body\"}")
            client.sendEventAsync(ackMsg, { _, _, _ ->
                println("[Telemetry] Sent acknowledgement for C2D.")
            }, null)
            
            IotHubMessageResult.COMPLETE
        }, null)
        
        // 2. Handle Direct Methods
        client.subscribeToMethods({ methodName, payload, _ ->
            println("\n[Direct Method] Called: $methodName")
            
            val responsePayload = "{\"result\": \"success\", \"details\": \"Method handled\"}"
            // Return status code 200 and the payload
            DirectMethodResponse(200, responsePayload)
        }, null)
        
        println("\nWaiting for data... (C2D messages or Direct Methods)")
        println("Press Ctrl+C to stop")
        
        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nShutting down...")
            client.close()
            latch.countDown()
        })
        
        latch.await()
    } catch (e: Exception) {
        println("Error in Test 2: ${e.message}")
    }
}

fun main() {
    when (mode) {
        "send" -> runSendTest(connString!!)
        "listen" -> runListenTest(connString!!)
        else -> {
            println("Usage: kotlin iot_demo.main.kts <mode> <connection_string>")
            println("Modes: send, listen")
        }
    }
}

main()
