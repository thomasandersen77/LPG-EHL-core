package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.model.RoadTaxSettings
import no.cloudberries.lpg.api.repository.RoadTaxSettingsRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/road-tax")
@Tag(name = "Road Tax", description = "Road tax (veitrafikkavgift) management endpoints")
class RoadTaxController(
    private val roadTaxSettingsRepository: RoadTaxSettingsRepository
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    data class RoadTaxResponse(
        val taxPerLiterOre: Int,
        val taxPerLiterKr: BigDecimal,
        val effectiveFrom: LocalDateTime,
        val description: String?,
        val lastUpdated: LocalDateTime
    )

    data class UpdateRoadTaxRequest(
        val taxPerLiterOre: Int,
        val description: String? = null
    )

    data class RoadTaxHistoryResponse(
        val history: List<RoadTaxResponse>
    )

    @GetMapping
    @Operation(
        summary = "Get current road tax",
        description = "Returns the currently effective road tax per liter"
    )
    fun getCurrentRoadTax(): ResponseEntity<RoadTaxResponse> {
        val currentTax = roadTaxSettingsRepository.findFirstByOrderByEffectiveFromDesc()
            ?: return ResponseEntity.notFound().build()

        val response = RoadTaxResponse(
            taxPerLiterOre = currentTax.taxPerLiterOre,
            taxPerLiterKr = BigDecimal(currentTax.taxPerLiterOre).divide(BigDecimal(100)),
            effectiveFrom = currentTax.effectiveFrom,
            description = currentTax.description,
            lastUpdated = currentTax.createdAt
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/update")
    @Operation(
        summary = "Update road tax",
        description = "Update the road tax per liter (admin only)"
    )
    fun updateRoadTax(@RequestBody request: UpdateRoadTaxRequest): ResponseEntity<RoadTaxResponse> {
        if (request.taxPerLiterOre < 0) {
            return ResponseEntity.badRequest().build()
        }

        logger.info("🚗 Road tax update request: {} øre/L ({})", 
            request.taxPerLiterOre, 
            BigDecimal(request.taxPerLiterOre).divide(BigDecimal(100)))

        // Invalidate previous tax setting
        val previousTax = roadTaxSettingsRepository.findFirstByOrderByEffectiveFromDesc()
        previousTax?.let {
            if (it.effectiveUntil == null) {
                it.effectiveUntil = LocalDateTime.now()
                roadTaxSettingsRepository.save(it)
                logger.info("✅ Previous road tax invalidated: {}", it)
            }
        }

        // Create new tax setting
        val newTax = RoadTaxSettings(
            taxPerLiterOre = request.taxPerLiterOre,
            effectiveFrom = LocalDateTime.now(),
            createdBy = "admin", // TODO: Get from security context when auth is enabled
            description = request.description ?: "Veitrafikkavgift oppdatert"
        )

        val saved = roadTaxSettingsRepository.save(newTax)
        logger.info("✅ New road tax saved: {}", saved)

        val response = RoadTaxResponse(
            taxPerLiterOre = saved.taxPerLiterOre,
            taxPerLiterKr = BigDecimal(saved.taxPerLiterOre).divide(BigDecimal(100)),
            effectiveFrom = saved.effectiveFrom,
            description = saved.description,
            lastUpdated = saved.createdAt
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/history")
    @Operation(
        summary = "Get road tax history",
        description = "Returns historical road tax settings"
    )
    fun getRoadTaxHistory(): ResponseEntity<RoadTaxHistoryResponse> {
        val history = roadTaxSettingsRepository.findAllByOrderByEffectiveFromDesc()
            .map { tax ->
                RoadTaxResponse(
                    taxPerLiterOre = tax.taxPerLiterOre,
                    taxPerLiterKr = BigDecimal(tax.taxPerLiterOre).divide(BigDecimal(100)),
                    effectiveFrom = tax.effectiveFrom,
                    description = tax.description,
                    lastUpdated = tax.createdAt
                )
            }

        return ResponseEntity.ok(RoadTaxHistoryResponse(history))
    }
}
