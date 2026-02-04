package no.cloudberries.lpg.api.config

import io.undertow.server.DefaultByteBufferPool
import io.undertow.websockets.jsr.WebSocketDeploymentInfo
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Undertow server configuration.
 * 
 * Fixes: UT026010: Buffer pool was not set on WebSocketDeploymentInfo
 * 
 * Configures a proper byte buffer pool for WebSocket connections
 * to improve stability and prevent channel closed errors.
 */
@Configuration
class UndertowConfig {

    @Bean
    fun undertowWebServerCustomizer(): WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.addDeploymentInfoCustomizers(UndertowDeploymentInfoCustomizer { deploymentInfo ->
                val webSocketDeploymentInfo = WebSocketDeploymentInfo().apply {
                    // Configure buffer pool for WebSocket connections
                    // Direct buffers = true for better performance
                    // Buffer size = 8KB (reasonable for most WebSocket messages)
                    // Max pool size = 100 (handles concurrent connections)
                    buffers = DefaultByteBufferPool(true, 8192, 100, 12)
                }
                deploymentInfo.addServletContextAttribute(
                    WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                    webSocketDeploymentInfo
                )
            })
        }
    }
}
