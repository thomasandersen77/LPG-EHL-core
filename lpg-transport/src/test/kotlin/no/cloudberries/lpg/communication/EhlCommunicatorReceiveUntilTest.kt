package no.cloudberries.lpg.communication

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCodec
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.StatusBitMasks
import no.cloudberries.lpg.transport.SerialTransport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Tests for receiveUntil: interleaved VOLUME, concatenated frames, missing OK, delayed open-bit.
 * Verifies robust UNBLOCK-style flows where dispenser sends STATE (not OK) and VOLUME may be interleaved.
 */
class EhlCommunicatorReceiveUntilTest {

    private val logger = LoggerFactory.getLogger(EhlCommunicatorReceiveUntilTest::class.java)

    private fun createCommunicator(inputData: ByteArray): EhlCommunicator {
        val port = InMemorySerialPort(inputData)
        port.connect()
        return EhlCommunicator(port, enableRawLogging = false)
    }

    private class InMemorySerialPort(private val inputData: ByteArray) : SerialTransport {
        private var readPosition = 0
        override val isConnected: Boolean get() = true
        override fun connect() = true
        override fun disconnect() {}
        override fun write(data: ByteArray) = data.size
        override fun readAvailable(maxBytes: Int): ByteArray {
            if (readPosition >= inputData.size) return ByteArray(0)
            val toRead = minOf(maxBytes, 32, inputData.size - readPosition)
            val result = inputData.copyOfRange(readPosition, readPosition + toRead)
            readPosition += toRead
            return result
        }
        override fun flush() {}
    }

    /** Build dispenser response frame (STX_DISPENSER 0x20) */
    private fun dispenserFrame(addr: Int, cmd: EhlCommand, data: ByteArray): ByteArray {
        val p = EhlPacket(addr, cmd, data)
        return EhlCodec.encode(p, fromController = false)
    }

    @Test
    fun `receiveUntil ignores interleaved VOLUME and returns STATE`() = runBlocking {
        val volBytes = dispenserFrame(1, EhlCommand.VOLUME, byteArrayOf(0x30, 0x35, 0x35, 0x34, 0x30))  // 45.50 L
        val stateBytes = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x5A))  // open_for_delivery=1
        val input = volBytes + stateBytes

        val comm = createCommunicator(input)
        val predicate: (EhlPacket) -> Boolean = { it.address == 1 && it.command == EhlCommand.STATE }
        val result = comm.withExclusive {
            receiveUntil(2000, predicate, "STATE(open bit 0x02)")
        }

        assertNotNull(result)
        assertEquals(EhlCommand.STATE, result!!.command)
        assertEquals(1, result.address)
        assertEquals(1, result.data.size)
        assertTrue((result.data[0].toInt() and StatusBitMasks.OPEN_FOR_DELIVERY) != 0)
    }

    @Test
    fun `receiveUntil handles concatenated frames`() = runBlocking {
        val s1 = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x00))
        val s2 = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x5A))
        val input = s1 + s2

        val comm = createCommunicator(input)
        val predicate: (EhlPacket) -> Boolean = { it.address == 1 && it.command == EhlCommand.STATE }
        val result = comm.withExclusive {
            receiveUntil(2000, predicate, "STATE")
        }

        assertNotNull(result)
        assertEquals(1, result!!.address)
        assertEquals(EhlCommand.STATE, result.command)
        assertEquals(0x00, result.data[0].toInt() and 0xFF)
    }

    @Test
    fun `receiveUntil succeeds when dispenser sends STATE instead of OK (missing OK)`() = runBlocking {
        val stateBytes = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x5A))
        val input = stateBytes

        val comm = createCommunicator(input)
        val predicate: (EhlPacket) -> Boolean = { it.address == 1 && it.command == EhlCommand.STATE }
        val result = comm.withExclusive {
            receiveUntil(2000, predicate, "STATE(open bit 0x02)")
        }

        assertNotNull(result)
        assertEquals(EhlCommand.STATE, result!!.command)
        assertTrue((result.data[0].toInt() and StatusBitMasks.OPEN_FOR_DELIVERY) != 0)
    }

    @Test
    fun `receiveUntil returns delayed open-bit when predicate requires open_for_delivery`() = runBlocking {
        val stateClosed = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x00))
        val stateOpen = dispenserFrame(1, EhlCommand.STATE, byteArrayOf(0x5A.toByte()))
        val input = stateClosed + stateOpen

        val comm = createCommunicator(input)
        val predicate: (EhlPacket) -> Boolean = { p ->
            p.address == 1 && p.command == EhlCommand.STATE &&
                p.data.isNotEmpty() && (p.data[0].toInt() and StatusBitMasks.OPEN_FOR_DELIVERY) != 0
        }
        val result = comm.withExclusive {
            receiveUntil(2000, predicate, "STATE(open bit 0x02)")
        }

        assertNotNull(result)
        assertEquals(0x5A, result!!.data[0].toInt() and 0xFF)
    }
}
