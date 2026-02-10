package no.cloudberries.lpg.payment.terminal.sim.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for pending reservations.
 * Maps OperationId -> reserved amount (minor units).
 */
@Service
class ReservationStore {
    private val log = LoggerFactory.getLogger(ReservationStore::class.java)
    private val pending = ConcurrentHashMap<String, Int>()

    fun put(operationId: String, amountMinor: Int) {
        pending[operationId] = amountMinor
        log.debug("Reservation stored: operationId={}, amount={} kr", operationId, amountMinor / 100.0)
    }

    fun get(operationId: String): Int? = pending[operationId]

    fun remove(operationId: String): Int? {
        val removed = pending.remove(operationId)
        if (removed != null) {
            log.debug("Reservation completed/removed: operationId={}", operationId)
        }
        return removed
    }

    fun hasPending(operationId: String): Boolean = pending.containsKey(operationId)
}
