package no.cloudberries.lpg.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * SPA Redirect Configuration
 *
 * Configures Spring Boot to serve React frontend as a Single Page Application.
 * Returns index.html for all non-API routes to support client-side routing.
 * 
 * IMPORTANT: This config has lowest precedence to ensure API controllers are matched first.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
class SpaRedirectConfig : WebMvcConfigurer {

    companion object {
        private val EXCLUDED_PREFIXES = listOf(
            "api/",
            "api-docs",
            "swagger",
            "actuator/",
            "ws/"
        )
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Static assets (js, css, images) - highest priority
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCachePeriod(3600)
        
        // Other static files
        registry.addResourceHandler("/*.svg", "/*.ico", "/*.png", "/*.json")
            .addResourceLocations("classpath:/static/")
        
        // SPA fallback - lowest priority, excludes API paths
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(SpaPathResourceResolver())
    }

    private class SpaPathResourceResolver : PathResourceResolver() {

        override fun getResource(resourcePath: String, location: Resource): Resource? {
            // Skip API and other excluded paths entirely
            if (EXCLUDED_PREFIXES.any { resourcePath.startsWith(it) }) {
                return null
            }
            
            val requestedResource = location.createRelative(resourcePath)

            // If the resource exists, serve it
            if (requestedResource.exists() && requestedResource.isReadable) {
                return requestedResource
            }

            // Otherwise return index.html for SPA routing
            return ClassPathResource("static/index.html")
        }
    }
}
