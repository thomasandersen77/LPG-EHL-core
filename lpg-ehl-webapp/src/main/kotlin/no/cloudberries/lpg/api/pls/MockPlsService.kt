package no.cloudberries.lpg.api.pls

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Mock PLS (Price List System) Service
 * 
 * Simulates a Price List System that can push price updates to the edge device.
 * In LAB mode, this generates random price fluctuations.
 * In PROD mode, this would connect to real PLS backend.
 */
@Service
@Profile("lab", "local", "default")
class MockPlsService {
    
    private val logger = LoggerFactory.getLogger(MockPlsService::class.java)
    private val random = Random.Default
    
    // Current prices (product code -> price)
    private val currentPrices = ConcurrentHashMap<String, PriceInfo>()
    
    // Price change listeners
    private val listeners = mutableListOf<PriceChangeListener>()
    
    init {
        // Initialize with default LPG price
        currentPrices["LPG"] = PriceInfo(
            productCode = "LPG",
            productName = "LPG (Flytende petroleumsgass)",
            pricePerLiter = BigDecimal("15.90"),
            effectiveFrom = LocalDateTime.now()
        )
        
        logger.info("🏷️ [MOCK PLS] Initialized with default prices")
    }
    
    /**
     * Get current price for a product
     */
    fun getCurrentPrice(productCode: String): PriceInfo? {
        return currentPrices[productCode]
    }
    
    /**
     * Get all current prices
     */
    fun getAllPrices(): List<PriceInfo> {
        return currentPrices.values.toList()
    }
    
    /**
     * Update price (simulates PLS push)
     */
    fun updatePrice(productCode: String, newPrice: BigDecimal) {
        logger.info("🏷️ [MOCK PLS] Price update: $productCode = $newPrice kr/L")
        
        val oldPrice = currentPrices[productCode]
        val newPriceInfo = PriceInfo(
            productCode = productCode,
            productName = oldPrice?.productName ?: productCode,
            pricePerLiter = newPrice,
            effectiveFrom = LocalDateTime.now()
        )
        
        currentPrices[productCode] = newPriceInfo
        
        // Notify listeners
        notifyPriceChange(oldPrice, newPriceInfo)
    }
    
    /**
     * Register a price change listener
     */
    fun addPriceChangeListener(listener: PriceChangeListener) {
        listeners.add(listener)
    }
    
    /**
     * Simulate random price fluctuations (LAB mode only)
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    fun simulatePriceFluctuation() {
        // Only run in LAB mode
        val profiles = System.getProperty("spring.profiles.active", "")
        if (!profiles.contains("lab") && !profiles.contains("local")) {
            return
        }
        
        // 30% chance of price change
        if (random.nextInt(100) < 30) {
            currentPrices.forEach { (productCode, currentPrice) ->
                // Fluctuate price by ±5%
                val fluctuation = (random.nextDouble() - 0.5) * 0.1 // -5% to +5%
                val newPrice = currentPrice.pricePerLiter * (BigDecimal.ONE + BigDecimal.valueOf(fluctuation))
                val roundedPrice = newPrice.setScale(2, java.math.RoundingMode.HALF_UP)
                
                // Don't let price go below 10 kr or above 25 kr
                val clampedPrice = when {
                    roundedPrice < BigDecimal("10.00") -> BigDecimal("10.00")
                    roundedPrice > BigDecimal("25.00") -> BigDecimal("25.00")
                    else -> roundedPrice
                }
                
                if (clampedPrice != currentPrice.pricePerLiter) {
                    logger.info("🏷️ [MOCK PLS] Simulated price fluctuation: $productCode ${currentPrice.pricePerLiter} → $clampedPrice kr/L")
                    updatePrice(productCode, clampedPrice)
                }
            }
        }
    }
    
    private fun notifyPriceChange(oldPrice: PriceInfo?, newPrice: PriceInfo) {
        listeners.forEach { listener ->
            try {
                listener.onPriceChanged(oldPrice, newPrice)
            } catch (e: Exception) {
                logger.error("Error notifying price change listener", e)
            }
        }
    }
}

/**
 * Price Information
 */
data class PriceInfo(
    val productCode: String,
    val productName: String,
    val pricePerLiter: BigDecimal,
    val effectiveFrom: LocalDateTime,
    val effectiveUntil: LocalDateTime? = null
)

/**
 * Price Change Listener
 */
fun interface PriceChangeListener {
    fun onPriceChanged(oldPrice: PriceInfo?, newPrice: PriceInfo)
}
