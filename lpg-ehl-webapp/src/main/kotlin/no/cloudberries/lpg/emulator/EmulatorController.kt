package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/emulator")
@Profile("local", "dev", "h2")
class EmulatorController(
    private val emulatorService: EmulatorService
) {
    private val logger = LoggerFactory.getLogger(EmulatorController::class.java)

    data class SetScenarioRequest(
        val dispenserAddress: Int,
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): EmulatorStatus {
        logger.info("🎬 Setting emulator scenario: address=${body.dispenserAddress}, scenario=${body.scenario}")
        emulatorService.setScenario(body.dispenserAddress, body.scenario)
        return emulatorService.status(body.dispenserAddress)
    }

    @PostMapping("/reset/{address}")
    fun reset(@PathVariable("address") dispenserAddress: Int): EmulatorStatus {
        logger.info("🔄 Resetting emulator: address=${dispenserAddress}")
        emulatorService.reset(dispenserAddress)
        return emulatorService.status(dispenserAddress)
    }

    @GetMapping("/status/{address}")
    fun status(@PathVariable("address") dispenserAddress: Int): EmulatorStatus =
        emulatorService.status(dispenserAddress)
}
