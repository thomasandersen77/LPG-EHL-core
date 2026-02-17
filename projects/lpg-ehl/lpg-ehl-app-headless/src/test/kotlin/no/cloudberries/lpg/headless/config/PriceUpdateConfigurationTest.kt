package no.cloudberries.lpg.headless.config

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.*

class PriceUpdateConfigurationTest {

    @Test
    fun `price update sends PRODUCT_SELECT then PROG_PRC with correct formatting`() = runBlocking {
        // Arrange
        val ehlCommunicator = mock<EhlCommunicator>()
        // Mock sendAndReceive to return a dummy packet (since it's a suspend function)
        whenever(ehlCommunicator.sendAndReceive(any(), any())).thenReturn(
            EhlPacket(1, EhlCommand.OK, byteArrayOf())
        )

        val config = PriceUpdateConfiguration()
        val callback = config.priceUpdateCallback(ehlCommunicator)
        val address = 5
        val price = BigDecimal("15.90")

        // Act
        callback(address, price)

        // Assert
        val packetCaptor = argumentCaptor<EhlPacket>()
        verify(ehlCommunicator, times(2)).sendAndReceive(packetCaptor.capture(), any())

        val packets = packetCaptor.allValues
        assertEquals(2, packets.size)

        // Step 1: PRODUCT_SELECT
        val packet1 = packets[0]
        assertEquals(address, packet1.address)
        assertEquals(EhlCommand.PRODUCT_SELECT, packet1.command)
        assertArrayEquals(byteArrayOf(0x30), packet1.data)

        // Step 2: PROG_PRC
        val packet2 = packets[1]
        assertEquals(address, packet2.address)
        assertEquals(EhlCommand.PROG_PRC, packet2.command)
        // 15.90 -> 1590 -> digits: 0, 9, 5, 1 -> 0x30, 0x39, 0x35, 0x31
        assertArrayEquals(byteArrayOf(0x30, 0x39, 0x35, 0x31), packet2.data)
    }
}
