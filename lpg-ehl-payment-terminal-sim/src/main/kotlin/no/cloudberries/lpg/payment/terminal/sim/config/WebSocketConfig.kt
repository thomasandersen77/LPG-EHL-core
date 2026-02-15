package no.cloudberries.lpg.payment.terminal.sim.config

import no.cloudberries.lpg.payment.terminal.sim.websocket.TerminalEventsWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val terminalEventsWebSocketHandler: TerminalEventsWebSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(terminalEventsWebSocketHandler, "/v1/events/ws")
            .setAllowedOrigins("*")
    }
}
