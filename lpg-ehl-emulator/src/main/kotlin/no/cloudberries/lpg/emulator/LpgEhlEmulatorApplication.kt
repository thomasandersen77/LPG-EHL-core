package no.cloudberries.lpg.emulator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LpgEhlEmulatorApplication

fun main(args: Array<String>) {
    runApplication<LpgEhlEmulatorApplication>(*args)
}
