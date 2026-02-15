package no.cloudberries.lpg.payment.terminal.sim.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

fun interface SseEventSender {
    fun send(emitter: SseEmitter, event: EventEnvelope): Boolean
}

@Component
class JacksonSseEventSender(
    private val objectMapper: ObjectMapper
) : SseEventSender {
    override fun send(emitter: SseEmitter, event: EventEnvelope): Boolean {
        return try {
            val eventData = objectMapper.writeValueAsString(event)
            emitter.send(
                SseEmitter.event()
                    .id(event.Cursor.toString())
                    .name(event.EventType)
                    .data(eventData)
                    .build()
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
