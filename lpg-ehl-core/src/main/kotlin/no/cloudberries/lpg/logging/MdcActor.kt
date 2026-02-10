package no.cloudberries.lpg.logging

import org.slf4j.MDC

/**
 * MDC Actor context for structured logging.
 *
 * Aktører:
 * - **OPERATOR** – Stasjonseier (manuell unblock, config, admin)
 * - **DEBUG** – Felt-debugging via curl
 * - **CUSTOMER** – Kunde ved pumpe (kortdragning → UNBLOCK)
 * - **SYSTEM** – Automatisk polling, Azure sync
 *
 * Bruk:
 * - HTTP: MdcActorFilter setter actor basert på request path
 * - Tjenester: `MdcActor.runWithActor(Actor.CUSTOMER) { ... }`
 */
object MdcActor {
    const val MDC_KEY = "actor"

    enum class Actor(val value: String) {
        OPERATOR("OPERATOR"),
        DEBUG("DEBUG"),
        CUSTOMER("CUSTOMER"),
        SYSTEM("SYSTEM")
    }

    /**
     * Kjør blokk med angitt actor i MDC.
     * Fjerner actor fra MDC etterpå (gjør ingenting hvis allerede satt av filter).
     */
    inline fun <T> runWithActor(actor: Actor, block: () -> T): T {
        val previous = MDC.get(MDC_KEY)
        try {
            MDC.put(MDC_KEY, actor.value)
            return block()
        } finally {
            if (previous != null) {
                MDC.put(MDC_KEY, previous)
            } else {
                MDC.remove(MDC_KEY)
            }
        }
    }
}
