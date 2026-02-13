package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class PaymentTerminalDiagnosticsService(
    @Value("\${payment.terminal.base-url:http://127.0.0.1:18080}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val objectMapper = ObjectMapper()

    fun getHealth(): JsonNode {
        log.info("Payment terminal diagnostics: GET /health")
        return getJson("/health")
    }

    fun getTerminalStatus(): JsonNode {
        log.info("Payment terminal diagnostics: GET /v1/terminal/status")
        return getJson("/v1/terminal/status")
    }

    fun openTerminal(): JsonNode {
        log.info("Payment terminal diagnostics: POST /v1/terminal/open")
        return postJson("/v1/terminal/open", "{}")
    }

    fun closeTerminal(): JsonNode {
        log.info("Payment terminal diagnostics: POST /v1/terminal/close")
        return postJson("/v1/terminal/close", "{}")
    }

    fun getDiagnosticsSchema(): JsonNode {
        log.info("Payment terminal diagnostics: GET /v1/diag/schema")
        return getJson("/v1/diag/schema")
    }

    fun sendDiagnosticsJson(payload: String): JsonNode {
        log.info("Payment terminal diagnostics: POST /v1/diag/sendjson (payload length={})", payload.length)
        return postJson(
            "/v1/diag/sendjson",
            objectMapper.createObjectNode().put("json", payload).toString()
        )
    }

    fun sendDiagnosticsTld(tldType: String, tldDataBase64: String): JsonNode {
        log.info(
            "Payment terminal diagnostics: POST /v1/diag/sendtld (tldType={}, dataLength={})",
            tldType,
            tldDataBase64.length
        )
        return postJson(
            "/v1/diag/sendtld",
            objectMapper.createObjectNode()
                .put("tldType", tldType)
                .put("tldData", tldDataBase64)
                .toString()
        )
    }

    fun confirmDiagnostics(id: Int, allow: Boolean): JsonNode {
        log.info("Payment terminal diagnostics: POST /v1/diag/confirm (id={}, allow={})", id, allow)
        return postJson(
            "/v1/diag/confirm",
            objectMapper.createObjectNode()
                .put("id", id)
                .put("allow", allow)
                .toString()
        )
    }

    /**
     * Generic proxy – forwards any request to the terminal.
     * Used for Purchase, Refund, Cashback, Admin, Events etc.
     */
    fun proxy(method: String, path: String, body: String? = null, params: Map<String, String>? = null): JsonNode {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val pathWithParams = if (!params.isNullOrEmpty()) {
            val query = params.entries.joinToString("&") { (k, v) -> "${k}=${java.net.URLEncoder.encode(v, Charsets.UTF_8)}" }
            "$normalizedPath?$query"
        } else normalizedPath
        log.info("Payment terminal proxy: {} {}", method, pathWithParams)
        return when (method.uppercase()) {
            "GET" -> getJson(pathWithParams)
            "POST" -> postJson(pathWithParams, body)
            else -> {
                log.warn("Unsupported proxy method: {}", method)
                objectMapper.createObjectNode().put("error", "Unsupported method: $method")
            }
        }
    }

    fun streamEvents(since: String): InputStream {
        val pathWithParams = "/v1/events/stream?since=${java.net.URLEncoder.encode(since, Charsets.UTF_8)}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$pathWithParams"))
            .timeout(Duration.ofMinutes(5))
            .header("Accept", "text/event-stream")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return response.body()
    }

    private fun getJson(path: String): JsonNode {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .GET()
            .build()

        return send(request)
    }

    private fun postJson(path: String, body: String?): JsonNode {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        val request = if (body == null) {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody()).build()
        } else {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build()
        }

        return send(request)
    }

    private fun send(request: HttpRequest): JsonNode {
        return try {
            log.info("Payment terminal diagnostics request: {} {}", request.method(), request.uri())
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            log.info(
                "Payment terminal diagnostics response: {} {} -> {}",
                request.method(),
                request.uri(),
                response.statusCode()
            )
            objectMapper.readTree(response.body())
        } catch (e: Exception) {
            log.error(
                "Payment terminal diagnostics call failed: {} {}",
                request.method(),
                request.uri(),
                e
            )
            objectMapper.createObjectNode()
                .put("error", e.message ?: "Unknown error")
        }
    }
}