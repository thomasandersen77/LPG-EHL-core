package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.protocol.*
import no.cloudberries.lpg.service.system.DiagnosticsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for DiagnosticsService
 * 
 * Tests fault recording, retrieval, and CRITICAL fault handling.
 * Per MM-Petro-Implementation.md:
 * - CRITICAL fault -> state ERROR -> commands blocked
 * - WARNING fault -> log but continue operation
 */
@DisplayName("DiagnosticsService Unit Tests")
class DiagnosticsServiceTest {
    
    private lateinit var diagnosticsService: DiagnosticsService
    
    @BeforeEach
    fun setup() {
        diagnosticsService = DiagnosticsService()
    }
    
    // ============================================================
    // FAULT RECORDING AND STATE TRANSITION TESTS
    // ============================================================
    
    @Test
    @DisplayName("CRITICAL fault should result in ERROR state in diagnostics")
    fun testCriticalFaultResultsInErrorState() {
        val dispenserAddress = 1
        val criticalFault = EhlFault.E_05 // CRITICAL: Configuration memory damage
        
        // Record CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, criticalFault)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Get diagnostics
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        
        // Assert: State should be ERROR
        assertNotNull(snapshot)
        assertTrue(snapshot!!.state is DispenserStatus.ERROR, 
            "CRITICAL fault should result in ERROR state")
        assertTrue(snapshot.hasCriticalFault(),
            "Should have CRITICAL fault flagged")
        assertTrue(snapshot.isInError(),
            "Should be in error state")
    }
    
    @Test
    @DisplayName("WARNING fault should NOT result in ERROR state")
    fun testWarningFaultDoesNotBlockOperations() {
        val dispenserAddress = 2
        val warningFault = EhlFault.E_01 // WARNING: Improper EHI operation
        
        // Record WARNING fault
        diagnosticsService.recordFault(dispenserAddress, warningFault)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Get diagnostics
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        
        // Assert: State should NOT be ERROR (should be IDLE)
        assertNotNull(snapshot)
        assertFalse(snapshot!!.state is DispenserStatus.ERROR,
            "WARNING fault should NOT result in ERROR state")
        assertFalse(snapshot.hasCriticalFault(),
            "Should NOT have CRITICAL fault flagged")
        assertFalse(snapshot.isInError(),
            "Should NOT be in error state")
    }
    
    @Test
    @DisplayName("CRITICAL fault should be detected in /faults endpoint")
    fun testCriticalFaultAppearsInFaultsEndpoint() {
        // Record mix of faults
        diagnosticsService.recordFault(1, EhlFault.E_05) // CRITICAL
        diagnosticsService.recordFault(2, EhlFault.E_01) // WARNING
        diagnosticsService.recordFault(3, EhlFault.E_06) // CRITICAL
        
        // Get all faulted dispensers
        val faulted = diagnosticsService.getDispensersWithFaults()
        
        // Assert: Should return all 3
        assertEquals(3, faulted.size)
    }
    
    @Test
    @DisplayName("Only CRITICAL faults should appear in /critical endpoint")
    fun testOnlyCriticalFaultsInCriticalEndpoint() {
        // Record mix of faults
        diagnosticsService.recordFault(1, EhlFault.E_05) // CRITICAL
        diagnosticsService.recordFault(2, EhlFault.E_01) // WARNING
        diagnosticsService.recordFault(3, EhlFault.E_06) // CRITICAL
        diagnosticsService.recordFault(4, EhlFault.E_09) // WARNING
        
        // Get only critical
        val critical = diagnosticsService.getDispenserWithCriticalFaults()
        
        // Assert: Should return only 2 (dispensers 1 and 3)
        assertEquals(2, critical.size)
        assertTrue(critical.all { it.hasCriticalFault() || it.isInError() })
    }
    
    // ============================================================
    // COMMAND BLOCKING TESTS (using DispenserFaultHandler)
    // ============================================================
    
    @Test
    @DisplayName("Commands should be blocked when dispenser is in ERROR state due to CRITICAL fault")
    fun testCommandsBlockedOnCriticalFault() {
        val dispenserAddress = 1
        val criticalFault = EhlFault.E_05
        
        // Record CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, criticalFault)
        
        // Get state
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        
        // Use DispenserFaultHandler to check if commands should be blocked
        val shouldBlockUnblock = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot!!.state,
            targetCommand = EhlCommand.UNBLOCK
        )
        val shouldBlockProductSelect = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot.state,
            targetCommand = EhlCommand.PRODUCT_SELECT
        )
        val shouldBlockProgPrice = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot.state,
            targetCommand = EhlCommand.PROG_PRC
        )
        
        // Assert: All operational commands should be blocked
        assertTrue(shouldBlockUnblock, "UNBLOCK should be blocked in ERROR state")
        assertTrue(shouldBlockProductSelect, "PRODUCT_SELECT should be blocked in ERROR state")
        assertTrue(shouldBlockProgPrice, "PROG_PRC should be blocked in ERROR state")
    }
    
    @Test
    @DisplayName("Diagnostic commands should NOT be blocked in ERROR state")
    fun testDiagnosticCommandsAllowedOnError() {
        val dispenserAddress = 1
        
        // Record CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, EhlFault.E_06)
        
        // Get state
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        
        // Check diagnostic commands are still allowed
        val shouldBlockState = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot!!.state,
            targetCommand = EhlCommand.STATE
        )
        val shouldBlockErrorQuery = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot.state,
            targetCommand = EhlCommand.ERROR_QUERY
        )
        
        // Assert: Diagnostic commands should NOT be blocked
        assertFalse(shouldBlockState, "STATE query should be allowed in ERROR state")
        assertFalse(shouldBlockErrorQuery, "ERROR_QUERY should be allowed in ERROR state")
    }
    
    @Test
    @DisplayName("Commands should NOT be blocked for WARNING faults")
    fun testCommandsNotBlockedOnWarningFault() {
        val dispenserAddress = 1
        val warningFault = EhlFault.E_01
        
        // Record WARNING fault
        diagnosticsService.recordFault(dispenserAddress, warningFault)
        
        // Get state
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        
        // Check commands are NOT blocked
        val shouldBlockUnblock = DispenserFaultHandler.shouldBlockCommand(
            currentStatus = snapshot!!.state,
            targetCommand = EhlCommand.UNBLOCK
        )
        
        // Assert: Should NOT be blocked
        assertFalse(shouldBlockUnblock, 
            "UNBLOCK should NOT be blocked for WARNING fault")
    }
    
    // ============================================================
    // CANFUEL TESTS
    // ============================================================
    
    @Test
    @DisplayName("canFuel should return false for CRITICAL fault")
    fun testCanFuelFalseForCriticalFault() {
        val dispenserAddress = 1
        val criticalFault = EhlFault.E_05
        
        // Record CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, criticalFault)
        
        // Get snapshot
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        
        // Check canFuel
        val canFuel = DispenserFaultHandler.canFuel(
            currentStatus = snapshot!!.state,
            lastFault = snapshot.lastFault?.let { 
                EhlFault.fromDisplayCode(it.code) 
            }
        )
        
        assertFalse(canFuel, "canFuel should return false for CRITICAL fault")
    }
    
    @Test
    @DisplayName("canFuel should return true for WARNING fault")
    fun testCanFuelTrueForWarningFault() {
        val dispenserAddress = 1
        val warningFault = EhlFault.E_01
        
        // Record WARNING fault
        diagnosticsService.recordFault(dispenserAddress, warningFault)
        
        // Get snapshot
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        
        // Check canFuel - WARNING fault should allow fueling
        val canFuel = DispenserFaultHandler.canFuel(
            currentStatus = snapshot!!.state,
            lastFault = snapshot.lastFault?.let {
                EhlFault.fromDisplayCode(it.code)
            }
        )
        
        assertTrue(canFuel, "canFuel should return true for WARNING fault")
    }
    
    // ============================================================
    // FAULT CLEARING TESTS
    // ============================================================
    
    @Test
    @DisplayName("Clearing fault should restore normal operation")
    fun testClearFaultRestoresNormalOperation() {
        val dispenserAddress = 1
        
        // Record CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, EhlFault.E_05)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Verify fault is present
        var snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot?.lastFault)
        assertTrue(snapshot!!.isInError())
        
        // Clear fault
        diagnosticsService.clearFault(dispenserAddress)
        
        // Verify fault is cleared and state is back to normal
        snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNull(snapshot?.lastFault, "Fault should be cleared")
        assertFalse(snapshot!!.isInError(), "Should no longer be in error")
    }
    
    // ============================================================
    // COMMUNICATION TRACKING TESTS
    // ============================================================
    
    @Test
    @DisplayName("Connection status based on lastRxAt")
    fun testConnectionStatusBasedOnLastRx() {
        val dispenserAddress = 1
        
        // Record recent communication
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Check connected status
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        assertTrue(snapshot!!.connected, "Should be connected with recent RX")
        assertNotNull(snapshot.lastRxAt)
    }
    
    @Test
    @DisplayName("lastTxAt tracked separately from lastRxAt")
    fun testLastTxAtTrackedSeparately() {
        val dispenserAddress = 1
        
        // Record TX
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = false)
        
        // Check TX tracked
        val snapshot = diagnosticsService.getDiagnosticsForDispenser(dispenserAddress)
        assertNotNull(snapshot)
        assertNotNull(snapshot!!.lastTxAt)
    }
}
