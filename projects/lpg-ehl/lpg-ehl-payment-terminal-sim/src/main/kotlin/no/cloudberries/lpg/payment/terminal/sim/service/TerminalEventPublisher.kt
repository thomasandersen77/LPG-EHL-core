package no.cloudberries.lpg.payment.terminal.sim.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ThreadLocalRandom

/**
 * Unified event publisher for REST polling, SSE and WebSocket subscribers.
 */
@Component
class TerminalEventPublisher(
    private val eventStore: TerminalEventStore,
    private val streamRegistry: TerminalEventStreamRegistry,
    private val objectMapper: ObjectMapper,
    private val sseEventSender: SseEventSender,
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(TerminalEventPublisher::class.java)

    fun publish(
        eventType: String,
        operationId: String? = null,
        payload: Map<String, Any> = emptyMap()
    ): EventEnvelope {
        maybeDirtyLatency()

        val event = eventStore.append(eventType, operationId, payload)
        broadcastEvent(event)

        maybeBroadcastDuplicate(event)
        maybeOutOfOrderHint(event)

        log.debug("Broadcast event cursor={} type={}", event.Cursor, event.EventType)
        return event
    }

    fun sendBacklog(since: String, sender: (EventEnvelope) -> Boolean): Boolean {
        val backlog = eventStore.resolveSince(since)
        backlog.forEach { event ->
            if (!sender(event)) {
                return false
            }
        }
        return true
    }

    fun registerSse(emitter: SseEmitter, since: String): Boolean {
        if (!sendBacklog(since) { event -> sseEventSender.send(emitter, event) }) {
            return false
        }
        streamRegistry.addSse(emitter)
        return true
    }

    fun unregisterSse(emitter: SseEmitter) {
        streamRegistry.removeSse(emitter)
    }

    fun registerWs(session: WebSocketSessionSender, since: String): Boolean {
        if (!sendBacklog(since) { event -> session.send(event) }) {
            return false
        }
        streamRegistry.addWs(session)
        return true
    }

    fun unregisterWs(session: WebSocketSessionSender) {
        streamRegistry.removeWs(session)
    }

    fun formatJson(event: EventEnvelope): String = objectMapper.writeValueAsString(event)

    private fun broadcastEvent(event: EventEnvelope) {
        log.info("WS broadcast event cursor={}", event.Cursor)

        streamRegistry.sseSubscribers().forEach { emitter ->
            if (!sseEventSender.send(emitter, event)) {
                streamRegistry.removeSse(emitter)
            }
        }

        streamRegistry.wsSubscribers().forEach { session ->
            if (!session.send(event)) {
                streamRegistry.removeWs(session)
            }
        }
    }

    private fun maybeDirtyLatency() {
        if (!config.dirty.latencyEnabled()) {
            return
        }
        Thread.sleep(config.dirty.latencyMs)
    }

    private fun maybeBroadcastDuplicate(event: EventEnvelope) {
        if (!config.dirty.duplicatesEnabled()) {
            return
        }
        val random = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        if (random <= config.dirty.duplicateEventProbability) {
            log.info("Dirty mode duplicate broadcast cursor={}", event.Cursor)
            broadcastEvent(event)
        }
    }

    private fun maybeOutOfOrderHint(event: EventEnvelope) {
        if (!config.dirty.outOfOrderEnabled()) {
            return
        }
        log.info("Dirty mode out-of-order enabled (placeholder), cursor={}", event.Cursor)
    }
}
