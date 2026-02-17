package no.cloudberries.lpg.api.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.logging.Logger

/**
 * Base class for integration tests.
 *
 * Bruker H2 in-memory database som standard - kjører alltid uten Docker.
 * For PostgreSQL-testing, se PostgresIntegrationTest (krever Docker/Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestTerminalConfiguration::class)
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    protected val baseUrl: String
        get() = "http://localhost:$port"

    companion object {
        private val logger = Logger.getLogger(BaseIntegrationTest::class.java.name)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // H2 in-memory database - kjører alltid uten Docker
            registry.add("spring.datasource.url") { "jdbc:h2:mem:lpg_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL" }
            registry.add("spring.datasource.driver-class-name") { "org.h2.Driver" }
            registry.add("spring.datasource.username") { "sa" }
            registry.add("spring.datasource.password") { "" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.H2Dialect" }
            
            // Disable Azure sync for tests
            registry.add("azure.enabled") { "false" }

            // Disable security for tests (permitAll)
            registry.add("security.api-token") { "test-token-12345" }

            // Use mock terminal for tests (disables NetsCloudTerminalAdapter)
            registry.add("terminal.provider") { "mock" }

            logger.info("✅ Integration tests using H2 in-memory database")
        }
    }

    protected fun authHeader(): Pair<String, String> {
        return "Authorization" to "Bearer test-token-12345"
    }
}
