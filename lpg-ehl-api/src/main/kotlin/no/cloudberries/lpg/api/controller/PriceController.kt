package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.pls.MockPlsService
import no.cloudberries.lpg.api.repository.PriceHistoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/prices")
@Tag(name = "Prices", description = "Product pricing endpoints")
class PriceController(
    @Value("\${lpg.price.per-liter:15.90}") private var defaultPricePerLiter: BigDecimal,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val priceService: no.cloudberries.lpg.api.service.PriceService
) {
    @Autowired(required = false)
    private var plsService: MockPlsService? = null
    
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    
    // In-memory price storage (fallback if PLS not available)
    private var currentPrice: BigDecimal = defaultPricePerLiter

    data class PriceResponse(
        val productCode: String,
        val productName: String,
        val pricePerLiter: BigDecimal,
        val pricePerLiterExclVat: BigDecimal,
        val vatRate: BigDecimal,
        val currency: String,
        val lastUpdated: LocalDateTime
    )

    data class PricesResponse(
        val prices: List<PriceResponse>,
        val displayPrice: BigDecimal,
        val displayProductName: String
    )

    @GetMapping
    @Operation(
        summary = "Get current gas prices",
        description = "Returns current pricing for all available products"
    )
    fun getPrices(): ResponseEntity<PricesResponse> {
        // Try to get price from PLS first (if available)
        val plsPrice = plsService?.getCurrentPrice("LPG")
        val effectivePrice = plsPrice?.pricePerLiter ?: currentPrice
        
        val vatRate = BigDecimal("0.25") // 25% VAT in Norway
        val priceExclVat = effectivePrice.divide(BigDecimal.ONE.add(vatRate), 2, java.math.RoundingMode.HALF_UP)
        
        val lpgPrice = PriceResponse(
            productCode = "LPG",
            productName = "LPG (Flytende petroleumsgass)",
            pricePerLiter = effectivePrice,
            pricePerLiterExclVat = priceExclVat,
            vatRate = vatRate,
            currency = "NOK",
            lastUpdated = LocalDateTime.now()
        )
        
        return ResponseEntity.ok(
            PricesResponse(
                prices = listOf(lpgPrice),
                displayPrice = effectivePrice,
                displayProductName = "LPG"
            )
        )
    }
    
    data class UpdatePriceRequest(
        val pricePerLiter: BigDecimal
    )
    
    @PostMapping("/update")
    @Operation(
        summary = "Update gas price",
        description = "Update the current price per liter for LPG (admin only)"
    )
    fun updatePrice(@RequestBody request: UpdatePriceRequest): ResponseEntity<PricesResponse> {
        if (request.pricePerLiter <= BigDecimal.ZERO) {
            return ResponseEntity.badRequest().build()
        }
        
        logger.info("💰 Price update request: {} kr/L", request.pricePerLiter)
        
        // Use PriceService to update price everywhere
        // (Database, Emulator, WebSocket, PumpStateService)
        priceService.updatePrice(
            productCode = "LPG",
            productName = "LPG (Flytende petroleumsgass)",
            pricePerLiter = request.pricePerLiter,
            createdBy = "admin" // TODO: Get from security context when auth is enabled
        )
        
        // Update in PLS if available
        if (plsService != null) {
            plsService!!.updatePrice("LPG", request.pricePerLiter)
        } else {
            currentPrice = request.pricePerLiter
        }
        
        // Return updated prices
        return getPrices()
    }
}
