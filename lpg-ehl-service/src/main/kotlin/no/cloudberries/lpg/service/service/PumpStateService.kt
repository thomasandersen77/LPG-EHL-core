package no.cloudberries.lpg.service.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import no.cloudberries.lpg.service.event.EventPublisher
import no.cloudberries.lpg.service.event.PumpStatusEvent
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.protocol.*
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
    private val priceService: PriceService
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
        var pendingTransactionId: UUID? = null
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
    
    /**
     * Unblock pump ("Fri pumpe") - Start pumping.
     * 
     * Sender UNBLOCK-kommando til dispenser via EhlCommunicator.
     * TX/RX HEX logges automatisk til Protocol-fanen.
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
        try {
            val unblockPacket = EhlPacket(address, EhlCommand.UNBLOCK, ByteArray(0))
            
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            protocolLogger.info("⛽ FRI PUMPE - Sender UNBLOCK til dispenser #$address")
            
            val response = runBlocking {
                ehlCommunicator.sendAndReceive(unblockPacket, 3000)
            }
            
            protocolLogger.info("✅ UNBLOCK OK - Respons: ${response.command}")
            protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
        } catch (e: Exception) {
            protocolLogger.error("❌ UNBLOCK FEILET: ${e.message}")
            return Result.failure(e)
        }
        
        // Start pumping
        state.state = "PUMPING"
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.pricePerLitreKr = currentPriceKr
        state.nozzleLifted = true
        state.pumpingStartTime = Instant.now()
        lastLoggedMilestone[address] = 0.0
        
        // Create transaction in database with status STARTED
        try {
            val transaction = transactionService.createStartedTransaction(address, currentPriceKr)
            state.pendingTransactionId = transaction.transactionId
            protocolLogger.info("📝 Transaksjon opprettet: ID=${transaction.transactionId}, status=STARTED")
        } catch (e: Exception) {
            logger.error("❌ Kunne ikke opprette transaksjon: ${e.message}")
        }
        
        logger.info("⛽ PUMPING START: Pump $address at ${state.pricePerLitreKr} kr/L")
        broadcastStatus(state)
        
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
                protocolLogger.info("📝 Transaksjon oppdatert: ID=$transactionId, status=PENDING")
            } catch (e: Exception) {
                logger.error("❌ Kunne ikke oppdatere transaksjon: ${e.message}")
            }
        }
        
        // Mark transaction as pending if there was any volume
        if (state.volumeLitres > 0) {
            state.hasPendingTransaction = true
            state.state = "PAYMENT_PENDING"
            logger.info("🛑 PUMPING STOP: Volume: ${state.volumeLitres}L, Amount: ${state.amountKr} kr")
        } else {
            logger.info("🛑 PUMPING STOP: No volume delivered for pump $address")
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
        
        // Mark existing transaction as paid in database
        val transactionId = state.pendingTransactionId
        if (transactionId != null) {
            try {
                transactionService.markTransactionPaid(transactionId, paymentMethod)
                protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                protocolLogger.info("💳 BETALING SIMULERT")
                protocolLogger.info("   Transaksjon: $transactionId")
                protocolLogger.info("   Volum: ${state.volumeLitres} L")
                protocolLogger.info("   Beløp: ${state.amountKr} kr")
                protocolLogger.info("   Metode: $paymentMethod")
                protocolLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                logger.error("❌ Failed to mark transaction as paid: ${e.message}")
            }
        } else {
            // Fallback: Create new transaction if no pending ID (shouldn't happen normally)
            try {
                val transaction = no.cloudberries.lpg.service.model.Transaction(
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
                logger.info("💾 Transaction saved to database (fallback)")
            } catch (e: Exception) {
                logger.error("❌ Failed to save transaction: ${e.message}")
            }
        }
        
        // Reset pump state
        state.state = "IDLE"
        state.volumeLitres = 0.0
        state.amountKr = 0.0
        state.hasPendingTransaction = false
        state.pendingTransactionId = null
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
        lastLoggedMilestone.remove(address)
        
        logger.info("🔄 Pump $address reset to IDLE")
        broadcastStatus(state)
    }
    
    /**
     * Update price for all pumps.
     * Synkroniserer også med emulator i LAB MODE.
     */
    fun updatePrice(priceKr: Double, roadTaxEnabled: Boolean = true) {
        currentPriceKr = priceKr
        pumpStates.values.forEach { state ->
            if (state.state == "IDLE") {
                state.pricePerLitreKr = priceKr
            }
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
