package no.cloudberries.lpg.emulator

import kotlinx.coroutines.*
import no.cloudberries.lpg.emulator.api.LpgApiClient
import no.cloudberries.lpg.emulator.api.SaveTransactionRequest
import no.cloudberries.lpg.emulator.service.TransactionSink
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Service
class EmulatorService(
    @Value("\${station.id:S000}") private val stationId: String,
    @Value("\${edge.id:EDGE-LOCAL}") private val edgeId: String,
    @Value("\${dispenser.id:D001}") private val dispenserId: String,
    @Value("\${emulator.address:1}") private val address: Int,
    @Value("\${emulator.price-per-litre-cents:1590}") private val pricePerLitreCents: Int,
    @Value("\${emulator.litres-per-second:0.5}") private val litresPerSecond: Double,
    @Value("\${emulator.port:9000}") private val port: Int,
    private val transactionSink: TransactionSink,
    private val lpgApiClient: LpgApiClient
) {
    private val logger = LoggerFactory.getLogger(EmulatorService::class.java)
    private val emulator = EhlDispenserEmulator(
        stationId = stationId,
        edgeId = edgeId,
        dispenserId = dispenserId,
        address = address,
        pricePerLitreCents = pricePerLitreCents,
        litresPerSecond = litresPerSecond
    )
    private val clientHandlers = ConcurrentHashMap<String, ClientHandler>()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isRunning = false

    @PostConstruct
    fun start() {
        try {
            // Bind to IPv4 explicitly for Windows compatibility (Parallels networking)
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("0.0.0.0", port))
            }
            isRunning = true

            logger.info("=".repeat(80))
            logger.info("🚀 EHL EMULATOR STARTED - MULTI-STATION EDGE DEVICE")
            logger.info("   Station ID: $stationId")
            logger.info("   Edge ID: $edgeId")
            logger.info("   Dispenser ID: $dispenserId")
            logger.info("   EHL Address: $address")
            logger.info("   Port: $port")
            logger.info("   Mode: Dual Protocol (EHL Binary + Legacy Text Tags)")
            logger.info("   Price: ${pricePerLitreCents / 100.0} NOK/L")
            logger.info("   Flow Rate: $litresPerSecond L/s")
            logger.info("   Ready to accept connections from Windows Dispenserkontroll...")
            logger.info("=".repeat(80))

            Thread { acceptConnections() }.apply {
                name = "EmulatorAcceptThread"
                isDaemon = false
                start()
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to start emulator server", e)
            throw e
        }
    }

    private fun acceptConnections() {
        val socket = serverSocket ?: return
        logger.info("👂 Listening for connections on port $port...")
        
        while (isRunning && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                
                logger.info("┌─────────────────────────────────────────────────────────────")
                logger.info("│ 📱 NEW CLIENT CONNECTION")
                logger.info("│ ID: $clientId")
                logger.info("│ From: ${clientSocket.inetAddress.hostName}")
                logger.info("│ Active clients: ${clientHandlers.size + 1}")
                logger.info("└─────────────────────────────────────────────────────────────")

                val handler = ClientHandler(clientSocket, clientId)
                clientHandlers[clientId] = handler

                Thread(handler).apply {
                    name = "Client-$clientId"
                    isDaemon = true
                    start()
                }
            } catch (e: Exception) {
                if (isRunning) logger.error("❌ Error accepting connection", e)
            }
        }
    }
    
    /**
     * Broadcast a legacy text message to all connected Windows clients.
     * Used to notify Windows Dispenserkontroll of state changes (e.g., reset after payment).
     * 
     * This method is fail-safe: if one client fails, others still receive the message.
     * 
     * @param message Text message (without newline)
     */
    private fun broadcastLegacy(message: String) {
        if (clientHandlers.isEmpty()) {
            logger.debug("📭 No clients connected - skipping broadcast")
            return
        }
        
        logger.info("📢 Broadcasting to ${clientHandlers.size} client(s): $message")
        
        var successCount = 0
        var failCount = 0
        
        clientHandlers.values.forEach { client ->
            runCatching { 
                client.sendLegacy(message)
                successCount++
            }.onFailure { e -> 
                logger.warn("⚠️ Failed to broadcast to one client: ${e.message}")
                failCount++
            }
        }
        
        logger.info("✅ Broadcast complete: $successCount OK, $failCount failed")
    }

    @PreDestroy
    fun stop() {
        logger.info("┌─────────────────────────────────────────────────────────────")
        logger.info("│ 🛑 SHUTTING DOWN EMULATOR")
        logger.info("└─────────────────────────────────────────────────────────────")
        
        isRunning = false
        
        logger.info("   Closing ${clientHandlers.size} active client(s)...")
        clientHandlers.values.forEach { it.close() }
        clientHandlers.clear()
        
        serverSocket?.close()
        logger.info("   Server socket closed")
        logger.info("✅ EHL Emulator stopped cleanly")
    }

    inner class ClientHandler(
        private val socket: Socket,
        private val clientId: String
    ) : Runnable {

        private val output = socket.getOutputStream()
        
        // Coroutine scope and job for simulation
        private val simulationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var simulationJob: Job? = null
        
        /**
         * Send a legacy text message to this client.
         * Thread-safe for concurrent broadcasts.
         * 
         * @param message Text message WITHOUT newline (will be added automatically)
         */
        @Synchronized
        fun sendLegacy(message: String) {
            try {
                synchronized(output) {
                    output.write((message + "\n").toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                logger.debug("📤 Sent legacy to $clientId: $message")
            } catch (e: Exception) {
                logger.error("❌ Failed to send legacy to $clientId", e)
                throw e  // Re-throw so broadcast can log failure
            }
        }
        
        // Track final transaction totals
        @Volatile
        private var lastVolumeLitres: Double = 0.0
        @Volatile
        private var lastAmountKr: Double = 0.0

        override fun run() {
            logger.info("🔌 Client handler started for $clientId")
            
            try {
                val input = socket.getInputStream()
                val buffer = ByteArray(1024)

                while (isRunning && !socket.isClosed) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) {
                        logger.debug("📭 End of stream from $clientId")
                        break
                    }

                    val rawData = buffer.copyOf(bytesRead)
                    val textCommand = String(rawData).trim()

                    logger.info("📥 RECEIVED from $clientId (${bytesRead} bytes)")
                    
                    // --- SJEKK: Er det en gammel tekst-kommando? (<...>) ---
                    if (textCommand.startsWith("<")) {
                        logger.info("   ├─ Protocol: LEGACY TEXT")
                        logger.info("   └─ Command: $textCommand")
                        handleLegacyCommand(textCommand)
                    } else {
                        logger.info("   ├─ Protocol: EHL BINARY")
                        logger.info("   └─ Hex: ${rawData.joinToString(" ") { "%02X".format(it) }}")
                        
                        // --- Standard EHL Binær Protocoll ---
                        val responses = emulator.onBytesFromHost(rawData)
                        logger.info("   📤 Sending ${responses.size} response(s)")
                        responses.forEach { sendBytes(it) }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) logger.error("❌ Error handling client $clientId", e)
            } finally {
                close()
                clientHandlers.remove(clientId)
                
                logger.info("┌─────────────────────────────────────────────────────────────")
                logger.info("│ 📱 CLIENT DISCONNECTED")
                logger.info("│ ID: $clientId")
                logger.info("│ Remaining clients: ${clientHandlers.size}")
                logger.info("└─────────────────────────────────────────────────────────────")
            }
        }

        // --- TOLKEN: Oversetter tekstkommandoer til handling VIA CORE ---
        private fun handleLegacyCommand(cmd: String) {
            logger.info("╔════════════════════════════════════════════════════════════")
            logger.info("║ 🏛️  LEGACY COMMAND HANDLER (via Core)")
            logger.info("╠════════════════════════════════════════════════════════════")

            if (cmd.contains("TANK_DISP_UNBLOCK")) {
                logger.info("║ Command: UNBLOCK (Start Fueling)")
                logger.info("║ Translation: UNBLOCK (0x77) → Core")
                
                // Translate to EHL binary command and send to Core
                val unblockPacket = no.cloudberries.lpg.protocol.EhlPacketBuilder.createUnblock(address)
                val encodedPacket = no.cloudberries.lpg.protocol.EhlCodec.encode(unblockPacket)
                
                logger.info("║ 📤 Sending to Core: ${encodedPacket.joinToString(" ") { "%02X".format(it) }}")
                
                // Process through Core
                val coreResponses = emulator.onBytesFromHost(encodedPacket)
                logger.info("║ ✅ Core returned ${coreResponses.size} response(s)")
                
                // CRITICAL: Check if there's a pending transaction
                val pendingTx = emulator.getPendingTransaction()
                
                if (pendingTx != null) {
                    // Emulator denied UNBLOCK due to pending payment
                    logger.error("║ ❌ UNBLOCK BLOCKED: Payment pending")
                    logger.error("║ ❌ Pending transaction: ${pendingTx.amountNok} NOK")
                    logger.error("╩════════════════════════════════════════════════════════════")
                    
                    // Send IDLE state to Windows (blocked)
                    val response = "<STATE_TANK>;00000000"
                    logger.info("📤 RESPONSE (legacy format): $response")
                    sendText(response)
                    return
                }
                
                // Simulate nozzle lift to trigger PUMPING
                emulator.simulateNozzleLift(true)
                
                if (simulationJob == null || simulationJob?.isActive == false) {
                    logger.info("║ Status: IDLE → FILLING")
                    logger.info("║ Action: Starting simulation coroutine...")
                    logger.info("╩════════════════════════════════════════════════════════════")
                    
                    // Send svar at vi er i gang (Status 1 på index 4 = Frigitt)
                    val response = "<STATE_TANK>;00001000"
                    logger.info("📤 RESPONSE (legacy format): $response")
                    sendText(response)

                    // Start simulation coroutine
                    startSimulation()
                } else {
                    logger.info("║ Status: Already filling, ignoring")
                    logger.info("╩════════════════════════════════════════════════════════════")
                }
            }
            else if (cmd.contains("TANK_DISP_STOP")) {
                logger.info("║ Command: STOP (Stop Fueling)")
                logger.info("║ Translation: BLOCK (0x69) → Core")
                
                // Translate to EHL binary command
                val blockPacket = no.cloudberries.lpg.protocol.EhlPacketBuilder.createBlock(address)
                val encodedPacket = no.cloudberries.lpg.protocol.EhlCodec.encode(blockPacket)
                
                logger.info("║ 📤 Sending to Core: ${encodedPacket.joinToString(" ") { "%02X".format(it) }}")
                
                // Process through Core
                val coreResponses = emulator.onBytesFromHost(encodedPacket)
                logger.info("║ ✅ Core returned ${coreResponses.size} response(s)")
                logger.info("╚════════════════════════════════════════════════════════════")
                
                // Simulate nozzle holster
                emulator.simulateNozzleLift(false)
                
                // Stop simulation immediately
                stopSimulation()
                
                // Send final TANK message with frozen values BEFORE state change
                val pendingTx = emulator.getPendingTransaction()
                if (pendingTx != null) {
                    val price = pricePerLitreCents / 100.0
                    val tankMsg = String.format(
                        "<TANK>;0;%.2f;%.2f;%.2f;0;",
                        pendingTx.amountNok, pendingTx.liters, price
                    ).replace(',', '.')
                    logger.info("📤 Sending frozen values to Windows: $tankMsg")
                    sendText(tankMsg)
                }
                
                // Enqueue frozen transaction if any
                enqueuePendingTransaction()
                
                val response = "<STATE_TANK>;00000000"
                logger.info("📤 RESPONSE (legacy format): $response")
                sendText(response) // Status idle
            }
            else if (cmd.contains("NOTAX")) {
                logger.info("║ Command: NOTAX (Tax Configuration - No-op)")
                logger.info("║ Action: Acknowledged, ignored in emulator")
                logger.info("╚════════════════════════════════════════════════════════════")
            }
            else {
                logger.info("║ Command: UNKNOWN")
                logger.info("║ Raw: $cmd")
                logger.info("║ Action: No handler, ignoring")
                logger.info("╚════════════════════════════════════════════════════════════")
            }
        }

        /**
         * Start fuel simulation coroutine.
         * Uses isActive to allow immediate cancellation.
         */
        private fun startSimulation() {
            // Cancel any existing job
            simulationJob?.cancel()
            
            simulationJob = simulationScope.launch {
                var volume = 0.0
                var amount = 0.0
                val price = pricePerLitreCents / 100.0

                val startTime = System.currentTimeMillis()
                
                logger.info("┌──────────────────────────────────────────────────────────")
                logger.info("│ ⛽ FUEL SIMULATION STARTED")
                logger.info("│ Price: $price NOK/L")
                logger.info("│ Flow rate: $litresPerSecond L/s")
                logger.info("│ Updates: Every 1 second")
                logger.info("└──────────────────────────────────────────────────────────")

                var updateCount = 0
                
                // Use isActive for immediate cancellation
                while (isActive && isRunning) {
                    delay(1000) // Suspend instead of Thread.sleep

                    // Check again after delay
                    if (!isActive) break

                    // Beregn ny status
                    volume += litresPerSecond
                    amount = volume * price
                    updateCount++

                    // Format: <TANK>;<Ignored>;<Beløp>;<Volum>;<Pris>;<BankVises>;<BankTekst>
                    val msg = String.format(
                        "<TANK>;0;%.2f;%.2f;%.2f;1;Kort Godkjent",
                        amount, volume, price
                    ).replace(',', '.') // Pass på punktum som desimaltegn!

                    logger.info("⛽ Update #$updateCount: %.2f L @ %.2f NOK = %.2f NOK".format(volume, price, amount))
                    logger.debug("   📤 Sending: $msg")
                    sendText(msg)
                }
                
                // Calculate final elapsed time
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                
                // Store final values
                lastVolumeLitres = volume
                lastAmountKr = amount
                
                logger.info("┌──────────────────────────────────────────────────────────")
                logger.info("│ 🏁 FUEL SIMULATION STOPPED")
                logger.info("│ Final volume: %.2f L".format(volume))
                logger.info("│ Final amount: %.2f NOK".format(amount))
                logger.info("│ Duration: %.1f seconds".format(elapsedSeconds))
                logger.info("│ Updates sent: $updateCount")
                logger.info("└──────────────────────────────────────────────────────────")
            }
        }
        
        /**
         * Stop simulation immediately by cancelling the job.
         */
        private fun stopSimulation() {
            simulationJob?.cancel()
            simulationJob = null
            logger.debug("⏸️ Simulation stopped")
        }
        
        /**
         * Enqueue pending transaction to TransactionSink for async persistence.
         */
        private fun enqueuePendingTransaction() {
            val tx = emulator.getPendingTransaction() ?: return
            
            logger.info("📥 Enqueueing transaction ${tx.idempotencyKey} for async save")
            transactionSink.enqueue(tx)
        }

        private fun sendText(text: String) {
            try {
                synchronized(output) {
                    output.write(text.toByteArray())
                    output.flush()
                }
            } catch (e: Exception) {
                logger.error("Failed to send text", e)
            }
        }

        private fun sendBytes(bytes: ByteArray) {
            try {
                synchronized(output) {
                    output.write(bytes)
                    output.flush()
                }
                logger.debug("   ✅ Binary response sent: ${bytes.size} bytes")
            } catch (e: Exception) {
                logger.error("❌ Failed to send binary response", e)
            }
        }

        fun close() {
            stopSimulation()
            simulationScope.cancel()
            try { socket.close() } catch (e: Exception) {}
        }
    }

    fun getStatus(): Map<String, Any> = mapOf("clients" to clientHandlers.size)
    fun reset() = emulator.reset()
    
    /**
     * Update the price per litre in the emulator.
     * Only takes effect for FUTURE deliveries (not ongoing ones).
     * 
     * @param priceCents New price in cents (øre)
     */
    fun updatePrice(priceCents: Int) {
        emulator.setPrice(priceCents)
        logger.info("💰 Price updated via EmulatorService: ${priceCents / 100.0} NOK/L")
    }
    
    /**
     * Get current emulator price in cents.
     */
    fun getPriceCents(): Int = emulator.getPriceCents()
    
    /**
     * Settle pending transaction and reset dispenser to IDLE.
     * 
     * @param method Payment method ("CARD" or "CREDIT")
     * @return The settled transaction, or null if no transaction was pending
     */
    fun settle(method: String = "CARD") = emulator.settleAndReset(method)
    
    // ==========================================================================
    // FRI PUMPE API - Direct pump control for field testing
    // ==========================================================================
    
    // Coroutine scope for Fri Pumpe simulation
    private val friPumpeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var friPumpeSimulationJob: Job? = null
    
    // Protocol logger for WebSocket streaming
    private val protocolLogger = org.slf4j.LoggerFactory.getLogger("no.cloudberries.lpg.protocol.EhlPacketStream")
    
    /**
     * Get current pump status.
     */
    fun getPumpStatus(): EhlDispenserEmulator.PumpStatus = emulator.getPumpStatus()
    
    /**
     * "Fri pumpe" - Unlock dispenser and start pumping.
     * Used for field testing when no PLS/terminal is available.
     * 
     * @return Result with new state or error message
     */
    fun unblockPump(): Result<EhlDispenserEmulator.PumpStatus> {
        logger.info("🔓 FRI PUMPE: unblockPump() called via API")
        val result = emulator.directUnblock()
        
        // Start simulation loop if successful
        result.onSuccess { status ->
            if (status.state == "PUMPING") {
                startFriPumpeSimulation()
            }
        }
        
        return result
    }
    
    /**
     * "Stopp pumpe" - Block dispenser and stop pumping.
     * Used for field testing when no PLS/terminal is available.
     * 
     * @return Result with final state and transaction data
     */
    fun blockPump(): Result<EhlDispenserEmulator.PumpStatus> {
        logger.info("🛑 FRI PUMPE: blockPump() called via API")
        
        // Stop simulation first
        stopFriPumpeSimulation()
        
        val result = emulator.directBlock()
        
        // CRITICAL: Save transaction to PostgreSQL via HTTP API call
        result.onSuccess { status ->
            if (status.hasPendingTransaction) {
                val completedTx = emulator.getPendingTransaction()
                if (completedTx != null) {
                    logger.info("💾 Persisting transaction ${completedTx.idempotencyKey} to PostgreSQL via API")
                    
                    // Create API request
                    val request = SaveTransactionRequest(
                        stationId = completedTx.stationId,
                        edgeId = completedTx.edgeId,
                        dispenserId = completedTx.dispenserId,
                        dispenserAddress = completedTx.dispenserAddress,
                        nozzleNumber = 1,
                        volumeDeciliters = (completedTx.liters * 10).toInt(),
                        amountOre = (completedTx.amountNok * 100).toInt(),
                        pricePerLiter = completedTx.unitPrice.toInt(),
                        productCode = "LPG",
                        includesRoadTax = true
                    )
                    
                    // Save to PostgreSQL via HTTP API (handles Azure sync automatically)
                    val transactionId = lpgApiClient.saveTransaction(request)
                    
                    if (transactionId != null) {
                        // Update the completedTx with database ID for later payment update
                        completedTx.databaseId = transactionId
                        logger.info("✅ Transaction saved to database via API: ID=$transactionId")
                    } else {
                        logger.error("❌ Failed to save transaction to database via API")
                    }
                    
                    // Also enqueue to TransactionSink for any legacy consumers
                    transactionSink.enqueue(completedTx)
                }
            }
        }
        
        return result
    }
    
    /**
     * Start Fri Pumpe simulation - logs volume/amount every second AND per whole liter.
     */
    private fun startFriPumpeSimulation() {
        // Cancel any existing simulation
        friPumpeSimulationJob?.cancel()
        
        friPumpeSimulationJob = friPumpeScope.launch {
            val price = pricePerLitreCents / 100.0
            var updateCount = 0
            var lastWholeLiter = 0  // Track last logged whole liter
            
            logger.info("┌" + "─".repeat(60) + "┐")
            logger.info("│ ⛽ FRI PUMPE: LEVERING STARTET" + " ".repeat(28) + "│")
            logger.info("│ Pris: %.2f kr/L | Flow: %.2f L/s".format(price, litresPerSecond).padEnd(59) + "│")
            logger.info("└" + "─".repeat(60) + "┘")
            
            while (isActive) {
                delay(1000) // Update every second
                
                if (!isActive) break
                
                // Get current status from emulator
                val status = emulator.getPumpStatus()
                
                // Stop if no longer pumping
                if (status.state != "PUMPING") {
                    logger.info("⛽ Simulation ended - state changed to ${status.state}")
                    break
                }
                
                updateCount++
                
                // Check if we passed a new whole liter
                val currentWholeLiter = status.volumeLitres.toInt()
                if (currentWholeLiter > lastWholeLiter) {
                    // Log each new liter milestone
                    for (liter in (lastWholeLiter + 1)..currentWholeLiter) {
                        val amountAtLiter = liter * price
                        logger.info("🔔 LITER $liter: %.2f kr (totalt så langt)".format(amountAtLiter))
                        protocolLogger.info("🔔 LITER_MILESTONE: $liter L | %.2f kr".format(amountAtLiter))
                    }
                    lastWholeLiter = currentWholeLiter
                }
                
                // Log to console (Emulator channel)
                logger.info("⛽ [#$updateCount] %.2f L | %.2f kr".format(status.volumeLitres, status.amountKr))
                
                // Log to Protocol channel (for WebSocket)
                protocolLogger.info("📥 VOLUME: %.2f L | AMOUNT: %.2f kr | PRICE: %.2f kr/L".format(
                    status.volumeLitres, status.amountKr, status.pricePerLitreKr
                ))
            }
            
            // Final status
            val finalStatus = emulator.getPumpStatus()
            logger.info("┌" + "─".repeat(60) + "┐")
            logger.info("│ 🏁 FRI PUMPE: LEVERING STOPPET" + " ".repeat(27) + "│")
            logger.info("│ Totalt: %.2f L @ %.2f kr".format(finalStatus.volumeLitres, finalStatus.amountKr).padEnd(59) + "│")
            logger.info("│ Oppdateringer sendt: $updateCount".padEnd(59) + "│")
            logger.info("└" + "─".repeat(60) + "┘")
        }
    }
    
    /**
     * Stop Fri Pumpe simulation.
     */
    private fun stopFriPumpeSimulation() {
        friPumpeSimulationJob?.cancel()
        friPumpeSimulationJob = null
    }
    
    /**
     * Settle pending transaction and broadcast reset to all Windows clients.
     * 
     * This is the CRITICAL method that solves the "Windows shows old values" problem.
     * 
     * Flow:
     * 1. Settle transaction internally (emulator.settleAndReset())
     * 2. Broadcast <TANK> with 0.00 / 0.00 to Windows
     * 3. Broadcast <STATE_TANK> with idle state to Windows
     * 
     * @param method Payment method ("CARD" or "CREDIT")
     * @return Settled transaction, or null if no pending transaction
     */
    fun settleAndBroadcast(method: String = "CARD"): no.cloudberries.lpg.emulator.service.CompletedTransaction? {
        logger.info("┌────────────────────────────────────────────────────────────")
        logger.info("│ 💳 SETTLE AND BROADCAST")
        logger.info("│ Method: $method")
        logger.info("└────────────────────────────────────────────────────────────")
        
        // 1. Settle internal state via emulator
        val settledTransaction = emulator.settleAndReset(method)
        
        if (settledTransaction == null) {
            logger.warn("⚠️ No transaction to settle - broadcast skipped")
            return null
        }
        
        logger.info("✅ Transaction settled: ${settledTransaction.liters} L @ ${settledTransaction.amountNok} NOK")
        
        // 2. Update payment status in PostgreSQL via HTTP API
        if (settledTransaction.databaseId != null) {
            logger.info("💾 Updating payment status via API for ID: ${settledTransaction.databaseId}")
            val updateSuccess = lpgApiClient.updatePaymentStatus(
                transactionId = settledTransaction.databaseId.toString(),
                paymentMethod = method
            )
            if (updateSuccess) {
                logger.info("✅ Payment status updated via API: $method / PAID")
            } else {
                logger.warn("⚠️ Failed to update payment status via API for: ${settledTransaction.databaseId}")
            }
        } else {
            logger.error("❌ Transaction database ID not available - payment status NOT updated")
        }
        
        // 3. Broadcast reset to all Windows clients
        logger.info("📢 Broadcasting reset to Windows clients...")
        
        val price = String.format(java.util.Locale.US, "%.2f", pricePerLitreCents / 100.0)
        
        // CRITICAL: <RESTART> tag triggers Windows Dispenserkontroll to reset its internal state
        // This is the legacy-correct way to clear the Windows UI completely
        broadcastLegacy("<RESTART>;00000000;<SLUTT>")
        
        // <TANK> format: <TANK>;<ignored>;<beløp>;<volum>;<pris>;<bank_status>;<bank_text>
        // Windows parser: parts[2]=amount, parts[3]=volume, parts[4]=price
        // Send TANK with all zeros to clear the display
        broadcastLegacy("<TANK>;0;0.00;0.00;$price;1;Kort;0;")
        
        // <STATE_TANK> format: 8-character string
        // Index 4 = '0' means idle (not released)
        broadcastLegacy("<STATE_TANK>;00000000")
        
        // Terminal message for user feedback
        broadcastLegacy("<TANK_TERMINAL_MESSAGE>;KLAR FOR NY FYLLING;<SLUTT>")
        
        logger.info("✅ Broadcast complete - Windows should now show 0.00 / 0.00")
        logger.info("🔄 <RESTART> sent - Windows Dispenserkontroll reset")
        logger.info("🟢 Dispenser ready for next customer")
        
        return settledTransaction
    }
}
