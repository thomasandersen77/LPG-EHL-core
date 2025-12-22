package no.cloudberries.lpg.emulator.service

import no.cloudberries.lpg.emulator.api.LpgApiClient
import no.cloudberries.lpg.emulator.api.SaveTransactionRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TransactionPersistenceService(
    private val lpgApiClient: LpgApiClient
) {
    private val logger = LoggerFactory.getLogger(TransactionPersistenceService::class.java)
    
    /**
     * Save a transaction to the database via API
     * 
     * @param dispenserAddress The address of the dispenser (1, 2, or 3)
     * @param volumeDeciliters Volume in deciliters (dl)
     * @param amountOre Amount in øre
     * @param pricePerLiter Price per liter in cents/øre
     */
    fun saveTransaction(
        dispenserAddress: Int,
        volumeDeciliters: Int,
        amountOre: Int,
        pricePerLiter: Int
    ) {
        logger.info("Saving transaction: Dispenser=$dispenserAddress, Volume=${volumeDeciliters/10.0}L, Amount=${amountOre/100.0} kr")
        
        val request = SaveTransactionRequest(
            dispenserAddress = dispenserAddress,
            volumeDeciliters = volumeDeciliters,
            amountOre = amountOre,
            pricePerLiter = pricePerLiter
        )
        
        val success = lpgApiClient.saveTransaction(request)
        
        if (success) {
            logger.info("✅ Transaction saved successfully to database")
        } else {
            logger.error("❌ Failed to save transaction to database")
        }
    }
}
