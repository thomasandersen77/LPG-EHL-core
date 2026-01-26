package no.cloudberries.lpg.service.system

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Diagnostics Service
 * 
 * Aggregates dispenser health information from multiple sources:
 * - Connection status and communication timestamps
 * - Current operational state
 * - Detected faults and their severity
 * - RS-485 communication health
 * 
 * ## Architecture:
 * - This service acts as a facade over DispenserService and fault tracking
 * - Provides read-only diagnostics snapshots
 * - Does not modify dispenser state
 * 
 * ## Data Sources:
 * - DispenserService: Current state and communication times
 * - FaultTracker: Detected faults with timestamps
 * - Connection metrics: RX/TX times, error rates
 */
@Service
class DiagnosticsService {
    private val logger = LoggerFactory.getLogger(DiagnosticsService::class.java)
    
    // Track faults per dispenser (address -> last fault)
    private val faultTracker = ConcurrentHashMap<Int, FaultInfo>()
    
    // Track communication times per dispenser
    private val communicationTracker = ConcurrentHashMap<Int, CommunicationMetrics>()
    
    // Connection timeout threshold (60 seconds)
    private val connectionTimeoutSeconds = 60L
    
    /**
     * Get diagnostics for all known dispensers
     */
    fun getAllDiagnostics(): List<EhlDiagnosticsSnapshot> {
        val knownAddresses = (faultTracker.keys + communicationTracker.keys).distinct()
        
        return knownAddresses.map { address ->
            buildDiagnosticsSnapshot(address)
        }.sortedBy { it.address }
    }
    
    /**
     * Get diagnostics for specific dispenser
     */
    fun getDiagnosticsForDispenser(address: Int): EhlDiagnosticsSnapshot? {
        // Return null if dispenser has never been seen
        if (!communicationTracker.containsKey(address) && !faultTracker.containsKey(address)) {
            return null
        }
        
        return buildDiagnosticsSnapshot(address)
    }
    
    /**
     * Get all dispensers with any detected fault
     */
    fun getDispensersWithFaults(): List<EhlDiagnosticsSnapshot> {
        return faultTracker.keys.map { address ->
            buildDiagnosticsSnapshot(address)
        }.sortedBy { it.address }
    }
    
    /**
     * Get dispensers with CRITICAL faults or ERROR state
     */
    fun getDispenserWithCriticalFaults(): List<EhlDiagnosticsSnapshot> {
        return getAllDiagnostics().filter { snapshot ->
            snapshot.hasCriticalFault() || snapshot.isInError()
        }
    }
    
    /**
     * Record a detected fault
     * 
     * Called by DispenserService or connection handlers when fault is detected
     */
    fun recordFault(address: Int, fault: EhlFault) {
        val faultInfo = FaultInfo.fromFault(fault)
        faultTracker[address] = faultInfo
        
        logger.info(
            "Recorded fault for dispenser $address: ${fault.code} (${fault.level})"
        )
    }
    
    /**
     * Clear fault for dispenser (e.g. after manual reset)
     */
    fun clearFault(address: Int) {
        faultTracker.remove(address)
        logger.info("Cleared fault for dispenser $address")
    }
    
    /**
     * Update communication metrics
     * 
     * Called when RX/TX occurs
     */
    fun recordCommunication(address: Int, isReceive: Boolean) {
        val metrics = communicationTracker.getOrPut(address) {
            CommunicationMetrics()
        }
        
        val now = Instant.now()
        if (isReceive) {
            communicationTracker[address] = metrics.copy(lastRxAt = now)
        } else {
            communicationTracker[address] = metrics.copy(lastTxAt = now)
        }
    }
    
    /**
     * Build complete diagnostics snapshot for a dispenser
     */
    private fun buildDiagnosticsSnapshot(address: Int): EhlDiagnosticsSnapshot {
        val metrics = communicationTracker[address]
        val lastFault = faultTracker[address]
        
        // Determine connection status based on last RX time
        val connected = metrics?.lastRxAt?.let { lastRx ->
            val secondsSinceLastRx = Duration.between(lastRx, Instant.now()).seconds
            secondsSinceLastRx < connectionTimeoutSeconds
        } ?: false
        
        // Determine current state
        // In real implementation, this would come from DispenserService
        val state = determineState(address, lastFault)
        
        // Generate RS-485 hints
        val lastRxDuration = metrics?.lastRxAt?.let { lastRx ->
            Duration.between(lastRx, Instant.now()).toMillis()
        }
        
        val rs485Hints = Rs485Hints.generateHints(
            connected = connected,
            lastRxDuration = lastRxDuration
        )
        
        return EhlDiagnosticsSnapshot(
            address = address,
            connected = connected,
            lastRxAt = metrics?.lastRxAt,
            lastTxAt = metrics?.lastTxAt,
            state = state,
            lastFault = lastFault,
            rs485Hints = rs485Hints
        )
    }
    
    /**
     * Determine dispenser state
     * 
     * In full implementation, this would delegate to DispenserService
     * For now, infer from faults
     */
    private fun determineState(address: Int, lastFault: FaultInfo?): DispenserStatus {
        return when {
            lastFault != null && lastFault.level == EhlErrorLevel.CRITICAL -> {
                DispenserStatus.ERROR(errorCode = lastFault.code.hashCode())
            }
            else -> DispenserStatus.IDLE
        }
    }
}

/**
 * Communication metrics for a dispenser
 */
private data class CommunicationMetrics(
    val lastRxAt: Instant? = null,
    val lastTxAt: Instant? = null,
    val totalErrors: Int = 0,
    val totalRequests: Int = 0
) {
    fun getErrorRate(): Double {
        return if (totalRequests > 0) {
            totalErrors.toDouble() / totalRequests.toDouble()
        } else {
            0.0
        }
    }
}
