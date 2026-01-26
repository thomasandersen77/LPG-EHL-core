package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for standalone emulator TCP server.
 * Only loaded when emulator.standalone.enabled=true.
 */
@RestController
@RequestMapping("/api/v1/emulator")
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class EmulatorController(
    private val emulatorService: EmulatorService,
    private val scenarioService: EmulatorScenarioService,
    private val logBuffer: LogBuffer
) {
    private val logger = LoggerFactory.getLogger(EmulatorController::class.java)

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(emulatorService.getStatus())
    }

    @PostMapping("/reset")
    fun reset(): ResponseEntity<Map<String, String>> {
        emulatorService.reset()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }

    data class SetScenarioRequest(
        val dispenserAddress: Int,
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): ResponseEntity<EmulatorStatus> {
        logger.info("🎬 Setting emulator scenario: address=${body.dispenserAddress}, scenario=${body.scenario}")
        scenarioService.setScenario(body.dispenserAddress, body.scenario)
        return ResponseEntity.ok(scenarioService.status(body.dispenserAddress))
    }

    @PostMapping("/reset/{address}")
    fun resetScenario(@PathVariable("address") dispenserAddress: Int): ResponseEntity<EmulatorStatus> {
        logger.info("🔄 Resetting emulator scenario: address=${dispenserAddress}")
        scenarioService.reset(dispenserAddress)
        return ResponseEntity.ok(scenarioService.status(dispenserAddress))
    }

    @GetMapping("/status/{address}")
    fun getScenarioStatus(@PathVariable("address") dispenserAddress: Int) =
        ResponseEntity.ok(scenarioService.status(dispenserAddress))
    
    @GetMapping("/internal/logs")
    fun getLogs(@RequestParam(defaultValue = "500") limit: Int): ResponseEntity<Map<String, Any>> {
        val logs = logBuffer.getRecentLogs(limit)
        return ResponseEntity.ok(mapOf(
            "count" to logs.size,
            "maxSize" to 1000,
            "logs" to logs
        ))
    }
    
    @DeleteMapping("/internal/logs")
    fun clearLogs(): ResponseEntity<Map<String, String>> {
        logBuffer.clear()
        return ResponseEntity.ok(mapOf("status" to "cleared"))
    }
    
    /**
     * Settle pending transaction and reset dispenser to IDLE.
     * This endpoint now BROADCASTS reset to Windows Dispenserkontroll.
     * 
     * @param id Dispenser address (currently only 1 is supported)
     * @param method Payment method: "CARD" (default) or "CREDIT"
     * @return Settled transaction details or error message
     */
    @PostMapping("/settle/{id}")
    fun settle(
        @PathVariable id: Int,
        @RequestParam(defaultValue = "CARD") method: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("💳 Settle payment request: dispenserId=$id, method=$method")
        
        // Map frontend payment methods to emulator payment methods
        val emulatorMethod = when (method.uppercase()) {
            "CARD" -> "CARD"
            "CREDIT" -> "CREDIT"
            else -> {
                logger.warn("⚠️ Invalid payment method: $method")
                return ResponseEntity.badRequest().body(mapOf(
                    "status" to "error",
                    "message" to "Invalid payment method. Use CARD or CREDIT"
                ))
            }
        }
        
        // Use new broadcast method instead of direct settle
        val settledTransaction = emulatorService.settleAndBroadcast(emulatorMethod)
        
        return if (settledTransaction != null) {
            logger.info("✅ Payment settled: ${settledTransaction.amountNok} NOK, ${settledTransaction.liters} L")
            ResponseEntity.ok(mapOf(
                "status" to "settled",
                "method" to method,
                "windowsBroadcastSent" to true,  // NEW: Indicate broadcast happened
                "transaction" to mapOf(
                    "dispenserId" to settledTransaction.dispenserId,
                    "liters" to settledTransaction.liters,
                    "amountNok" to settledTransaction.amountNok,
                    "unitPrice" to settledTransaction.unitPrice,
                    "finishedAt" to settledTransaction.finishedAt.toString(),
                    "idempotencyKey" to settledTransaction.idempotencyKey
                )
            ))
        } else {
            logger.info("ℹ️ No pending transaction to settle")
            ResponseEntity.ok(mapOf(
                "status" to "no_pending_transaction",
                "message" to "No pending transaction to settle"
            ))
        }
    }
    
    // ==========================================================================
    // FRI PUMPE API - Direct pump control for field testing
    // ==========================================================================
    
    /**
     * Get current pump status.
     * 
     * @param address Dispenser address (currently only 1 is supported)
     */
    @GetMapping("/pump/{address}/status")
    fun getPumpStatus(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        val status = emulatorService.getPumpStatus()
        return ResponseEntity.ok(mapOf(
            "state" to status.state,
            "address" to status.address,
            "volumeLitres" to status.volumeLitres,
            "amountKr" to status.amountKr,
            "pricePerLitreKr" to status.pricePerLitreKr,
            "nozzleLifted" to status.nozzleLifted,
            "hasPendingTransaction" to status.hasPendingTransaction
        ))
    }
    
    /**
     * "Fri pumpe" - Unlock dispenser and start pumping.
     * Used for field testing when no PLS/terminal is available.
     * 
     * @param address Dispenser address (currently only 1 is supported)
     */
    @PostMapping("/pump/{address}/unblock")
    fun unblockPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("🔓 FRI PUMPE: Unblock request for address $address")
        
        val result = emulatorService.unblockPump()
        
        return result.fold(
            onSuccess = { status ->
                logger.info("✅ Pump unblocked: state=${status.state}")
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "Pumpe frigitt - levering startet",
                    "state" to status.state,
                    "volumeLitres" to status.volumeLitres,
                    "amountKr" to status.amountKr,
                    "pricePerLitreKr" to status.pricePerLitreKr
                ))
            },
            onFailure = { error ->
                logger.warn("❌ Unblock failed: ${error.message}")
                ResponseEntity.status(409).body(mapOf(
                    "success" to false,
                    "error" to "UNBLOCK_FAILED",
                    "message" to (error.message ?: "Kunne ikke frigjøre pumpen")
                ))
            }
        )
    }
    
    /**
     * "Stopp pumpe" - Block dispenser and stop pumping.
     * Used for field testing when no PLS/terminal is available.
     * 
     * @param address Dispenser address (currently only 1 is supported)
     */
    @PostMapping("/pump/{address}/block")
    fun blockPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("🛑 FRI PUMPE: Block request for address $address")
        
        val result = emulatorService.blockPump()
        
        return result.fold(
            onSuccess = { status ->
                logger.info("✅ Pump blocked: state=${status.state}, volume=${status.volumeLitres}L")
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to if (status.hasPendingTransaction) 
                        "Levering stoppet - venter på betaling" 
                        else "Pumpe stoppet",
                    "state" to status.state,
                    "volumeLitres" to status.volumeLitres,
                    "amountKr" to status.amountKr,
                    "pricePerLitreKr" to status.pricePerLitreKr,
                    "hasPendingTransaction" to status.hasPendingTransaction
                ))
            },
            onFailure = { error ->
                logger.warn("❌ Block failed: ${error.message}")
                ResponseEntity.status(500).body(mapOf(
                    "success" to false,
                    "error" to "BLOCK_FAILED",
                    "message" to (error.message ?: "Kunne ikke stoppe pumpen")
                ))
            }
        )
    }
}
