package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.model.response.EventEnvelope

/**
 * Listener for real-time event notifications from [EventStore].
 */
fun interface EventStoreListener {
    fun onEvent(event: EventEnvelope)
}
