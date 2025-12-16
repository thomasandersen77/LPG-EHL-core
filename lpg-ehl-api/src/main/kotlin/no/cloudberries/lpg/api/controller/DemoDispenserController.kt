package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.model.Transaction
import no.cloudberries.lpg.api.service.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Demo controller for testing frontend without authentication.
 * Provides simple endpoints for dispenser control simulation.
 */
@RestController
@RequestMapping("/api/v1/dispenser")
@Tag(name = "Demo Dispenser", description = "Demo endpoints for frontend testing")
class DemoDispenserController(
    private val transactionService: TransactionService
) {

    // Simulated state
    private var state: DispenserState = DispenserState.IDLE
    private var litres: Double = 0.0
    private var amountToPay: Double = 0.0
    private val pricePerLitre: Double = 15.90
    private var lastUnblockTime: Long = 0
    private var currentPaymentType: String = "CASH"
    
    private val logger = LoggerFactory.getLogger(javaClass)

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
    fun unblock(
        @RequestParam(defaultValue = "CASH") paymentType: String
    ): ResponseEntity<DispenserStateDto> {
        if (state == DispenserState.IDLE || state == DispenserState.FINISHED) {
            state = DispenserState.DELIVERING
            lastUnblockTime = System.currentTimeMillis()
            litres = 0.0
            amountToPay = 0.0
            currentPaymentType = paymentType
        }
        return getState()
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop fuel delivery", description = "Stop the current delivery and finalize the transaction")
    fun stop(): ResponseEntity<DispenserStateDto> {
        println("=== STOP METHOD CALLED ===")
        if (state == DispenserState.DELIVERING) {
            // Calculate final volume before changing state
            val secondsElapsed = (System.currentTimeMillis() - lastUnblockTime) / 1000.0
            litres = secondsElapsed * 0.5 // 0.5 L/s flow rate
            amountToPay = litres * pricePerLitre
            
            println("Calculated: $litres L, $amountToPay kr")
            
            state = DispenserState.FINISHED
            
            // Save transaction to database
            if (litres > 0.01) { // Only save if we delivered at least 0.01 liters
                println("Attempting to save transaction...")
                logger.info("Saving transaction: {} liters, {} kr", litres, amountToPay)
                
                try {
                    val transaction = Transaction(
                        dispenserAddress = 1, // Demo dispenser address
                        nozzleNumber = 1,
                        volumeDeciliters = (litres * 10).toInt(), // Convert liters to deciliters
                        amountOre = (amountToPay * 100).toInt(), // Convert kr to øre
                        pricePerLiter = BigDecimal.valueOf(pricePerLitre),
                        paymentType = currentPaymentType,
                        includesRoadTax = true,
                        timestamp = LocalDateTime.now(),
                        productCode = "LPG"
                    )
                    println("Transaction object created")
                    val saved = transactionService.saveTransaction(transaction)
                    println("Transaction saved! ID: ${saved.transactionId}")
                    logger.info("Transaction saved with ID: {}", saved.transactionId)
                } catch (e: Exception) {
                    println("ERROR saving transaction: ${e.message}")
                    e.printStackTrace()
                    logger.error("Failed to save transaction", e)
                }
            } else {
                println("Volume too small: $litres L")
                logger.warn("Not saving transaction - insufficient volume: {} liters", litres)
            }
        } else {
            println("State is not DELIVERING: $state")
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
