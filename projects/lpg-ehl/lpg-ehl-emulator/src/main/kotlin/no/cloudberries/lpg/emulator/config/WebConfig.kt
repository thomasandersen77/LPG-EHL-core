package no.cloudberries.lpg.emulator.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * Web configuration for serving the React SPA frontend.
 * 
 * This configuration:
 * 1. Serves static files from /static/ in the JAR
 * 2. Falls back to index.html for client-side routing (React Router)
 * 3. Enables CORS for development (React dev server on different port)
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        // Local development origins
        val localOrigins = arrayOf(
            "http://localhost:5173",
            "http://localhost:3000", 
            "http://localhost:9001"
        )
        
        // ngrok wildcard patterns - allows any subdomain under ngrok-free.app/dev
        val ngrokPatterns = arrayOf(
            "https://*.ngrok-free.app",
            "https://*.ngrok-free.dev"
        )
        
        registry.addMapping("/api/**")
            .allowedOrigins(*localOrigins)
            .allowedOriginPatterns(*ngrokPatterns)  // Wildcard for ngrok
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
        
        registry.addMapping("/ws/**")
            .allowedOrigins(*localOrigins)
            .allowedOriginPatterns(*ngrokPatterns)  // Wildcard for ngrok
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve static assets (JS, CSS, images)
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")

        // Fallback to index.html for React Router
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requestedResource = location.createRelative(resourcePath)
                    
                    // If the resource exists, serve it
                    if (requestedResource.exists() && requestedResource.isReadable) {
                        return requestedResource
                    }
                    
                    // Otherwise, return index.html for client-side routing
                    // But only for paths that look like pages (not API calls or actual files)
                    if (!resourcePath.startsWith("api/") && 
                        !resourcePath.startsWith("ws/") &&
                        !resourcePath.contains(".")) {
                        return ClassPathResource("/static/index.html")
                    }
                    
                    return null
                }
            })
    }
}
