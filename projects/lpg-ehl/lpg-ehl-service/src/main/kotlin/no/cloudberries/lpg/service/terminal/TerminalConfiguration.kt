package no.cloudberries.lpg.service.terminal

import org.springframework.beans.factory.annotation.Value
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

    @Value("\${payment.terminal.baxi.host:192.168.1.100}")
    private lateinit var baxiHost: String

    @Value("\${payment.terminal.baxi.port:7200}")
    private var baxiPort: Int = 7200

    @Value("\${payment.terminal.base-url:http://localhost:18080}")
    private lateinit var simulatorBaseUrl: String

    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "baxi")
    fun baxiTerminalClient(): TerminalClient = BaxiTerminalClient(baxiHost, baxiPort)

    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "simulated")
    fun simulatedTerminalClient(): TerminalClient = SimulatedTerminalClient(simulatorBaseUrl)

    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "mock")
    fun mockTerminalClient(): TerminalClient = MockBaxiTerminalClient()
}
