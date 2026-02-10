package no.cloudberries.lpg.payment.terminal.sim.model.response

/**
 * Event envelope for SSE and polling (PascalCase).
 */
data class EventEnvelope(
    val Cursor: Long,
    val EventId: String,
    val OperationId: String? = null,
    val Timestamp: String,
    val EventType: String,
    val Payload: Map<String, Any> = emptyMap()
)
