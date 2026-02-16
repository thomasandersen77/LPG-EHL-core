package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/emulator")
@Profile("local", "dev", "h2")
class EmulatorController(
    private val emulatorScenarioService: EmulatorScenarioService,
    @Value("\${lpg.dispenser.address:1}") private val defaultAddress: Int
) {
    private val logger = LoggerFactory.getLogger(EmulatorController::class.java)

    data class SetScenarioRequest(
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): EmulatorStatus {
        logger.info("🎬 Setting emulator scenario: address=$defaultAddress, scenario=${body.scenario}")
        emulatorScenarioService.setScenario(defaultAddress, body.scenario)
        return emulatorScenarioService.status(defaultAddress)
    }

    @PostMapping("/reset")
    fun reset(): EmulatorStatus {
        logger.info("🔄 Resetting emulator: address=$defaultAddress")
        emulatorScenarioService.reset(defaultAddress)
        return emulatorScenarioService.status(defaultAddress)
    }

    @GetMapping("/status")
    fun status(): EmulatorStatus =
        emulatorScenarioService.status(defaultAddress)
}
