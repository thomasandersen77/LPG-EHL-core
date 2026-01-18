package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.service.DiagnosticsService
import no.cloudberries.lpg.protocol.EhlDiagnosticsSnapshot
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
    private val diagnosticsService: DiagnosticsService
) {
    
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
}
