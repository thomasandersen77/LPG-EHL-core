package no.cloudberries.lpg.api.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import no.cloudberries.lpg.api.websocket.LogWebSocketHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * Logback appender that streams log events to WebSocket clients.
 * 
 * This enables real-time log viewing in the Control Panel.
 * The appender automatically determines the log channel based on logger name.
 */
class WebSocketLogAppender : AppenderBase<ILoggingEvent>() {
    
    companion object {
        // Static reference to the WebSocket handler (set by Spring)
        @Volatile
        private var webSocketHandler: LogWebSocketHandler? = null
        
        fun setHandler(handler: LogWebSocketHandler) {
            webSocketHandler = handler
        }
    }
    
    override fun append(event: ILoggingEvent) {
        val handler = webSocketHandler ?: return
        
        val loggerName = event.loggerName
        val channel = determineChannel(loggerName)
        
        handler.broadcast(
            channel = channel,
            level = event.level.toString(),
            loggerName = loggerName,
            message = event.formattedMessage
        )
    }
    
    private fun determineChannel(loggerName: String): LogWebSocketHandler.LogChannel {
        return when {
            // Protocol logs (HEX packets) - matches both protocol and communication packages
            loggerName.contains("protocol", ignoreCase = true) -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("communication", ignoreCase = true) -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("Codec") -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("Packet") -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("EhlCommand") -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("Communicator") -> LogWebSocketHandler.LogChannel.PROTOCOL
            loggerName.contains("SerialPort") -> LogWebSocketHandler.LogChannel.PROTOCOL
            
            // Emulator logs
            loggerName.contains("Emulator") -> LogWebSocketHandler.LogChannel.EMULATOR
            loggerName.contains("Dispenser") -> LogWebSocketHandler.LogChannel.EMULATOR
            loggerName.contains("PumpState") -> LogWebSocketHandler.LogChannel.EMULATOR
            loggerName.contains("Pump") -> LogWebSocketHandler.LogChannel.EMULATOR
            
            // API logs (controllers, services)
            loggerName.contains("Controller") -> LogWebSocketHandler.LogChannel.API
            loggerName.contains("Service") -> LogWebSocketHandler.LogChannel.API
            
            // Default to API
            else -> LogWebSocketHandler.LogChannel.API
        }
    }
}

/**
 * Spring component that injects the WebSocket handler into the log appender.
 */
@Component
class WebSocketLogAppenderInitializer(
    private val logWebSocketHandler: LogWebSocketHandler
) : ApplicationContextAware {
    
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        WebSocketLogAppender.setHandler(logWebSocketHandler)
    }
}
