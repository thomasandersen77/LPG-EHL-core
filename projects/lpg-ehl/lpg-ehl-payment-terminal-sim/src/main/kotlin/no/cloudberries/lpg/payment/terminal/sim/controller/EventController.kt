package no.cloudberries.lpg.payment.terminal.sim.controller

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import no.cloudberries.lpg.payment.terminal.sim.service.EventStore
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Event streaming controller (SSE and polling).
 */
@RestController
@RequestMapping("/v1/events")
class EventController(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(EventController::class.java)

    // Executor for SSE background tasks
    private val executor = Executors.newCachedThreadPool()

    /**
     * GET /v1/events
     *
     * Poll for events since a given cursor or timestamp.
     *
     * @param since Cursor (numeric) or ISO-8601 timestamp. Use "0" for all buffered events.
     */
    @GetMapping
    fun pollEvents(
        @RequestParam(defaultValue = "0") since: String
    ): ResponseEntity<List<EventEnvelope>> {
        log.debug("Polling events since: {}", since)

        val events = if (since.toLongOrNull() != null) {
            // Numeric cursor
            eventStore.getEventsSince(since.toLong())
        } else {
            // Timestamp
            eventStore.getEventsSinceTimestamp(since)
        }

        log.debug("Returning {} events", events.size)
        return ResponseEntity.ok(events)
    }

    /**
     * GET /v1/events/stream
     *
     * Subscribe to event stream via Server-Sent Events (SSE).
     *
     * @param since Start from cursor (0 for beginning) or ISO-8601 timestamp
     */
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(
        @RequestParam(defaultValue = "0") since: String
    ): SseEmitter {
        log.info("SSE connection established, since={}", since)

        val emitter = SseEmitter(Long.MAX_VALUE) // No timeout

        // Send initial events
        executor.execute {
            try {
                val initialEvents = if (since.toLongOrNull() != null) {
                    eventStore.getEventsSince(since.toLong())
                } else {
                    eventStore.getEventsSinceTimestamp(since)
                }

                initialEvents.forEach { event ->
                    sendEvent(emitter, event)
                }

                // Keep-alive: send heartbeat every 30 seconds
                var lastCursor = eventStore.getCurrentCursor()
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(5000) // Poll every 5 seconds

                    // Send new events
                    val newEvents = eventStore.getEventsSince(lastCursor)
                    newEvents.forEach { event ->
                        sendEvent(emitter, event)
                        lastCursor = event.Cursor
                    }

                    // Send heartbeat comment
                    if (newEvents.isEmpty()) {
                        emitter.send(SseEmitter.event()
                            .comment("heartbeat")
                            .build())
                    }
                }
            } catch (ex: InterruptedException) {
                log.debug("SSE thread interrupted")
                emitter.complete()
            } catch (ex: Exception) {
                log.error("SSE error", ex)
                emitter.completeWithError(ex)
            }
        }

        emitter.onCompletion {
            log.info("SSE connection closed")
        }

        emitter.onTimeout {
            log.warn("SSE connection timeout")
            emitter.complete()
        }

        emitter.onError { ex ->
            log.error("SSE error", ex)
        }

        return emitter
    }

    /**
     * Send an event via SSE emitter.
     */
    private fun sendEvent(emitter: SseEmitter, event: EventEnvelope) {
        try {
            val eventData = objectMapper.writeValueAsString(event)
            emitter.send(
                SseEmitter.event()
                    .id(event.Cursor.toString())
                    .name(event.EventType)
                    .data(eventData)
                    .build()
            )
            log.debug("SSE event sent: type={}, cursor={}", event.EventType, event.Cursor)
        } catch (ex: Exception) {
            log.error("Failed to send SSE event", ex)
            throw ex
        }
    }
}
