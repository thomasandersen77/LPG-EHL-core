package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration", description = "Application configuration endpoints")
class ConfigController(
    private val environment: Environment
) {

    data class ModeResponse(
        val mode: String,
        val profiles: List<String>,
        val description: String
    )

    @GetMapping("/mode")
    @Operation(
        summary = "Get application mode",
        description = "Returns LAB (simulation) or KIOSK (production) mode based on active Spring profiles"
    )
    fun getMode(): ResponseEntity<ModeResponse> {
        val activeProfiles = environment.activeProfiles.toList()
        
        val mode = when {
            activeProfiles.contains("lab") || activeProfiles.contains("local") -> "LAB"
            activeProfiles.contains("prod") || activeProfiles.contains("production") -> "KIOSK"
            activeProfiles.isEmpty() -> "LAB" // Default to LAB if no profile set
            else -> "LAB"
        }
        
        val description = when (mode) {
            "LAB" -> "Simulation mode with emulated hardware"
            "KIOSK" -> "Production mode with real hardware"
            else -> "Unknown mode"
        }
        
        return ResponseEntity.ok(
            ModeResponse(
                mode = mode,
                profiles = activeProfiles,
                description = description
            )
        )
    }
}
