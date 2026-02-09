package no.cloudberries.lpg.api.controller

import no.cloudberries.lpg.service.pump.PumpAuthorizationService
import no.cloudberries.lpg.service.pump.PumpStateService
import no.cloudberries.lpg.service.transaction.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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
    private val authorizationService: PumpAuthorizationService,
    private val transactionRepository: TransactionRepository
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
     * Mark all unpaid (PENDING) transactions as PAID.
     * 
     * VIKTIG: Bruk dette når det finnes ubetalte transaksjoner fra tidligere testing
     * som blokkerer nye fyllinger.
     * 
     * Bruk: POST /api/v1/admin/mark-all-paid
     */
    @PostMapping("/mark-all-paid")
    @Transactional
    fun markAllTransactionsPaid(): ResponseEntity<Map<String, Any>> {
        logger.warn("🧹 ADMIN: Marking all PENDING transactions as PAID...")
        
        try {
            val pendingTransactions = transactionRepository.findByPaymentStatus("PENDING")
            
            if (pendingTransactions.isEmpty()) {
                logger.info("ℹ️ No pending transactions found")
                return ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "No pending transactions found",
                    "updatedCount" to 0
                ))
            }
            
            var updatedCount = 0
            pendingTransactions.forEach { tx ->
                tx.paymentStatus = "PAID"
                tx.paymentType = "ADMIN_OVERRIDE"
                transactionRepository.save(tx)
                updatedCount++
                logger.info("   ✅ Marked transaction {} as PAID", tx.transactionId)
            }
            
            logger.info("✅ Marked {} transaction(s) as PAID", updatedCount)
            
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Marked $updatedCount transaction(s) as PAID",
                "updatedCount" to updatedCount,
                "transactions" to pendingTransactions.map { it.transactionId }
            ))
        } catch (e: Exception) {
            logger.error("❌ Failed to mark transactions as paid: ${e.message}", e)
            return ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "error" to "UPDATE_FAILED",
                "message" to (e.message ?: "Failed to update transactions")
            ))
        }
    }
    
    /**
     * Full system reset: Mark all transactions as paid AND reset dispenser state.
     * 
     * Bruk: POST /api/v1/admin/full-reset
     */
    @PostMapping("/full-reset")
    @Transactional
    fun fullSystemReset(): ResponseEntity<Map<String, Any>> {
        logger.warn("🔄 ADMIN FULL RESET: Resetting entire system...")
        
        try {
            // 1. Mark all pending transactions as paid
            val pendingTransactions = transactionRepository.findByPaymentStatus("PENDING")
            pendingTransactions.forEach { tx ->
                tx.paymentStatus = "PAID"
                tx.paymentType = "ADMIN_RESET"
                transactionRepository.save(tx)
            }
            
            // 2. Cancel stuck authorizations
            val cancelledAuth = authorizationService.cancelAllStuckAuthorizations()
            
            // 3. Reset all pumps
            pumpStateService.resetAllPumps()
            
            logger.info("✅ Full reset completed: {} transactions marked paid, {} authorizations cancelled", 
                pendingTransactions.size, cancelledAuth)
            
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Full system reset completed",
                "transactionsMarkedPaid" to pendingTransactions.size,
                "authorizationsCancelled" to cancelledAuth
            ))
        } catch (e: Exception) {
            logger.error("❌ Full reset failed: ${e.message}", e)
            return ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "error" to "RESET_FAILED",
                "message" to (e.message ?: "Failed to reset system")
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
