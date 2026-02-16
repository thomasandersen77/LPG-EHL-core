package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/emulator")
@Profile("local", "dev", "h2")
class EmulatorController(
    private val emulatorScenarioService: EmulatorScenarioService
) {
    private val logger = LoggerFactory.getLogger(EmulatorController::class.java)

    data class SetScenarioRequest(
        val dispenserAddress: Int,
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): EmulatorStatus {
        logger.info("🎬 Setting emulator scenario: address=${body.dispenserAddress}, scenario=${body.scenario}")
        emulatorScenarioService.setScenario(body.dispenserAddress, body.scenario)
        return emulatorScenarioService.status(body.dispenserAddress)
    }

    @PostMapping("/reset/{address}")
    fun reset(@PathVariable("address") dispenserAddress: Int): EmulatorStatus {
        logger.info("🔄 Resetting emulator: address=${dispenserAddress}")
        emulatorScenarioService.reset(dispenserAddress)
        return emulatorScenarioService.status(dispenserAddress)
    }

    @GetMapping("/status/{address}")
    fun status(@PathVariable("address") dispenserAddress: Int): EmulatorStatus =
        emulatorScenarioService.status(dispenserAddress)
}
