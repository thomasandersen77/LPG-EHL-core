package no.cloudberries.lpg.payment.terminal.sim.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Registry for active event stream subscribers.
 */
@Component
class TerminalEventStreamRegistry {
    private val log = LoggerFactory.getLogger(TerminalEventStreamRegistry::class.java)

    private val sseEmitters = CopyOnWriteArrayList<SseEmitter>()
    private val wsSessions = CopyOnWriteArraySet<WebSocketSessionSender>()

    fun addSse(emitter: SseEmitter) {
        sseEmitters.add(emitter)
        log.debug("SSE subscriber registered (total={})", sseEmitters.size)
    }

    fun removeSse(emitter: SseEmitter) {
        sseEmitters.remove(emitter)
        log.debug("SSE subscriber removed (total={})", sseEmitters.size)
    }

    fun sseSubscribers(): List<SseEmitter> = sseEmitters.toList()

    fun addWs(session: WebSocketSessionSender) {
        wsSessions.add(session)
    }

    fun removeWs(session: WebSocketSessionSender) {
        wsSessions.remove(session)
    }

    fun wsSubscribers(): List<WebSocketSessionSender> = wsSessions.toList()
}
