package no.cloudberries.lpg.service.terminal

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Terminal integration configuration.
 * Active when payment.terminal.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalConfiguration {

    @Bean
    @Primary
    fun terminalClient(simulatedClient: SimulatedTerminalClient): TerminalClient = simulatedClient
}
