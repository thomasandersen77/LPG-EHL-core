package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory event store for SSE streaming and polling.
 *
 * Events are stored in a circular buffer with cursor-based access.
 */
@Service
class EventStore(
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(EventStore::class.java)

    // Monotonic cursor for event ordering
    private val cursorGenerator = AtomicLong(0)

    // Event buffer (circular, limited size)
    private val events = ConcurrentLinkedDeque<EventEnvelope>()

    /**
     * Publish an event.
     */
    fun publishEvent(
        eventType: String,
        operationId: String? = null,
        payload: Map<String, Any> = emptyMap()
    ) {
        val cursor = cursorGenerator.incrementAndGet()
        val event = EventEnvelope(
            Cursor = cursor,
            EventId = UUID.randomUUID().toString(),
            OperationId = operationId,
            Timestamp = Instant.now().toString(),
            EventType = eventType,
            Payload = payload
        )

        events.addLast(event)

        // Limit buffer size (remove oldest if exceeds limit)
        while (events.size > config.eventBufferSize) {
            events.removeFirst()
        }

        log.debug("Event published: type={}, cursor={}, operationId={}", eventType, cursor, operationId)
    }

    /**
     * Get events since a given cursor.
     *
     * @param sinceCursor Start from cursor (exclusive). Use 0 to get all buffered events.
     * @return List of events with Cursor > sinceCursor
     */
    fun getEventsSince(sinceCursor: Long): List<EventEnvelope> {
        return events.filter { it.Cursor > sinceCursor }
    }

    /**
     * Get events since a timestamp.
     *
     * @param sinceTimestamp ISO-8601 timestamp
     * @return List of events with Timestamp >= sinceTimestamp
     */
    fun getEventsSinceTimestamp(sinceTimestamp: String): List<EventEnvelope> {
        val targetInstant = try {
            Instant.parse(sinceTimestamp)
        } catch (ex: Exception) {
            log.warn("Invalid timestamp: {}, returning all events", sinceTimestamp)
            return events.toList()
        }

        return events.filter {
            try {
                val eventInstant = Instant.parse(it.Timestamp)
                !eventInstant.isBefore(targetInstant)
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Get current cursor (latest event cursor).
     */
    fun getCurrentCursor(): Long {
        return cursorGenerator.get()
    }

    /**
     * Clear all events (for testing).
     */
    fun clear() {
        events.clear()
        log.debug("Event store cleared")
    }
}
