Ja – ideen om en emulator er spot on, og det passer perfekt med slik du allerede har bygget EhlCodec/EhlCommunicator. Jeg lager både design og konkret Kotlin-kode du kan lime rett inn i prosjektet.

⸻

1. Anbefalt design – hvordan emulatoren bør funke

Mål: Kunne teste all protokoll-logikk (EHL) uten fysisk dispenser, men på en måte som ligner mest mulig på virkeligheten.

a) Separat “virtuell seriellport”

Første grep er å løsne EhlCommunicator fra den konkrete SerialPortManager:

interface SerialPortIO {
    val isConnected: Boolean
    fun connect(): Boolean
    fun disconnect()
    fun write(data: ByteArray): Int
    fun read(maxBytes: Int = 256): ByteArray
    fun flush()
}

	•	Produksjon: SerialPortManager implementerer SerialPortIO (samme metoder som i dag, bare legger til : SerialPortIO).
	•	Test: Vi lager en InMemorySerialPort som implementerer SerialPortIO, men snakker med emulator-objektet i stedet for jSerialComm.

Da kan EhlCommunicator endres til å ta et interface:

class EhlCommunicator(private val serialPort: SerialPortIO) { ... }

Det er en liten refaktorering, men gjør testingen enormt mye enklere.

b) EHL-emulator som “fake dispenser”

Emulatoren:
	•	har en intern state machine (IDLE → READY → DELIVERING → FINISHED / ERROR),
	•	tar imot rå bytes fra “kontrolleren”,
	•	bruker din egen EhlCodec.decode til å parse disse,
	•	svarer med EhlPacket som kodes med EhlCodec.encode,
	•	støtter i første omgang:
	•	STATE (kode 75),
	•	UNBLOCK (kode 119 – start levering),
	•	STOP (kode 47),
	•	VOLUME (kode 69 – mengde).

Det er mer enn nok til å:
	•	verifisere at protokollen er korrekt implementert,
	•	kjøre hele “UNBLOCK → fylling → STOP → STATE/VOLUME”-flyten,
	•	teste feilsituasjoner (du kan senere legge inn “force timeout”, “feil checksum” osv).

⸻

2. Kotlin-kode: in-memory serial + EHL-emulator

2.1 Interface + in-memory “seriellport”

SerialPortIO.kt:

package no.cloudberries.lpg.communication

interface SerialPortIO {
    val isConnected: Boolean
    fun connect(): Boolean
    fun disconnect()
    fun write(data: ByteArray): Int
    fun read(maxBytes: Int = 256): ByteArray
    fun flush()
}

Gjør så eksisterende SerialPortManager til:

class SerialPortManager(private val config: SerialPortConfig) : SerialPortIO {
    // resten av filen din som før – bare legg til : SerialPortIO på klassen
}

In-memory “port” til tester:

package no.cloudberries.lpg.communication

import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import java.util.concurrent.ConcurrentLinkedQueue

class InMemorySerialPort(
    private val emulator: EhlDispenserEmulator
) : SerialPortIO {

    private val toEmulator = ConcurrentLinkedQueue<Byte>()
    private val fromEmulator = ConcurrentLinkedQueue<Byte>()
    private var connected = false

    override val isConnected: Boolean
        get() = connected

    override fun connect(): Boolean {
        connected = true
        return true
    }

    override fun disconnect() {
        connected = false
        toEmulator.clear()
        fromEmulator.clear()
    }

    override fun write(data: ByteArray): Int {
        check(connected) { "Port not connected" }

        // Legg data i kø til emulatoren
        data.forEach { toEmulator.add(it) }

        // La emulatoren behandle det som har kommet inn
        val inBytes = ByteArray(toEmulator.size) { toEmulator.poll() }
        val responses = emulator.onBytesFromHost(inBytes)

        responses.forEach { frame ->
            frame.forEach { b -> fromEmulator.add(b) }
        }

        return data.size
    }

    override fun read(maxBytes: Int): ByteArray {
        check(connected) { "Port not connected" }

        if (fromEmulator.isEmpty()) return ByteArray(0)

        val result = mutableListOf<Byte>()
        while (result.size < maxBytes && !fromEmulator.isEmpty()) {
            result.add(fromEmulator.poll())
        }
        return result.toByteArray()
    }

    override fun flush() {
        // ingenting å gjøre i memory-variant
    }
}

2.2 Selve EHL-emulatoren

EhlDispenserEmulator.kt:

package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.protocol.EhlCodec
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketParseResult
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

class EhlDispenserEmulator(
    private val address: Int = 1,
    private val pricePerLitreCents: Int = 1126,      // 11,26 kr/l
    private val litresPerSecond: Double = 0.5        // “falsk” flow til testing
) {
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulator::class.java)

    private var state: DispenserState = DispenserState.IDLE
    private var startedAtMs: Long? = null

    private var volumeLitres: Double = 0.0
    private var amountCents: Int = 0

    enum class DispenserState(val code: Int) {
        IDLE(0),
        READY(1),
        DELIVERING(2),
        FINISHED(3),
        ERROR(9)
    }

    fun reset() {
        state = DispenserState.IDLE
        startedAtMs = null
        volumeLitres = 0.0
        amountCents = 0
    }

    /**
     * Ta imot rå bytes fra "kontrolleren" (din Kotlin-kode) og
     * returner en liste med rå svarpakker som skal leses av.
     */
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()

        return when (val parsed = EhlCodec.decode(bytes)) {
            is EhlPacketParseResult.Success -> {
                logger.debug("Emulator mottok: ${parsed.packet}")
                handlePacket(parsed.packet).map { EhlCodec.encode(it) }
            }
            is EhlPacketParseResult.Incomplete -> {
                logger.warn("Emulator fikk ufullstendig pakke")
                emptyList()
            }
            is EhlPacketParseResult.ChecksumError -> {
                logger.warn("Emulator checksum-feil: ${parsed.expected} vs ${parsed.actual}")
                listOf(buildErrorPacket(0x01))   // 0x01 = “checksum error” e.l.
            }
            is EhlPacketParseResult.InvalidFormat -> {
                logger.warn("Emulator invalid format: ${parsed.reason}")
                listOf(buildErrorPacket(0x02))
            }
        }
    }

    private fun handlePacket(packet: EhlPacket): List<EhlPacket> {
        if (packet.address != address) {
            // Feil adresse – ignorerer
            return emptyList()
        }

        return when (packet.command) {
            EhlCommand.STATE   -> listOf(buildStateResponse())
            EhlCommand.UNBLOCK -> handleUnblock(packet)
            EhlCommand.STOP    -> handleStop(packet)
            EhlCommand.VOLUME  -> listOf(buildVolumeResponse())
            else               -> listOf(buildErrorPacket(0x10)) // unsupported command
        }
    }

    private fun handleUnblock(packet: EhlPacket): List<EhlPacket> {
        // Simpel logikk: hvis vi er IDLE/READY -> start levering
        if (state == DispenserState.IDLE || state == DispenserState.READY || state == DispenserState.FINISHED) {
            logger.info("Emulator: UNBLOCK – starter levering")
            state = DispenserState.DELIVERING
            startedAtMs = System.currentTimeMillis()
            volumeLitres = 0.0
            amountCents = 0
        }
        // Svar med OK + STATE
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }

    private fun handleStop(packet: EhlPacket): List<EhlPacket> {
        if (state == DispenserState.DELIVERING) {
            logger.info("Emulator: STOP – avslutter levering")
            updateDelivery() // regn ut endelig volum/beløp
            state = DispenserState.FINISHED
        }
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse(),
            buildVolumeResponse()
        )
    }

    /** Oppdatér volum/beløp basert på tid siden start (for enkel simulering). */
    private fun updateDelivery() {
        val start = startedAtMs ?: return
        val seconds = (System.currentTimeMillis() - start) / 1000.0
        volumeLitres = (seconds * litresPerSecond).coerceAtLeast(0.0)
        amountCents = (volumeLitres * pricePerLitreCents).roundToInt()
    }

    private fun buildStateResponse(): EhlPacket {
        // Oppdatér under levering slik at STATE gir “live” status
        if (state == DispenserState.DELIVERING) {
            updateDelivery()
        }
        val data = byteArrayOf(state.code.toByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }

    private fun buildVolumeResponse(): EhlPacket {
        // eksempel: send liter i desiliter og beløp i øre som 4-byte binær felt
        val volDeci = (volumeLitres * 10).roundToInt()
        val data = ByteArray(4)
        data[0] = ((volDeci shr 8) and 0xFF).toByte()
        data[1] = (volDeci and 0xFF).toByte()
        data[2] = ((amountCents shr 8) and 0xFF).toByte()
        data[3] = (amountCents and 0xFF).toByte()
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }

    private fun buildErrorPacket(code: Int): EhlPacket {
        val data = byteArrayOf(code.toByte())
        return EhlPacket(address, EhlCommand.ERROR, data)
    }
}

Tallformatet i buildVolumeResponse() er “påfunn” bare for emulatoren sin del – poenget er å ha konsistent bytes du kan teste encode/decode på. Når du har full forståelse av VB-koden, kan du endre data-layouten til å matche nøyaktig EHL-spesifikasjon.

⸻

3. Eksempel: hvordan teste mot emulatoren

En enkel JUnit-test:

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.InMemorySerialPort
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.Test

class EhlEmulatorIntegrationTest {

    @Test
    fun `unblock, deliver litt, og stop gir fornuftige svar`() = runBlocking {
        val emulator = EhlDispenserEmulator()
        val port = InMemorySerialPort(emulator)
        val comm = EhlCommunicator(port)

        port.connect()

        // 1) be om STATE
        comm.send(EhlPacket(1, EhlCommand.STATE))
        val s1 = comm.receive()
        println("STATE1 = $s1")

        // 2) UNBLOCK
        comm.send(EhlPacket(1, EhlCommand.UNBLOCK))
        val ack = comm.receive()
        val stateAfterUnblock = comm.receive()

        // 3) vent litt (simulert fylling)
        Thread.sleep(1500)

        // 4) STOP
        comm.send(EhlPacket(1, EhlCommand.STOP))
        val stopAck = comm.receive()
        val finalState = comm.receive()
        val volume = comm.receive()

        port.disconnect()
    }
}


⸻

4. Oppsummert
	•	Ja – det er helt realistisk å lage en Kotlin-emulator for EHL-protokollen basert på VB-koden du har.
	•	Forslaget over:
	•	holder protokoll-logikken i din eksisterende EhlCodec,
	•	gir deg en in-memory seriellport for tester,
	•	gir en enkel, men utvidbar state machine for dispenseren.
	•	Når du senere har tolket flere detaljer fra fra_dispenser.bas, kan du:
	•	justere state-kodene,
	•	endre dataformat på VOLUME/PRICE,
	•	legge til flere kommandoer og feilsituasjoner.

Hvis du vil, kan jeg neste gang ta én konkret bit av VB-koden (for eksempel Case 75/STATE) og oversette den direkte til Kotlin-logikk inne i emulatoren, så du har én 1:1-portet del å bruke som “mal” for resten.
