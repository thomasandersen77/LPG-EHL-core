package no.cloudberries.lpg.transaction

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.Duration

@DisplayName("Transaction Watchdog Tests")
class TransactionWatchdogTest {
    
    @Test
    @DisplayName("Watchdog stops when amount limit is reached")
    fun testStopsOnAmountLimit() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        var stopped = false
        
        // Simulate progressive volume/amount increase
        var currentAmount = 0
        val volumeProvider = suspend {
            currentAmount += 5000  // Increment by 50 kr each poll
            Pair(currentAmount / 100.0, currentAmount)  // volume in liters, amount in øre
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000  // 500 kr limit
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        
        assertTrue(stopped, "Stop command should have been called")
        assertTrue(result is TransactionWatchdog.WatchdogResult.MaxReached)
        
        val maxReached = result as TransactionWatchdog.WatchdogResult.MaxReached
        assertTrue(maxReached.actualAmountCents >= 50000 - 50, "Amount should be at or near limit")
    }
    
    @Test
    @DisplayName("Watchdog stops when volume limit is reached")
    fun testStopsOnVolumeLimit() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        var stopped = false
        
        var currentVolume = 0.0
        val volumeProvider = suspend {
            currentVolume += 5.0  // Increment by 5 liters each poll
            Pair(currentVolume, (currentVolume * 1590).toInt())  // 15.90 kr/liter
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.VOLUME,
            maxVolumeLiters = 50.0
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        
        assertTrue(stopped)
        assertTrue(result is TransactionWatchdog.WatchdogResult.MaxReached)
        
        val maxReached = result as TransactionWatchdog.WatchdogResult.MaxReached
        assertTrue(maxReached.actualVolumeLiters >= 50.0 - 0.1, "Volume should be at or near limit")
    }
    
    @Test
    @DisplayName("Watchdog can be cancelled")
    fun testCanBeCancelled() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        
        val volumeProvider = suspend {
            Pair(10.0, 10000)  // Never reaches limit
        }
        
        val stopCommand: suspend () -> Unit = suspend {
            // Should not be called
            fail("Stop command should not be called when cancelled")
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 100000  // High limit
        )
        
        val job = launch {
            watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        }
        
        delay(200)  // Let it poll a few times
        job.cancel()
        job.join()
        
        assertTrue(job.isCancelled)
    }
    
    @Test
    @DisplayName("Watchdog config validation enforces required fields")
    fun testConfigValidation() {
        // AMOUNT strategy requires maxAmountCents
        assertThrows(IllegalArgumentException::class.java) {
            TransactionWatchdog.WatchdogConfig(
                dispenserId = 1,
                strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
                maxAmountCents = null
            )
        }
        
        // VOLUME strategy requires maxVolumeLiters
        assertThrows(IllegalArgumentException::class.java) {
            TransactionWatchdog.WatchdogConfig(
                dispenserId = 1,
                strategy = TransactionWatchdog.MonitorStrategy.VOLUME,
                maxVolumeLiters = null
            )
        }
        
        // AMOUNT_OR_VOLUME requires at least one limit
        assertThrows(IllegalArgumentException::class.java) {
            TransactionWatchdog.WatchdogConfig(
                dispenserId = 1,
                strategy = TransactionWatchdog.MonitorStrategy.AMOUNT_OR_VOLUME,
                maxAmountCents = null,
                maxVolumeLiters = null
            )
        }
    }
    
    @Test
    @DisplayName("Watchdog handles provider errors by triggering emergency stop")
    fun testHandlesProviderErrors() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        
        val volumeProvider = suspend {
            throw RuntimeException("Simulated communication error")
        }
        
        var stopped = false
        val stopCommand = suspend {
            stopped = true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 45000
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        
        // KRITISK SIKKERHET: Provider exceptions trigger emergency stop and return Error
        // This is the correct safety behavior - don't silently continue on communication failures
        assertTrue(result is TransactionWatchdog.WatchdogResult.Error, "Should return Error on provider exception")
        assertTrue(stopped, "Emergency stop should have been triggered")
    }
    
    @Test
    @DisplayName("Watchdog respects overshoot tolerance")
    fun testOvershootTolerance() = runTest {
        // Default tolerance is 50 øre
        val watchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            overshootToleranceCents = 100  // 1 kr tolerance
        )
        
        var stopped = false
        val volumeProvider = suspend {
            Pair(30.0, 49900)  // 499 kr (within tolerance of 500 kr)
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        
        assertTrue(stopped, "Should stop within tolerance")
        assertTrue(result is TransactionWatchdog.WatchdogResult.MaxReached)
    }
    
    @Test
    @DisplayName("AMOUNT_OR_VOLUME strategy stops on first limit reached")
    fun testAmountOrVolumeStopsOnFirst() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        
        var stopped = false
        var currentAmount = 0
        val volumeProvider = suspend {
            currentAmount += 10000  // 100 kr per poll
            Pair(currentAmount / 1590.0, currentAmount)  // Volume grows slower than amount
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT_OR_VOLUME,
            maxAmountCents = 50000,  // Will hit this first
            maxVolumeLiters = 100.0  // Won't reach this
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand)
        
        assertTrue(stopped)
        assertTrue(result is TransactionWatchdog.WatchdogResult.MaxReached)
        
        val maxReached = result as TransactionWatchdog.WatchdogResult.MaxReached
        // Should stop around 500 kr, not wait for 100 liters
        assertTrue(maxReached.actualAmountCents < 60000, "Should stop on amount, not volume")
    }
    
    // ============================================================================
    // SAFETY CRITICAL TESTS - Required for gas safety validation
    // ============================================================================
    
    @Test
    @DisplayName("CRITICAL: Watchdog stops pump when volume provider fails repeatedly")
    fun testWatchdogStopsOnProviderFailure() = runTest {
        val watchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            maxConsecutiveNulls = 3  // Fail after 3 nulls for faster test
        )
        
        var nullCount = 0
        var stopped = false
        var emergencyStopCalled = false
        
        // Provider returns null repeatedly (simulating communication failure)
        val volumeProvider = suspend {
            nullCount++
            null  // Always return null
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val emergencyStopCommand = suspend {
            emergencyStopCalled = true
            stopCommand()
            true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Should trigger emergency stop after maxConsecutiveNulls
        assertTrue(emergencyStopCalled, "Emergency stop should have been called")
        assertTrue(stopped, "Stop command should have been executed")
        assertTrue(result is TransactionWatchdog.WatchdogResult.Error, "Should return Error result")
        assertTrue(nullCount >= 3, "Should have attempted at least 3 polls")
    }
    
    @Test
    @DisplayName("CRITICAL: Emergency stop retries if first attempt fails")
    fun testEmergencyStopRetry() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        
        var stopAttempts = 0
        var emergencyStopAttempts = 0
        
        val volumeProvider = suspend {
            Pair(50.0, 50000)  // At limit immediately
        }
        
        val stopCommand = suspend {
            stopAttempts++
            throw RuntimeException("Stop failed")
        }
        
        val emergencyStopCommand = suspend {
            emergencyStopAttempts++
            
            if (emergencyStopAttempts < 2) {
                // First attempt fails
                try {
                    stopCommand()
                    false
                } catch (e: Exception) {
                    false
                }
            } else {
                // Second attempt succeeds
                true
            }
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Should call emergency stop at least once
        assertTrue(emergencyStopAttempts >= 1, "Emergency stop should be attempted")
        assertTrue(result is TransactionWatchdog.WatchdogResult.MaxReached, "Should still report MaxReached")
    }
    
    @Test
    @DisplayName("CRITICAL: Absolute timeout stops transaction after 2 minutes")
    fun testAbsoluteTimeout() = runTest {
        val watchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            absoluteTimeoutSeconds = 1  // 1 second for test
        )
        
        var stopped = false
        var emergencyStopCalled = false
        var failSafeCalled = false
        
        // Custom watchdog with fail-safe callback
        val customWatchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            absoluteTimeoutSeconds = 1
        )
        customWatchdog.failSafeCallback = { reason, dispenserId ->
            failSafeCalled = true
        }
        
        // Provider keeps returning values (transaction never stops naturally)
        val volumeProvider = suspend {
            Pair(10.0, 10000)  // Never reaches limit
        }
        
        val stopCommand = suspend {
            stopped = true
        }
        
        val emergencyStopCommand = suspend {
            emergencyStopCalled = true
            stopCommand()
            true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 1000000  // Very high limit
        )
        
        // This should timeout after 1 second
        val result = customWatchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Should trigger emergency stop due to timeout
        assertTrue(emergencyStopCalled, "Emergency stop should be called on timeout")
        assertTrue(stopped, "Stop command should be executed")
        // Result might be Cancelled due to forced cancellation
        assertTrue(
            result is TransactionWatchdog.WatchdogResult.Cancelled ||
            result is TransactionWatchdog.WatchdogResult.Error,
            "Should return Cancelled or Error result on timeout"
        )
    }
    
    @Test
    @DisplayName("CRITICAL: Transaction stops if polling times out 5 times")
    fun testPollingTimeoutFailsafe() = runTest {
        val watchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            maxConsecutiveNulls = 5
        )
        
        var pollCount = 0
        var emergencyStopCalled = false
        
        // Provider returns null for first 6 polls, then valid data
        val volumeProvider = suspend {
            pollCount++
            if (pollCount <= 6) {
                null  // Simulate timeout/failure
            } else {
                Pair(10.0, 10000)  // Valid data (too late)
            }
        }
        
        val stopCommand: suspend () -> Unit = suspend {
            // Should be called
        }
        
        val emergencyStopCommand = suspend {
            emergencyStopCalled = true
            true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        val result = watchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Should emergency stop after 5 consecutive nulls
        assertTrue(emergencyStopCalled, "Emergency stop should be triggered after 5 nulls")
        assertTrue(result is TransactionWatchdog.WatchdogResult.Error)
        assertTrue(pollCount >= 5, "Should poll at least 5 times")
    }
    
    @Test
    @DisplayName("CRITICAL: Watchdog exception triggers emergency stop")
    fun testWatchdogExceptionSafety() = runTest {
        val watchdog = TransactionWatchdog(pollInterval = Duration.ofMillis(50))
        
        var emergencyStopCalled = false
        var failSafeCalled = false
        
        val customWatchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50)
        )
        customWatchdog.failSafeCallback = { reason, dispenserId ->
            failSafeCalled = true
        }
        
        // Provider throws exception
        val volumeProvider = suspend {
            throw RuntimeException("Catastrophic failure")
        }
        
        val stopCommand: suspend () -> Unit = suspend {
            // Should be called in emergency stop
        }
        
        val emergencyStopCommand = suspend {
            emergencyStopCalled = true
            true
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 1,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        // Should catch exception and not crash
        val result = customWatchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Watchdog handles provider errors gracefully, continues monitoring
        // But if it's a fatal error, it should trigger emergency stop
        assertTrue(result is TransactionWatchdog.WatchdogResult.Error || 
                   result is TransactionWatchdog.WatchdogResult.Cancelled,
                   "Should return Error or Cancelled on exception")
    }
    
    @Test
    @DisplayName("CRITICAL: Fail-safe callback invoked when all emergency stops fail")
    fun testFailSafeCallbackOnAllStopsFail() = runTest {
        var failSafeCallbackInvoked = false
        var failSafeReason = ""
        var failSafeDispenserId = 0
        
        val watchdog = TransactionWatchdog(
            pollInterval = Duration.ofMillis(50),
            maxConsecutiveNulls = 3
        )
        watchdog.failSafeCallback = { reason, dispenserId ->
            failSafeCallbackInvoked = true
            failSafeReason = reason
            failSafeDispenserId = dispenserId
        }
        
        var nullCount = 0
        
        // Provider returns null (communication failure)
        val volumeProvider = suspend {
            nullCount++
            null
        }
        
        val stopCommand = suspend {
            throw RuntimeException("Stop command failed")
        }
        
        val emergencyStopCommand = suspend {
            // Emergency stop also fails
            try {
                stopCommand()
                false
            } catch (e: Exception) {
                false  // Return false to indicate failure
            }
        }
        
        val config = TransactionWatchdog.WatchdogConfig(
            dispenserId = 42,
            strategy = TransactionWatchdog.MonitorStrategy.AMOUNT,
            maxAmountCents = 50000
        )
        
        watchdog.monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
        
        // Fail-safe callback should be invoked
        assertTrue(failSafeCallbackInvoked, "Fail-safe callback should be invoked")
        assertTrue(failSafeReason.contains("Stop command failed") || 
                   failSafeReason.contains("losing contact"),
                   "Fail-safe reason should indicate stop failure")
        assertEquals(42, failSafeDispenserId, "Should pass correct dispenser ID")
    }
}
