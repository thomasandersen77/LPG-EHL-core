package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.emulator.service.TransactionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlin.concurrent.thread

@Service
class EmulatorService(
    @Value("\${emulator.address:1}") private val address: Int,
    @Value("\${emulator.price-per-litre-cents:1590}") private val pricePerLitreCents: Int,
    @Value("\${emulator.litres-per-second:0.5}") private val litresPerSecond: Double,
    @Value("\${emulator.port:9000}") private val port: Int,
    private val transactionPersistenceService: TransactionPersistenceService
) {
    private val logger = LoggerFactory.getLogger(EmulatorService::class.java)
    private val emulator = EhlDispenserEmulator(address, pricePerLitreCents, litresPerSecond)
    private val clientHandlers = ConcurrentHashMap<String, ClientHandler>()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isRunning = false

    @PostConstruct
    fun start() {
        try {
            serverSocket = ServerSocket(port)
            isRunning = true

            logger.info("=".repeat(80))
            logger.info("🚀 EHL EMULATOR STARTED - LEGACY INTEGRATION BRIDGE")
            logger.info("   Port: $port")
            logger.info("   Mode: Dual Protocol (EHL Binary + Legacy Text Tags)")
            logger.info("   Dispenser Address: $address")
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

        private val isFilling = AtomicBoolean(false)
        private val output = socket.getOutputStream()
        
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
                
                // Simulate nozzle lift to trigger PUMPING
                emulator.simulateNozzleLift(true)
                
                if (!isFilling.get()) {
                    isFilling.set(true)
                    logger.info("║ Status: IDLE → FILLING")
                    logger.info("║ Action: Starting simulation thread...")
                    logger.info("╚════════════════════════════════════════════════════════════")
                    
                    // Send svar at vi er i gang (Status 1 på index 4 = Frigitt)
                    val response = "<STATE_TANK>;00001000"
                    logger.info("📤 RESPONSE (legacy format): $response")
                    sendText(response)

                    // Start en tråd som teller opp liter og penger
                    thread(start = true, isDaemon = true) {
                        simulateFillingLoop()
                    }
                } else {
                    logger.info("║ Status: Already filling, ignoring")
                    logger.info("╚════════════════════════════════════════════════════════════")
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
                
                // Stopp fylling
                isFilling.set(false)
                
                // Save transaction to database (use tracked values from simulation)
                saveCurrentTransaction()
                
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

        private fun simulateFillingLoop() {
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
            
            while (isFilling.get() && isRunning) {
                Thread.sleep(1000) // Oppdater hvert sekund

                // Beregn ny status
                volume += litresPerSecond
                amount = volume * price
                updateCount++

                // Format: <TANK>;<Ignored>;<Beløp>;<Volum>;<Pris>;<BankVises>;<BankTekst>
                // Eksempel: <TANK>;0;15.90;1.00;15.90;1;BankTerminal...
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
            
            // Store final values for transaction save
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
            isFilling.set(false)
            try { socket.close() } catch (e: Exception) {}
        }
        
        private fun saveCurrentTransaction() {
            try {
                // Use final values from simulation
                val volumeDeciliters = (lastVolumeLitres * 10).toInt() // Convert L to dl
                val amountOre = (lastAmountKr * 100).toInt() // Convert kr to øre
                
                // Only save if there's actual volume
                if (volumeDeciliters > 0) {
                    transactionPersistenceService.saveTransaction(
                        dispenserAddress = address,
                        volumeDeciliters = volumeDeciliters,
                        amountOre = amountOre,
                        pricePerLiter = pricePerLitreCents
                    )
                    logger.info("💾 Transaction saved: ${lastVolumeLitres}L, ${lastAmountKr} kr")
                } else {
                    logger.debug("No volume dispensed, skipping transaction save")
                }
            } catch (e: Exception) {
                logger.error("Failed to save transaction", e)
            }
        }
    }

    fun getStatus(): Map<String, Any> = mapOf("clients" to clientHandlers.size)
    fun reset() = emulator.reset()
}
