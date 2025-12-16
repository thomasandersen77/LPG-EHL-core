package no.cloudberries.lpg.emulator

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

/**
 * Configuration to wire the LogBuffer instance to the Logback appender.
 * This allows the custom appender to capture logs into the in-memory buffer.
 */
@Configuration
class LoggingConfiguration(
    private val logBuffer: LogBuffer
) {
    
    @PostConstruct
    fun setupLogBufferAppender() {
        // Set static reference so Logback appender can access it
        LogBufferAppender.logBuffer = logBuffer
    }
}
