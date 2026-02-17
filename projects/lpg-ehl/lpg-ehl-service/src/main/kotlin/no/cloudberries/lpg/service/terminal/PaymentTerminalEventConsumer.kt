package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

/**
 * Consumes events from the Payment Terminal Simulator.
 * Triggers pump release on CARD_RESERVED events.
 *
 * IMPORTANT: This consumer is ONLY for legacy terminal simulators.
 * When using Nets Cloud Connect (terminal.provider=nets-cloud-connect),
 * events are handled via PaymentTerminalClient.terminalEvents() in PumpPaymentOrchestrator.
 */
@Component
@ConditionalOnProperty(name = ["payment.events.enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("'\${terminal.provider:simulator}' != 'nets-cloud-connect'")
class PaymentTerminalEventConsumer(
    private val pumpStateService: PumpStateService,
    private val objectMapper: ObjectMapper,
    @Value("\${payment.terminal.base-url:http://localhost:18080}") private val simulatorBaseUrl: String,
    @Value("\${payment.events.mode:sse}") private val mode: String,
    @Value("\${lpg.dispenser.address:1}") private val dispenserAddress: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var subscription: Disposable? = null
    
    private val webClient = WebClient.builder().baseUrl(simulatorBaseUrl).build()
    private val wsClient = ReactorNettyWebSocketClient()

    @PostConstruct
    fun start() {
        log.info("Starting PaymentTerminalEventConsumer (mode={}, url={})", mode, simulatorBaseUrl)
        if (mode.equals("websocket", ignoreCase = true)) {
            startWebSocketSubscription()
        } else {
            startSseSubscription()
        }
    }

    @PreDestroy
    fun stop() {
        subscription?.dispose()
    }

    private fun startSseSubscription() {
        subscription = webClient.get()
            .uri("/v1/events/stream?since=now")
            .retrieve()
            .bodyToFlux(String::class.java)
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry { log.info("Reconnecting to terminal SSE events...") })
            .subscribe(
                { eventJson: String -> handleEvent(eventJson) },
                { error: Throwable -> log.error("Error in terminal SSE stream: {}", error.message) }
            )
    }

    private fun startWebSocketSubscription() {
        val wsUrl = simulatorBaseUrl.replaceFirst("http".toRegex(), "ws") + "/v1/events/ws"
        log.info("Connecting to terminal WebSocket: $wsUrl")
        
        subscription = wsClient.execute(URI.create(wsUrl)) { session ->
            session.receive()
                .map { it.payloadAsText }
                .doOnNext { handleEvent(it) }
                .then()
        }
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
            .maxBackoff(Duration.ofSeconds(30))
            .doBeforeRetry { log.info("Reconnecting to terminal WebSocket...") })
        .subscribe(
            {},
            { error -> log.error("Error in terminal WebSocket: {}", error.message) }
        )
    }

    private fun handleEvent(eventJson: String) {
        try {
            val root: JsonNode = objectMapper.readTree(eventJson)
            val eventType = root.get("EventType")?.asText() ?: root.get("eventType")?.asText()
            
            if (eventType == "CARD_RESERVED") {
                val operationId = root.get("OperationId")?.asText() ?: root.get("operationId")?.asText()
                log.info("💳 CARD_RESERVED received (opId={}). Releasing pump {}...", operationId, dispenserAddress)
                
                // Call unified service-level releasePump
                val result = pumpStateService.releasePump(dispenserAddress)
                
                result.onSuccess {
                    log.info("✅ Pump {} released automatically via card event", dispenserAddress)
                }.onFailure { error ->
                    log.error("❌ Failed to release pump {} on card event: {}", dispenserAddress, error.message)
                }
            }
        } catch (e: Exception) {
            log.debug("Ignoring non-JSON or invalid event: {}", eventJson)
        }
    }
}
