package no.cloudberries.lpg.payment.terminal.sim.controller

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventPublisher
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventStore
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
    private val eventStore: TerminalEventStore,
    private val eventPublisher: TerminalEventPublisher,
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(EventController::class.java)

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

        val events = eventStore.resolveSince(since)

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
        if (!eventPublisher.registerSse(emitter, since)) {
            emitter.complete()
            return emitter
        }

        executor.execute {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    TimeUnit.MILLISECONDS.sleep(config.sseHeartbeatMs)
                    emitter.send(
                        SseEmitter.event()
                            .comment("heartbeat")
                            .build()
                    )
                }
            } catch (ex: InterruptedException) {
                log.debug("SSE thread interrupted")
                emitter.complete()
            } catch (ex: Exception) {
                log.error("SSE error", ex)
                eventPublisher.unregisterSse(emitter)
                emitter.completeWithError(ex)
            }
        }

        emitter.onCompletion {
            log.info("SSE connection closed")
            eventPublisher.unregisterSse(emitter)
        }

        emitter.onTimeout {
            log.warn("SSE connection timeout")
            eventPublisher.unregisterSse(emitter)
            emitter.complete()
        }

        emitter.onError { ex ->
            log.error("SSE error", ex)
            eventPublisher.unregisterSse(emitter)
        }

        return emitter
    }
}
