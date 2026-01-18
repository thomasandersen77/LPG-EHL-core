package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.service.model.PriceHistory
import no.cloudberries.lpg.service.repository.PriceHistoryRepository
import no.cloudberries.lpg.service.event.EventPublisher
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Sentral service for prisoppdatering.
 * 
 * Sørger for at prisendringer propageres til:
 * 1. Database (price_history tabell)
 * 2. PumpStateService (for nye transaksjoner)
 * 3. Emulator (hvis tilgjengelig)
 * 4. Event Publisher (for real-time GUI oppdatering)
 */
@Service
class PriceService(
    private val priceHistoryRepository: PriceHistoryRepository,
    private val eventPublisher: EventPublisher,
    private val dispenserEmulator: EhlDispenserEmulator?
) {
    private val logger = LoggerFactory.getLogger(PriceService::class.java)
    
    /**
     * Oppdater pris for produkt og broadcast til alle systemer.
     * 
     * @param productCode Produktkode (f.eks. "LPG")
     * @param productName Produktnavn
     * @param pricePerLiter Ny pris per liter (inkl. MVA)
     * @param createdBy Hvem som endret prisen
     * @return Lagret PriceHistory entry
     */
    fun updatePrice(
        productCode: String = "LPG",
        productName: String = "LPG (Flytende petroleumsgass)",
        pricePerLiter: BigDecimal,
        createdBy: String = "admin"
    ): PriceHistory {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("💰 PRISOPPDATERING: {} kr/L for {}", pricePerLiter, productCode)
        
        // 1. Lagre til database
        val vatRate = BigDecimal("0.25") // 25% MVA i Norge
        val priceHistory = PriceHistory(
            productCode = productCode,
            productName = productName,
            pricePerLiter = pricePerLiter,
            vatRate = vatRate,
            effectiveFrom = LocalDateTime.now(),
            createdBy = createdBy
        )
        
        val saved = priceHistoryRepository.save(priceHistory)
        logger.info("✅ Lagret til database: ID={}", saved.id)
        
        // 2. Oppdater emulator (hvis tilgjengelig)
        dispenserEmulator?.let { emulator ->
            val priceOre = (pricePerLiter.toDouble() * 100).toInt()
            emulator.setPrice(priceOre)
            logger.info("⚙️  Emulator oppdatert: {} øre", priceOre)
        }
        
        // 3. Publish price update event (WebSocket, etc.)
        val priceKr = pricePerLiter.toDouble()
        eventPublisher.publishPriceUpdate(priceKr)
        logger.info("📡 Price update event published")
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return saved
    }
    
    /**
     * Hent gjeldende pris fra database.
     * 
     * @param productCode Produktkode (f.eks. "LPG")
     * @return Gjeldende pris, eller null hvis ingen pris er satt
     */
    fun getCurrentPrice(productCode: String = "LPG"): PriceHistory? {
        return priceHistoryRepository.findCurrentPrice(productCode, LocalDateTime.now())
    }
    
    /**
     * Hent prishistorikk for produkt.
     * 
     * @param productCode Produktkode (f.eks. "LPG")
     * @return Liste av prisendringer, nyeste først
     */
    fun getPriceHistory(productCode: String = "LPG"): List<PriceHistory> {
        return priceHistoryRepository.findByProductCodeOrderByEffectiveFromDesc(productCode)
    }
}
