package no.cloudberries.lpg.headless.service

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.service.pump.DispenserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/**
 * Headless Polling Service
 * 
 * Denne servicen holder headless-applikasjonen i live ved å kontinuerlig
 * polle dispenser status. Den kjører automatisk som en @Scheduled task
 * og logger aktivitet for debugging og overvåking.
 * 
 * Funksjoner:
 * - Poller dispenser STATE og VOLUME hver 2. sekund
 * - Sender packets til DispenserService for state machine oppdatering
 * - Logger regelmessig aktivitet (hver 10. poll)
 * - Holder Spring Boot-applikasjonen kjørende som daemon
 * 
 * Dette sikrer at headless-appen ikke "dør" etter oppstart.
 */
@Service
class HeadlessPollingService(
    private val ehlCommunicator: EhlCommunicator,
    private val dispenserService: DispenserService,
    @Value("\${lpg.dispenser.address:1}") private val dispenserAddress: Int,
    @Value("\${lpg.polling.interval-ms:2000}") private val pollingIntervalMs: Long
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pollCount = AtomicLong(0)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    private var isRunning = false
    
    /**
     * Scheduled task som poller dispenser status.
     * Kjører automatisk hver ${pollingIntervalMs} millisekund (default 2000ms = 2 sekunder).
     * 
     * Denne tasken holder applikasjonen i live - så lenge den kjører,
     * vil Spring Boot-prosessen fortsette.
     */
    @Scheduled(fixedDelayString = "\${lpg.polling.interval-ms:2000}", initialDelay = 5000)
    fun pollDispenserStatus() {
        if (!isRunning) {
            logger.info("🚀 Starting dispenser polling loop...")
            isRunning = true
        }
        
        val count = pollCount.incrementAndGet()
        
        try {
            // 1. Poll STATE
            val statePacket = EhlPacket(
                address = dispenserAddress,
                command = EhlCommand.STATE,
                data = byteArrayOf()
            )
            
            val stateResponse = runBlocking {
                ehlCommunicator.sendAndReceive(statePacket, 1000)
            }
            
            // Send til DispenserService for state machine processing
            dispenserService.handlePacket(stateResponse)
            
            // 2. Poll VOLUME
            val volumePacket = EhlPacket(
                address = dispenserAddress,
                command = EhlCommand.VOLUME,
                data = byteArrayOf()
            )
            
            val volumeResponse = runBlocking {
                ehlCommunicator.sendAndReceive(volumePacket, 1000)
            }
            
            // Send til DispenserService for state machine processing
            dispenserService.handlePacket(volumeResponse)
            
            // Logg hver 10. poll for å vise at det fungerer uten å spamme
            if (count % 10 == 0L) {
                val time = LocalDateTime.now().format(timeFormatter)
                val dispenserState = dispenserService.getDispenserState(dispenserAddress)
                logger.info("📊 [$time] Polling #$count - Dispenser $dispenserAddress state: $dispenserState")
            }
            
        } catch (e: Exception) {
            // Logg feil, men fortsett polling (ikke crash appen)
            logger.error("❌ Polling error (poll #$count): ${e.message}")
            
            // Logg full stacktrace hvert 30. forsøk for debugging
            if (count % 30 == 0L) {
                logger.error("Full error details:", e)
            }
        }
    }
    
    /**
     * Heartbeat task som logger hver 30. sekund for å bekrefte at appen lever.
     * Dette er nyttig for overvåking og debugging.
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    fun heartbeat() {
        val time = LocalDateTime.now().format(timeFormatter)
        logger.info("💓 [$time] Headless app is alive - Poll count: ${pollCount.get()}")
    }
}
