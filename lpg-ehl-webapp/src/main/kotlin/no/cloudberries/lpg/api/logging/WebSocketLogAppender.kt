package no.cloudberries.lpg.api.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import no.cloudberries.lpg.service.event.EventPublisher
import no.cloudberries.lpg.service.event.LogChannel
import no.cloudberries.lpg.service.event.LogEvent
import no.cloudberries.lpg.service.event.LogLevel
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Logback appender that streams log events to WebSocket clients.
 * 
 * This enables real-time log viewing in the Control Panel.
 * The appender automatically determines the log channel based on logger name.
 */
class WebSocketLogAppender : AppenderBase<ILoggingEvent>() {
    
    companion object {
        // Static reference to the event publisher (set by Spring)
        @Volatile
        private var eventPublisher: EventPublisher? = null
        
        fun setPublisher(publisher: EventPublisher) {
            eventPublisher = publisher
        }
    }
    
    override fun append(event: ILoggingEvent) {
        val publisher = eventPublisher ?: return
        
        val loggerName = event.loggerName
        val channel = determineChannel(loggerName)
        val level = mapLogLevel(event.level.toString())
        
        val logEvent = LogEvent(
            channel = channel,
            level = level,
            logger = loggerName,
            message = event.formattedMessage,
            timestamp = Instant.now()
        )
        
        publisher.publishLogEvent(logEvent)
    }
    
    private fun determineChannel(loggerName: String): LogChannel {
        return when {
            // Protocol logs (HEX packets) - matches both protocol and communication packages
            loggerName.contains("protocol", ignoreCase = true) -> LogChannel.PROTOCOL
            loggerName.contains("communication", ignoreCase = true) -> LogChannel.PROTOCOL
            loggerName.contains("Codec") -> LogChannel.PROTOCOL
            loggerName.contains("Packet") -> LogChannel.PROTOCOL
            loggerName.contains("EhlCommand") -> LogChannel.PROTOCOL
            loggerName.contains("Communicator") -> LogChannel.PROTOCOL
            loggerName.contains("SerialPort") -> LogChannel.PROTOCOL
            
            // Emulator logs - ONLY from emulator package (LAB mode only)
            loggerName.startsWith("no.cloudberries.lpg.emulator") -> LogChannel.EMULATOR
            
            // Service logs (PumpStateService, PumpAuthorizationService, etc.)
            loggerName.startsWith("no.cloudberries.lpg.service") -> LogChannel.SERVICE
            
            // API logs (controllers)
            loggerName.contains("Controller") -> LogChannel.API
            
            // Default to API
            else -> LogChannel.API
        }
    }
    
    private fun mapLogLevel(levelStr: String): LogLevel {
        return when (levelStr.uppercase()) {
            "TRACE" -> LogLevel.TRACE
            "DEBUG" -> LogLevel.DEBUG
            "INFO" -> LogLevel.INFO
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
    }
}

/**
 * Spring component that injects the EventPublisher into the log appender.
 */
@Component
class WebSocketLogAppenderInitializer(
    private val eventPublisher: EventPublisher
) : ApplicationContextAware {
    
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        WebSocketLogAppender.setPublisher(eventPublisher)
    }
}
