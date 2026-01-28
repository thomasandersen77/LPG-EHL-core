package no.cloudberries.lpg.service.event

import java.time.Instant

/**
 * Event Publisher Interface (Hexagonal Port)
 * 
 * Dette er en OUTPUT PORT i hexagonal architecture.
 * Service-laget publiserer events gjennom dette interfacet
 * uten å vite om implementasjonen (WebSocket, Message Queue, etc.)
 * 
 * Implementasjoner (adapters) kan være:
 * - WebSocket (webapp)
 * - No-op (headless, cli)
 * - Message Queue (Kafka, RabbitMQ)
 * - Logging only
 */
interface EventPublisher {
    
    /**
     * Publish a price update event.
     */
    fun publishPriceUpdate(pricePerLiterKr: Double)
    
    /**
     * Publish a pump status update event.
     */
    fun publishPumpStatusUpdate(pumpStatus: PumpStatusEvent)
    
    /**
     * Publish a generic log event.
     */
    fun publishLogEvent(logEvent: LogEvent)
}

/**
 * Pump status event data.
 */
data class PumpStatusEvent(
    val address: Int,
    val state: String,
    val volumeLitres: Double,
    val amountKr: Double,
    val pricePerLitreKr: Double,
    val nozzleLifted: Boolean,
    val hasPendingTransaction: Boolean,
    val timestamp: Instant = Instant.now()
)

/**
 * Log event data.
 */
data class LogEvent(
    val channel: LogChannel,
    val level: LogLevel,
    val logger: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)

enum class LogChannel {
    API,      // REST API logs (controllers, REST endpoints)
    SERVICE,  // Service layer logs (PumpStateService, PumpAuthorizationService, TransactionService)
    EMULATOR, // Emulator logs (EhlDispenserEmulator - LAB mode only)
    PROTOCOL, // EHL protocol packet logs (TX/RX HEX, SerialPortManager)
    ALL       // Meta-channel for subscribing to all logs
}

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR
}

/**
 * No-op implementation for headless and CLI modes.
 * Does nothing - events are simply discarded.
 */
class NoOpEventPublisher : EventPublisher {
    override fun publishPriceUpdate(pricePerLiterKr: Double) {
        // No-op
    }
    
    override fun publishPumpStatusUpdate(pumpStatus: PumpStatusEvent) {
        // No-op
    }
    
    override fun publishLogEvent(logEvent: LogEvent) {
        // No-op
    }
}
