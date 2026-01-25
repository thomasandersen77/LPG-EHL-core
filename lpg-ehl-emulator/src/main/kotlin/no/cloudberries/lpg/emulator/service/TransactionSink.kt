package no.cloudberries.lpg.emulator.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * Data class representing a completed fuel transaction ready for persistence.
 * 
 * ## Multi-Station Support:
 * Each transaction now includes station and edge identifiers to support
 * multiple stations and dispensers in the cloud system.
 * 
 * @property stationId Station identifier (e.g., "S001", "S002") - identifies which physical station
 * @property edgeId Edge device identifier (unique per station) - identifies the hardware running this code
 * @property dispenserId Dispenser ID within the station (e.g., "D001", "D002")
 * @property dispenserAddress Legacy EHL address (1-255) - for hardware protocol compatibility
 * @property liters Volume dispensed in liters
 * @property amountNok Total amount in Norwegian kroner
 * @property unitPrice Price per liter in kroner
 * @property finishedAt Timestamp when transaction completed
 * @property idempotencyKey Unique key to prevent duplicate saves (UUID)
 * @property databaseId Cloud database ID after successful sync (set by cloud API)
 */
data class CompletedTransaction(
    val stationId: String,
    val edgeId: String,
    val dispenserId: String,
    val dispenserAddress: Int,
    val liters: Double,
    val amountNok: Double,
    val unitPrice: Double,
    val finishedAt: Instant,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    var databaseId: String? = null  // Set after successful save
)

/**
 * Asynchronous transaction persistence sink with retry logic.
 * 
 * This component decouples transaction completion from database persistence:
 * - STOP/BLOCK commands return immediately to Windows client
 * - Transactions are enqueued and saved in background
 * - Failed saves are automatically retried with exponential backoff
 * 
 * Design:
 * - Channel-based queue (buffered, non-blocking enqueue)
 * - Single consumer coroutine for sequential processing
 * - Retry with delay on failure (2s, 4s, 8s, ...)
 */
@Component
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class TransactionSink(
    private val persistenceService: TransactionPersistenceService
) {
    private val logger = LoggerFactory.getLogger(TransactionSink::class.java)
    
    // Coroutine scope for background processing
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Buffered channel - non-blocking enqueue up to capacity
    private val queue = Channel<CompletedTransaction>(capacity = Channel.BUFFERED)
    
    // Track already-saved transaction IDs for idempotency
    private val savedTransactionIds = Collections.synchronizedSet(mutableSetOf<String>())
    
    private var consumerJob: Job? = null
    
    @PostConstruct
    fun start() {
        logger.info("🚀 TransactionSink starting...")
        consumerJob = scope.launch {
            consumeTransactions()
        }
        logger.info("✅ TransactionSink consumer started")
    }
    
    @PreDestroy
    fun stop() {
        logger.info("🛑 TransactionSink stopping...")
        runBlocking {
            queue.close()
            consumerJob?.join()
        }
        scope.cancel()
        logger.info("✅ TransactionSink stopped")
    }
    
    /**
     * Enqueue a transaction for asynchronous persistence.
     * Non-blocking - returns immediately.
     * 
     * @param transaction The completed transaction to save
     */
    fun enqueue(transaction: CompletedTransaction) {
        // Idempotency check - skip if already saved or queued
        if (savedTransactionIds.contains(transaction.idempotencyKey)) {
            logger.debug("⚠️ Transaction ${transaction.idempotencyKey} already saved, skipping duplicate")
            return
        }
        
        savedTransactionIds.add(transaction.idempotencyKey)
        
        val result = queue.trySend(transaction)
        if (result.isSuccess) {
            logger.debug("📥 Transaction enqueued: ${transaction.idempotencyKey} (${transaction.liters}L)")
        } else {
            logger.error("❌ Failed to enqueue transaction: queue full or closed")
            // Remove from set if enqueue failed so it can be retried
            savedTransactionIds.remove(transaction.idempotencyKey)
        }
    }
    
    /**
     * Background consumer coroutine.
     * Processes transactions sequentially with retry on failure.
     */
    private suspend fun consumeTransactions() {
        logger.info("👂 Transaction consumer listening...")
        
        for (transaction in queue) {
            var retryCount = 0
            var success = false
            
            while (!success && retryCount < 5) {
                try {
                    logger.info("💾 Saving transaction ${transaction.idempotencyKey} (attempt ${retryCount + 1}) [${transaction.stationId}/${transaction.dispenserId}]")
                    
                    val databaseId = persistenceService.saveTransaction(
                        stationId = transaction.stationId,
                        edgeId = transaction.edgeId,
                        dispenserId = transaction.dispenserId,
                        dispenserAddress = transaction.dispenserAddress,
                        volumeDeciliters = (transaction.liters * 10).toInt(),
                        amountOre = (transaction.amountNok * 100).toInt(),
                        pricePerLiter = (transaction.unitPrice * 100).toInt()
                    )
                    
                    if (databaseId != null) {
                        transaction.databaseId = databaseId
                        logger.info("✅ Transaction ${transaction.idempotencyKey} saved with database ID: $databaseId")
                        success = true
                    } else {
                        throw Exception("API returned null database ID")
                    }
                    
                } catch (e: Exception) {
                    retryCount++
                    val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(8000)
                    
                    logger.error(
                        "❌ Failed to save transaction ${transaction.idempotencyKey} " +
                        "(attempt $retryCount): ${e.message}"
                    )
                    
                    if (retryCount < 5) {
                        logger.info("⏳ Retrying in ${delayMs}ms...")
                        delay(delayMs)
                    } else {
                        logger.error(
                            "💥 CRITICAL: Transaction ${transaction.idempotencyKey} " +
                            "failed after 5 attempts. Data: ${transaction.liters}L, ${transaction.amountNok} kr"
                        )
                    }
                }
            }
        }
        
        logger.info("👋 Transaction consumer stopped")
    }
}
