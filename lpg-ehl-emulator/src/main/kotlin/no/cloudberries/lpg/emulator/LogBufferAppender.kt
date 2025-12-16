package no.cloudberries.lpg.emulator

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

/**
 * Custom Logback appender that captures log events into the LogBuffer.
 * This allows streaming logs to the frontend/diagnostics endpoints.
 */
class LogBufferAppender : AppenderBase<ILoggingEvent>() {
    
    companion object {
        // Static reference to LogBuffer instance (set by Spring)
        var logBuffer: LogBuffer? = null
    }
    
    override fun append(eventObject: ILoggingEvent) {
        logBuffer?.append(
            level = eventObject.level.toString(),
            logger = eventObject.loggerName,
            message = eventObject.formattedMessage
        )
    }
}
