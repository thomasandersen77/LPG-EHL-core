package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.model.Transaction
import no.cloudberries.lpg.api.service.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Demo controller for testing frontend without authentication.
 * Provides simple endpoints for dispenser control simulation.
 * 
 * Enabled for local development and demo profiles.
 */
@Profile("local", "demo")
@RestController
@RequestMapping("/api/v1/dispenser")
@Tag(name = "Demo Dispenser", description = "Demo endpoints for frontend testing (deprecated)")
class DemoDispenserController(
    private val transactionService: TransactionService,
    private val transactionRepository: no.cloudberries.lpg.api.repository.TransactionRepository,
    private val plsService: no.cloudberries.lpg.api.pls.MockPlsService?
) {

    // Simulated state
    private var state: DispenserState = DispenserState.IDLE
    private var litres: Double = 0.0
    private var amountToPay: Double = 0.0
    private var pricePerLitre: Double = 15.90 // Dynamic price, updated from PLS
    private var lastUnblockTime: Long = 0
    private var currentPaymentType: String = "CASH"
    
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/state")
    @Operation(summary = "Get current dispenser state", description = "Returns the current state of the demo dispenser")
    fun getState(): ResponseEntity<DispenserStateDto> {
        // Update price from PLS if available
        val currentPrice = plsService?.getCurrentPrice("LPG")?.pricePerLiter?.toDouble() ?: 15.90
        
        // Simulate delivery progress
        if (state == DispenserState.DELIVERING) {
            val secondsElapsed = (System.currentTimeMillis() - lastUnblockTime) / 1000.0
            litres = secondsElapsed * 0.5 // 0.5 L/s flow rate
            amountToPay = litres * pricePerLitre // Use price that was set when starting
        }

        return ResponseEntity.ok(
            DispenserStateDto(
                state = state.name,
                amountToPay = amountToPay,
                litres = litres,
                pricePerLitre = if (state == DispenserState.DELIVERING) pricePerLitre else currentPrice,
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
    ): ResponseEntity<*> {
        // Check current state - must be IDLE to start
        if (state != DispenserState.IDLE) {
            logger.warn("⚠️ Cannot start pumping - dispenser state is: {}", state)
            
            // If state is FINISHED, there's an unpaid transaction
            if (state == DispenserState.FINISHED) {
                val unpaidTransaction = transactionRepository.findFirstByDispenserAddressAndPaymentStatusOrderByTimestampDesc(1, "PENDING")
                return ResponseEntity.status(409).body(mapOf(
                    "error" to "UNPAID_TRANSACTION",
                    "message" to "Du må betale for forrige fylling før du kan starte på nytt",
                    "unpaidTransaction" to mapOf(
                        "id" to unpaidTransaction?.transactionId,
                        "amount" to unpaidTransaction?.amountKr,
                        "liters" to unpaidTransaction?.volumeLiters,
                        "timestamp" to unpaidTransaction?.timestamp
                    )
                ))
            }
            
            return ResponseEntity.status(409).body(mapOf(
                "error" to "INVALID_STATE",
                "message" to "Dispenseren er ikke klar for fylling (state: ${state.name})",
                "currentState" to state.name
            ))
        }
        
        // Double-check for unpaid transactions (should not happen if state management is correct)
        val hasUnpaid = transactionRepository.existsByDispenserAddressAndPaymentStatus(1, "PENDING")
        if (hasUnpaid) {
            val unpaidTransaction = transactionRepository.findFirstByDispenserAddressAndPaymentStatusOrderByTimestampDesc(1, "PENDING")
            logger.warn("⚠️ Cannot start pumping - unpaid transaction exists: {}", unpaidTransaction?.transactionId)
            return ResponseEntity.status(409).body(mapOf(
                "error" to "UNPAID_TRANSACTION",
                "message" to "Du må betale for forrige fylling før du kan starte på nytt",
                "unpaidTransaction" to mapOf(
                    "id" to unpaidTransaction?.transactionId,
                    "amount" to unpaidTransaction?.amountKr,
                    "liters" to unpaidTransaction?.volumeLiters,
                    "timestamp" to unpaidTransaction?.timestamp
                )
            ))
        }
        
        // All checks passed - start delivery
        // Lock in the current price when starting delivery
        pricePerLitre = plsService?.getCurrentPrice("LPG")?.pricePerLiter?.toDouble() ?: 15.90
        logger.info("🚀 Starting delivery at price: {} kr/L", pricePerLitre)
        
        state = DispenserState.DELIVERING
        lastUnblockTime = System.currentTimeMillis()
        litres = 0.0
        amountToPay = 0.0
        currentPaymentType = paymentType
        
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
    
    @PostMapping("/settle")
    @Operation(summary = "Settle payment for completed transaction", description = "Mark the last transaction as paid and reset to IDLE")
    fun settle(
        @RequestParam(defaultValue = "CARD") paymentMethod: String
    ): ResponseEntity<*> {
        logger.info("💳 Settle payment request: method={}", paymentMethod)
        
        // Find the latest unpaid transaction
        val unpaidTransaction = transactionRepository.findFirstByDispenserAddressAndPaymentStatusOrderByTimestampDesc(1, "PENDING")
        
        if (unpaidTransaction == null) {
            logger.warn("⚠️ No unpaid transaction found")
            return ResponseEntity.status(404).body(mapOf(
                "error" to "NO_UNPAID_TRANSACTION",
                "message" to "Ingen ubetalt transaksjon funnet"
            ))
        }
        
        // Update payment status
        val updated = transactionService.updatePaymentStatus(unpaidTransaction.transactionId!!, paymentMethod, "PAID")
        logger.info("✅ Transaction {} marked as PAID with method {}", updated?.transactionId, paymentMethod)
        
        // Reset to IDLE
        state = DispenserState.IDLE
        litres = 0.0
        amountToPay = 0.0
        
        return ResponseEntity.ok(mapOf(
            "status" to "PAID",
            "message" to "Betaling fullført",
            "transaction" to mapOf(
                "id" to updated?.transactionId,
                "amount" to updated?.amountKr,
                "liters" to updated?.volumeLiters,
                "paymentMethod" to paymentMethod
            )
        ))
    }

    @PostMapping("/product-select")
    @Operation(summary = "Select product/pistol", description = "VB6-compatible product selection before pricing operations")
    fun selectProduct(
        @RequestBody request: ProductSelectRequest
    ): ResponseEntity<ProtocolResponse> {
        logger.info("Product selection: address={}, product={}", request.address, request.product)
        return ResponseEntity.ok(
            ProtocolResponse(
                success = true,
                message = "Product selected: ${request.product}",
                responseCode = "0x1E" // OK response
            )
        )
    }

    @PostMapping("/program-price")
    @Operation(summary = "Program price per liter", description = "VB6-compatible price programming with LSB-first format")
    fun programPrice(
        @RequestBody request: PriceProgramRequest
    ): ResponseEntity<ProtocolResponse> {
        logger.info("Price programming: address={}, price={}", request.address, request.priceKrPerLiter)
        // In real implementation, this would set the price on the dispenser
        return ResponseEntity.ok(
            ProtocolResponse(
                success = true,
                message = "Price programmed: ${request.priceKrPerLiter} kr/L",
                responseCode = "0x1E" // OK response
            )
        )
    }

    @PostMapping("/program-amount")
    @Operation(summary = "Program amount preset", description = "Set amount preset in øre (cents)")
    fun programAmount(
        @RequestBody request: AmountPresetRequest
    ): ResponseEntity<ProtocolResponse> {
        logger.info("Amount preset: address={}, amount={} øre", request.address, request.amountOre)
        return ResponseEntity.ok(
            ProtocolResponse(
                success = true,
                message = "Amount preset: ${request.amountOre} øre",
                responseCode = "0x1E"
            )
        )
    }

    @PostMapping("/program-volume")
    @Operation(summary = "Program volume preset", description = "Set volume preset in liters")
    fun programVolume(
        @RequestBody request: VolumePresetRequest
    ): ResponseEntity<ProtocolResponse> {
        logger.info("Volume preset: address={}, volume={} L", request.address, request.volumeLiters)
        return ResponseEntity.ok(
            ProtocolResponse(
                success = true,
                message = "Volume preset: ${request.volumeLiters} L",
                responseCode = "0x1E"
            )
        )
    }

    @GetMapping("/volume")
    @Operation(summary = "Get current volume", description = "Query current delivery volume")
    fun getCurrentVolume(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<VolumeResponse> {
        return ResponseEntity.ok(
            VolumeResponse(
                address = address,
                currentVolumeLiters = litres,
                deliveryInProgress = state == DispenserState.DELIVERING
            )
        )
    }

    @GetMapping("/tank")
    @Operation(summary = "Get tank status", description = "Query tank level and pump info")
    fun getTankStatus(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<TankResponse> {
        return ResponseEntity.ok(
            TankResponse(
                address = address,
                tankLevelPercent = 85.5, // Simulated tank level
                pumpInfo = "ARK-3600 Emulator",
                connected = true
            )
        )
    }

    @GetMapping("/price")
    @Operation(summary = "Get current price", description = "Query active price per liter")
    fun getCurrentPrice(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<PriceResponse> {
        return ResponseEntity.ok(
            PriceResponse(
                address = address,
                priceKrPerLiter = pricePerLitre,
                includesRoadTax = true
            )
        )
    }

    @PostMapping("/linetest")
    @Operation(summary = "Communication line test", description = "VB6-compatible communication verification")
    fun lineTest(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<ProtocolResponse> {
        return ResponseEntity.ok(
            ProtocolResponse(
                success = true,
                message = "Line test OK for address $address",
                responseCode = "0x1E"
            )
        )
    }

    @GetMapping("/error")
    @Operation(summary = "Get error status", description = "Query error codes from dispenser")
    fun getErrorStatus(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.ok(
            ErrorResponse(
                address = address,
                hasError = false,
                mainErrorCode = "00",
                subErrorCode = "00",
                errorDescription = "No errors"
            )
        )
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

    // VB6-compatible protocol request/response DTOs
    data class ProductSelectRequest(
        val address: Int = 1,
        val product: String // e.g. "0x30" for pistol selection
    )

    data class PriceProgramRequest(
        val address: Int = 1,
        val priceKrPerLiter: String // e.g. "15.90" - will be encoded LSB-first
    )

    data class AmountPresetRequest(
        val address: Int = 1,
        val amountOre: Int // Amount in øre (cents)
    )

    data class VolumePresetRequest(
        val address: Int = 1,
        val volumeLiters: Double // Volume in liters
    )

    data class ProtocolResponse(
        val success: Boolean,
        val message: String,
        val responseCode: String // Hex response code from protocol
    )

    data class VolumeResponse(
        val address: Int,
        val currentVolumeLiters: Double,
        val deliveryInProgress: Boolean
    )

    data class TankResponse(
        val address: Int,
        val tankLevelPercent: Double,
        val pumpInfo: String,
        val connected: Boolean
    )

    data class PriceResponse(
        val address: Int,
        val priceKrPerLiter: Double,
        val includesRoadTax: Boolean
    )

    data class ErrorResponse(
        val address: Int,
        val hasError: Boolean,
        val mainErrorCode: String, // 2-digit hex code
        val subErrorCode: String,  // 2-digit hex code
        val errorDescription: String
    )
}
