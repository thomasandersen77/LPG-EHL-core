package no.cloudberries.lpg.pls.sim

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
    log.info("    Mode:      {}", cliArgs.mode)
    log.info("    Chunked:   {}", cliArgs.chunk)
    log.info("    Latency:   {} ms", cliArgs.latencyMs)
    log.info("    Log Hex:   {}", cliArgs.logHex)
    log.info("")
    log.info("  Dispenser Configuration:")
    log.info("    Address:   {}", cliArgs.dispenserAddress)
    log.info("    Price:     {} kr/L", cliArgs.priceCents / 100.0)
    log.info("    Blocked:   {}", cliArgs.initiallyBlocked)
    log.info("══════════════════════════════════════════════════════════")
    log.info("")

    // Create state with configured parameters
    val plsState = PlsState(
        defaultAddress = cliArgs.dispenserAddress,
        priceCents = cliArgs.priceCents,
        initiallyBlocked = cliArgs.initiallyBlocked
    )

    val handler = SerialPortHandler(
        portName = cliArgs.port,
        baud = cliArgs.baud,
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

        // Periodic heartbeat (INFO) - shows simulator is alive + state
        val heartbeatThread = Thread({
            try {
                while (true) {
                    Thread.sleep(5000)
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
