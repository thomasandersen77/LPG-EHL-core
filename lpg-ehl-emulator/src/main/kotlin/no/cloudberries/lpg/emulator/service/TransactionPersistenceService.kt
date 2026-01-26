package no.cloudberries.lpg.emulator.service

import no.cloudberries.lpg.emulator.api.LpgApiClient
import no.cloudberries.lpg.emulator.api.SaveTransactionRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class TransactionPersistenceService(
    private val lpgApiClient: LpgApiClient
) {
    private val logger = LoggerFactory.getLogger(TransactionPersistenceService::class.java)
    
    /**
     * Save a transaction to the database via API
     * 
     * Multi-Station Support:
     * Includes station, edge, and dispenser identifiers for proper multi-tenant tracking.
     * 
     * @param stationId Station identifier (e.g., "S001")
     * @param edgeId Edge device identifier
     * @param dispenserId Dispenser identifier (e.g., "D001")
     * @param dispenserAddress The EHL address of the dispenser (1, 2, or 3)
     * @param volumeDeciliters Volume in deciliters (dl)
     * @param amountOre Amount in øre
     * @param pricePerLiter Price per liter in cents/øre
     * @return Database transaction ID (UUID) or null if failed
     */
    fun saveTransaction(
        stationId: String,
        edgeId: String,
        dispenserId: String,
        dispenserAddress: Int,
        volumeDeciliters: Int,
        amountOre: Int,
        pricePerLiter: Int
    ): String? {
        logger.info("Saving transaction: Station=$stationId, Dispenser=$dispenserId, Volume=${volumeDeciliters/10.0}L, Amount=${amountOre/100.0} kr")
        
        val request = SaveTransactionRequest(
            stationId = stationId,
            edgeId = edgeId,
            dispenserId = dispenserId,
            dispenserAddress = dispenserAddress,
            volumeDeciliters = volumeDeciliters,
            amountOre = amountOre,
            pricePerLiter = pricePerLiter
        )
        
        val databaseId = lpgApiClient.saveTransaction(request)
        
        if (databaseId != null) {
            logger.info("✅ Transaction saved successfully to database with ID: $databaseId")
        } else {
            logger.error("❌ Failed to save transaction to database")
        }
        
        return databaseId
    }
}
