package no.cloudberries.lpg.payment.terminal.sim

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PaymentTerminalSimApplication

private val log = LoggerFactory.getLogger(PaymentTerminalSimApplication::class.java)

fun main(args: Array<String>) {
    runApplication<PaymentTerminalSimApplication>(*args)
    log.info("=== Payment Terminal Simulator Started ===")
}
