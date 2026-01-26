package no.cloudberries.lpg.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["no.cloudberries.lpg.api.repository"])
@EnableTransactionManagement
class DatabaseConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            findAndRegisterModules() // This finds and registers JavaTimeModule
            
            // Serialize dates as ISO-8601 strings instead of arrays
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            
            // Additional useful settings
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        }
    }
}
