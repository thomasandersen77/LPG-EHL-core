package no.cloudberries.lpg.service.event

import org.springframework.context.ApplicationEvent

/**
 * Published when a pump stops with a pending transaction (volume > 0).
 * Used by terminal integration to trigger capture when reservation session exists.
 */
class PumpStoppedEvent(
    source: Any,
    val pumpId: Int,
    val volumeLitres: Double,
    val amountKr: Double
) : ApplicationEvent(source)
