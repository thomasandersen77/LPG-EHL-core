package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Single source of truth for terminal events.
 *
 * Stores all emitted events in-memory with monotonic cursor ordering and allows
 * consumers to retrieve events by cursor or timestamp.
 */
@Service
class TerminalEventStore(
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(TerminalEventStore::class.java)

    private val cursorGenerator = AtomicLong(0)
    private val events = ConcurrentLinkedDeque<EventEnvelope>()
    private val listeners = CopyOnWriteArrayList<EventStoreListener>()

    fun append(
        eventType: String,
        operationId: String? = null,
        payload: Map<String, Any> = emptyMap()
    ): EventEnvelope {
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
        while (events.size > config.eventBufferSize) {
            events.removeFirst()
        }

        listeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (ex: Exception) {
                log.warn("Event listener error: {}", ex.message)
            }
        }

        log.debug("Event appended: type={}, cursor={}, operationId={}", eventType, cursor, operationId)
        return event
    }

    fun addListener(listener: EventStoreListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EventStoreListener) {
        listeners.remove(listener)
    }

    fun getEventsSince(sinceCursor: Long): List<EventEnvelope> {
        return events.filter { it.Cursor > sinceCursor }
    }

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

    fun resolveSince(since: String): List<EventEnvelope> {
        return since.toLongOrNull()?.let { getEventsSince(it) }
            ?: getEventsSinceTimestamp(since)
    }

    fun getCurrentCursor(): Long = cursorGenerator.get()

    fun clear() {
        events.clear()
        log.debug("Event store cleared")
    }
}
