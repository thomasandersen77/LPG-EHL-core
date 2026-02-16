package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import no.cloudberries.lpg.service.terminal.dto.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Polls terminal simulator events and invokes TerminalEventHandler.
 * Runs when payment.terminal.enabled=true.
 */
@Component
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalEventPoller(
    private val eventHandler: TerminalEventHandler,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
    @Value("\${payment.terminal.base-url:http://localhost:18080}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    @Volatile
    private var lastCursor: Long = 0

    @Volatile
    private var pollCount: Long = 0

    @Scheduled(fixedRate = 1000, initialDelay = 3000)
    fun pollEvents() {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/events?since=$lastCursor"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                log.trace("Terminal events poll: HTTP {}", response.statusCode())
                return
            }

            val root = objectMapper.readTree(response.body())
            if (!root.isArray) return

            var eventCount = 0
            for (node in root) {
                val cursor = node.path("Cursor").asLong(0)
                lastCursor = maxOf(lastCursor, cursor)

                val payloadNode = node.path("Payload")
                val payload = mutableMapOf<String, Any>()
                payloadNode.fields().forEachRemaining { (k, v) ->
                    when {
                        v.isBoolean -> payload[k] = v.asBoolean()
                        v.isNumber -> payload[k] = v.asDouble()
                        else -> payload[k] = v.asText("")
                    }
                }

                val envelope = EventEnvelope(
                    cursor = cursor,
                    eventId = node.path("EventId").takeIf { !it.isMissingNode }?.asText(),
                    operationId = node.path("OperationId").takeIf { !it.isMissingNode }?.asText(),
                    timestamp = node.path("Timestamp").takeIf { !it.isMissingNode }?.asText(),
                    eventType = node.path("EventType").takeIf { !it.isMissingNode }?.asText(),
                    payload = payload
                )

                dispatchEvent(envelope)
                eventCount++
            }

            if (pollCount++ % 60 == 0L && pollCount > 0) {
                log.debug("Terminal event poll: cursor={}, events this batch={}", lastCursor, eventCount)
            }
        } catch (e: Exception) {
            log.trace("Terminal events poll error: {}", e.message)
        }
    }

    private fun dispatchEvent(event: EventEnvelope) {
        when (event.eventType) {
            "OperationStarted" -> eventHandler.handleOperationStarted(event)
            "OperationCompleted" -> eventHandler.handleOperationCompleted(event)
            "OperationTimeout" -> eventHandler.handleOperationTimeout(event)
            "DisplayText" -> logDisplayText(event)
            else -> log.trace("Terminal event ignored: {}", event.eventType)
        }
    }

    private fun logDisplayText(event: EventEnvelope) {
        val text = (event.payload["text"] ?: event.payload["Text"])?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) {
            log.info("Terminal display: {}", text)
        } else {
            log.info("Terminal display event received (eventId={})", event.eventId)
        }
    }
}
