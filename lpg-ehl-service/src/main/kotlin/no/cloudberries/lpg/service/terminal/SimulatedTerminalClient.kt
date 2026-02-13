package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * HTTP client for Payment Terminal Simulator.
 * Implements OPEN -> PURCHASE flow for station-owner operations.
 */
@Component
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class SimulatedTerminalClient(
    @Value("\${payment.terminal.base-url:http://localhost:18080}") private val baseUrl: String
) : TerminalClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val objectMapper = ObjectMapper()

    override fun openTerminal(): TerminalSimpleResponse {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/terminal/open"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = readBoolean(json, "Success", "success")
            val message = readText(json, "Message", "message")
            val error = readText(json, "Error", "error")

            log.info("Terminal open: success={}, message={}", success, message)
            TerminalSimpleResponse(success = success, message = message, error = error)
        } catch (e: Exception) {
            log.error("Terminal open failed: {}", e.message)
            TerminalSimpleResponse(success = false, error = e.message)
        }
    }

    override fun getHealth(): TerminalHealthResponse {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/health"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val status = readText(json, "status", "Status") ?: "unknown"
            val configLoaded = readBoolean(json, "configLoaded", "ConfigLoaded")

            TerminalHealthResponse(status = status, configLoaded = configLoaded)
        } catch (e: Exception) {
            log.error("Terminal health failed: {}", e.message)
            TerminalHealthResponse(status = "error", configLoaded = false)
        }
    }

    override fun getStatus(): TerminalStatusResponse {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/terminal/status"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val terminalOpen = readBoolean(json, "TerminalOpen", "terminalOpen")
            val terminalReady = readBoolean(json, "TerminalReady", "terminalReady")
            val connectionState = readText(json, "ConnectionState", "connectionState")
            val lastError = readText(json, "LastError", "lastError")

            TerminalStatusResponse(
                terminalOpen = terminalOpen,
                terminalReady = terminalReady,
                connectionState = connectionState,
                lastError = lastError
            )
        } catch (e: Exception) {
            log.error("Terminal status failed: {}", e.message)
            TerminalStatusResponse(terminalOpen = false, terminalReady = false, lastError = e.message)
        }
    }

    override fun closeTerminal(): TerminalSimpleResponse {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/terminal/close"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = readBoolean(json, "Success", "success")
            val message = readText(json, "Message", "message")
            val error = readText(json, "Error", "error")

            log.info("Terminal close: success={}, message={}", success, message)
            TerminalSimpleResponse(success = success, message = message, error = error)
        } catch (e: Exception) {
            log.error("Terminal close failed: {}", e.message)
            TerminalSimpleResponse(success = false, error = e.message)
        }
    }

    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        return try {
            val body = buildPurchaseBody(request)
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/payments/purchase"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build()

            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = readBoolean(json, "Success", "success")
            val operationId = readText(json, "OperationId", "operationId")
            val callResult = readInt(json, "CallResult", "callResult")
            val entryMode = readText(json, "EntryMode", "entryMode")
            val entryModeCode = readText(json, "EntryModeCode", "entryModeCode")
            val localModeResultData = readText(json, "LocalModeResultData", "localModeResultData")
            val responseCode = readText(json, "ResponseCode", "responseCode")
            val rejectionReason = readText(json, "RejectionReason", "rejectionReason")
            val printTextRaw = readText(json, "PrintTextRaw", "printTextRaw")
            val printTextSanitized = readText(json, "PrintTextSanitized", "printTextSanitized")
            val lastDisplayText = readText(json, "LastDisplayText", "lastDisplayText")
            val localModeResult = readInt(json, "LocalModeResult", "localModeResult")
            val durationMs = readLong(json, "DurationMs", "durationMs")
            val error = readText(json, "Error", "error")
            val errorCode = readText(json, "ErrorCode", "errorCode")

            log.info("Purchase: operationId={}, amountMinor={}, success={}", operationId, request.amountMinor, success)
            TerminalOperationResponse(
                success = success,
                operationId = operationId,
                callResult = callResult,
                entryMode = entryMode,
                entryModeCode = entryModeCode,
                localModeResultData = localModeResultData,
                responseCode = responseCode,
                rejectionReason = rejectionReason,
                printTextRaw = printTextRaw,
                printTextSanitized = printTextSanitized,
                lastDisplayText = lastDisplayText,
                localModeResult = localModeResult,
                durationMs = durationMs,
                error = error,
                errorCode = errorCode
            )
        } catch (e: Exception) {
            log.error("Purchase failed: {}", e.message)
            TerminalOperationResponse(success = false, error = e.message)
        }
    }

    override fun reversal(operationId: String?): TerminalOperationResponse {
        return try {
            val body = objectMapper.createObjectNode()
                .put("Password", "0000")
                .toString()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/admin/reversal"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = readBoolean(json, "Success", "success")
            val opId = readText(json, "OperationId", "operationId")
            val callResult = readInt(json, "CallResult", "callResult")
            val error = readText(json, "Error", "error")
            val errorCode = readText(json, "ErrorCode", "errorCode")

            log.info("Reversal: operationId={}, success={}", operationId ?: opId, success)
            TerminalOperationResponse(
                success = success,
                operationId = opId,
                callResult = callResult,
                error = error,
                errorCode = errorCode
            )
        } catch (e: Exception) {
            log.error("Reversal failed: {}", e.message)
            TerminalOperationResponse(success = false, operationId = operationId, error = e.message)
        }
    }

    private fun buildPurchaseBody(request: TerminalPurchaseRequest): String {
        val root = objectMapper.createObjectNode()
        root.put("AmountMinor", request.amountMinor)
        root.put("OperatorId", request.operatorId)
        root.put("Currency", request.currency)
        request.optionalData?.let { root.put("OptionalData", it) }
        request.clientRequestId?.let { root.put("ClientRequestId", it) }
        request.preAvstemming?.let { config ->
            root.putObject("PreAvstemming")
                .put("Enabled", config.enabled)
                .put("Password", config.password)
                .apply {
                    config.timeoutSeconds?.let { put("TimeoutSeconds", it) }
                }
        }
        return root.toString()
    }

    private fun readBoolean(node: JsonNode, vararg names: String): Boolean {
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull }
            ?.asBoolean(false)
            ?: false
    }

    private fun readText(node: JsonNode, vararg names: String): String? {
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull }
            ?.asText()
    }

    private fun readInt(node: JsonNode, vararg names: String): Int? {
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull }
            ?.asInt()
    }

    private fun readLong(node: JsonNode, vararg names: String): Long? {
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull }
            ?.asLong()
    }
}
