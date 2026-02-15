package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope

fun interface WebSocketSessionSender {
    fun send(event: EventEnvelope): Boolean
}
