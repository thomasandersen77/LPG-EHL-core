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
 * Configures Spring Boot to serve React frontend as a Single Page Application.
 * Returns index.html for all non-API routes to support client-side routing.
 */
@Configuration
class SpaRedirectConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(SpaPathResourceResolver())
    }

    private class SpaPathResourceResolver : PathResourceResolver() {

        override fun getResource(resourcePath: String, location: Resource): Resource? {
            val requestedResource = location.createRelative(resourcePath)

            if (requestedResource.exists() && requestedResource.isReadable) {
                return requestedResource
            }

            return if (!resourcePath.startsWith("api/")) {
                ClassPathResource("static/index.html")
            } else {
                null
            }
        }
    }
}
