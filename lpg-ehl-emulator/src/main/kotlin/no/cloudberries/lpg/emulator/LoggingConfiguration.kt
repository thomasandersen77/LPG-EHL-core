package no.cloudberries.lpg.emulator

import jakarta.annotation.PostConstruct
import no.cloudberries.lpg.emulator.websocket.LogWebSocketHandler
import org.springframework.context.annotation.Configuration

/**
 * Configuration to wire LogBuffer and WebSocket to the Logback appender.
 * This allows real-time streaming of logs to frontend clients.
 */
@Configuration
class LoggingConfiguration(
    private val logBuffer: LogBuffer,
    private val logWebSocketHandler: LogWebSocketHandler
) {
    
    @PostConstruct
    fun setupLogBufferAppender() {
        // Set static references so Logback appender can access them
        LogBufferAppender.logBuffer = logBuffer
        LogBufferAppender.webSocketHandler = logWebSocketHandler
    }
}
