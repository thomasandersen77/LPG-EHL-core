package no.cloudberries.lpg.service.terminal

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks active terminal reservation sessions per pump.
 * Used to link reservation operationId with pump for capture when filling stops.
 */
@Component
class TerminalPumpSession {

    private val sessions = ConcurrentHashMap<Int, Session>()

    data class Session(
        val operationId: String,
        val reservedAmountMinor: Int,
        val pumpId: Int
    )

    fun put(pumpId: Int, operationId: String, reservedAmountMinor: Int) {
        sessions[pumpId] = Session(operationId, reservedAmountMinor, pumpId)
    }

    fun get(pumpId: Int): Session? = sessions[pumpId]

    fun remove(pumpId: Int): Session? = sessions.remove(pumpId)

    fun hasActiveSession(pumpId: Int): Boolean = sessions.containsKey(pumpId)
}
