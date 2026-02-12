package no.cloudberries.lpg.communication

/**
 * Tracks command attempts and outcomes for attempt-based watchdog logic.
 */
interface CommandAttemptTracker {
    fun recordAttempt()
    fun recordSuccess()
    fun recordFailure()
    fun resetFailures()

    val lastAttemptAt: Long
    val lastSuccessfulCommandAt: Long
    val consecutiveFailures: Int
}