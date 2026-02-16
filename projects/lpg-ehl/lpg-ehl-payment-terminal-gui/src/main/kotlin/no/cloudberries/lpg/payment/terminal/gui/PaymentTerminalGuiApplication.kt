package no.cloudberries.lpg.payment.terminal.gui

import javafx.application.Application
import javafx.stage.Stage
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventStore
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

/**
 * Payment Terminal GUI Application.
 *
 * Starts the embedded HTTP simulator (same as payment-terminal-sim.jar)
 * AND opens a visual JavaFX GUI showing the terminal display in real-time.
 *
 * Usage:
 *   java -jar payment-terminal-gui.jar                     # GUI + HTTP on :18080
 *   java -jar payment-terminal-gui.jar --server.port=8080  # Custom port
 *   java -jar payment-terminal-gui.jar --terminal.gui.enabled=false  # HTTP only (headless)
 */
@SpringBootApplication(scanBasePackages = ["no.cloudberries.lpg.payment.terminal"])
@ConfigurationPropertiesScan(basePackages = ["no.cloudberries.lpg.payment.terminal"])
class PaymentTerminalGuiApplication

private val log = LoggerFactory.getLogger(PaymentTerminalGuiApplication::class.java)

/**
 * JavaFX Application wrapper that bridges Spring Boot and JavaFX lifecycle.
 */
class TerminalFxApplication : Application() {

    override fun start(primaryStage: Stage) {
        val context = springContext ?: return
        val port = context.environment.getProperty("server.port", "18080")

        val eventStore = context.getBean(TerminalEventStore::class.java)
        val stateManager = context.getBean(TerminalStateManager::class.java)
        val config = context.getBean(SimulatorConfig::class.java)

        val gui = TerminalGuiFrame(eventStore, stateManager, config, port.toInt())
        gui.show(primaryStage)
        log.info("JavaFX GUI window opened")
    }

    override fun stop() {
        springContext?.close()
    }

    companion object {
        var springContext: ConfigurableApplicationContext? = null
    }
}

fun main(args: Array<String>) {
    // Check if GUI is disabled via args
    val guiEnabled = args.none { it.contains("terminal.gui.enabled=false") }

    log.info("Starting Payment Terminal Simulator (GUI={})", guiEnabled)

    val context = runApplication<PaymentTerminalGuiApplication>(*args)

    val port = context.environment.getProperty("server.port", "18080")
    log.info("=== Payment Terminal Simulator Ready on port {} ===", port)

    if (guiEnabled) {
        // Pass Spring context to JavaFX Application and launch it
        TerminalFxApplication.springContext = context
        Application.launch(TerminalFxApplication::class.java)
    } else {
        log.info("GUI disabled (--terminal.gui.enabled=false)")
    }
}
