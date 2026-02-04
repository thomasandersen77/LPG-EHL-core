package no.cloudberries.lpg.service.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.cloudberries.lpg.service.pump.PumpAuthorizationService
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Pump control endpoints for the Control Panel ("Fri Pumpe" testing).
 * 
 * Disse endepunktene støtter både:
 * 1. Direkte pumpe-kontroll (UNBLOCK/BLOCK) for testing i lab
 * 2. Kortdragning-simulering som går via autorisasjons-tabellen
 * 
 * For felt-testing med headless:
 * - Bruk /card-swipe endpoint for å simulere kortdragning
 * - Headless-appen vil oppdage PENDING autorisasjon og sende UNBLOCK
 * 
 * NOTE: Denne controlleren er KUN aktiv i LAB mode (lpg.mode=LAB).
 */
@RestController
@RequestMapping("/api/v1/emulator")
class PumpController(
    private val pumpStateService: PumpStateService,
    private val authorizationService: PumpAuthorizationService
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
     * Start pumping (simulerer at kunde løfter pistol og trykker på knapp).
     * 
     * I LAB MODE: Brukes av /control GUI for å simulere nozzle lift.
     * I FIELD MODE: Ikke brukt - pumping trigges av hardware state (0x06/0x07).
     */
    @PostMapping("/pump/{address}/start-pumping")
    fun startPumping(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("⛽ START PUMPING: Request for address $address (GUI simulation)")
        
        val result = pumpStateService.simulateStartPumping(address)
        
        return result.fold(
            onSuccess = { status ->
                logger.info("✅ Pumping started: state=${status.state}")
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "Pumping startet (simulert)",
                    "state" to status.state,
                    "volumeLitres" to status.volumeLitres,
                    "amountKr" to status.amountKr,
                    "pricePerLitreKr" to status.pricePerLitreKr
                ))
            },
            onFailure = { error ->
                logger.warn("❌ Start pumping failed: ${error.message}")
                ResponseEntity.status(409).body(mapOf(
                    "success" to false,
                    "error" to "START_PUMPING_FAILED",
                    "message" to (error.message ?: "Kunne ikke starte pumping")
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
    
    // ==================== KORTDRAGNING-SIMULERING ====================
    
    /**
     * Simuler kortdragning.
     * 
     * VIKTIG: Denne endepunktet sender ALDRI UNBLOCK til pumpen!
     * - Oppretter authorization med 60s timeout
     * - Pumpe-state settes til AUTHORIZED_WAITING
     * - Bruker må trykke "FRI DISPENSER" (POST /pump/{address}/unblock) for å frigjøre pumpen
     * 
     * Request body:
     * - maxAmountKr: Maks beløp å reservere (default: 2000)
     * - triggeredBy: Hvem/hva som trigget (for logging)
     * - paymentMethod: SIMULATION, CARD, CREDIT, CASH
     */
    @PostMapping("/pump/{address}/card-swipe")
    fun simulateCardSwipe(
        @PathVariable address: Int,
        @RequestBody(required = false) request: CardSwipeRequest?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("💳 SIMULER KORTDRAGNING: Dispenser $address")
        
        try {
            // Opprette authorization med 60s timeout - IKKE send UNBLOCK!
            val auth = authorizationService.simulateCardSwipe(
                dispenserAddress = address,
                maxAmountKr = request?.maxAmountKr ?: 2000.0,
                triggeredBy = request?.triggeredBy ?: "WEBAPP_SIMULATION",
                paymentMethod = request?.paymentMethod ?: "SIMULATION"
            )
            
            logger.info("✅ Autorisasjon opprettet: ${auth.authorizationId}")
            logger.info("⏱️ 60s nedtelling startet - venter på FRI DISPENSER")
            
            // Sett pump state til AUTHORIZED_WAITING (kort dratt, venter på FRI DISPENSER)
            pumpStateService.setAuthorizedWaiting(address, auth.authorizationId)
            
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Kortdragning simulert - trykk FRI DISPENSER for å frigjøre pumpen",
                "authorization" to mapOf(
                    "authorizationId" to auth.authorizationId.toString(),
                    "dispenserAddress" to auth.dispenserAddress,
                    "status" to auth.status.name,
                    "maxAmountKr" to auth.maxAmountKr,
                    "pricePerLiterKr" to auth.pricePerLiterKr,
                    "triggeredBy" to auth.triggeredBy,
                    "createdAt" to auth.createdAt.toString(),
                    "timeoutSeconds" to 60
                )
            ))
        } catch (e: IllegalStateException) {
            logger.warn("⚠️ Kortdragning avvist: ${e.message}")
            return ResponseEntity.status(409).body(mapOf(
                "success" to false,
                "error" to "ACTIVE_AUTHORIZATION_EXISTS",
                "message" to (e.message ?: "Det finnes allerede en aktiv autorisasjon")
            ))
        } catch (e: Exception) {
            logger.error("❌ Feil ved kortdragning: ${e.message}", e)
            return ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "error" to "CARD_SWIPE_FAILED",
                "message" to (e.message ?: "Ukjent feil ved kortdragning")
            ))
        }
    }
    
    /**
     * Hent status for gjeldende autorisasjon.
     */
    @GetMapping("/pump/{address}/authorization")
    fun getAuthorization(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        val auth = authorizationService.findActiveAuthorization(address)
        
        return if (auth != null) {
            ResponseEntity.ok(mapOf(
                "hasActiveAuthorization" to true,
                "authorization" to mapOf(
                    "authorizationId" to auth.authorizationId.toString(),
                    "dispenserAddress" to auth.dispenserAddress,
                    "status" to auth.status.name,
                    "maxAmountKr" to auth.maxAmountKr,
                    "pricePerLiterKr" to auth.pricePerLiterKr,
                    "actualVolumeLiters" to (auth.actualVolumeLiters ?: 0.0),
                    "actualAmountKr" to (auth.actualAmountKr ?: 0.0),
                    "triggeredBy" to auth.triggeredBy,
                    "paymentMethod" to auth.paymentMethod,
                    "createdAt" to auth.createdAt.toString(),
                    "authorizedAt" to auth.authorizedAt?.toString()
                )
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "hasActiveAuthorization" to false,
                "message" to "Ingen aktiv autorisasjon"
            ))
        }
    }
    
    /**
     * Bekreft betaling og avslutt autorisasjon.
     * 
     * Kalles når pumping er ferdig og betaling er gjennomført.
     * Resetter pumpe til IDLE for neste kunde.
     */
    @PostMapping("/pump/{address}/confirm-payment")
    fun confirmPayment(
        @PathVariable address: Int,
        @RequestBody(required = false) request: ConfirmPaymentRequest?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("💳 BEKREFT BETALING: Dispenser $address")
        
        val auth = authorizationService.findActiveAuthorization(address)
        
        if (auth == null) {
            return ResponseEntity.status(404).body(mapOf(
                "success" to false,
                "error" to "NO_ACTIVE_AUTHORIZATION",
                "message" to "Ingen aktiv autorisasjon å avslutte"
            ))
        }
        
        try {
            // Bekreft betaling og marker autorisasjon som COMPLETED
            val paymentMethod = request?.paymentMethod ?: auth.paymentMethod ?: "SIMULATION"
            val completed = authorizationService.confirmPayment(
                authorizationId = auth.authorizationId,
                paymentMethod = paymentMethod
            )
            
            logger.info("✅ Betaling bekreftet: ${completed.actualVolumeLiters} L = ${completed.actualAmountKr} kr")
            
            // VIKTIG: Kall settle() for å resette pumpe til IDLE for neste kunde
            val settled = pumpStateService.settle(address, paymentMethod)
            if (settled != null) {
                logger.info("🚀 Pumpe $address frigitt for neste kunde")
            }
            
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Betaling bekreftet - pumpe frigitt",
                "authorization" to mapOf(
                    "authorizationId" to completed.authorizationId.toString(),
                    "status" to completed.status.name,
                    "actualVolumeLiters" to (completed.actualVolumeLiters ?: 0.0),
                    "actualAmountKr" to (completed.actualAmountKr ?: 0.0),
                    "completedAt" to completed.completedAt?.toString()
                )
            ))
        } catch (e: IllegalStateException) {
            logger.warn("⚠️ Kan ikke bekrefte betaling: ${e.message}")
            return ResponseEntity.status(409).body(mapOf(
                "success" to false,
                "error" to "INVALID_STATUS",
                "message" to (e.message ?: "Autorisasjonen er ikke i riktig status")
            ))
        }
    }
    
    /**
     * Kanseller aktiv autorisasjon.
     */
    @PostMapping("/pump/{address}/cancel-authorization")
    fun cancelAuthorization(
        @PathVariable address: Int,
        @RequestBody(required = false) request: CancelRequest?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("❌ KANSELLER AUTORISASJON: Dispenser $address")
        
        val auth = authorizationService.findActiveAuthorization(address)
        
        if (auth == null) {
            return ResponseEntity.status(404).body(mapOf(
                "success" to false,
                "error" to "NO_ACTIVE_AUTHORIZATION",
                "message" to "Ingen aktiv autorisasjon å kansellere"
            ))
        }
        
        val cancelled = authorizationService.cancel(
            authorizationId = auth.authorizationId,
            reason = request?.reason ?: "Manuell kansellering fra GUI"
        )
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Autorisasjon kansellert",
            "authorization" to mapOf(
                "authorizationId" to cancelled.authorizationId.toString(),
                "status" to cancelled.status.name,
                "errorMessage" to cancelled.errorMessage
            )
        ))
    }
}

// Request DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class CardSwipeRequest(
    val maxAmountKr: Double? = 2000.0,
    val triggeredBy: String? = "WEBAPP_SIMULATION",
    val paymentMethod: String? = "SIMULATION"
    // NOTE: 'immediate' parameter removed - card-swipe should NEVER auto-unblock
    // User must explicitly click "FRI DISPENSER" to send UNBLOCK
)

data class ConfirmPaymentRequest(
    val paymentMethod: String? = "SIMULATION"
)

data class CancelRequest(
    val reason: String? = "Manuell kansellering"
)
