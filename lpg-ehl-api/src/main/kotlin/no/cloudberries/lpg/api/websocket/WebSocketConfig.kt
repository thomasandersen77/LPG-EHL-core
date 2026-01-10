package no.cloudberries.lpg.api.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * WebSocket configuration for real-time log streaming.
 * 
 * Endpoint: ws://localhost:8080/ws/logs
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val logWebSocketHandler: LogWebSocketHandler
) : WebSocketConfigurer {
    
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(logWebSocketHandler, "/ws/logs")
            .setAllowedOrigins("*") // Allow all origins for development
    }
}
