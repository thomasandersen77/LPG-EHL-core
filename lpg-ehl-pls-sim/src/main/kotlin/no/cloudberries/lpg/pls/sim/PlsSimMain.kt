package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("PlsSimMain")

/**
 * PLS Simulator - Serial port simulator for testing EHL protocol.
 *
 * Usage:
 *   java -jar pls-sim.jar --port=/dev/ttys013 --baud=9600 --mode=line --chunk=true
 */
fun main(args: Array<String>) {
    log.info("=== PLS Simulator Starting ===")
    
    val cliArgs = CliArgs.parse(args)
    
    log.info("Configuration:")
    log.info("  Port:      {}", cliArgs.port)
    log.info("  Baud:      {}", cliArgs.baud)
    log.info("  Mode:      {}", cliArgs.mode)
    log.info("  Chunked:   {}", cliArgs.chunk)
    log.info("  Latency:   {} ms", cliArgs.latencyMs)
    log.info("  Log Hex:   {}", cliArgs.logHex)

    val handler = SerialPortHandler(
        portName = cliArgs.port,
        baud = cliArgs.baud,
        mode = cliArgs.mode,
        chunked = cliArgs.chunk,
        latencyMs = cliArgs.latencyMs,
        logHex = cliArgs.logHex
    )

    // Shutdown hook for clean exit
    val shutdownLatch = CountDownLatch(1)
    
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown signal received")
        handler.stop()
        shutdownLatch.countDown()
    })

    try {
        handler.start()
        log.info("PLS Simulator running. Press Ctrl+C to stop.")
        
        // Wait indefinitely until shutdown
        shutdownLatch.await()
        
    } catch (e: Exception) {
        log.error("Fatal error: {}", e.message, e)
        handler.stop()
        exitProcess(1)
    }
    
    log.info("=== PLS Simulator Stopped ===")
}
