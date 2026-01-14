package no.cloudberries.lpg.pls.sim

import com.fazecast.jSerialComm.SerialPort
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Handles serial port communication using jSerialComm.
 */
class SerialPortHandler(
    private val portName: String,
    private val baud: Int,
    private val mode: FrameMode,
    private val chunked: Boolean,
    private val latencyMs: Int,
    private val logHex: Boolean
) {
    private val log = LoggerFactory.getLogger(SerialPortHandler::class.java)
    
    private var serialPort: SerialPort? = null
    private val running = AtomicBoolean(false)
    private var readerThread: Thread? = null
    
    private val state = PlsState()
    private val frameExtractor = FrameExtractor(mode, logHex)

    companion object {
        const val READ_TIMEOUT_MS = 100
    }

    /**
     * Open serial port and start reading.
     */
    fun start() {
        log.info("Opening serial port: {} at {} baud, mode={}, chunked={}", portName, baud, mode, chunked)

        val port = SerialPort.getCommPort(portName)
        
        // Configure port: 8N1, no parity
        port.baudRate = baud
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY
        
        // Semi-blocking read with timeout
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            READ_TIMEOUT_MS,
            0
        )

        if (!port.openPort()) {
            throw RuntimeException("Failed to open serial port: $portName")
        }

        serialPort = port
        log.info("Serial port opened successfully: {}", port.systemPortName)

        running.set(true)
        readerThread = Thread({ readLoop() }, "pls-sim-reader")
        readerThread?.start()
    }

    /**
     * Stop reading and close port.
     */
    fun stop() {
        log.info("Stopping serial port handler...")
        running.set(false)
        readerThread?.join(1000)
        serialPort?.closePort()
        serialPort = null
        log.info("Serial port closed")
    }

    /**
     * Main read loop - runs in dedicated thread.
     */
    private fun readLoop() {
        val buffer = ByteArray(256)
        
        while (running.get()) {
            try {
                // Optional latency jitter
                if (latencyMs > 0) {
                    Thread.sleep(Random.nextLong(0, latencyMs.toLong() + 1))
                }

                val port = serialPort ?: break
                val bytesRead = port.readBytes(buffer, buffer.size)
                
                if (bytesRead > 0) {
                    val received = buffer.copyOfRange(0, bytesRead)
                    
                    if (logHex) {
                        log.info("RX: {} bytes: {}", bytesRead, received.toHexString())
                    }

                    // Extract frames
                    val frames = frameExtractor.append(received)
                    
                    for (frame in frames) {
                        processFrame(frame)
                    }
                }
            } catch (e: InterruptedException) {
                log.debug("Reader thread interrupted")
                break
            } catch (e: Exception) {
                log.error("Error in read loop: {}", e.message, e)
                Thread.sleep(100)  // Avoid tight loop on error
            }
        }
        
        log.debug("Read loop exited")
    }

    /**
     * Process a complete frame.
     */
    private fun processFrame(frame: ByteArray) {
        if (mode == FrameMode.EHL) {
            // Binary EHL protocol
            if (logHex) {
                log.info("EHL frame received: {}", frame.toHexString())
            }
            
            val ehlFrame = EhlFrameCodec.decode(frame)
            if (ehlFrame == null) {
                log.warn("Invalid EHL frame - ignoring")
                return
            }
            
            log.info("EHL command: 0x{} from addr 0x{}", ehlFrame.cmd.toHex(), ehlFrame.addr.toHex())
            
            val result = state.processEhlCommand(ehlFrame)
            val response = when (result) {
                is EhlCommandResult.OkAck -> {
                    EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_OK)
                }
                is EhlCommandResult.StateResponse -> {
                    EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_STATE, result.data)
                }
                is EhlCommandResult.VolumeResponse -> {
                    EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_VOLUME, result.data)
                }
            }
            
            sendResponse(response)
            
        } else {
            // ASCII protocol (LINE or STX_ETX)
            val command = String(frame, Charsets.US_ASCII)
            log.info("Frame received: '{}'", command)

            val result = state.processCommand(command)
            
            val response = when (result) {
                is CommandResult.OK -> buildResponse("OK")
                is CommandResult.ACK -> buildResponse("ACK")
                is CommandResult.Status -> buildResponse(if (result.blocked) "BLOCKED" else "UNBLOCKED")
                is CommandResult.Ignored -> null
            }

            if (response != null) {
                sendResponse(response)
            }
        }
    }
    
    private fun Byte.toHex(): String = "%02X".format(this)

    /**
     * Build response according to mode (not used for EHL).
     */
    private fun buildResponse(text: String): ByteArray {
        return when (mode) {
            FrameMode.LINE -> "$text\n".toByteArray(Charsets.US_ASCII)
            FrameMode.STX_ETX -> {
                val payload = text.toByteArray(Charsets.US_ASCII)
                ByteArray(payload.size + 2).also { buf ->
                    buf[0] = FrameExtractor.STX
                    payload.copyInto(buf, 1)
                    buf[buf.size - 1] = FrameExtractor.ETX
                }
            }
            FrameMode.EHL -> {
                // EHL mode uses EhlFrameCodec.encode() directly
                throw IllegalStateException("buildResponse should not be called in EHL mode")
            }
        }
    }

    /**
     * Send response, optionally in chunks for realism.
     */
    private fun sendResponse(response: ByteArray) {
        val port = serialPort ?: return

        if (logHex) {
            log.info("TX: {} bytes: {}", response.size, response.toHexString())
        }

        if (chunked && response.size > 1) {
            writeChunked(port, response)
        } else {
            port.writeBytes(response, response.size)
            if (mode != FrameMode.EHL) {
                log.info("TX: '{}'", String(response, Charsets.US_ASCII).trim())
            } else {
                log.info("TX: EHL frame {} bytes", response.size)
            }
        }
    }

    /**
     * Write response in random 2-5 chunks with 5-30ms delays.
     */
    private fun writeChunked(port: SerialPort, response: ByteArray) {
        val numChunks = Random.nextInt(2, 6).coerceAtMost(response.size)
        val chunkSize = response.size / numChunks
        
        var offset = 0
        var chunkNum = 0
        
        while (offset < response.size) {
            val remaining = response.size - offset
            val thisChunk = if (chunkNum == numChunks - 1) {
                remaining  // Last chunk gets remainder
            } else {
                chunkSize.coerceAtMost(remaining)
            }
            
            val chunk = response.copyOfRange(offset, offset + thisChunk)
            port.writeBytes(chunk, chunk.size)
            
            if (logHex) {
                log.debug("TX chunk {}: {}", chunkNum + 1, chunk.toHexString())
            }
            
            offset += thisChunk
            chunkNum++
            
            if (offset < response.size) {
                Thread.sleep(Random.nextLong(5, 31))
            }
        }
        
        if (mode != FrameMode.EHL) {
            log.info("TX (chunked {}x): '{}'", chunkNum, String(response, Charsets.US_ASCII).trim())
        } else {
            log.info("TX (chunked {}x): EHL frame {} bytes", chunkNum, response.size)
        }
    }
}
