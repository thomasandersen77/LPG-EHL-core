package no.cloudberries.lpg.api.integration

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Base class for integration tests using Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    protected val baseUrl: String
        get() = "http://localhost:$port"

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("lpg_ehl_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("test-schema.sql")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            
            // Disable Azure sync for tests
            registry.add("azure.enabled") { "false" }
            
            // Use in-memory auth token
            registry.add("security.api-token") { "test-token-12345" }
        }

        @JvmStatic
        @BeforeAll
        fun setup() {
            // Ensure containers are started
            postgres.start()
        }
    }

    protected fun authHeader(): Pair<String, String> {
        return "Authorization" to "Bearer test-token-12345"
    }
}
