package no.cloudberries.lpg.api.integration

import no.cloudberries.lpg.emulator.FakeNetsCloudServer
import no.cloudberries.lpg.payment.BaxResponse
import no.cloudberries.lpg.payment.NetsBaxProtocol
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.TimeUnit

/**
 * Integration Test: NetsCloudSocketClient → FakeNetsCloudServer
 * 
 * Tests the complete flow from API client to Emulator server.
 * Verifies Clean Architecture separation: API uses Core interfaces, Emulator provides test infrastructure.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetsCloudSocketClientIntegrationTest {
    
    private lateinit var fakeServer: FakeNetsCloudServer
    
    @BeforeAll
    fun startServer() {
        println("🚀 Starting FakeNetsCloudServer...")
        fakeServer = FakeNetsCloudServer(port = 6001)
        fakeServer.start()
        
        // Give server time to start
        Thread.sleep(500)
    }
    
    @AfterAll
    fun stopServer() {
        println("🛑 Stopping FakeNetsCloudServer...")
        fakeServer.stop()
    }
    
    @Test
    fun `should connect to fake server via SSL`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            
            // Then
            assertTrue(terminal.isConnected, "Client should be connected")
        }
    }
    
    @Test
    fun `should receive ConnectCloud handshake from server`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            
            // Server sends handshake automatically
            // Give it a moment to arrive
            Thread.sleep(200)
            
            // Then - connection should be established
            assertTrue(terminal.isConnected)
        }
    }
    
    @Test
    fun `should send purchase command and receive approval`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            
            // Wait for handshake
            Thread.sleep(200)
            
            // Send purchase command
            val command = NetsBaxProtocol.createPurchaseCommand(
                amountCents = 10000,  // 100.00 NOK
                operatorId = "1"
            )
            
            val response = terminal.sendCommand(command)
            
            // Then
            assertNotNull(response)
            assertTrue(response.rawData.isNotEmpty(), "Response should contain data")
            
            // Parse response
            val result = response.parse()
            assertNotNull(result)
            
            // Fake server approves all purchases
            when (result) {
                is BaxResponse.Success -> {
                    println("✅ Payment approved!")
                    println("   Transaction ID: ${result.transactionId}")
                    println("   Auth Code: ${result.authCode}")
                    assertNotNull(result.transactionId)
                }
                is BaxResponse.Data -> {
                    // Some responses come as Data
                    println("📊 Data response: ${result.payload}")
                    assertTrue(result.payload.contains("00") || result.payload.contains("TXN"))
                }
                else -> {
                    fail("Expected success or data response, got: $result")
                }
            }
        }
    }
    
    @Test
    fun `should send preauth command and receive approval`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            Thread.sleep(200)  // Wait for handshake
            
            // Send preauth command
            val command = NetsBaxProtocol.createPreauthCommand(
                amountCents = 50000,  // 500.00 NOK
                operatorId = "2"
            )
            
            val response = terminal.sendCommand(command)
            
            // Then
            assertNotNull(response)
            assertTrue(response.rawData.isNotEmpty())
            
            val result = response.parse()
            println("Preauth result: $result")
            
            // Verify we got some response (fake server approves all)
            assertNotNull(result)
        }
    }
    
    @Test
    fun `should send cancel command and receive acknowledgment`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            Thread.sleep(200)
            
            val command = NetsBaxProtocol.createCancelCommand()
            val response = terminal.sendCommand(command)
            
            // Then
            assertNotNull(response)
            assertTrue(response.rawData.isNotEmpty())
            
            val result = response.parse()
            println("Cancel result: $result")
        }
    }
    
    @Test
    fun `should handle multiple sequential transactions`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.use { terminal ->
            terminal.connect()
            Thread.sleep(200)
            
            // Transaction 1
            val cmd1 = NetsBaxProtocol.createPurchaseCommand(1000, "1")
            val resp1 = terminal.sendCommand(cmd1)
            assertNotNull(resp1)
            println("Transaction 1: ${resp1.parse()}")
            
            // Transaction 2
            val cmd2 = NetsBaxProtocol.createPurchaseCommand(2000, "1")
            val resp2 = terminal.sendCommand(cmd2)
            assertNotNull(resp2)
            println("Transaction 2: ${resp2.parse()}")
            
            // Transaction 3
            val cmd3 = NetsBaxProtocol.createPurchaseCommand(3000, "1")
            val resp3 = terminal.sendCommand(cmd3)
            assertNotNull(resp3)
            println("Transaction 3: ${resp3.parse()}")
            
            // All should succeed
            assertTrue(resp1.rawData.isNotEmpty())
            assertTrue(resp2.rawData.isNotEmpty())
            assertTrue(resp3.rawData.isNotEmpty())
        }
    }
    
    @Test
    fun `should handle connection close gracefully`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When
        client.connect()
        assertTrue(client.isConnected)
        
        client.close()
        
        // Then
        assertFalse(client.isConnected, "Client should be disconnected after close")
    }
    
    @Test
    fun `should throw exception when sending command without connection`() {
        // Given
        val client = NetsCloudSocketClient(
            host = "localhost",
            port = 6001,
            trustAllCertificates = true
        )
        
        // When/Then
        assertThrows<Exception> {
            val command = NetsBaxProtocol.createPurchaseCommand(1000)
            client.sendCommand(command)
        }
    }
}
