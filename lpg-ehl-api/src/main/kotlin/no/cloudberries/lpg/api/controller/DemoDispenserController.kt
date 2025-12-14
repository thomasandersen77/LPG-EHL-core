package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Demo controller for testing frontend without authentication.
 * Provides simple endpoints for dispenser control simulation.
 */
@RestController
@RequestMapping("/api/v1/dispenser")
@Tag(name = "Demo Dispenser", description = "Demo endpoints for frontend testing")
class DemoDispenserController {

    // Simulated state
    private var state: DispenserState = DispenserState.IDLE
    private var litres: Double = 0.0
    private var amountToPay: Double = 0.0
    private val pricePerLitre: Double = 15.90
    private var lastUnblockTime: Long = 0

    @GetMapping("/state")
    @Operation(summary = "Get current dispenser state", description = "Returns the current state of the demo dispenser")
    fun getState(): ResponseEntity<DispenserStateDto> {
        // Simulate delivery progress
        if (state == DispenserState.DELIVERING) {
            val secondsElapsed = (System.currentTimeMillis() - lastUnblockTime) / 1000.0
            litres = secondsElapsed * 0.5 // 0.5 L/s flow rate
            amountToPay = litres * pricePerLitre
        }

        return ResponseEntity.ok(
            DispenserStateDto(
                state = state.name,
                amountToPay = amountToPay,
                litres = litres,
                pricePerLitre = pricePerLitre,
                includeRoadTax = true,
                cardModeActive = false,
                dayMode = true,
                stationCreditActive = false,
                connected = true
            )
        )
    }

    @PostMapping("/unblock")
    @Operation(summary = "Start fuel delivery", description = "Unblock the dispenser and start delivery")
    fun unblock(): ResponseEntity<DispenserStateDto> {
        if (state == DispenserState.IDLE || state == DispenserState.FINISHED) {
            state = DispenserState.DELIVERING
            lastUnblockTime = System.currentTimeMillis()
            litres = 0.0
            amountToPay = 0.0
        }
        return getState()
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop fuel delivery", description = "Stop the current delivery and finalize the transaction")
    fun stop(): ResponseEntity<DispenserStateDto> {
        if (state == DispenserState.DELIVERING) {
            state = DispenserState.FINISHED
        }
        return getState()
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset dispenser", description = "Reset the dispenser to IDLE state")
    fun reset(): ResponseEntity<DispenserStateDto> {
        state = DispenserState.IDLE
        litres = 0.0
        amountToPay = 0.0
        return getState()
    }

    enum class DispenserState {
        IDLE, READY, DELIVERING, FINISHED, ERROR
    }

    data class DispenserStateDto(
        val state: String,
        val amountToPay: Double,
        val litres: Double,
        val pricePerLitre: Double,
        val includeRoadTax: Boolean,
        val cardModeActive: Boolean,
        val dayMode: Boolean,
        val stationCreditActive: Boolean,
        val connected: Boolean
    )
}
