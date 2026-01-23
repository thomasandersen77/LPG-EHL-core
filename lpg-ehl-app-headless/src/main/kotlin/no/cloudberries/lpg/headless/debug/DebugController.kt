package no.cloudberries.lpg.headless.debug

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Debug API Controller for felt-testing via curl.
 * 
 * Kun aktiv med profil: --spring.profiles.active=debug-api
 * 
 * Eksempler:
 * ```
 * curl http://localhost:8080/api/debug/health
 * curl http://localhost:8080/api/debug/state/1
 * curl -X POST http://localhost:8080/api/debug/linetest/1
 * curl -X POST http://localhost:8080/api/debug/unblock/1
 * curl -X POST http://localhost:8080/api/debug/block/1
 * ```
 */
@Profile("debug-api")
@RestController
@RequestMapping("/api/debug")
class DebugController(
    private val ehlCommunicator: EhlCommunicator,
    private val pumpStateService: PumpStateService,
    @Value("\${lpg.mode:FIELD}") private val mode: String,
    @Value("\${ehl.serial.port:/dev/ttyS0}") private val serialPort: String,
    @Value("\${ehl.emulator.enabled:false}") private val emulatorEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(DebugController::class.java)
    
    init {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("🔧 DEBUG API AKTIVERT")
        logger.info("   Endepunkter tilgjengelig på /api/debug/*")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * Health check - verify system is running.
     */
    @GetMapping("/health")
    fun health(): HealthResponse {
        logger.debug("Health check requested")
        return HealthResponse(
            status = "UP",
            mode = mode,
            serialPort = serialPort,
            emulatorEnabled = emulatorEnabled
        )
    }
    
    /**
     * Get pump state from PumpStateService (includes business logic state).
     */
    @GetMapping("/state/{addr}")
    fun getState(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("STATE request for address $addr")
        
        return try {
            val status = pumpStateService.getStatus(addr)
            ResponseEntity.ok(StateResponse(
                address = addr,
                state = status.state,
                volumeLitres = status.volumeLitres,
                amountKr = status.amountKr,
                pricePerLitreKr = status.pricePerLitreKr,
                nozzleLifted = status.nozzleLifted,
                hasPendingTransaction = status.hasPendingTransaction
            ))
        } catch (e: Exception) {
            logger.error("STATE failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "STATE_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Get volume directly from dispenser via EHL protocol.
     */
    @GetMapping("/volume/{addr}")
    fun getVolume(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("VOLUME request for address $addr")
        
        return try {
            val response = runBlocking {
                val packet = EhlPacket(addr, EhlCommand.VOLUME)
                ehlCommunicator.sendAndReceive(packet, 3000)
            }
            
            val volumeLitres = if (response.data.size >= 5) {
                EhlDataParser.parseVolumeDataVb6(response.data)
            } else {
                0.0
            }
            
            ResponseEntity.ok(VolumeResponse(
                address = addr,
                volumeLitres = volumeLitres,
                volumeCentilitres = (volumeLitres * 100).toInt(),
                raw = response.data.joinToString(" ") { "%02X".format(it) }
            ))
        } catch (e: Exception) {
            logger.error("VOLUME failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "VOLUME_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Line test - verify communication with dispenser.
     */
    @PostMapping("/linetest/{addr}")
    fun linetest(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("LINETEST request for address $addr")
        
        return try {
            val response = runBlocking {
                val packet = EhlPacket(addr, EhlCommand.LINETEST)
                ehlCommunicator.sendAndReceive(packet, 3000)
            }
            
            val success = response.command == EhlCommand.OK
            ResponseEntity.ok(CommandResponse(
                command = "LINETEST",
                address = addr,
                success = success,
                message = if (success) "Kommunikasjon OK" else "Uventet respons: ${response.command}",
                responseCode = response.command.name
            ))
        } catch (e: Exception) {
            logger.error("LINETEST failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "LINETEST_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Unblock pump - allow fuel delivery.
     * Uses PumpStateService which includes 60s timeout logic.
     */
    @PostMapping("/unblock/{addr}")
    fun unblock(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("UNBLOCK request for address $addr")
        
        return try {
            val result = pumpStateService.unblock(addr)
            
            result.fold(
                onSuccess = { status ->
                    ResponseEntity.ok(CommandResponse(
                        command = "UNBLOCK",
                        address = addr,
                        success = true,
                        message = "Pumpe frigjort - 60s timeout startet",
                        responseCode = status.state
                    ))
                },
                onFailure = { error ->
                    ResponseEntity.badRequest().body(ErrorResponse(
                        error = "UNBLOCK_REJECTED",
                        message = error.message ?: "Unknown error"
                    ))
                }
            )
        } catch (e: Exception) {
            logger.error("UNBLOCK failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "UNBLOCK_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Block pump - stop fuel delivery.
     * Uses PumpStateService which handles transaction finalization.
     */
    @PostMapping("/block/{addr}")
    fun block(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("BLOCK request for address $addr")
        
        return try {
            val result = pumpStateService.block(addr)
            
            result.fold(
                onSuccess = { status ->
                    ResponseEntity.ok(CommandResponse(
                        command = "BLOCK",
                        address = addr,
                        success = true,
                        message = "Pumpe blokkert - volum: ${status.volumeLitres}L, beløp: ${status.amountKr} kr",
                        responseCode = status.state
                    ))
                },
                onFailure = { error ->
                    ResponseEntity.badRequest().body(ErrorResponse(
                        error = "BLOCK_REJECTED",
                        message = error.message ?: "Unknown error"
                    ))
                }
            )
        } catch (e: Exception) {
            logger.error("BLOCK failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "BLOCK_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Settle transaction - simulate payment.
     * NOTE: LAB/DEBUG only - not part of production payment flow.
     */
    @PostMapping("/settle/{addr}")
    fun settle(
        @PathVariable addr: Int,
        @RequestParam(defaultValue = "DEBUG") paymentMethod: String
    ): ResponseEntity<Any> {
        logger.info("SETTLE request for address $addr with method $paymentMethod")
        
        return try {
            val settled = pumpStateService.settle(addr, paymentMethod)
            
            if (settled != null) {
                ResponseEntity.ok(CommandResponse(
                    command = "SETTLE",
                    address = addr,
                    success = true,
                    message = "Betaling simulert: ${settled.liters}L = ${settled.amountNok} kr via $paymentMethod"
                ))
            } else {
                ResponseEntity.badRequest().body(ErrorResponse(
                    error = "SETTLE_NO_TRANSACTION",
                    message = "Ingen ventende transaksjon å gjøre opp"
                ))
            }
        } catch (e: Exception) {
            logger.error("SETTLE failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "SETTLE_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Reset pump to idle state.
     */
    @PostMapping("/reset/{addr}")
    fun reset(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("RESET request for address $addr")
        
        return try {
            pumpStateService.reset(addr)
            ResponseEntity.ok(CommandResponse(
                command = "RESET",
                address = addr,
                success = true,
                message = "Pumpe tilbakestilt til IDLE"
            ))
        } catch (e: Exception) {
            logger.error("RESET failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "RESET_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    /**
     * Send raw EHL STATE command to dispenser.
     */
    @GetMapping("/raw-state/{addr}")
    fun getRawState(@PathVariable addr: Int): ResponseEntity<Any> {
        logger.info("RAW STATE request for address $addr")
        
        return try {
            val response = runBlocking {
                val packet = EhlPacket(addr, EhlCommand.STATE)
                ehlCommunicator.sendAndReceive(packet, 3000)
            }
            
            ResponseEntity.ok(mapOf(
                "address" to addr,
                "command" to response.command.name,
                "data" to response.data.joinToString(" ") { "%02X".format(it) },
                "dataSize" to response.data.size
            ))
        } catch (e: Exception) {
            logger.error("RAW STATE failed for address $addr: ${e.message}")
            ResponseEntity.internalServerError().body(ErrorResponse(
                error = "RAW_STATE_FAILED",
                message = e.message ?: "Unknown error"
            ))
        }
    }
}
