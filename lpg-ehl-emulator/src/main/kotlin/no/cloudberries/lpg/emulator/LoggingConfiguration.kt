package no.cloudberries.lpg.emulator

import jakarta.annotation.PostConstruct
import no.cloudberries.lpg.emulator.websocket.LogWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

/**
 * Configuration to wire LogBuffer and WebSocket to the Logback appender.
 * This allows real-time streaming of logs to frontend clients.
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class LoggingConfiguration(
    private val logBuffer: LogBuffer,
    private val logWebSocketHandler: LogWebSocketHandler
) {
    private val logger = LoggerFactory.getLogger(LoggingConfiguration::class.java)
    
    @PostConstruct
    fun setupLogBufferAppender() {
        // Set static references so Logback appender can access them
        LogBufferAppender.logBuffer = logBuffer
        LogBufferAppender.webSocketHandler = logWebSocketHandler
        
        println("✅ LoggingConfiguration: LogBuffer and WebSocketHandler wired to Logback appender")
        println("   LogBuffer: $logBuffer")
        println("   WebSocketHandler: $logWebSocketHandler")
        
        // Send test messages to verify WebSocket broadcasting
        logger.info("📡 WebSocket logging initialized - test message for API channel")
        logger.info("📡 Emulator logging initialized - test message for Emulator channel")
        logger.debug("📡 Protocol logging initialized - test message for Protocol channel")
    }
}
