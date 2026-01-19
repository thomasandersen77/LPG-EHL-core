package no.cloudberries.lpg.api.controller

import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Pump control endpoints for the Control Panel ("Fri Pumpe" testing).
 * 
 * These endpoints allow field testing of dispensers without PLS/terminal.
 */
@RestController
@RequestMapping("/api/v1/emulator")
class PumpController(
    private val pumpStateService: PumpStateService
) {
    private val logger = LoggerFactory.getLogger(PumpController::class.java)
    
    /**
     * Get current pump status.
     */
    @GetMapping("/pump/{address}/status")
    fun getPumpStatus(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        val status = pumpStateService.getStatus(address)
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
     * "Fri pumpe" - Unblock dispenser and start pumping.
     */
    @PostMapping("/pump/{address}/unblock")
    fun unblockPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("🔓 FRI PUMPE: Unblock request for address $address")
        
        val result = pumpStateService.unblock(address)
        
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
     */
    @PostMapping("/pump/{address}/block")
    fun blockPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("🛑 FRI PUMPE: Block request for address $address")
        
        val result = pumpStateService.block(address)
        
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
    
    /**
     * Settle pending transaction and reset dispenser to IDLE.
     */
    @PostMapping("/settle/{id}")
    fun settle(
        @PathVariable id: Int,
        @RequestParam(defaultValue = "CARD") method: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("💳 Settle payment request: dispenserId=$id, method=$method")
        
        // Map frontend payment methods
        val emulatorMethod = when (method.uppercase()) {
            "CASH" -> "CASH"
            "CARD" -> "CARD"
            "CREDIT" -> "CREDIT"
            else -> {
                logger.warn("⚠️ Invalid payment method: $method")
                return ResponseEntity.badRequest().body(mapOf(
                    "status" to "error",
                    "message" to "Invalid payment method. Use CASH, CARD, or CREDIT"
                ))
            }
        }
        
        val settledTransaction = pumpStateService.settle(id, emulatorMethod)
        
        return if (settledTransaction != null) {
            logger.info("✅ Payment settled: ${settledTransaction.amountNok} NOK, ${settledTransaction.liters} L")
            ResponseEntity.ok(mapOf(
                "status" to "settled",
                "method" to method,
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
    
    /**
     * Reset pump to IDLE state.
     */
    @PostMapping("/pump/{address}/reset")
    fun resetPump(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("🔄 Reset pump request for address $address")
        pumpStateService.reset(address)
        val status = pumpStateService.getStatus(address)
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Pumpe nullstilt",
            "state" to status.state
        ))
    }
}
