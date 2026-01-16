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
    private val environment: Environment,
    @Value("\${ehl.emulator.enabled:true}")
    private val emulatorEnabled: Boolean
) {

    data class ModeResponse(
        val mode: String,
        val profiles: List<String>,
        val description: String
    )

    data class HardwareModeResponse(
        val hardwareMode: String,  // "LAB" or "FIELD"
        val isRealHardware: Boolean,
        val description: String,
        val serialPort: String?,
        val baudRate: Int?
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

    @GetMapping("/hardware-mode")
    @Operation(
        summary = "Get hardware mode",
        description = "Returns whether we're running with real hardware (FIELD) or emulated hardware (LAB)"
    )
    fun getHardwareMode(): ResponseEntity<HardwareModeResponse> {
        val hardwareMode = if (emulatorEnabled) "LAB" else "FIELD"
        val isRealHardware = !emulatorEnabled

        val serialPort = if (!emulatorEnabled) environment.getProperty("ehl.serial.port") else null
        val baudRateStr = environment.getProperty("ehl.serial.baud-rate")
        val baudRate = baudRateStr?.toIntOrNull()

        val description = when (hardwareMode) {
            "FIELD" -> "REAL HARDWARE - Communicating via serial port"
            "LAB" -> "SIMULATED HARDWARE - Using in-memory emulator"
            else -> "Unknown hardware mode"
        }

        return ResponseEntity.ok(
            HardwareModeResponse(
                hardwareMode = hardwareMode,
                isRealHardware = isRealHardware,
                description = description,
                serialPort = serialPort,
                baudRate = baudRate
            )
        )
    }
}
