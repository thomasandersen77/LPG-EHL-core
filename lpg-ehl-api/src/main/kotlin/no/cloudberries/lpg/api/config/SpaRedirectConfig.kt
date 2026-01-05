package no.cloudberries.lpg.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * SPA Redirect Configuration
 * 
 * Configures Spring Boot to serve React frontend as a Single Page Application (SPA).
 * 
 * **Problem:**
 * React Router uses client-side routing. When a user navigates to /transactions
 * and then refreshes the page (F5), the browser requests /transactions from the server.
 * Without this config, Spring Boot would return 404 because it doesn't have a controller
 * for that path.
 * 
 * **Solution:**
 * This configuration intercepts all requests that:
 * 1. Are NOT API calls (don't start with /api)
 * 2. Are NOT static resources (js, css, images, etc.)
 * 3. Don't match an actual file in /static
 * 
 * For these requests, we return index.html instead, allowing React Router to handle
 * the routing on the client side.
 * 
 * **How it works:**
 * 1. All requests go through PathResourceResolver
 * 2. If the resource exists (CSS, JS, images), serve it normally
 * 3. If the resource doesn't exist and it's not an API call, serve index.html
 * 4. React then loads and React Router takes over, showing the correct page
 * 
 * **IntelliJ Development:**
 * When running LpgEhlApiApplication in IntelliJ, the React frontend will be served
 * on localhost:8080 (after building frontend with build_monolith.sh).
 * 
 * @see <a href="https://spring.io/guides/tutorials/react-and-spring-data-rest/">Spring + React Guide</a>
 */
@Configuration
class SpaRedirectConfig : WebMvcConfigurer {
    
    /**
     * Configure resource handlers for SPA support
     * 
     * Maps all requests (/**) to static resources, with a custom resolver
     * that falls back to index.html for client-side routes.
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(SpaPathResourceResolver())
    }
    
    /**
     * Custom PathResourceResolver that returns index.html for SPA routes
     */
    private class SpaPathResourceResolver : PathResourceResolver() {
        
        override fun getResource(resourcePath: String, location: Resource): Resource? {
            val requestedResource = location.createRelative(resourcePath)
            
            // If the actual resource exists (CSS, JS, images, etc.), serve it
            if (requestedResource.exists() && requestedResource.isReadable) {
                return requestedResource
            }
            
            // If resource doesn't exist and path doesn't look like an API call,
            // return index.html to let React Router handle it
            // 
            // This allows URLs like /transactions, /payments, /fueling to work
            // when the user refreshes the page or shares a deep link
            return if (!resourcePath.startsWith("api/")) {
                ClassPathResource("static/index.html")
            } else {
                // For API calls that don't exist, let Spring Boot handle it
                // (will result in 404 from controller layer)
                null
            }
        }
    }
}
