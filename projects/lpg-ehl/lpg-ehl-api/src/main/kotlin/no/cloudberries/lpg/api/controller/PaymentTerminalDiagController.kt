package no.cloudberries.lpg.api.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.cloudberries.lpg.service.terminal.PaymentTerminalDiagnosticsService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/**
 * REST controller for Payment Terminal diagnostics page.
 * Proxies all calls to the configured payment terminal (simulator or real).
 *
 * The frontend calls this API instead of the terminal directly, avoiding
 * CORS and enabling deployment to different hosts (e.g. http://10.0.0.169:8080).
 */
@RestController
@RequestMapping("/api/v1/terminal-diag")
class PaymentTerminalDiagController(
    private val serviceProvider: ObjectProvider<PaymentTerminalDiagnosticsService>
) {
    private fun service(): PaymentTerminalDiagnosticsService? = serviceProvider.ifAvailable

    @GetMapping("/health")
    fun getHealth(): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.getHealth()) }

    @GetMapping("/terminal/status")
    fun getTerminalStatus(): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.getTerminalStatus()) }

    @PostMapping("/terminal/open")
    fun openTerminal(): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.openTerminal()) }

    @PostMapping("/terminal/close")
    fun closeTerminal(): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.closeTerminal()) }

    @GetMapping("/diag/schema")
    fun getDiagnosticsSchema(): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.getDiagnosticsSchema()) }

    @PostMapping("/diag/sendjson")
    fun sendDiagnosticsJson(@RequestBody request: SendJsonRequest): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.sendDiagnosticsJson(request.json)) }

    @PostMapping("/diag/sendtld")
    fun sendDiagnosticsTld(@RequestBody request: SendTldRequest): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.sendDiagnosticsTld(request.tldType, request.tldData)) }

    @PostMapping("/diag/confirm")
    fun confirmDiagnostics(@RequestBody request: ConfirmRequest): ResponseEntity<Any> =
        withService { ResponseEntity.ok(it.confirmDiagnostics(request.id, request.allow)) }

    @GetMapping("/events/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(@RequestParam(defaultValue = "0") since: String): ResponseEntity<Any> {
        val svc = service()
        if (svc == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal disabled"))
        }
        val body = StreamingResponseBody { out ->
            svc.streamEvents(since).use { input ->
                input.copyTo(out)
            }
        }
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(body)
    }

    /**
     * Generic proxy – forwards any request to the terminal.
     * Used for Purchase, Refund, Cashback, Admin, Events, etc.
     */
    @PostMapping("/proxy")
    fun proxy(@RequestBody request: ProxyRequest): ResponseEntity<Any> =
        withService {
            val bodyStr = request.body?.let { b ->
                when (b) {
                    is String -> b
                    else -> jacksonObjectMapper().writeValueAsString(b)
                }
            }
            ResponseEntity.ok(it.proxy(request.method, request.path, bodyStr, request.params))
        }

    private fun withService(block: (PaymentTerminalDiagnosticsService) -> ResponseEntity<Any>): ResponseEntity<Any> {
        val svc = service()
        return if (svc != null) block(svc)
        else ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("error" to "Payment terminal disabled (payment.terminal.enabled=false)"))
    }

    data class SendJsonRequest(val json: String)
    data class SendTldRequest(val tldType: String, val tldData: String)
    data class ConfirmRequest(val id: Int, val allow: Boolean)
    data class ProxyRequest(
        val method: String,
        val path: String,
        val body: Any? = null,
        val params: Map<String, String>? = null
    )
}
