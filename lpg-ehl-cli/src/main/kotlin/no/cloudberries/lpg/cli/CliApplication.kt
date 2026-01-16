package no.cloudberries.lpg.cli

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "no.cloudberries.lpg.cli",
    "no.cloudberries.lpg.core",
    "no.cloudberries.lpg.emulator",
    "no.cloudberries.lpg.transport"
])
class CliApplication

fun main(args: Array<String>) {
    runApplication<CliApplication>(*args)
}
