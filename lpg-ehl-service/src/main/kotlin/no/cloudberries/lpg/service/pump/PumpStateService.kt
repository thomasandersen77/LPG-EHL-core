package no.cloudberries.lpg.service.pump

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.protocol.*
import no.cloudberries.lpg.service.event.*
import no.cloudberries.lpg.service.price.PriceService
import no.cloudberries.lpg.service.transaction.Transaction
import no.cloudberries.lpg.service.transaction.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Pump state service som kommuniserer med dispenser via EhlCommunicator.
 * 
 * Sender ekte EHL protokoll-kommandoer med HEX-logging:
 * - UNBLOCK: Frigir pumpe for leveranse
 * - BLOCK: Stopper leveranse
 * - VOLUME: Henter volumet under og etter pumping
 * 
 * Alle kommandoer logges med TX/RX HEX til Protocol-fanen.
 */
@Service
class PumpStateService(
    private val eventPublisher: EventPublisher,
    private val transactionService: TransactionService,
    private val ehlCommunicator: EhlCommunicator,
    private val dispenserEmulator: EhlDispenserEmulator?,  // Null i FIELD MODE
    private val priceService: PriceService,
    private val authorizationService: PumpAuthorizationService? = null  // Optional - for kortdragning-flow
) {
    private val logger = LoggerFactory.getLogger(PumpStateService::class.java)
    private val protocolLogger = LoggerFactory.getLogger("no.cloudberries.lpg.protocol")
    
    // Pump states
    private val pumpStates = ConcurrentHashMap<Int, PumpState>()
    
    // Current price (can be updated via /api/v1/prices/update)
    @Volatile
    var currentPriceKr: Double = 15.90
    
    // Flow rate for simulation (liters per second)
    private val flowRateLitersPerSecond = 0.5
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Last logged volume milestone (for 0.5L logging)
    private val lastLoggedMilestone = ConcurrentHashMap<Int, Double>()
    
    /**
     * Ved oppstart: Les pris fra database og synkroniser med emulator og UI.
     * 
     * Prioritet:
     * 1. Database (price_history) - persistent pris fra forrige sesjon
     * 2. Emulator default - hvis ingen DB-pris finnes
     * 3. Hardkodet 15.90 - fallback
     */
    @PostConstruct
    fun initializePriceFromDatabase() {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("🌟 OPPSTART: Initialiserer pris...")
        
        try {
            // Hent gjeldende pris fra PriceService
            val dbPrice = priceService.getCurrentPrice("LPG")
            
            if (dbPrice != null) {
                val priceKr = dbPrice.pricePerLiter.toDouble()
                currentPriceKr = priceKr
                
                logger.info("🏷️ STARTUP: Gjenopprettet pris $priceKr kr/L fra database")
                logger.info("   Satt av: ${dbPrice.createdBy ?: "ukjent"}")
                logger.info("   Gyldig fra: ${dbPrice.effectiveFrom}")
                
                // Publish price update event
                eventPublisher.publishPriceUpdate(priceKr)
                
            } else {
                // Ingen pris i DB - bruk emulator eller default
                dispenserEmulator?.let { emulator ->
                    currentPriceKr = emulator.getPricePerLitreKr()
                    logger.warn("🏷️ STARTUP: Ingen prishistorikk funnet. Bruker emulator-pris: $currentPriceKr kr/L")
                } ?: run {
                    logger.warn("🏷️ STARTUP: Ingen prishistorikk funnet. Bruker default: $currentPriceKr kr/L")
                }
                
                // Publish default price
                eventPublisher.publishPriceUpdate(currentPriceKr)
            }
        } catch (e: Exception) {
            logger.error("❌ Kunne ikke lese pris fra database: ${e.message}")
            logger.warn("🏷️ STARTUP: Bruker default pris: $currentPriceKr kr/L")
            eventPublisher.publishPriceUpdate(currentPriceKr)
        }
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    data class PumpState(
        val address: Int = 1,
        var state: String = "IDLE",
        var volumeLitres: Double = 0.0,
        var amountKr: Double = 0.0,
        var pricePerLitreKr: Double = 15.90,
        var nozzleLifted: Boolean = false,
        var hasPendingTransaction: Boolean = false,
        var pumpingStartTime: Instant? = null,
        var pendingTransactionId: UUID? = null,
        var authorizationId: UUID? = null,  // Linked authorization for kortdragning flow
        var unblockTime: Instant? = null,  // When UNBLOCK was sent (for 60s timeout)
        var timeoutJob: kotlinx.coroutines.Job? = null  // Coroutine job for timeout
    )
    
    data class PumpStatus(
        val state: String,
        val address: Int,
        val volumeLitres: Double,
        val amountKr: Double,
        val pricePerLitreKr: Double,
        val nozzleLifted: Boolean,
        val hasPendingTransaction: Boolean
    )
    
    /**
     * Get current pump status for an address.
     */
    fun getStatus(address: Int = 1): PumpStatus {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        return PumpStatus(
            state = state.state,
            address = state.address,
            volumeLitres = state.volumeLitres,
            amountKr = state.amountKr,
            pricePerLitreKr = state.pricePerLitreKr,
            nozzleLifted = state.nozzleLifted,
            hasPendingTransaction = state.hasPendingTransaction
        )
    }
    
    // STATE byte flags for dispenser status (from EHL protocol)
    companion object {
        const val STATE_FLAG_TRANSACTION_COMPLETE = 0x08  // Payment pending - transaction not settled
        const val STATE_FLAG_PUMPING = 0x04               // Currently pumping
        const val STATE_FLAG_AUTHORIZED = 0x02            // Authorized for pumping
        const val STATE_FLAG_NOZZLE_LIFTED = 0x01         // Nozzle is lifted
    }
    
    /**
     * Helper to log to SERVICE channel (WebSocket + console).
     */
    private fun serviceLog(level: LogLevel, message: String) {
        when (level) {
            LogLevel.ERROR -> logger.error(message)
            LogLevel.WARN -> logger.warn(message)
            LogLevel.INFO -> logger.info(message)
            LogLevel.DEBUG -> logger.debug(message)
            else -> logger.trace(message)
        }
        eventPublisher.publishLogEvent(LogEvent(
            channel = LogChannel.SERVICE,
            level = level,
            logger = "PumpStateService",
            message = message
        ))
    }
    
    /**
     * Unblock pump ("Fri pumpe") - Start pumping.
     * 
     * Sender UNBLOCK-kommando til dispenser via EhlCommunicator.
     * TX/RX HEX logges automatisk til Protocol-fanen.
     * 
     * VIKTIG: Validerer respons for å sikre at UNBLOCK faktisk ble akseptert:
     * - Sjekker at respons er OK
     * - Henter STATE og verifiserer at TRANSACTION_COMPLETE flag (0x08) ikke er satt
     * - Oppretter transaksjon KUN etter bekreftet UNBLOCK
     */
    fun unblock(address: Int = 1): Result<PumpStatus> {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        if (state.state == "PUMPING") {
            return Result.failure(IllegalStateException("Pump is already pumping"))
        }
        
        if (state.hasPendingTransaction) {
            return Result.failure(IllegalStateException("Previous transaction not settled"))
        }
        
        // Send UNBLOCK-kommando via EhlCommunicator (HEX logges automatisk)
        val unblockResponse: EhlPacket
        try {
            val unblockPacket = EhlPacket(address, EhlCommand.UNBLOCK, ByteArray(0))
            
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            protocolLogger.info("⛽ FRI PUMPE - Sender UNBLOCK til dispenser #$address")
            
            unblockResponse = runBlocking {
                ehlCommunicator.sendAndReceive(unblockPacket, 3000)
            }
            
            protocolLogger.info("📥 UNBLOCK Respons: ${unblockResponse.command}")
            
        } catch (e: Exception) {
            protocolLogger.error("❌ UNBLOCK FEILET: ${e.message}")
            return Result.failure(e)
        }
        
        // Steg 1: Valider at respons er OK
        if (unblockResponse.command != EhlCommand.OK) {
            protocolLogger.warn("⚠️ UNBLOCK avvist: Forventet OK, fikk ${unblockResponse.command}")
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return Result.failure(IllegalStateException("UNBLOCK rejected by dispenser: ${unblockResponse.command}"))
        }
        
        // Steg 2: Hent STATE for å verifisere at dispenser faktisk er unblocked
        val stateResponse: EhlPacket = try {
            val statePacket = EhlPacket(address, EhlCommand.STATE, ByteArray(0))
            protocolLogger.info("🔍 Verifiserer dispenser-state...")
            
            val response = runBlocking {
                ehlCommunicator.sendAndReceive(statePacket, 3000)
            }
            
            protocolLogger.info("📥 STATE Respons: ${response.command}, data=${response.data.joinToString(" ") { "%02X".format(it) }}")
            response
            
        } catch (e: Exception) {
            protocolLogger.warn("⚠️ Kunne ikke hente STATE etter UNBLOCK: ${e.message}")
            // Fortsett likevel - UNBLOCK ble akseptert, men returner tom STATE for å hoppe over validering
            EhlPacket(address, EhlCommand.OK, ByteArray(0))  // Return OK (not STATE) to skip validation
        }
        
        // Steg 3: Sjekk TRANSACTION_COMPLETE flag (0x08) i STATE-respons
        if (stateResponse.command == EhlCommand.STATE && stateResponse.data.isNotEmpty()) {
            val stateByte = stateResponse.data[0].toInt() and 0xFF
            
            if ((stateByte and STATE_FLAG_TRANSACTION_COMPLETE) != 0) {
                protocolLogger.error("❌ UNBLOCK AVVIST: Dispenser har ubetalt transaksjon (STATE=0x%02X, TRANSACTION_COMPLETE flag satt)".format(stateByte))
                protocolLogger.info("💳 Forrige transaksjon må betales før ny pumping kan starte")
                protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return Result.failure(IllegalStateException("Dispenser has pending payment - settle transaction before unblocking"))
            }
            
            // Log state flags for debugging
            val flagsDesc = buildString {
                if ((stateByte and STATE_FLAG_PUMPING) != 0) append("PUMPING ")
                if ((stateByte and STATE_FLAG_AUTHORIZED) != 0) append("AUTHORIZED ")
                if ((stateByte and STATE_FLAG_NOZZLE_LIFTED) != 0) append("NOZZLE_LIFTED ")
            }
            protocolLogger.info("✅ STATE validert: 0x%02X (%s)".format(stateByte, flagsDesc.ifEmpty { "IDLE" }))
        }
        
        protocolLogger.info("✅ UNBLOCK BEKREFTET - Dispenser klar for pumping")
        protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // === UNBLOCK ER NÅ BEKREFTET - FØRST NÅ OPPRETTER VI TRANSAKSJON ===
        
        // Cancel any pending timeout job
        state.timeoutJob?.cancel()
        state.timeoutJob = null
        state.unblockTime = Instant.now()
        
        // Pumpe er nå UNBLOCKED og klar til pumping
        state.state = "READY_TO_PUMP"  // Ny state: Venter på at kunde starter pumping
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.pricePerLitreKr = currentPriceKr
        
        // Create transaction in database with status STARTED - KUN etter bekreftet UNBLOCK
        try {
            val transaction = transactionService.createStartedTransaction(address, currentPriceKr)
            state.pendingTransactionId = transaction.transactionId
            serviceLog(LogLevel.INFO, "📝 Transaksjon opprettet: ID=${transaction.transactionId}, pris=${currentPriceKr} kr/L")
        } catch (e: Exception) {
            serviceLog(LogLevel.ERROR, "❌ Kunne ikke opprette transaksjon: ${e.message}")
        }
        
        // Find active authorization if any
        authorizationService?.let { authService ->
            val auth = authService.findActiveAuthorization(address)
            if (auth != null) {
                logger.info("📋 Pumping med autorisasjon: ${auth.authorizationId}")
                state.authorizationId = auth.authorizationId
                // Mark authorization as PUMPING
                authService.markPumping(auth.authorizationId)
            }
        }
        
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("⏱️ 60s TIMEOUT STARTED: Pump $address")
        logger.info("   Venter på at kunde starter pumping...")
        logger.info("═══════════════════════════════════════════════════════════")
        
        // Start 60-second timeout - auto BLOCK if pumping doesn't start
        state.timeoutJob = scope.launch {
            delay(60000)  // 60 seconds
            
            // Check if pumping has started
            if (state.state != "PUMPING") {
                logger.info("═══════════════════════════════════════════════════════════")
                logger.info("⏰ 60s TIMEOUT EXPIRED: Pump $address")
                logger.info("   Pumping ikke startet - sender BLOCK")
                logger.info("═══════════════════════════════════════════════════════════")
                
                try {
                    val blockPacket = EhlPacket(address, EhlCommand.BLOCK, ByteArray(0))
                    runBlocking {
                        ehlCommunicator.sendAndReceive(blockPacket, 3000)
                    }
                    
                    // Cancel STARTED transaction with 0 volume
                    val transactionId = state.pendingTransactionId
                    if (transactionId != null) {
                        try {
                            transactionService.updateTransactionVolume(
                                transactionId,
                                0.0,
                                0.0,
                                "CANCELLED"
                            )
                            logger.info("📝 Transaction $transactionId marked as CANCELLED (60s timeout)")
                        } catch (e: Exception) {
                            logger.warn("Could not cancel transaction: ${e.message}")
                        }
                    }
                    
                    state.state = "IDLE"
                    state.unblockTime = null
                    state.pendingTransactionId = null
                    
                    // Cancel authorization if exists
                    state.authorizationId?.let { authId ->
                        authorizationService?.cancel(authId, "60s timeout - pumping ikke startet")
                    }
                    state.authorizationId = null
                    
                    logger.info("🛑 BLOCK SENT: Pump $address blocked after 60s timeout")
                    broadcastStatus(state)
                } catch (e: Exception) {
                    logger.error("❌ Kunne ikke sende BLOCK etter timeout: ${e.message}")
                }
            }
        }
        
        serviceLog(LogLevel.INFO, "🔓 PUMPE FRIGJORT: Pump #$address klar til fylling (60s timeout startet)")
        broadcastStatus(state)
        
        return Result.success(getStatus(address))
    }
    
    /**
     * Simulate start pumping - for GUI /control panel in LAB MODE.
     * 
     * In LAB MODE: Called by frontend to simulate nozzle lift.
     * In FIELD/SOCAT MODE: Not used - pumping is triggered by hardware state detection.
     * 
     * @see pollStateForReadyPumps for hardware-driven detection
     */
    fun simulateStartPumping(address: Int = 1): Result<PumpStatus> {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        if (state.state != "READY_TO_PUMP") {
            return Result.failure(IllegalStateException("Pump must be in READY_TO_PUMP state (current: ${state.state})"))
        }
        
        logger.info("🎮 GUI SIMULATION: Start pumping requested for pump $address")
        transitionToPumping(state)
        
        return Result.success(getStatus(address))
    }
    
    /**
     * Block pump ("Stopp pumpe") - Stop pumping.
     * 
     * Sender BLOCK-kommando til dispenser via EhlCommunicator.
     * Henter finalt volum med VOLUME-kommando.
     * TX/RX HEX logges automatisk til Protocol-fanen.
     */
    fun block(address: Int = 1): Result<PumpStatus> {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        if (state.state != "PUMPING") {
            return Result.failure(IllegalStateException("Pump is not pumping"))
        }
        
        // Send BLOCK-kommando via EhlCommunicator (HEX logges automatisk)
        try {
            val blockPacket = EhlPacket(address, EhlCommand.BLOCK, ByteArray(0))
            
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            protocolLogger.info("🛑 STOPP PUMPE - Sender BLOCK til dispenser #$address")
            
            val response = runBlocking {
                ehlCommunicator.sendAndReceive(blockPacket, 3000)
            }
            
            protocolLogger.info("✅ BLOCK OK - Respons: ${response.command}")
            
            // Hent finalt volum
            val volumePacket = EhlPacket(address, EhlCommand.VOLUME, ByteArray(0))
            protocolLogger.info("📊 Henter finalt volum...")
            
            val volumeResponse = runBlocking {
                ehlCommunicator.sendAndReceive(volumePacket, 3000)
            }
            
            // Parse volum fra respons (VB6 format: 5 ASCII bytes LSB-first)
            if (volumeResponse.data.size >= 5) {
                val volumeCentilitres = parseVb6Volume(volumeResponse.data)
                state.volumeLitres = volumeCentilitres / 100.0
                state.amountKr = state.volumeLitres * state.pricePerLitreKr
                state.volumeLitres = (state.volumeLitres * 100).roundToInt() / 100.0
                state.amountKr = (state.amountKr * 100).roundToInt() / 100.0
                
                protocolLogger.info("📊 Finalt volum: ${state.volumeLitres} L = ${state.amountKr} kr")
            }
            
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
        } catch (e: Exception) {
            protocolLogger.error("❌ BLOCK FEILET: ${e.message}")
            // Continue anyway to update state
        }
        
        // Stop pumping
        state.state = "STOPPED"
        state.nozzleLifted = false
        state.pumpingStartTime = null
        
        // Update authorization if kortdragning flow
        val authId = state.authorizationId
        if (authId != null && state.volumeLitres > 0) {
            try {
                authorizationService?.markStopped(authId, state.volumeLitres, state.amountKr)
                protocolLogger.info("📋 Autorisasjon oppdatert til STOPPED: $authId")
            } catch (e: Exception) {
                logger.error("❌ Kunne ikke oppdatere autorisasjon: ${e.message}")
            }
        }
        
        // Update transaction in database with status PENDING
        val transactionId = state.pendingTransactionId
        if (transactionId != null && state.volumeLitres > 0) {
            try {
                transactionService.updateTransactionVolume(
                    transactionId, 
                    state.volumeLitres, 
                    state.amountKr, 
                    "PENDING"
                )
                serviceLog(LogLevel.INFO, "📋 Transaksjon oppdatert til PENDING: ID=$transactionId, ${state.volumeLitres} L = ${state.amountKr} kr")
            } catch (e: Exception) {
                serviceLog(LogLevel.ERROR, "❌ Kunne ikke oppdatere transaksjon: ${e.message}")
            }
        }
        
        // Mark transaction as pending if there was any volume
        if (state.volumeLitres > 0) {
            state.hasPendingTransaction = true
            state.state = "PAYMENT_PENDING"
            serviceLog(LogLevel.INFO, "🛑 Pumping stoppet: ${state.volumeLitres} L = ${state.amountKr} kr - venter betaling")
        } else {
            serviceLog(LogLevel.INFO, "🛑 Pumping stoppet: Ingen volum levert for pumpe #$address")
        }
        
        broadcastStatus(state)
        return Result.success(getStatus(address))
    }
    
    /**
     * Parse VB6 volume format: 5 ASCII bytes LSB-first (centilitres).
     * Example: [0x30, 0x35, 0x35, 0x34, 0x30] = "04550" reversed = 4550 cL = 45.50 L
     */
    private fun parseVb6Volume(data: ByteArray): Int {
        return try {
            val str = StringBuilder()
            for (i in 4 downTo 0) {
                str.append((data[i].toInt() and 0xFF).toChar())
            }
            str.toString().toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Settle pending transaction ("Simuler betaling").
     * 
     * Markerer eksisterende transaksjon som PAID i databasen.
     * Markerer også tilhørende autorisasjon som COMPLETED.
     */
    fun settle(address: Int = 1, paymentMethod: String = "CARD"): SettledTransaction? {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        if (!state.hasPendingTransaction) {
            logger.info("ℹ️ No pending transaction to settle for pump $address")
            return null
        }
        
        val settled = SettledTransaction(
            dispenserId = address,
            liters = state.volumeLitres,
            amountNok = state.amountKr,
            unitPrice = state.pricePerLitreKr,
            paymentMethod = paymentMethod,
            finishedAt = Instant.now(),
            idempotencyKey = UUID.randomUUID().toString()
        )
        
        // Mark authorization as COMPLETED (VIKTIG: Frigiør autorisasjon før ny kortdragning)
        val authorizationId = state.authorizationId
        if (authorizationId != null && authorizationService != null) {
            try {
                val auth = authorizationService.getStatus(authorizationId)
                if (auth != null && auth.status == AuthorizationStatus.STOPPED) {
                    authorizationService.confirmPayment(authorizationId, paymentMethod)
                    logger.info("✅ Autorisasjon $authorizationId markert som COMPLETED")
                } else {
                    logger.warn("⚠️ Autorisasjon $authorizationId er ikke i STOPPED status (${auth?.status}), hopper over")
                }
            } catch (e: Exception) {
                logger.warn("⚠️ Kunne ikke oppdatere autorisasjon: ${e.message}")
                // Continue anyway - transaction will still be marked as paid
            }
        }
        
        // Mark existing transaction as paid in database
        val transactionId = state.pendingTransactionId
        if (transactionId != null) {
            try {
                transactionService.markTransactionPaid(transactionId, paymentMethod)
                serviceLog(LogLevel.INFO, "💳 Betaling fullført: ${state.volumeLitres} L = ${state.amountKr} kr via $paymentMethod")
            } catch (e: Exception) {
                serviceLog(LogLevel.ERROR, "❌ Kunne ikke markere transaksjon som betalt: ${e.message}")
            }
        } else {
            // Fallback: Create new transaction if no pending ID (shouldn't happen normally)
            try {
                val transaction = Transaction(
                    dispenserAddress = address,
                    nozzleNumber = 1,
                    volumeDeciliters = (state.volumeLitres * 10).roundToInt(),
                    amountOre = (state.amountKr * 100).roundToInt(),
                    pricePerLiter = java.math.BigDecimal.valueOf(state.pricePerLitreKr),
                    paymentType = paymentMethod,
                    paymentStatus = "PAID",
                    productCode = "LPG",
                    includesRoadTax = true
                )
                transactionService.saveTransaction(transaction)
                serviceLog(LogLevel.INFO, "💾 Transaksjon lagret til database (fallback)")
            } catch (e: Exception) {
                serviceLog(LogLevel.ERROR, "❌ Kunne ikke lagre transaksjon: ${e.message}")
            }
        }
        
        // Reset pump state
        state.state = "IDLE"
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.hasPendingTransaction = false
        state.pendingTransactionId = null
        state.authorizationId = null
        lastLoggedMilestone.remove(address)
        
        logger.info("💳 Pump $address settled: ${settled.liters}L = ${settled.amountNok} kr via $paymentMethod")
        broadcastStatus(state)
        
        return settled
    }
    
    /**
     * Reset pump to idle state (without settling).
     */
    fun reset(address: Int = 1) {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        state.state = "IDLE"
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.pricePerLitreKr = currentPriceKr
        state.nozzleLifted = false
        state.hasPendingTransaction = false
        state.pumpingStartTime = null
        state.pendingTransactionId = null
        state.authorizationId = null
        lastLoggedMilestone.remove(address)
        
        logger.info("🔄 Pump $address reset to IDLE")
        broadcastStatus(state)
    }
    
    /**
     * Set pump to AUTHORIZED_WAITING state after card swipe.
     * 
     * This state indicates:
     * - Card has been swiped
     * - Authorization created
     * - 60s timeout started
     * - Pump is NOT unblocked yet - waiting for "FRI DISPENSER" button
     * 
     * UNBLOCK is only sent when user explicitly calls /pump/{address}/unblock
     */
    fun setAuthorizedWaiting(address: Int, authorizationId: UUID) {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        // Cancel any existing timeout
        state.timeoutJob?.cancel()
        
        state.state = "AUTHORIZED_WAITING"
        state.authorizationId = authorizationId
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.pricePerLitreKr = currentPriceKr
        
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("💳 KORTDRAGNING: Pump $address state -> AUTHORIZED_WAITING")
        logger.info("   Auth ID: $authorizationId")
        logger.info("   Venter på FRI DISPENSER (60s timeout)")
        logger.info("═══════════════════════════════════════════════════════════")
        
        // Start 60-second timeout for authorization
        state.timeoutJob = scope.launch {
            delay(60000)  // 60 seconds
            
            // Check if still in AUTHORIZED_WAITING (user hasn't clicked FRI DISPENSER)
            if (state.state == "AUTHORIZED_WAITING") {
                logger.info("═══════════════════════════════════════════════════════════")
                logger.info("⏰ 60s TIMEOUT EXPIRED: Pump $address")
                logger.info("   Bruker trykket ikke FRI DISPENSER - kansellerer autorisasjon")
                logger.info("═══════════════════════════════════════════════════════════")
                
                // Cancel authorization
                state.authorizationId?.let { authId ->
                    authorizationService?.cancel(authId, "60s timeout - FRI DISPENSER ikke trykket")
                }
                
                // Reset to IDLE
                state.state = "IDLE"
                state.authorizationId = null
                
                logger.info("❌ Autorisasjon kansellert - pump $address tilbake til IDLE")
                broadcastStatus(state)
            }
        }
        
        broadcastStatus(state)
    }
    
    /**
     * Confirm payment for pending transaction on pump.
     * Finds latest PENDING transaction for this dispenser and marks it as PAID.
     */
    fun confirmPayment(address: Int = 1, paymentMethod: String = "SIMULATION"): Result<Transaction> {
        val state = pumpStates.getOrPut(address) { PumpState(address = address, pricePerLitreKr = currentPriceKr) }
        
        // Find pending transaction
        val transactionId = state.pendingTransactionId
            ?: return Result.failure(IllegalStateException("Ingen ventende transaksjon funnet"))
        
        try {
            // Mark authorization as COMPLETED
            val authorizationId = state.authorizationId
            if (authorizationId != null && authorizationService != null) {
                try {
                    val auth = authorizationService.getStatus(authorizationId)
                    if (auth != null && auth.status == AuthorizationStatus.STOPPED) {
                        authorizationService.confirmPayment(authorizationId, paymentMethod)
                        logger.info("✅ Autorisasjon $authorizationId markert som COMPLETED")
                    } else {
                        logger.warn("⚠️ Autorisasjon $authorizationId er ikke i STOPPED status (${auth?.status}), hopper over")
                    }
                } catch (e: Exception) {
                    logger.warn("⚠️ Kunne ikke oppdatere autorisasjon: ${e.message}")
                }
            }
            
            // Mark transaction as PAID
            val paidTransaction = transactionService.markTransactionPaid(transactionId, paymentMethod)
            
            if (paidTransaction != null) {
                protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                protocolLogger.info("💳 BETALING SIMULERT")
                protocolLogger.info("   Transaksjon: $transactionId")
                protocolLogger.info("   Volum: ${state.volumeLitres} L")
                protocolLogger.info("   Beløp: ${state.amountKr} kr")
                protocolLogger.info("   Metode: $paymentMethod")
                protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                
                // Reset pump state
                state.state = "IDLE"
                state.volumeLitres = 0.0
                state.amountKr = 0.0
                state.hasPendingTransaction = false
                state.pendingTransactionId = null
                state.authorizationId = null
                lastLoggedMilestone.remove(address)
                
                logger.info("💳 Pump $address settled: ${paidTransaction.volumeLiters}L = ${paidTransaction.amountKr} kr via $paymentMethod")
                logger.info("🚀 Pumpe $address frigitt for neste kunde")
                broadcastStatus(state)
                
                return Result.success(paidTransaction)
            } else {
                return Result.failure(IllegalStateException("Kunne ikke markere transaksjon som betalt"))
            }
        } catch (e: Exception) {
            logger.error("❌ Feil ved bekreftelse av betaling: ${e.message}")
            return Result.failure(e)
        }
    }
    
    /**
     * Update price for all pumps.
     * Synkroniserer også med emulator i LAB MODE.
     * 
     * Prisen oppdateres for ALLE pumper uavhengig av tilstand.
     * Dette sikrer at GUI alltid viser korrekt pris.
     */
    fun updatePrice(priceKr: Double, roadTaxEnabled: Boolean = true) {
        currentPriceKr = priceKr
        
        // Oppdater pris for ALLE pumper (ikke bare IDLE)
        // GUI viser alltid gjeldende pris, uavhengig av pumpetilstand
        pumpStates.values.forEach { state ->
            state.pricePerLitreKr = priceKr
            // Broadcast oppdatert status til frontend
            broadcastStatus(state)
        }
        
        // Synkroniser med emulator hvis tilgjengelig (LAB MODE)
        dispenserEmulator?.let { emulator ->
            val priceOre = (priceKr * 100).toInt()
            emulator.setPrice(priceOre)
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            protocolLogger.info("💰 PRIS OPPDATERT")
            protocolLogger.info("   Ny pris: $priceKr kr/L")
            protocolLogger.info("   Vegavgift: ${if (roadTaxEnabled) "inkludert" else "ekskludert"}")
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
        
        logger.info("🚀 PRICE UPDATE: New price set to $priceKr kr/L (RoadTax: $roadTaxEnabled)")
        eventPublisher.publishPriceUpdate(priceKr)
    }
    
    /**
     * Scheduled task to poll dispenser state when pump is in READY_TO_PUMP.
     * Detects when customer starts pumping (raw state 0x06/0x07 = PUMPING).
     * Runs every 500ms.
     */
    @Scheduled(fixedRate = 500)
    fun pollStateForReadyPumps() {
        pumpStates.values.filter { it.state == "READY_TO_PUMP" }.forEach { state ->
            try {
                val statePacket = EhlPacket(state.address, EhlCommand.STATE, ByteArray(0))
                val response = runBlocking {
                    ehlCommunicator.sendAndReceive(statePacket, 1000)
                }
                
                if (response.data.isNotEmpty()) {
                    val rawState = response.data[0].toInt() and 0xFF
                    
                    // Check for PUMPING: bits 0x04 (DELIVERY_ACTIVE) + 0x02 (NOZZLE_LIFTED)
                    // Raw state 0x06 or 0x07 indicates pumping has started
                    val deliveryActive = (rawState and 0x04) != 0
                    val nozzleLifted = (rawState and 0x02) != 0
                    
                    if (deliveryActive && nozzleLifted) {
                        logger.info("⛽ HARDWARE PUMPING DETECTED: Raw state 0x%02X for pump %d".format(rawState, state.address))
                        transitionToPumping(state)
                    }
                }
            } catch (e: Exception) {
                // Timeout or error - continue polling
                logger.debug("State poll timeout for pump ${state.address}: ${e.message}")
            }
        }
    }
    
    /**
     * Internal method to transition from READY_TO_PUMP to PUMPING.
     * Called when hardware detects pumping has started.
     */
    private fun transitionToPumping(state: PumpState) {
        if (state.state != "READY_TO_PUMP") return
        
        // Cancel 60s timeout - pumping has started
        state.timeoutJob?.cancel()
        state.timeoutJob = null
        
        val previousState = state.state
        state.state = "PUMPING"
        state.nozzleLifted = true
        state.pumpingStartTime = Instant.now()
        lastLoggedMilestone[state.address] = 0.0
        
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("⛽ STATE TRANSITION: $previousState → PUMPING (pump ${state.address})")
        logger.info("   60s timeout cancelled - customer started pumping")
        logger.info("═══════════════════════════════════════════════════════════")
        
        broadcastStatus(state)
    }
    
    /**
     * Scheduled task to poll volume from dispenser when pump is active.
     * Runs every 500ms and logs every 0.5L milestone.
     * 
     * Sends VOLUME-kommando til emulator og logger HEX.
     */
    @Scheduled(fixedRate = 500)
    fun pollVolume() {
        pumpStates.values.filter { it.state == "PUMPING" }.forEach { state ->
            try {
                // Send VOLUME query to get current volume from emulator
                val volumePacket = EhlPacket(state.address, EhlCommand.VOLUME, ByteArray(0))
                
                val response = runBlocking {
                    ehlCommunicator.sendAndReceive(volumePacket, 1000)
                }
                
                // Parse volume from VB6 format
                if (response.data.size >= 5) {
                    val volumeCentilitres = parseVb6Volume(response.data)
                    state.volumeLitres = volumeCentilitres / 100.0
                    state.amountKr = state.volumeLitres * state.pricePerLitreKr
                    state.volumeLitres = (state.volumeLitres * 100).roundToInt() / 100.0
                    state.amountKr = (state.amountKr * 100).roundToInt() / 100.0
                    
                    // Log every 0.5L milestone
                    val lastMilestone = lastLoggedMilestone[state.address] ?: 0.0
                    val currentMilestone = (state.volumeLitres / 0.5).toInt() * 0.5
                    
                    if (currentMilestone > lastMilestone) {
                        protocolLogger.info("⛽ MILEPÆL: ${currentMilestone} L fyllt (${state.amountKr} kr)")
                        lastLoggedMilestone[state.address] = currentMilestone
                        
                        // Update transaction volume periodically
                        val transactionId = state.pendingTransactionId
                        if (transactionId != null) {
                            scope.launch {
                                try {
                                    transactionService.updateTransactionVolume(
                                        transactionId,
                                        state.volumeLitres,
                                        state.amountKr,
                                        null  // Keep status as STARTED
                                    )
                                } catch (e: Exception) {
                                    logger.debug("Could not update transaction volume: ${e.message}")
                                }
                            }
                        }
                    }
                }
                
                broadcastStatus(state)
            } catch (e: Exception) {
                // Timeout or error - continue with simulation fallback
                val volumeIncrement = flowRateLitersPerSecond * 0.5
                state.volumeLitres += volumeIncrement
                state.amountKr = state.volumeLitres * state.pricePerLitreKr
                state.volumeLitres = (state.volumeLitres * 100).roundToInt() / 100.0
                state.amountKr = (state.amountKr * 100).roundToInt() / 100.0
                
                broadcastStatus(state)
            }
        }
    }
    
    private fun broadcastStatus(state: PumpState) {
        val pumpEvent = PumpStatusEvent(
            address = state.address,
            state = state.state,
            volumeLitres = state.volumeLitres,
            amountKr = state.amountKr,
            pricePerLitreKr = state.pricePerLitreKr,
            nozzleLifted = state.nozzleLifted,
            hasPendingTransaction = state.hasPendingTransaction
        )
        eventPublisher.publishPumpStatusUpdate(pumpEvent)
    }
    
    data class SettledTransaction(
        val dispenserId: Int,
        val liters: Double,
        val amountNok: Double,
        val unitPrice: Double,
        val paymentMethod: String,
        val finishedAt: Instant,
        val idempotencyKey: String
    )
}
