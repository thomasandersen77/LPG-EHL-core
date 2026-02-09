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
@Profile("local", "lab", "test", "demo", "default", "h2", "field")
class SecurityConfig(
    @Value("\${security.api-token:dev-token-12345}") private val apiToken: String,
    @Value("\${security.cors.allowed-origins:http://localhost:3001,http://localhost:8080}") private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .anonymous { it.authorities("ROLE_ANONYMOUS") } // Enable anonymous for local demo
            // Removed authentication filter - all /api/v1/** endpoints are public for local demo
            // .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    // CORS preflight requests (OPTIONS) - MUST be first
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    // Public endpoints - no authentication required
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // WebSocket endpoints - must be open for real-time updates
                    .requestMatchers("/ws/**").permitAll()
                    // All API v1 endpoints open for local demo testing
                    .requestMatchers("/api/v1/**").permitAll()
                    .requestMatchers("/api/**").permitAll() // Also allow /api/** for any sub-paths
                    .requestMatchers("/error").permitAll() // Allow error endpoint
                    // Static assets (CSS, JS, images, etc.)
                    .requestMatchers("/assets/**", "/*.ico", "/*.png", "/*.svg", "/*.js", "/*.css").permitAll()
                    // SPA routes - allow all non-API routes (SpaRedirectConfig handles forwarding to index.html)
                    .requestMatchers("/", "/index.html").permitAll()
                    .requestMatchers("/price-admin", "/price-admin/**").permitAll()
                    .requestMatchers("/control", "/control/**").permitAll()
                    .requestMatchers("/transactions", "/transactions/**").permitAll()
                    .requestMatchers("/demo", "/demo/**").permitAll()
                    // Protected actuator endpoints (except health)
                    .requestMatchers("/actuator/**").authenticated()
                    // Allow everything else for SPA fallback (local dev mode)
                    .anyRequest().permitAll()
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
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("*")
        configuration.allowedHeaders = listOf("*")
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
