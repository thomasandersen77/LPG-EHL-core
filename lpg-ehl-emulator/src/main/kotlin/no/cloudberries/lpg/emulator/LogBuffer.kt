package no.cloudberries.lpg.emulator

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-memory circular buffer for collecting recent log entries.
 * Used to stream logs to frontend/diagnostics without file system access.
 */
@Service
class LogBuffer {
    private val buffer = ConcurrentLinkedDeque<LogEntry>()
    private val maxSize = 1000 // Keep last 1000 log entries
    
    data class LogEntry(
        val timestamp: Instant = Instant.now(),
        val level: String,
        val logger: String,
        val message: String,
        val thread: String = Thread.currentThread().name
    )
    
    /**
     * Add a log entry to the buffer.
     * Automatically removes oldest entries when buffer exceeds maxSize.
     */
    fun append(level: String, logger: String, message: String) {
        buffer.addLast(LogEntry(
            level = level,
            logger = logger,
            message = message
        ))
        
        // Keep buffer size limited
        while (buffer.size > maxSize) {
            buffer.pollFirst()
        }
    }
    
    /**
     * Get the most recent N log entries.
     * @param limit Maximum number of entries to return (default: all)
     * @return List of log entries, most recent first
     */
    fun getRecentLogs(limit: Int = buffer.size): List<LogEntry> {
        val actualLimit = limit.coerceAtMost(buffer.size)
        return buffer.toList().takeLast(actualLimit).reversed()
    }
    
    /**
     * Clear all log entries from the buffer.
     */
    fun clear() {
        buffer.clear()
    }
    
    /**
     * Get current buffer size.
     */
    fun size(): Int = buffer.size
}
