package no.cloudberries.lpg.iothub

import com.microsoft.azure.sdk.iot.device.*
import com.microsoft.azure.sdk.iot.device.twin.DirectMethodResponse
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AzureIotHubClient(
    private val config: IotHubConfig
) : IotHubClient {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var client: DeviceClient? = null
    
    // Callbacks
    private var messageCallback: ((String) -> Unit)? = null
    private var directMethodCallback: ((String, String) -> String)? = null

    @PostConstruct
    fun init() {
        if (config.enabled && !config.connectionString.isBlank()) {
           connect()
        } else {
             logger.warn("IoT Hub client is disabled or connection string is missing. Skipping connection.")
        }
    }

    override fun connect() {
        try {
            if (client == null) {
                logger.info("Connecting to IoT Hub...")
                client = DeviceClient(config.connectionString, IotHubClientProtocol.MQTT)
                client?.open(false) // Assuming retry logic is handled or default
                
                setupCallbacks()
                
                logger.info("Connected to IoT Hub")
            }
        } catch (e: Exception) {
            logger.error("Failed to connect to IoT Hub", e)
        }
    }
    
    private fun setupCallbacks() {
        // C2D Message Callback
        client?.setMessageCallback({ message, _ ->
            try {
                val body = String(message.bytes, Message.DEFAULT_IOTHUB_MESSAGE_CHARSET)
                logger.debug("Received C2D message: $body")
                messageCallback?.invoke(body)
                IotHubMessageResult.COMPLETE
            } catch (e: Exception) {
                logger.error("Error processing C2D message", e)
                IotHubMessageResult.ABANDON
            }
        }, null)

        // Direct Method Callback
        client?.subscribeToMethods({ methodName, payload, _ ->
            try {
                logger.debug("Direct method called: $methodName")
                val payloadStr = payload?.toString() ?: ""
                
                val response = if (directMethodCallback != null) {
                    directMethodCallback!!.invoke(methodName, payloadStr)
                } else {
                    logger.warn("No direct method callback registered for: $methodName")
                    "{\"error\": \"Method not implemented\"}"
                }
                
                // For now, assume 200 OK. In a real scenario, the callback might return status code too.
                DirectMethodResponse(200, response)
            } catch (e: Exception) {
                logger.error("Error processing direct method: $methodName", e)
                DirectMethodResponse(500, "{\"error\": \"${e.message}\"}")
            }
        }, null)
    }

    override fun disconnect() {
        try {
            if (client != null) {
                client?.close() // Use closeNow() if needed or just close()
                client = null
                logger.info("Disconnected from IoT Hub")
            }
        } catch (e: Exception) {
             logger.error("Error disconnecting from IoT Hub", e)
        }
    }

    @PreDestroy
    fun cleanup() {
        disconnect()
    }

    override fun sendTelemetry(message: String) {
        if (client == null) {
             logger.warn("Cannot send telemetry: Client not connected")
             return
        }

        try {
            val msg = Message(message)
            msg.contentType = "application/json"
            msg.contentEncoding = "utf-8"

            client?.sendEventAsync(msg, { responseStatus, _, _ ->
                logger.debug("Telemetry sent. Status: $responseStatus")
            }, null)
        } catch (e: Exception) {
            logger.error("Error sending telemetry", e)
        }
    }

    override fun setReceiveMessageCallback(callback: (String) -> Unit) {
        this.messageCallback = callback
    }

    override fun setDirectMethodCallback(callback: (methodName: String, payload: String) -> String) {
        this.directMethodCallback = callback
    }
}
