package no.cloudberries.lpg.pls.sim

import javafx.application.Application
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("PlsSimMain")

/**
 * PLS Simulator - Serial port simulator for testing EHL protocol.
 *
 * Usage:
 *   java -jar pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --address=1 --price=1590
 */
fun main(args: Array<String>) {
    log.info("")
    log.info("══════════════════════════════════════════════════════════")
    log.info("  ⛽ PLS SIMULATOR")
    log.info("══════════════════════════════════════════════════════════")
    
    val cliArgs = CliArgs.parse(args)
    
    log.info("  Serial Configuration:")
    log.info("    Port:      {}", cliArgs.port)
    log.info("    Baud:      {}", cliArgs.baud)
    log.info("    Parity:    {}", cliArgs.parity)
    log.info("    Mode:      {}", cliArgs.mode)
    log.info("    Chunked:   {}", cliArgs.chunk)
    log.info("    Latency:   {} ms", cliArgs.latencyMs)
    log.info("    Log Hex:   {}", cliArgs.logHex)
    log.info("")
    log.info("  Dispenser Configuration:")
    log.info("    Address:   {}", cliArgs.dispenserAddress)
    if (cliArgs.legacyAddressEnabled) {
        log.info("    Legacy:    {} (32 + {})", 32 + cliArgs.dispenserAddress, cliArgs.dispenserAddress)
    }
    log.info("    Price:     {} kr/L", cliArgs.priceCents / 100.0)
    log.info("    Blocked:   {}", cliArgs.initiallyBlocked)
    log.info("")
    log.info("  Commands supported (Alejandro tested):")
    log.info("    STATE (0x4B), ERROR_QUERY (0x4C), VOLUME (0x45), TANKBIT (0xC5)")
    log.info("")
    log.info("  Logging:")
    log.info("    Heartbeat: {} ms", cliArgs.heartbeatIntervalMs)
    
    // Log fault injection config if present
    if (cliArgs.disconnectAfterSeconds != null || cliArgs.badChecksumRate > 0.0 || cliArgs.powerfaultAfterSeconds != null) {
        log.info("")
        log.info("  ⚠️ Fault Injection:")
        if (cliArgs.disconnectAfterSeconds != null) {
            log.info("    Disconnect:  {:.1f} seconds", cliArgs.disconnectAfterSeconds)
        }
        if (cliArgs.badChecksumRate > 0.0) {
            log.info("    Bad Checksum: {:.1f}%", cliArgs.badChecksumRate * 100)
        }
        if (cliArgs.powerfaultAfterSeconds != null) {
            log.info("    Power Fault: {:.1f} seconds", cliArgs.powerfaultAfterSeconds)
        }
    }
    log.info("══════════════════════════════════════════════════════════")
    log.info("")

    // Handler reference for callbacks
    var handler: SerialPortHandler? = null
    
    // Create state with configured parameters
    val plsState = PlsState(
        defaultAddress = cliArgs.dispenserAddress,
        priceCents = cliArgs.priceCents,
        initiallyBlocked = cliArgs.initiallyBlocked,
        legacyAddressEnabled = cliArgs.legacyAddressEnabled,
        disconnectAfterSeconds = cliArgs.disconnectAfterSeconds,
        badChecksumRate = cliArgs.badChecksumRate,
        powerfaultAfterSeconds = cliArgs.powerfaultAfterSeconds,
        onDisconnect = { handler?.forceDisconnect() },
        onPowerfault = { handler?.forceDisconnect() },
        manualNozzleControl = cliArgs.gui
    )

    handler = SerialPortHandler(
        portName = cliArgs.port,
        baud = cliArgs.baud,
        parity = cliArgs.parity,
        mode = cliArgs.mode,
        chunked = cliArgs.chunk,
        latencyMs = cliArgs.latencyMs,
        logHex = cliArgs.logHex,
        plsState = plsState
    )

    // Shutdown hook for clean exit
    val shutdownLatch = CountDownLatch(1)
    
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown signal received")
        plsState.shutdown()  // Stop auto-pumping thread
        handler.stop()
        shutdownLatch.countDown()
    })

    try {
        handler.start()
        log.info("PLS Simulator running. Press Ctrl+C to stop.")

        if (cliArgs.gui) {
            log.info("Starting PLS GUI...")
            PlsGuiApp.state = plsState
            PlsGuiApp.onGuiClosed = {
                plsState.shutdown()
                handler.stop()
                shutdownLatch.countDown()
            }
            Application.launch(PlsGuiApp::class.java)
            // When GUI closes, launch() returns and we fall through
        }

        // Periodic heartbeat (skipped in GUI mode - launch blocks until GUI closed) (INFO) - shows simulator is alive + state
        val heartbeatIntervalMs = cliArgs.heartbeatIntervalMs
        val heartbeatThread = Thread({
            try {
                while (true) {
                    Thread.sleep(heartbeatIntervalMs)
                    log.info(plsState.heartbeatLine())
                }
            } catch (_: InterruptedException) {
                // Shutdown signal
            } catch (e: Exception) {
                log.warn("Heartbeat stopped: {}", e.message)
            }
        }, "pls-sim-heartbeat")
        heartbeatThread.isDaemon = true
        heartbeatThread.start()
        
        // Wait indefinitely until shutdown
        shutdownLatch.await()
        
    } catch (e: Exception) {
        log.error("Fatal error: {}", e.message, e)
        handler.stop()
        exitProcess(1)
    }
    
    log.info("=== PLS Simulator Stopped ===")
}
