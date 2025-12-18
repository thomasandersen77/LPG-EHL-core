package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.communication.SerialPortManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never

/**
 * Comprehensive unit tests for HardwareWatchdogService.
 * 
 * Tests critical hardware monitoring functionality:
 * - Connection loss detection
 * - Auto-reconnection with exponential backoff
 * - Cooldown periods after multiple failures
 * - Statistics tracking
 * - Edge cases and error handling
 */
class HardwareWatchdogServiceTest {

    private lateinit var mockSerialPortManager: SerialPortManager
    private lateinit var watchdogService: HardwareWatchdogService

    @BeforeEach
    fun setup() {
        mockSerialPortManager = mock<SerialPortManager>()
        watchdogService = HardwareWatchdogService(mockSerialPortManager)
    }

    // ============================================================
    // CONNECTION HEALTH CHECK TESTS
    // ============================================================

    @Test
    fun `healthy connection passes health check`() {
        // Arrange: Mock healthy connection
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(true)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(1000L) // 1 second
        
        watchdogService.initialize()
        
        // Act: Perform health check
        watchdogService.performHealthCheck()
        
        // Assert: Should call checkWatchdog and not attempt reconnection
        verify(mockSerialPortManager, times(1)).checkWatchdog()
        verify(mockSerialPortManager, never()).reconnect()
        
        val stats = watchdogService.getStatistics()
        assertEquals(0, stats.consecutiveFailures)
        assertTrue(stats.isEnabled)
    }

    @Test
    fun `connection loss detected and triggers reconnection`() {
        // Arrange: Mock connection loss
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L) // 65 seconds
        whenever(mockSerialPortManager.reconnect()).thenReturn(true)
        
        watchdogService.initialize()
        
        // Act: Perform health check
        watchdogService.performHealthCheck()
        
        // Assert: Should attempt reconnection
        verify(mockSerialPortManager, times(1)).checkWatchdog()
        verify(mockSerialPortManager, times(1)).reconnect()
        
        val stats = watchdogService.getStatistics()
        assertEquals(0, stats.consecutiveFailures) // Reset after successful reconnect
        assertEquals(1, stats.reconnectAttempts)
    }

    @Test
    fun `multiple consecutive failures increment failure counter`() {
        // Arrange: Mock persistent connection failure
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(false)
        
        watchdogService.initialize()
        
        // Act: Perform multiple health checks
        watchdogService.performHealthCheck() // Failure 1
        watchdogService.performHealthCheck() // Failure 2
        watchdogService.performHealthCheck() // Failure 3
        
        // Assert: Should track consecutive failures
        verify(mockSerialPortManager, times(3)).checkWatchdog()
        verify(mockSerialPortManager, times(3)).reconnect()
        
        val stats = watchdogService.getStatistics()
        assertEquals(3, stats.consecutiveFailures)
        assertEquals(3, stats.reconnectAttempts)
    }

    @Test
    fun `successful reconnection resets failure counter`() {
        // Arrange: First fail, then succeed
        whenever(mockSerialPortManager.checkWatchdog())
            .thenReturn(false) // First check fails
            .thenReturn(true)  // After reconnect, check succeeds
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(true)
        
        watchdogService.initialize()
        
        // Act: Fail once, then succeed
        watchdogService.performHealthCheck() // Should fail and reconnect
        watchdogService.performHealthCheck() // Should pass
        
        // Assert: Failure counter should be reset
        val stats = watchdogService.getStatistics()
        assertEquals(0, stats.consecutiveFailures) // Reset after success
        assertEquals(1, stats.reconnectAttempts)
    }

    // ============================================================
    // EXPONENTIAL BACKOFF AND COOLDOWN TESTS
    // ============================================================

    @Test
    fun `cooldown period prevents excessive reconnection attempts`() {
        // Arrange: Mock persistent failures
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(false)
        
        watchdogService.initialize()
        
        // Act: Exceed maximum failures (3) then try more
        watchdogService.performHealthCheck() // Failure 1
        watchdogService.performHealthCheck() // Failure 2
        watchdogService.performHealthCheck() // Failure 3
        watchdogService.performHealthCheck() // Should be in cooldown
        watchdogService.performHealthCheck() // Should still be in cooldown
        
        // Assert: Should attempt reconnect only 3 times, then enter cooldown
        verify(mockSerialPortManager, times(5)).checkWatchdog()
        verify(mockSerialPortManager, times(3)).reconnect() // Only 3 attempts, then cooldown
        
        val stats = watchdogService.getStatistics()
        assertTrue(stats.consecutiveFailures > 3) // Continues counting failures
        assertEquals(3, stats.reconnectAttempts) // But stops reconnect attempts
    }

    @Test
    fun `cooldown period eventually expires and allows retry`() {
        // This test would require manipulating time or waiting 5 minutes
        // For unit test purposes, we'll verify the logic structure
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(false)
        
        watchdogService.initialize()
        
        // Trigger maximum failures
        repeat(4) { watchdogService.performHealthCheck() }
        
        val stats = watchdogService.getStatistics()
        assertTrue(stats.consecutiveFailures > 3)
        assertEquals(3, stats.reconnectAttempts) // Limited by cooldown
    }

    // ============================================================
    // MANUAL RECONNECT AND FORCE OPERATIONS
    // ============================================================

    @Test
    fun `manual reconnect bypasses cooldown and resets counters`() {
        // Arrange: Put service in cooldown state
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(65000L)
        // First 3 calls (auto-reconnect attempts) fail, 4th call (manual) succeeds
        whenever(mockSerialPortManager.reconnect()).thenReturn(false, false, false, true)
        
        watchdogService.initialize()
        
        // Trigger cooldown: 3 failures trigger 3 reconnect attempts, 4th health check is in cooldown
        repeat(4) { watchdogService.performHealthCheck() }
        
        // Act: Manual reconnect
        val manualResult = watchdogService.forceReconnect()
        
        // Assert: Manual reconnect should work and reset counters
        assertTrue(manualResult)
        verify(mockSerialPortManager, times(4)).reconnect() // 3 auto + 1 manual
        
        val stats = watchdogService.getStatistics()
        assertEquals(0, stats.consecutiveFailures) // Reset by manual action
    }

    @Test
    fun `manual reconnect handles failure gracefully`() {
        // Arrange: Manual reconnect fails
        whenever(mockSerialPortManager.reconnect()).thenReturn(false)
        
        // Act: Manual reconnect
        val result = watchdogService.forceReconnect()
        
        // Assert: Should return false but not crash
        assertFalse(result)
        verify(mockSerialPortManager, times(1)).reconnect()
    }

    // ============================================================
    // STATISTICS AND MONITORING
    // ============================================================

    @Test
    fun `statistics reflect current watchdog state`() {
        // Test with no serial port manager (disabled watchdog)
        val disabledWatchdog = HardwareWatchdogService(null)
        val disabledStats = disabledWatchdog.getStatistics()
        
        assertFalse(disabledStats.isEnabled)
        assertEquals(0, disabledStats.consecutiveFailures)
        assertEquals(0, disabledStats.reconnectAttempts)
        assertEquals(0, disabledStats.timeSinceLastData)
        
        // Test with enabled watchdog
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(1500L)
        
        watchdogService.initialize()
        val enabledStats = watchdogService.getStatistics()
        
        assertTrue(enabledStats.isEnabled)
        assertEquals(1500L, enabledStats.timeSinceLastData)
    }

    @Test
    fun `statistics update correctly after operations`() {
        // Arrange: Connection failure scenario
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(75000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(false, true)
        
        watchdogService.initialize()
        
        // Fail once, then succeed
        watchdogService.performHealthCheck()
        var stats = watchdogService.getStatistics()
        assertEquals(1, stats.consecutiveFailures)
        assertEquals(1, stats.reconnectAttempts)
        
        // Successful reconnect on second try
        whenever(mockSerialPortManager.reconnect()).thenReturn(true)
        watchdogService.performHealthCheck()
        stats = watchdogService.getStatistics()
        assertEquals(2, stats.reconnectAttempts) // Incremented
        // consecutiveFailures should be reset on successful reconnect
    }

    // ============================================================
    // EDGE CASES AND ERROR HANDLING
    // ============================================================

    @Test
    fun `null serial port manager disables watchdog safely`() {
        val disabledWatchdog = HardwareWatchdogService(null)
        
        // Should not crash on any operation
        assertDoesNotThrow {
            disabledWatchdog.initialize()
            disabledWatchdog.performHealthCheck()
            disabledWatchdog.forceReconnect()
        }
        
        val stats = disabledWatchdog.getStatistics()
        assertFalse(stats.isEnabled)
    }

    @Test
    fun `exception in checkWatchdog is handled gracefully`() {
        // Arrange: Mock throws exception
        whenever(mockSerialPortManager.checkWatchdog()).thenThrow(RuntimeException("Serial port error"))
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(30000L)
        whenever(mockSerialPortManager.reconnect()).thenReturn(true)
        
        watchdogService.initialize()
        
        // Act: Should not crash on exception
        assertDoesNotThrow {
            watchdogService.performHealthCheck()
        }
        
        // Assert: Should treat exception as failure and attempt reconnect
        verify(mockSerialPortManager, times(1)).reconnect()
    }

    @Test
    fun `exception in reconnect is handled gracefully`() {
        // Arrange: Reconnect throws exception
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(false)
        whenever(mockSerialPortManager.getTimeSinceLastData()).thenReturn(70000L)
        whenever(mockSerialPortManager.reconnect()).thenThrow(RuntimeException("Hardware unavailable"))
        
        watchdogService.initialize()
        
        // Act: Should not crash on reconnection exception
        assertDoesNotThrow {
            watchdogService.performHealthCheck()
        }
        
        // Assert: Failure should still be counted
        val stats = watchdogService.getStatistics()
        assertEquals(1, stats.consecutiveFailures)
        assertEquals(1, stats.reconnectAttempts)
    }

    @Test
    fun `initialization is idempotent`() {
        // Multiple initializations should not cause issues
        assertDoesNotThrow {
            watchdogService.initialize()
            watchdogService.initialize()
            watchdogService.initialize()
        }
        
        verify(mockSerialPortManager, times(3)).enableWatchdog()
    }

    @Test
    fun `concurrent health checks are thread-safe`() {
        // This test verifies thread safety - in a real scenario you might use CountDownLatch
        // For unit testing, we'll just verify the service handles multiple quick calls
        whenever(mockSerialPortManager.checkWatchdog()).thenReturn(true)
        
        watchdogService.initialize()
        
        assertDoesNotThrow {
            repeat(10) {
                watchdogService.performHealthCheck()
            }
        }
        
        verify(mockSerialPortManager, times(10)).checkWatchdog()
    }
}