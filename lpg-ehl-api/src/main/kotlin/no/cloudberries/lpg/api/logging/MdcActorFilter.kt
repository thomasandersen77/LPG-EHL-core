package no.cloudberries.lpg.api.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.cloudberries.lpg.logging.MdcActor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.slf4j.MDC

/**
 * Setter MDC actor basert på HTTP request path.
 *
 * - /api/debug → DEBUG (felt-debugging)
 * - /api/v1 → OPERATOR (stasjonseier)
 * - /actuator → SYSTEM (health, metrics)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcActorFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: ""
        val actor = when {
            path.startsWith("/api/debug") -> MdcActor.Actor.DEBUG
            path.startsWith("/actuator") -> MdcActor.Actor.SYSTEM
            path.startsWith("/api/v1") -> MdcActor.Actor.OPERATOR
            else -> MdcActor.Actor.OPERATOR  // Swagger, etc. = operator context
        }
        try {
            MDC.put(MdcActor.MDC_KEY, actor.value)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MdcActor.MDC_KEY)
        }
    }
}
