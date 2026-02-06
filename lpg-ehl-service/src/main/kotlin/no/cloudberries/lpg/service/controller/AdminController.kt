package no.cloudberries.lpg.service.controller

import no.cloudberries.lpg.service.pump.PumpAuthorizationService
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin endpoints for maintenance and debugging.
 * 
 * Disse endepunktene er tilgjengelig i ALLE moduser (LAB og FIELD).
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val pumpStateService: PumpStateService,
    private val authorizationService: PumpAuthorizationService
) {
    private val logger = LoggerFactory.getLogger(AdminController::class.java)
    
    /**
     * Cleanup ALL stuck/active authorizations.
     * 
     * VIKTIG: Bruk dette når autorisasjoner har hengt seg fra tidligere kjøringer.
     * Dette vil:
     * - Kansellere alle PENDING/AUTHORIZED/PUMPING autorisasjoner
     * - Resette alle pumper til IDLE
     * 
     * Bruk: POST /api/v1/admin/cleanup-authorizations
     */
    @PostMapping("/cleanup-authorizations")
    fun cleanupStuckAuthorizations(): ResponseEntity<Map<String, Any>> {
        logger.warn("🧹 ADMIN CLEANUP: Cancelling all stuck authorizations...")
        
        try {
            val cancelledCount = authorizationService.cancelAllStuckAuthorizations()
            
            // Reset all pumps to IDLE
            pumpStateService.resetAllPumps()
            
            logger.info("✅ Cleanup completed: $cancelledCount authorization(s) cancelled, all pumps reset to IDLE")
            
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Cleanup completed successfully",
                "cancelledCount" to cancelledCount
            ))
        } catch (e: Exception) {
            logger.error("❌ Cleanup failed: ${e.message}", e)
            return ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "error" to "CLEANUP_FAILED",
                "message" to (e.message ?: "Failed to cleanup authorizations")
            ))
        }
    }
    
    /**
     * Health check endpoint for admin operations.
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "ok",
            "message" to "Admin endpoints are available"
        ))
    }
}
