package no.cloudberries.lpg.headless

import no.cloudberries.lpg.service.event.EventPublisher
import no.cloudberries.lpg.service.event.NoOpEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Event Publisher Configuration for Headless Mode
 * 
 * Headless-applikasjonen trenger IKKE WebSocket events,
 * så vi bruker No-op implementasjon som bare forkaster events.
 * 
 * Dette lar service-laget kalle EventPublisher uten å vite
 * om det er webapp (med WebSocket) eller headless (uten).
 */
@Configuration
class HeadlessEventConfiguration {
    
    @Bean
    fun eventPublisher(): EventPublisher {
        return NoOpEventPublisher()
    }
}
