package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.dto.DispenserStatusResponse
import no.cloudberries.lpg.service.service.DispenserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/dispensers")
@Tag(name = "Dispensers", description = "Dispenser status endpoints")
// @SecurityRequirement(name = "bearer-token") // Disabled for local demo testing
class DispenserController(
    private val dispenserService: DispenserService,
    private val plsService: no.cloudberries.lpg.api.pls.MockPlsService?
) {

    @GetMapping
    @Operation(
        summary = "List all dispensers",
        description = "Get status for all dispensers"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getAllDispensers(): ResponseEntity<List<DispenserStatusResponse>> {
        val dispensers = dispenserService.getAllDispensers()
        return ResponseEntity.ok(dispensers)
    }

    @GetMapping("/active")
    @Operation(
        summary = "List active dispensers",
        description = "Get dispensers that have been seen recently"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getActiveDispensers(
        @Parameter(description = "Minutes since last seen (default 60)")
        @RequestParam(defaultValue = "60")
        minutesSinceLastSeen: Long
    ): ResponseEntity<List<DispenserStatusResponse>> {
        val dispensers = dispenserService.getActiveDispensers(minutesSinceLastSeen)
        return ResponseEntity.ok(dispensers)
    }

    @GetMapping("/{address}")
    @Operation(
        summary = "Get dispenser status",
        description = "Get status for a specific dispenser by address"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Dispenser found"),
            ApiResponse(responseCode = "404", description = "Dispenser not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getDispenserStatus(
        @Parameter(description = "Dispenser address")
        @PathVariable address: Int
    ): ResponseEntity<DispenserStatusResponse> {
        val dispenser = dispenserService.getDispenserStatus(address)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dispenser)
    }
    
    data class LiveStatusResponse(
        val state: String,
        val volumeLiters: Double,
        val amountKr: Double,
        val pricePerLiter: Double,
        val isActive: Boolean,
        val message: String
    )
    
    @GetMapping("/status")
    @Operation(
        summary = "Get live dispenser status",
        description = "Get real-time status for customer display during fueling"
    )
    fun getLiveStatus(): ResponseEntity<LiveStatusResponse> {
        // Get current price from PLS
        val currentPrice = plsService?.getCurrentPrice("LPG")?.pricePerLiter?.toDouble() ?: 15.90
        
        // Get status for dispenser 1 (default)
        val dispenser = dispenserService.getDispenserStatus(1)
        
        return if (dispenser != null) {
            // DispenserStatusResponse doesn't have volume/amount, so return default for now
            ResponseEntity.ok(
                LiveStatusResponse(
                    state = dispenser.state,
                    volumeLiters = 0.0,  // Would need to track this separately
                    amountKr = 0.0,       // Would need to track this separately
                    pricePerLiter = currentPrice,
                    isActive = dispenser.state == "DISPENSING" || dispenser.state == "AUTHORIZED",
                    message = when (dispenser.state) {
                        "IDLE" -> "Klar for fylling"
                        "AUTHORIZED" -> "Autorisert - Start fylling"
                        "DISPENSING" -> "Fyller..."
                        "FINISHED" -> "Fylling fullført"
                        else -> "Ukjent status"
                    }
                )
            )
        } else {
            ResponseEntity.ok(
                LiveStatusResponse(
                    state = "IDLE",
                    volumeLiters = 0.0,
                    amountKr = 0.0,
                    pricePerLiter = currentPrice,
                    isActive = false,
                    message = "Klar for fylling"
                )
            )
        }
    }
}
