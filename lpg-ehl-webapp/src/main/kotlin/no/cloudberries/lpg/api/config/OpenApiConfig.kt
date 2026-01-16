package no.cloudberries.lpg.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("LPG EHL API")
                    .version("1.0.0")
                    .description("REST API for LPG dispenser transaction management and Azure sync")
                    .license(License().name("Proprietary").url("https://cloudberries.no"))
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearer-token",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Enter your API token (without 'Bearer' prefix)")
                    )
            )
    }
}
