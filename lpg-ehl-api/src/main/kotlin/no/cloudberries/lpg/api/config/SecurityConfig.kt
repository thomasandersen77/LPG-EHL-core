package no.cloudberries.lpg.api.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@Profile("local", "test", "demo")
class SecurityConfig(
    @Value("\${security.api-token}") private val apiToken: String,
    @Value("\${security.cors.allowed-origins}") private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .anonymous { it.disable() } // Disable anonymous authentication - not needed for demo
            // Removed authentication filter - all /api/v1/** endpoints are public for local demo
            // .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints - no authentication required
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // All API v1 endpoints open for local demo testing
                    .requestMatchers("/api/v1/**").permitAll()
                    .requestMatchers("/error").permitAll() // Allow error endpoint
                    // Protected actuator endpoints
                    .requestMatchers("/actuator/**").authenticated()
                    .requestMatchers("/*").permitAll()
                    .anyRequest().denyAll() // Deny everything else explicitly
            }

        return http.build()
    }

    @Bean
    fun tokenAuthenticationFilter(): TokenAuthenticationFilter {
        return TokenAuthenticationFilter(apiToken)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

/**
 * Simple bearer token authentication filter
 */
class TokenAuthenticationFilter(private val validToken: String) : AbstractPreAuthenticatedProcessingFilter() {

    override fun getPreAuthenticatedPrincipal(request: HttpServletRequest): Any? {
        val authHeader = request.getHeader("Authorization") ?: return null
        
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return null
        }

        val token = authHeader.substring(7)
        return if (token == validToken) "api-user" else null
    }

    override fun getPreAuthenticatedCredentials(request: HttpServletRequest): Any {
        return "N/A"
    }

    override fun afterPropertiesSet() {
        // Set AuthenticationManager BEFORE calling super.afterPropertiesSet()
        setAuthenticationManager { authentication ->
            if (authentication.principal != null) {
                authentication.isAuthenticated = true
                authentication as Authentication
            } else {
                null
            }
        }
        super.afterPropertiesSet()
    }

    override fun unsuccessfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        failed: org.springframework.security.core.AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error":"Unauthorized","message":"Invalid or missing bearer token"}""")
    }

    init {
        setAuthenticationDetailsSource { request ->
            mapOf(
                "remoteAddress" to request.remoteAddr,
                "sessionId" to request.session?.id
            )
        }
    }
}
