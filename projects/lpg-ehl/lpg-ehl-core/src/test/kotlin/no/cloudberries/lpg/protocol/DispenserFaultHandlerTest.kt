package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Dispenser Fault Handler Tests")
class DispenserFaultHandlerTest {
    
    @Test
    @DisplayName("CRITICAL fault should transition to ERROR state")
    fun testCriticalFaultTransitionsToError() {
        val fault = EhlFault.E_05 // CRITICAL
        
        val newStatus = DispenserFaultHandler.determineStatusFromFault(
            fault = fault,
            currentStatus = DispenserStatus.IDLE
        )
        
        assertTrue(newStatus is DispenserStatus.ERROR)
        val errorStatus = newStatus as DispenserStatus.ERROR
        assertNotEquals(0, errorStatus.errorCode)
    }
    
    @Test
    @DisplayName("WARNING fault should maintain current status")
    fun testWarningFaultMaintainsStatus() {
        val fault = EhlFault.E_01 // WARNING
        val currentStatus = DispenserStatus.AUTHORIZED
        
        val newStatus = DispenserFaultHandler.determineStatusFromFault(
            fault = fault,
            currentStatus = currentStatus
        )
        
        assertEquals(currentStatus, newStatus)
    }
    
    @Test
    @DisplayName("Check fault detection from display data")
    fun testCheckForFault() {
        // Test with CRITICAL fault
        val criticalData = "E-05".toByteArray()
        val criticalResult = DispenserFaultHandler.checkForFault(criticalData)
        
        assertTrue(criticalResult is FaultCheckResult.FaultDetected)
        val detectedFault = (criticalResult as FaultCheckResult.FaultDetected).fault
        assertEquals(EhlFault.E_05, detectedFault)
        
        // Test with normal data
        val normalData = "15.90".toByteArray()
        val normalResult = DispenserFaultHandler.checkForFault(normalData)
        
        assertTrue(normalResult is FaultCheckResult.NoFault)
    }
    
    @Test
    @DisplayName("ERROR state should block operational commands")
    fun testErrorStateBlocksCommands() {
        val errorStatus = DispenserStatus.ERROR(errorCode = 123)
        
        // These commands should be blocked
        val blockedCommands = listOf(
            EhlCommand.UNBLOCK,
            EhlCommand.PRODUCT_SELECT,
            EhlCommand.PROG_PRC,
            EhlCommand.PROG_AMOUNT,
            EhlCommand.PROG_VOLUME
        )
        
        blockedCommands.forEach { command ->
            assertTrue(
                DispenserFaultHandler.shouldBlockCommand(errorStatus, command),
                "Expected $command to be blocked in ERROR state"
            )
        }
        
        // These commands should NOT be blocked (diagnostic/query commands)
        val allowedCommands = listOf(
            EhlCommand.STATE,
            EhlCommand.ERROR_QUERY,
            EhlCommand.LINETEST
        )
        
        allowedCommands.forEach { command ->
            assertFalse(
                DispenserFaultHandler.shouldBlockCommand(errorStatus, command),
                "Expected $command to be allowed in ERROR state"
            )
        }
    }
    
    @Test
    @DisplayName("IDLE state should not block any commands")
    fun testIdleStateAllowsCommands() {
        val idleStatus = DispenserStatus.IDLE
        
        val allCommands = listOf(
            EhlCommand.UNBLOCK,
            EhlCommand.PRODUCT_SELECT,
            EhlCommand.PROG_PRC,
            EhlCommand.STATE,
            EhlCommand.BLOCK
        )
        
        allCommands.forEach { command ->
            assertFalse(
                DispenserFaultHandler.shouldBlockCommand(idleStatus, command),
                "Expected $command to be allowed in IDLE state"
            )
        }
    }
    
    @Test
    @DisplayName("canFuel should return false for ERROR state")
    fun testCanFuelWithErrorState() {
        val errorStatus = DispenserStatus.ERROR(errorCode = 123)
        
        assertFalse(DispenserFaultHandler.canFuel(errorStatus, null))
        assertFalse(DispenserFaultHandler.canFuel(errorStatus, EhlFault.E_05))
    }
    
    @Test
    @DisplayName("canFuel should return false for unresolved CRITICAL fault")
    fun testCanFuelWithCriticalFault() {
        val idleStatus = DispenserStatus.IDLE
        val criticalFault = EhlFault.E_06
        
        assertFalse(
            DispenserFaultHandler.canFuel(idleStatus, criticalFault),
            "Expected fueling to be blocked with unresolved CRITICAL fault"
        )
    }
    
    @Test
    @DisplayName("canFuel should return true for WARNING fault")
    fun testCanFuelWithWarningFault() {
        val idleStatus = DispenserStatus.IDLE
        val warningFault = EhlFault.E_01
        
        assertTrue(
            DispenserFaultHandler.canFuel(idleStatus, warningFault),
            "Expected fueling to be allowed with WARNING fault"
        )
    }
    
    @Test
    @DisplayName("canFuel should return true for normal operation")
    fun testCanFuelNormal() {
        val authorizedStatus = DispenserStatus.AUTHORIZED
        
        assertTrue(
            DispenserFaultHandler.canFuel(authorizedStatus, null),
            "Expected fueling to be allowed in normal operation"
        )
    }
    
    @Test
    @DisplayName("All CRITICAL faults should produce ERROR status")
    fun testAllCriticalFaultsProduceError() {
        val criticalFaults = listOf(
            EhlFault.E_05,
            EhlFault.E_06,
            EhlFault.E_07,
            EhlFault.E_08
        )
        
        criticalFaults.forEach { fault ->
            val status = DispenserFaultHandler.determineStatusFromFault(
                fault = fault,
                currentStatus = DispenserStatus.IDLE
            )
            
            assertTrue(status is DispenserStatus.ERROR,
                "Expected ${fault.code} to produce ERROR status")
        }
    }
    
    @Test
    @DisplayName("All WARNING faults should maintain status")
    fun testAllWarningFaultsMaintainStatus() {
        val warningFaults = listOf(
            EhlFault.E_01,
            EhlFault.E_02,
            EhlFault.E_09
        )
        
        val testStatus = DispenserStatus.PUMPING
        
        warningFaults.forEach { fault ->
            val status = DispenserFaultHandler.determineStatusFromFault(
                fault = fault,
                currentStatus = testStatus
            )
            
            assertEquals(testStatus, status,
                "Expected ${fault.code} to maintain PUMPING status")
        }
    }
}
