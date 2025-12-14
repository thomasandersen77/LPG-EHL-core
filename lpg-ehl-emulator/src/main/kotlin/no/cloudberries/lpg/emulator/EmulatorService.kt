package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Service
class EmulatorService(
    @Value("\${emulator.address:1}") private val address: Int,
    @Value("\${emulator.price-per-litre-cents:1590}") private val pricePerLitreCents: Int,
    @Value("\${emulator.litres-per-second:0.5}") private val litresPerSecond: Double,
    @Value("\${emulator.port:9000}") private val port: Int
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
            logger.info("🚀 EHL Emulator started on port $port")
            logger.info("   Address: $address")
            logger.info("   Price: ${pricePerLitreCents / 100.0} kr/L")
            logger.info("   Flow rate: $litresPerSecond L/s")
            logger.info("=".repeat(80))
            
            // Start accepting connections in a separate thread
            Thread {
                acceptConnections()
            }.apply {
                name = "EmulatorAcceptThread"
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            logger.error("Failed to start emulator server", e)
            throw e
        }
    }

    private fun acceptConnections() {
        val socket = serverSocket ?: return
        
        while (isRunning && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                val clientId = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                logger.info("📱 Client connected: $clientId")
                
                val handler = ClientHandler(clientSocket, clientId)
                clientHandlers[clientId] = handler
                
                Thread(handler).apply {
                    name = "EmulatorClientHandler-$clientId"
                    isDaemon = true
                    start()
                }
            } catch (e: Exception) {
                if (isRunning) {
                    logger.error("Error accepting connection", e)
                }
            }
        }
    }

    @PreDestroy
    fun stop() {
        isRunning = false
        
        // Close all client connections
        clientHandlers.values.forEach { it.close() }
        clientHandlers.clear()
        
        // Close server socket
        serverSocket?.close()
        
        logger.info("🛑 EHL Emulator stopped")
    }

    inner class ClientHandler(
        private val socket: Socket,
        private val clientId: String
    ) : Runnable {
        
        override fun run() {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val buffer = ByteArray(1024)
                
                while (isRunning && !socket.isClosed) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    
                    val request = buffer.copyOf(bytesRead)
                    val responses = emulator.onBytesFromHost(request)
                    
                    responses.forEach { response ->
                        output.write(response)
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    logger.error("Error handling client $clientId", e)
                }
            } finally {
                close()
                clientHandlers.remove(clientId)
                logger.info("📱 Client disconnected: $clientId")
            }
        }
        
        fun close() {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun getStatus(): Map<String, Any> {
        return mapOf(
            "running" to isRunning,
            "port" to port,
            "address" to address,
            "pricePerLitreCents" to pricePerLitreCents,
            "litresPerSecond" to litresPerSecond,
            "connectedClients" to clientHandlers.size
        )
    }

    fun reset() {
        emulator.reset()
        logger.info("🔄 Emulator reset")
    }
}
