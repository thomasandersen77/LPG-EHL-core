package no.cloudberries.lpg.iothub

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

class AzureIotHubClientTest {

    @Test
    fun `should initialize without connection string when disabled`() = runTest {
        val config = IotHubConfig(enabled = false, connectionString = "")
        val client = AzureIotHubClient(config)
        
        // Should not throw and not connect
        assertDoesNotThrow {
            client.init()
        }
    }
}
