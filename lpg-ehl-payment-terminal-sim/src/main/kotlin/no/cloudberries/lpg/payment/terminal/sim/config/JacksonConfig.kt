package no.cloudberries.lpg.payment.terminal.sim.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Jackson configuration for JSON response casing.
 *
 * NOTE: /health endpoint returns lowercase keys manually.
 */
@Configuration
class JacksonConfig(
    private val simulatorConfig: SimulatorConfig
) {

    @Bean
    fun jacksonBuilder(): Jackson2ObjectMapperBuilder {
        val namingStrategy = if (simulatorConfig.responseCasing.equals("camelCase", ignoreCase = true)) {
            PropertyNamingStrategies.LOWER_CAMEL_CASE
        } else {
            PropertyNamingStrategies.UPPER_CAMEL_CASE
        }
        return Jackson2ObjectMapperBuilder()
            .propertyNamingStrategy(namingStrategy)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
