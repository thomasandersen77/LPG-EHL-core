package no.cloudberries.lpg.payment.terminal.sim.baxi

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.domain.Scenario
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioSelection
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioSource
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class BaxiTcpServer(
    private val config: SimulatorConfig,
    private val stateManager: TerminalStateManager,
    private val scenarioManager: ScenarioManager
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    @PostConstruct
    fun start() {
        if (!config.baxi.enabled) return
        running = true
        executor.execute {
            try {
                serverSocket = ServerSocket(config.baxi.port)
                log.info("Baxi TCP Server listening on port {}", config.baxi.port)
                while (running) {
                    val client = serverSocket?.accept()
                    if (client != null) {
                        executor.execute { handleClient(client) }
                    }
                }
            } catch (e: Exception) {
                if (running) log.error("Baxi TCP Server error", e)
            }
        }
    }

    @PreDestroy
    fun stop() {
        running = false
        serverSocket?.close()
        executor.shutdown()
    }

    private fun handleClient(socket: Socket) {
        log.info("Baxi client connected: {}", socket.remoteSocketAddress)
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        
        // Mark terminal as open/ready in simulator state
        stateManager.open()

        // Terminal usually sends Terminal Ready (12605) spontaneously on connection
        sendTerminalReady(output)

        try {
            while (running) {
                val frame = readFrame(input) ?: break
                handleFrame(frame, output)
            }
        } catch (e: Exception) {
            log.debug("Client disconnected: {}", e.message)
        } finally {
            socket.close()
        }
    }

    private fun sendTerminalReady(output: OutputStream) {
        log.info("Sending TERMINAL_READY (12605) to client")
        // 0x49 (SendData) + 12605 (TERMINAL_READY = "1=") + 8 filler + ";"
        val payload = "1=00000000;".toByteArray(Charsets.UTF_8)
        val frame = ByteArray(1 + payload.size)
        frame[0] = 0x49
        System.arraycopy(payload, 0, frame, 1, payload.size)
        sendFrame(frame, output)
    }

    private fun readFrame(input: InputStream): ByteArray? {
        val h = input.read()
        if (h == -1) return null
        val l = input.read()
        if (l == -1) return null
        val len = ((h and 0xFF) shl 8) or (l and 0xFF)
        if (len < 0 || len > 10000) {
            log.error("Invalid frame length: {}", len)
            return null
        }
        val data = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = input.read(data, read, len - read)
            if (n == -1) break
            read += n
        }
        return data
    }

    private fun sendFrame(data: ByteArray, output: OutputStream) {
        val len = data.size
        log.info("Sending Baxi frame of length {}: {}", len, data.map { String.format("%02X", it) }.joinToString(" "))
        output.write((len shr 8) and 0xFF)
        output.write(len and 0xFF)
        output.write(data)
        output.flush()
    }

    private fun handleFrame(data: ByteArray, output: OutputStream) {
        if (data.isEmpty()) return
        val cmd = data[0].toInt() and 0xFF
        log.debug("Received Baxi command: 0x{}", Integer.toHexString(cmd))
        
        when (cmd) {
            0x41 -> handleOpen(output)
            0x51 -> handleTransferAmount(data, output)
            0x53 -> handleAdministration(data, output)
            0x43 -> handleClose(output)
            else -> log.warn("Unknown Baxi command: 0x{}", Integer.toHexString(cmd))
        }
    }

    private fun handleOpen(output: OutputStream) {
        log.info("Baxi Open received")
        stateManager.open()
        // Send ACK for Open
        sendFrame(byteArrayOf(0x41, '1'.toByte()), output)
        
        // Simuler onTerminalReady etter litt tid
        executor.schedule({
            sendTerminalReady(output)
        }, 500, TimeUnit.MILLISECONDS)
    }

    private fun handleClose(output: OutputStream) {
        log.info("Baxi Close received")
        stateManager.close()
        sendFrame(byteArrayOf(0x43, '1'.toByte()), output)
    }

    private fun handleTransferAmount(data: ByteArray, output: OutputStream) {
        val s = String(data, 1, data.size - 1)
        log.info("Baxi TransferAmount received: {}", s)
        
        // Send ACK immediately
        sendFrame(byteArrayOf(0x51, '1'.toByte()), output)

        executor.execute {
            val opId = "baxi-${System.currentTimeMillis()}"
            try {
                val scenarioSelection = getScenario()
                stateManager.beginOperation(opId)
                
                // Sekvens av meldinger
                sendDisplay("SETT INN KORT", output)
                Thread.sleep(1000)
                sendDisplay("TASTER PIN", output)
                Thread.sleep(1500)
                sendDisplay("VENNLIGST VENT", output)
                
                val delay = scenarioManager.getOperationDelay(scenarioSelection)
                Thread.sleep(delay)

                if (scenarioSelection.enumScenario == Scenario.APPROVED) {
                    sendDisplay("GODKJENT", output)
                    sendPrint("***************************\n   LPG NORGE\n   GODKJENT\n***************************", output)
                    sendLocalMode(1, "00", output)
                    sendFinancialResult(1, "OK", output)
                } else {
                    sendDisplay("AVVIST", output)
                    sendLocalMode(0, "05", output)
                    sendFinancialResult(0, "FAILED", output)
                }
            } catch (e: Exception) {
                log.error("Error in TransferAmount simulation", e)
            } finally {
                stateManager.endOperation(opId)
            }
        }
    }

    private fun handleAdministration(data: ByteArray, output: OutputStream) {
        val s = String(data, 1, data.size - 1)
        log.info("Baxi Administration received: {}", s)
        sendFrame(byteArrayOf(0x53, '1'.toByte()), output)
        
        executor.execute {
            sendDisplay("ADMINISTRASJON", output)
            Thread.sleep(1000)
            sendLocalMode(1, "00", output)
            sendFinancialResult(1, "OK", output)
        }
    }

    private fun sendDisplay(text: String, output: OutputStream) {
        // 0x41 + 3 bytes filler + text
        val textBytes = text.toByteArray()
        val frame = ByteArray(1 + 3 + textBytes.size)
        frame[0] = 0x41
        frame[1] = 0x20
        frame[2] = 0x20
        frame[3] = 0x20
        System.arraycopy(textBytes, 0, frame, 4, textBytes.size)
        sendFrame(frame, output)
    }

    private fun sendPrint(text: String, output: OutputStream) {
        // 0x42 + 3 bytes filler + text
        val textBytes = text.toByteArray()
        val frame = ByteArray(1 + 3 + textBytes.size)
        frame[0] = 0x42
        frame[1] = 0x20
        frame[2] = 0x20
        frame[3] = 0x20
        System.arraycopy(textBytes, 0, frame, 4, textBytes.size)
        sendFrame(frame, output)
    }

    private fun sendLocalMode(result: Int, responseCode: String, output: OutputStream) {
        // 0x44 + Result;Timestamp;VerificationMethod;SessionNumber;StanAuth;SequenceNumber;TotalAmount;RejectionSource;RejectionReason;;;TerminalID;AcquirerMerchantID;;ResponseCode
        // tokens[14] is ResponseCode. result is at tokens[0].
        val data = "$result;;;;;;;;;;;;;;$responseCode".toByteArray()
        val frame = ByteArray(data.size + 1)
        frame[0] = 0x44
        System.arraycopy(data, 0, frame, 1, data.size)
        sendFrame(frame, output)
    }

    private fun sendFinancialResult(result: Int, data: String, output: OutputStream) {
        // 0x45 + Result;Data
        val payload = "$result;$data".toByteArray()
        val frame = ByteArray(payload.size + 1)
        frame[0] = 0x45
        System.arraycopy(payload, 0, frame, 1, payload.size)
        sendFrame(frame, output)
    }

    private fun getScenario(): ScenarioSelection {
        val name = config.defaultScenario
        val enumValue = try { Scenario.valueOf(name.uppercase()) } catch(e: Exception) { Scenario.APPROVED }
        return ScenarioSelection(name, enumValue, null, ScenarioSource.DEFAULT)
    }

    private fun java.util.concurrent.ExecutorService.schedule(command: () -> Unit, delay: Long, unit: TimeUnit) {
        val sched = Executors.newSingleThreadScheduledExecutor()
        sched.schedule({
            command()
            sched.shutdown()
        }, delay, unit)
    }
}
