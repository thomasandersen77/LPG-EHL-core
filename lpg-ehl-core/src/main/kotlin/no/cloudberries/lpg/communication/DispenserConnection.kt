package no.cloudberries.lpg.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages communication with a single LPG dispenser.
 * Handles command queuing, response correlation, and connection state.
 */
class DispenserConnection(
    val address: Int,
    private val communicator: EhlCommunicator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val logger = LoggerFactory.getLogger(DispenserConnection::class.java)
    private val commandQueue = Channel<CommandRequest>(Channel.UNLIMITED)
    private val isProcessing = AtomicBoolean(false)
    
    var lastCommunication: Instant? = null
        private set
    
    var lastCommand: EhlCommand? = null
        private set
    
    var lastResponse: EhlPacket? = null
        private set

    /**
     * State of the dispenser connection
     */
    enum class ConnectionState {
        IDLE,           // No active communication
        SENDING,        // Sending command
        WAITING,        // Waiting for response
        ERROR,          // Communication error
        DISCONNECTED    // Not connected
    }

    var state: ConnectionState = ConnectionState.IDLE
        private set

    /**
     * Data class representing a command request with response handling
     */
    private data class CommandRequest(
        val packet: EhlPacket,
        val timeoutMs: Long,
        val responseChannel: CompletableDeferred<EhlPacket>
    )

    init {
        // Start command processing coroutine
        scope.launch {
            processCommands()
        }
    }

    /**
     * Send a command to the dispenser and wait for response.
     *
     * @param command EHL command to send
     * @param data Optional command data
     * @param timeoutMs Timeout in milliseconds
     * @return Response packet
     */
    suspend fun sendCommand(
        command: EhlCommand,
        data: ByteArray = ByteArray(0),
        timeoutMs: Long = 2000
    ): EhlPacket {
        val packet = EhlPacket(
            address = address,
            command = command,
            data = data
        )
        
        return sendPacket(packet, timeoutMs)
    }

    /**
     * Send a packet to the dispenser and wait for response.
     *
     * @param packet EHL packet to send
     * @param timeoutMs Timeout in milliseconds
     * @return Response packet
     */
    suspend fun sendPacket(packet: EhlPacket, timeoutMs: Long = 2000): EhlPacket {
        require(packet.address == address) {
            "Packet address ${packet.address} does not match dispenser address $address"
        }

        val responseDeferred = CompletableDeferred<EhlPacket>()
        val request = CommandRequest(packet, timeoutMs, responseDeferred)
        
        commandQueue.send(request)
        logger.debug("Queued command ${packet.command} for dispenser $address")
        
        return try {
            withTimeout(timeoutMs + 1000) { // Add extra time for queue processing
                responseDeferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            state = ConnectionState.ERROR
            throw Exception("Timeout waiting for response from dispenser $address", e)
        }
    }

    /**
     * Process queued commands sequentially
     */
    private suspend fun processCommands() {
        isProcessing.set(true)
        
        try {
            for (request in commandQueue) {
                try {
                    state = ConnectionState.SENDING
                    lastCommand = request.packet.command
                    
                    logger.debug("Sending command ${request.packet.command} to dispenser $address")
                    
                    val response = communicator.sendAndReceive(request.packet, request.timeoutMs)
                    
                    state = ConnectionState.IDLE
                    lastResponse = response
                    lastCommunication = Instant.now()
                    
                    logger.debug("Received response from dispenser $address: $response")
                    
                    request.responseChannel.complete(response)
                    
                } catch (e: Exception) {
                    state = ConnectionState.ERROR
                    logger.error("Error communicating with dispenser $address: ${e.message}", e)
                    request.responseChannel.completeExceptionally(e)
                }
            }
        } finally {
            isProcessing.set(false)
        }
    }

    /**
     * Query the dispenser state
     */
    suspend fun queryState(timeoutMs: Long = 2000): EhlPacket {
        return sendCommand(EhlCommand.STATE, timeoutMs = timeoutMs)
    }

    /**
     * Unblock the dispenser (start delivery mode)
     */
    suspend fun unblock(timeoutMs: Long = 2000): EhlPacket {
        return sendCommand(EhlCommand.UNBLOCK, timeoutMs = timeoutMs)
    }

    /**
     * Block the dispenser (stop/pause)
     */
    suspend fun block(timeoutMs: Long = 2000): EhlPacket {
        return sendCommand(EhlCommand.BLOCK, timeoutMs = timeoutMs)
    }

    /**
     * Program fuel price
     */
    suspend fun programPrice(priceData: ByteArray, timeoutMs: Long = 2000): EhlPacket {
        return sendCommand(EhlCommand.PROG_PRC, data = priceData, timeoutMs = timeoutMs)
    }

    /**
     * Program value preset (amount in øre)
     */
    suspend fun programValue(valueData: ByteArray, timeoutMs: Long = 2000): EhlPacket {
        return sendCommand(EhlCommand.PROG_W, data = valueData, timeoutMs = timeoutMs)
    }

    /**
     * Get the number of pending commands in queue
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPendingCommandCount(): Int {
        return commandQueue.isEmpty.let { if (it) 0 else 1 } // Channel doesn't expose size
    }

    /**
     * Check if dispenser is responsive
     */
    fun isResponsive(): Boolean {
        return state != ConnectionState.ERROR && 
               state != ConnectionState.DISCONNECTED &&
               lastCommunication != null
    }

    /**
     * Get time since last successful communication
     */
    fun getTimeSinceLastCommunication(): Long? {
        return lastCommunication?.let { 
            Instant.now().toEpochMilli() - it.toEpochMilli() 
        }
    }

    /**
     * Close the connection and cleanup resources
     */
    fun close() {
        logger.info("Closing connection to dispenser $address")
        commandQueue.close()
        scope.cancel()
        state = ConnectionState.DISCONNECTED
    }

    override fun toString(): String {
        return "DispenserConnection(address=$address, state=$state, lastCommand=$lastCommand, " +
                "lastCommunication=$lastCommunication)"
    }
}
