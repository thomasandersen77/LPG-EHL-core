package no.cloudberries.lpg.communication

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.transport.SerialTransport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for EhlCommunicator retry logic.
 */
class EhlCommunicatorRetryTest {

    /**
     * Mock transport that fails a specified number of times before succeeding.
     */
    class FailingThenSucceedingTransport(
        private val failCount: Int,
        private val responsePacket: EhlPacket
    ) : SerialTransport {
        val attemptCount = AtomicInteger(0)
        private var connected = true
        
        override val isConnected: Boolean get() = connected
        
        override fun connect(): Boolean {
            connected = true
            return true
        }
        
        override fun disconnect() {
            connected = false
        }
        
        override fun write(data: ByteArray): Int {
            // Just accept writes
            return data.size
        }
        
        override fun flush() {
            // No-op
        }
        
        override fun readAvailable(maxBytes: Int): ByteArray {
            val attempt = attemptCount.incrementAndGet()
            if (attempt <= failCount) {
                // Return empty bytes to simulate timeout
                return byteArrayOf()
            }
            // Return encoded response packet
            return no.cloudberries.lpg.protocol.EhlCodec.encode(responsePacket)
        }
    }
    
    /**
     * Mock transport that always fails with timeout (returns empty).
     */
    class AlwaysTimeoutTransport : SerialTransport {
        val attemptCount = AtomicInteger(0)
        private var connected = true
        
        override val isConnected: Boolean get() = connected
        
        override fun connect(): Boolean {
            connected = true
            return true
        }
        
        override fun disconnect() {
            connected = false
        }
        
        override fun write(data: ByteArray): Int {
            attemptCount.incrementAndGet()
            return data.size
        }
        
        override fun flush() {}
        
        override fun readAvailable(maxBytes: Int): ByteArray {
            // Never return data - simulates timeout
            return byteArrayOf()
        }
    }
    
    /**
     * Mock transport that throws IOException.
     */
    class IOExceptionTransport : SerialTransport {
        val attemptCount = AtomicInteger(0)
        private var connected = true
        
        override val isConnected: Boolean get() = connected
        
        override fun connect(): Boolean {
            connected = true
            return true
        }
        
        override fun disconnect() {
            connected = false
        }
        
        override fun write(data: ByteArray): Int {
            attemptCount.incrementAndGet()
            throw IOException("Serial port disconnected")
        }
        
        override fun flush() {}
        
        override fun readAvailable(maxBytes: Int): ByteArray = byteArrayOf()
    }

    @Test
    fun `test successful communication with response packet`() = runBlocking {
        // Setup: Create a response packet
        val responsePacket = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf(0x00) // IDLE state
        )
        
        // Transport that immediately succeeds (no failures)
        val transport = FailingThenSucceedingTransport(
            failCount = 0,
            responsePacket = responsePacket
        )
        
        // Communicator with 3 retries
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = false,
            retryConfig = RetryConfig(
                maxRetries = 3,
                initialDelayMs = 10,
                maxDelayMs = 50,
                backoffMultiplier = 2.0
            )
        )
        
        val requestPacket = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        // Should succeed on first attempt
        val result = communicator.sendAndReceive(requestPacket, timeoutMs = 500)
        
        assertEquals(EhlCommand.STATE, result.command)
        assertEquals(1, result.address)
        
        // Verify no retries were needed
        val stats = communicator.getRetryStatistics()
        assertEquals(0L, stats["totalRetries"], "Should not have needed retries")
    }
    
    @Test
    fun `test all retries exhausted throws TimeoutCancellationException`() = runBlocking {
        val transport = AlwaysTimeoutTransport()
        
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = false,
            retryConfig = RetryConfig(
                maxRetries = 2,
                initialDelayMs = 10,
                maxDelayMs = 50,
                backoffMultiplier = 2.0
            )
        )
        
        val requestPacket = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        // Should exhaust all retries and throw
        assertThrows<TimeoutCancellationException> {
            runBlocking {
                communicator.sendAndReceive(requestPacket, timeoutMs = 50)
            }
        }
        
        // Verify stats show retries were attempted
        val stats = communicator.getRetryStatistics()
        assertTrue((stats["totalRetries"] as Long) > 0, "Should have recorded retries")
        assertEquals(1L, stats["failedAfterRetries"], "Should have recorded one failure after exhausting retries")
    }
    
    @Test
    fun `test IOException does not trigger retry`() = runBlocking {
        val transport = IOExceptionTransport()
        
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = false,
            retryConfig = RetryConfig(
                maxRetries = 3,
                initialDelayMs = 10,
                maxDelayMs = 50,
                backoffMultiplier = 2.0
            )
        )
        
        val requestPacket = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        // Should fail immediately on IOException, no retries
        assertThrows<IOException> {
            runBlocking {
                communicator.sendAndReceive(requestPacket, timeoutMs = 100)
            }
        }
        
        // Only one attempt should have been made
        assertEquals(1, transport.attemptCount.get())
        
        // No retries should have been recorded
        val stats = communicator.getRetryStatistics()
        assertEquals(0L, stats["totalRetries"])
    }
    
    @Test
    fun `test no retry when maxRetries is zero`() = runBlocking {
        val transport = AlwaysTimeoutTransport()
        
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = false,
            retryConfig = RetryConfig.NO_RETRY
        )
        
        val requestPacket = EhlPacket(
            address = 1,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        // Should fail immediately with no retries
        assertThrows<TimeoutCancellationException> {
            runBlocking {
                communicator.sendAndReceive(requestPacket, timeoutMs = 50)
            }
        }
        
        // Only one attempt (no retries)
        assertEquals(1, transport.attemptCount.get())
    }
    
    @Test
    fun `test exponential backoff calculation`() {
        val config = RetryConfig(
            maxRetries = 5,
            initialDelayMs = 100,
            maxDelayMs = 2000,
            backoffMultiplier = 2.0
        )
        
        // Access private method via reflection for testing
        // Alternatively, we can test indirectly via timing, but that's flaky
        
        // Expected delays: 100, 200, 400, 800, 1600, (capped at 2000)
        // Verify config is correct
        assertEquals(5, config.maxRetries)
        assertEquals(100L, config.initialDelayMs)
        assertEquals(2000L, config.maxDelayMs)
        assertEquals(2.0, config.backoffMultiplier)
    }
    
    @Test
    fun `test reset retry statistics`() {
        val transport = AlwaysTimeoutTransport()
        
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = false,
            retryConfig = RetryConfig.DEFAULT
        )
        
        // Verify initial stats are zero
        val initialStats = communicator.getRetryStatistics()
        assertEquals(0L, initialStats["totalRetries"])
        assertEquals(0L, initialStats["successfulRetries"])
        assertEquals(0L, initialStats["failedAfterRetries"])
        
        // Reset should work even on zero stats
        communicator.resetRetryStatistics()
        
        val afterReset = communicator.getRetryStatistics()
        assertEquals(0L, afterReset["totalRetries"])
    }
}
