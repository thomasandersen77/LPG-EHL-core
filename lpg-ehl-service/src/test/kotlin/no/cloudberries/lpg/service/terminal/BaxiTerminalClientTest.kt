package no.cloudberries.lpg.service.terminal

import no.cloudberries.norgesgass.baxi.client.BaxiClient
import no.cloudberries.norgesgass.baxi.client.OpenResult
import no.cloudberries.norgesgass.baxi.client.CallAcceptResult
import no.cloudberries.norgesgass.baxi.client.TransferAmountArgs
import no.cloudberries.norgesgass.baxi.client.AdministrationArgs
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import no.cloudberries.norgesgass.baxi.events.LastFinancialResultEvent
import no.cloudberries.norgesgass.baxi.events.LocalModeEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.*
import java.util.concurrent.TimeUnit

class BaxiTerminalClientTest {

    private lateinit var baxiClient: BaxiClient
    private lateinit var terminalClient: BaxiTerminalClient
    private lateinit var eventListenerCaptor: ArgumentCaptor<BaxiEventListener>

    @BeforeEach
    fun setup() {
        baxiClient = mock()
        eventListenerCaptor = ArgumentCaptor.forClass(BaxiEventListener::class.java)
        terminalClient = BaxiTerminalClient("127.0.0.1", 7200, baxiClient)
        verify(baxiClient).setEventListener(eventListenerCaptor.capture())
    }

    @Test
    fun `openTerminal should wait for onTerminalReady callback`() {
        whenever(baxiClient.open(any())).thenReturn(OpenResult(1))
        
        val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit<TerminalSimpleResponse> {
            terminalClient.openTerminal()
        }
        
        Thread.sleep(100)
        assertThat(future.isDone).isFalse()
        
        eventListenerCaptor.value.onTerminalReady()
        
        val response = future.get(1, TimeUnit.SECONDS)
        assertThat(response.success).isTrue()
    }

    @Test
    fun `purchase should map to transferAmount with type1=10`() {
        whenever(baxiClient.transferAmount(any())).thenReturn(CallAcceptResult(1))
        
        // Mock terminal ready
        whenever(baxiClient.open(any())).thenReturn(OpenResult(1))
        terminalClient.openTerminal()
        eventListenerCaptor.value.onTerminalReady()

        val request = TerminalPurchaseRequest(amountMinor = 5000)
        
        val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit<TerminalOperationResponse> {
            terminalClient.purchase(request)
        }
        
        Thread.sleep(100)
        
        val captor = argumentCaptor<TransferAmountArgs>()
        verify(baxiClient).transferAmount(captor.capture())
        assertThat(captor.firstValue.amount1).isEqualTo(5000)
        assertThat(captor.firstValue.type1).isEqualTo(10)

        eventListenerCaptor.value.onLocalMode(LocalModeEvent(1, "00", null, null, null, emptyMap()))
        eventListenerCaptor.value.onLastFinancialResult(LastFinancialResultEvent(1, "Data"))
        
        val response = future.get(1, TimeUnit.SECONDS)
        assertThat(response.success).isTrue()
    }

    @Test
    fun `reversal should map to administration with admCode=9100`() {
        whenever(baxiClient.administration(any())).thenReturn(CallAcceptResult(1))
        
        // Mock terminal ready
        whenever(baxiClient.open(any())).thenReturn(OpenResult(1))
        terminalClient.openTerminal()
        eventListenerCaptor.value.onTerminalReady()

        val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit<TerminalOperationResponse> {
            terminalClient.reversal("123456")
        }
        
        Thread.sleep(100)
        
        val captor = argumentCaptor<AdministrationArgs>()
        verify(baxiClient).administration(captor.capture())
        assertThat(captor.firstValue.admCode).isEqualTo(9100)
        assertThat(captor.firstValue.optionalData).isEqualTo("123456")

        eventListenerCaptor.value.onLocalMode(LocalModeEvent(1, "00", null, null, null, emptyMap()))
        eventListenerCaptor.value.onLastFinancialResult(LastFinancialResultEvent(1, "Data"))
        
        val response = future.get(1, TimeUnit.SECONDS)
        assertThat(response.success).isTrue()
    }
}
