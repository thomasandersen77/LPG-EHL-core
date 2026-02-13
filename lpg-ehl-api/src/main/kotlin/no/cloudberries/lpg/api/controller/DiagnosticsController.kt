package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.system.DiagnosticsService
import no.cloudberries.lpg.service.terminal.PaymentTerminalDiagnosticsService
import no.cloudberries.lpg.protocol.EhlDiagnosticsSnapshot
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Diagnostics REST Controller
 * 
 * Provides EHL dispenser diagnostics information for:
 * - Admin monitoring dashboard
 * - Service technician troubleshooting
 * - Alert system integration
 * - Historical fault tracking
 * 
 * ## Endpoints:
 * - GET /admin/ehl/diagnostics - All dispensers diagnostics
 * - GET /admin/ehl/diagnostics/{address} - Single dispenser diagnostics
 * 
 * ## Security:
 * This endpoint is intended for admin/service use only.
 * In production, enable @SecurityRequirement with appropriate roles.
 */
@RestController
@RequestMapping("/admin/ehl/diagnostics")
@Tag(name = "Diagnostics", description = "EHL dispenser diagnostics and health monitoring")
class DiagnosticsController(
    private val diagnosticsService: DiagnosticsService,
    private val paymentTerminalDiagnosticsServiceProvider: ObjectProvider<PaymentTerminalDiagnosticsService>
) {
    private fun paymentTerminalDiagnosticsService(): PaymentTerminalDiagnosticsService? =
        paymentTerminalDiagnosticsServiceProvider.ifAvailable
    
    @GetMapping
    @Operation(
        summary = "Get diagnostics for all dispensers",
        description = """
            Returns comprehensive diagnostics information for all known dispensers:
            - Connection status and last communication times
            - Current operational state
            - Last detected fault (if any)
            - RS-485 configuration hints
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Diagnostics retrieved successfully"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
        ]
    )
    fun getAllDiagnostics(): ResponseEntity<List<EhlDiagnosticsSnapshot>> {
        val diagnostics = diagnosticsService.getAllDiagnostics()
        return ResponseEntity.ok(diagnostics)
    }
    
    @GetMapping("/{address}")
    @Operation(
        summary = "Get diagnostics for specific dispenser",
        description = """
            Returns diagnostics for a single dispenser by address.
            Useful for focused troubleshooting of a specific unit.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Dispenser diagnostics found"),
            ApiResponse(responseCode = "404", description = "Dispenser not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
        ]
    )
    fun getDispenserDiagnostics(
        @Parameter(description = "Dispenser address on RS-485 bus")
        @PathVariable address: Int
    ): ResponseEntity<EhlDiagnosticsSnapshot> {
        val diagnostics = diagnosticsService.getDiagnosticsForDispenser(address)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(diagnostics)
    }
    
    @GetMapping("/faults")
    @Operation(
        summary = "Get all dispensers with active faults",
        description = """
            Returns diagnostics only for dispensers that have detected faults.
            Useful for alert dashboards and fault monitoring.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Faulted dispensers retrieved"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
        ]
    )
    fun getDispensersWithFaults(): ResponseEntity<List<EhlDiagnosticsSnapshot>> {
        val faulted = diagnosticsService.getDispensersWithFaults()
        return ResponseEntity.ok(faulted)
    }
    
    @GetMapping("/critical")
    @Operation(
        summary = "Get dispensers with CRITICAL faults",
        description = """
            Returns only dispensers in ERROR state or with unresolved CRITICAL faults.
            These dispensers are blocked and require immediate service intervention.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Critical faults retrieved"),
            ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required")
        ]
    )
    fun getCriticalFaults(): ResponseEntity<List<EhlDiagnosticsSnapshot>> {
        val critical = diagnosticsService.getDispenserWithCriticalFaults()
        return ResponseEntity.ok(critical)
    }

    @GetMapping("/payment-terminal/health")
    @Operation(
        summary = "Payment terminal health check",
        description = """
            Proxies /health to the configured payment terminal (real or simulator).
            Returns lowercase keys as provided by the terminal server.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Health retrieved"),
            ApiResponse(responseCode = "503", description = "Terminal unreachable")
        ]
    )
    fun getPaymentTerminalHealth(): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.getHealth()
        return ResponseEntity.ok(response)
    }

    @GetMapping("/payment-terminal/status")
    @Operation(
        summary = "Payment terminal status",
        description = """
            Proxies /v1/terminal/status to the configured payment terminal.
            Returns PascalCase keys as provided by the terminal server.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Status retrieved"),
            ApiResponse(responseCode = "503", description = "Terminal unreachable")
        ]
    )
    fun getPaymentTerminalStatus(): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.getTerminalStatus()
        return ResponseEntity.ok(response)
    }

    @PostMapping("/payment-terminal/open")
    @Operation(
        summary = "Open payment terminal",
        description = "Proxies /v1/terminal/open to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Terminal opened"),
            ApiResponse(responseCode = "500", description = "Terminal open failed")
        ]
    )
    fun openPaymentTerminal(): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.openTerminal()
        return ResponseEntity.ok(response)
    }

    @PostMapping("/payment-terminal/close")
    @Operation(
        summary = "Close payment terminal",
        description = "Proxies /v1/terminal/close to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Terminal closed"),
            ApiResponse(responseCode = "500", description = "Terminal close failed")
        ]
    )
    fun closePaymentTerminal(): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.closeTerminal()
        return ResponseEntity.ok(response)
    }

    @GetMapping("/payment-terminal/diag/schema")
    @Operation(
        summary = "Payment terminal diagnostics schema",
        description = "Proxies /v1/diag/schema to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Schema retrieved"),
            ApiResponse(responseCode = "403", description = "Diagnostics disabled")
        ]
    )
    fun getPaymentTerminalDiagnosticsSchema(): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.getDiagnosticsSchema()
        return ResponseEntity.ok(response)
    }

    data class DiagnosticsJsonRequest(
        val json: String
    )

    @PostMapping("/payment-terminal/diag/sendjson")
    @Operation(
        summary = "Send raw JSON to terminal",
        description = "Proxies /v1/diag/sendjson to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Diagnostics JSON sent"),
            ApiResponse(responseCode = "403", description = "Diagnostics disabled")
        ]
    )
    fun sendPaymentTerminalDiagnosticsJson(
        @RequestBody request: DiagnosticsJsonRequest
    ): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.sendDiagnosticsJson(request.json)
        return ResponseEntity.ok(response)
    }

    data class DiagnosticsTldRequest(
        val tldType: String,
        val tldData: String
    )

    @PostMapping("/payment-terminal/diag/sendtld")
    @Operation(
        summary = "Send TLD data to terminal",
        description = "Proxies /v1/diag/sendtld to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Diagnostics TLD sent"),
            ApiResponse(responseCode = "403", description = "Diagnostics disabled")
        ]
    )
    fun sendPaymentTerminalDiagnosticsTld(
        @RequestBody request: DiagnosticsTldRequest
    ): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.sendDiagnosticsTld(request.tldType, request.tldData)
        return ResponseEntity.ok(response)
    }

    data class DiagnosticsConfirmRequest(
        val id: Int,
        val allow: Boolean
    )

    @PostMapping("/payment-terminal/diag/confirm")
    @Operation(
        summary = "Confirm diagnostics operation",
        description = "Proxies /v1/diag/confirm to the configured payment terminal."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Diagnostics confirm processed"),
            ApiResponse(responseCode = "403", description = "Diagnostics disabled")
        ]
    )
    fun confirmPaymentTerminalDiagnostics(
        @RequestBody request: DiagnosticsConfirmRequest
    ): ResponseEntity<Any> {
        val service = paymentTerminalDiagnosticsService()
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "Payment terminal diagnostics disabled"))
        val response = service.confirmDiagnostics(request.id, request.allow)
        return ResponseEntity.ok(response)
    }
}
