package no.cloudberries.lpg.api.integration

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.logging.Logger

/**
 * Base class for integration tests using Testcontainers.
 * Tests will be skipped if Docker is not available or cannot start containers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    protected val baseUrl: String
        get() = "http://localhost:$port"

    companion object {
        private val logger = Logger.getLogger(BaseIntegrationTest::class.java.name)
        
        private val dockerAvailable: Boolean by lazy {
            try {
                DockerClientFactory.instance().isDockerAvailable
            } catch (e: Exception) {
                logger.warning("Docker not available: ${e.message}")
                false
            }
        }
        
        @JvmStatic
        val postgres: PostgreSQLContainer<*>? by lazy {
            if (!dockerAvailable) {
                null
            } else {
                try {
                    PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                        .withDatabaseName("lpg_ehl_test")
                        .withUsername("test_user")
                        .withPassword("test_password")
                        .withInitScript("test-schema.sql")
                        .also { it.start() }
                } catch (e: Exception) {
                    logger.warning("Failed to start PostgreSQL container: ${e.message}")
                    null
                }
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            if (postgres != null) {
                registry.add("spring.datasource.url") { postgres?.jdbcUrl }
                registry.add("spring.datasource.username") { postgres?.username }
                registry.add("spring.datasource.password") { postgres?.password }
            } else {
                // Provide dummy values to prevent Spring context failure
                registry.add("spring.datasource.url") { "jdbc:h2:mem:test" }
                registry.add("spring.datasource.username") { "sa" }
                registry.add("spring.datasource.password") { "" }
            }
            
            // Disable Azure sync for tests
            registry.add("azure.enabled") { "false" }
            
            // Use in-memory auth token
            registry.add("security.api-token") { "test-token-12345" }
        }

        @JvmStatic
        @BeforeAll
        fun setup() {
            // Skip tests if Docker is not available or container failed to start
            Assumptions.assumeTrue(
                postgres?.isRunning == true,
                "PostgreSQL container is not running. Skipping integration tests."
            )
        }
    }

    protected fun authHeader(): Pair<String, String> {
        return "Authorization" to "Bearer test-token-12345"
    }
}
