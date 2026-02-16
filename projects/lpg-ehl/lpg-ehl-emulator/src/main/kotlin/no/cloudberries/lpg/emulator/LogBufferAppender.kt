package no.cloudberries.lpg.emulator

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import no.cloudberries.lpg.emulator.websocket.LogWebSocketHandler

/**
 * Custom Logback appender that captures log events into the LogBuffer
 * and broadcasts to WebSocket clients.
 * 
 * This allows streaming logs to the frontend in real-time.
 */
class LogBufferAppender : AppenderBase<ILoggingEvent>() {
    
    companion object {
        // Static reference to LogBuffer instance (set by Spring)
        var logBuffer: LogBuffer? = null
        
        // Static reference to WebSocket handler (set by Spring)
        var webSocketHandler: LogWebSocketHandler? = null
    }
    
    override fun append(eventObject: ILoggingEvent) {
        val level = eventObject.level.toString()
        val loggerName = eventObject.loggerName
        val message = eventObject.formattedMessage ?: return
        
        // Silently skip if beans not initialized yet (during Spring startup)
        // This is expected during early application bootstrap
        
        // Append to in-memory buffer (for polling API)
        logBuffer?.append(
            level = level,
            logger = loggerName,
            message = message
        )
        
        // Broadcast to WebSocket clients (real-time)
        webSocketHandler?.let { handler ->
            val channel = LogWebSocketHandler.determineChannel(loggerName)
            handler.broadcast(channel, level, loggerName, message)
        }
    }
}
