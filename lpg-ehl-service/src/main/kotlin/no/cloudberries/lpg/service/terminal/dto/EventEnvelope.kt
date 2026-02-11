package no.cloudberries.lpg.service.terminal.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Terminal event envelope.
 *
 * The payment terminal simulator uses PascalCase field names (Cursor, EventId, ...).
 * We keep idiomatic Kotlin camelCase properties and map using @JsonProperty.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EventEnvelope(
    @JsonProperty("Cursor")
    val cursor: Long? = null,

    @JsonProperty("EventId")
    val eventId: String? = null,

    @JsonProperty("OperationId")
    val operationId: String? = null,

    @JsonProperty("Timestamp")
    val timestamp: String? = null,

    @JsonProperty("EventType")
    val eventType: String? = null,

    @JsonProperty("Payload")
    val payload: Map<String, Any> = emptyMap()
)
