package no.cloudberries.lpg.netscloud

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Manual test for NetsCloudConnectTerminalClient against real hardware.
 *
 * This test is disabled by default for CI/CD pipelines.
 * To run manually:
 * 1. Ensure the terminal hardware is connected and powered on
 * 2. Configure credentials in application-nets-cloud.yaml or via environment variables:
 *    - NETS_USERNAME
 *    - NETS_PASSWORD
 *    - NETS_TERMINAL_ID
 * 3. Remove @Disabled annotation or run with: mvn test -Dtest=NetsCloudConnectTerminalClientTestManual
 *
 * Note: Purchase tests require a physical card to be tapped within 60 seconds.
 */
@Disabled("Manual test - requires real terminal hardware")
@TestMethodOrder(OrderAnnotation::class)
class NetsCloudConnectTerminalClientTestManual {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var config: NetsCloudConnectConfig
    private lateinit var authClient: NetsCloudAuthClient
    private lateinit var messageBuilder: NetsMessageBuilder
    private lateinit var responseParser: NetsResponseParser
    private lateinit var terminalClient: NetsCloudConnectTerminalClient

    @BeforeEach
    fun setup() {
        logger.info("=" * 80)
        logger.info("Setting up manual terminal test")
        logger.info("=" * 80)

        // Load configuration from environment or defaults
        config = NetsCloudConnectConfig(
            baseUrl = System.getenv("NETS_BASE_URL") ?: "https://connectcloud.aws.nets.eu",
            username = System.getenv("NETS_USERNAME") ?: "cloudberries_shared",
            password = System.getenv("NETS_PASSWORD") ?: "B8PnVjmVq-SMM9QD",
            terminalId = System.getenv("NETS_TERMINAL_ID") ?: "42696609",
            websocket = WebSocketConfig(
                pingIntervalMs = 20_000,
                reconnectDelayMs = 5_000,
                maxReconnectAttempts = 10
            ),
            timeouts = TimeoutConfig(
                loginTimeoutMs = 10_000,
                openTerminalTimeoutMs = 30_000,
                purchaseTimeoutMs = 60_000,  // 10 seconds for manual card tap
                reversalTimeoutMs = 60_000
            )
        )

        // Initialize components (NetsResponseParser must be created before NetsCloudWebSocketClient)
        authClient = NetsCloudAuthClient(config)
        messageBuilder = NetsMessageBuilder(config)
        responseParser = NetsResponseParser()

        terminalClient = NetsCloudConnectTerminalClient(
            config,
            authClient,
            messageBuilder,
            responseParser
        )

        logger.info("Configuration loaded:")
        logger.info("  Base URL: ${config.baseUrl}")
        logger.info("  Username: ${config.username}")
        logger.info("  Terminal ID: ${config.terminalId}")
        logger.info("  Purchase timeout: ${config.timeouts.purchaseTimeoutMs}ms")
    }

    @AfterEach
    fun teardown() = runTest {
        logger.info("=" * 80)
        logger.info("Cleaning up after test")
        logger.info("=" * 80)

        try {
            terminalClient.closeTerminal()
            authClient.close()
        } catch (e: Exception) {
            logger.warn("Error during cleanup: ${e.message}")
        }
    }

    @Test
    @Order(1)
    fun `test getHealth before opening terminal`() {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Get Health (Before Opening)")
        logger.info("=" * 80)

            val health = terminalClient.getHealth()

        logger.info("Health Status: ${health.status}")
        logger.info("Config Loaded: ${health.configLoaded}")

        assertTrue(health.configLoaded, "Configuration should be loaded")
        assertEquals("unhealthy", health.status, "Terminal should be unhealthy when not open")
    }

    @Test
    @Order(2)
    fun `test getStatus before opening terminal`() {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Get Status (Before Opening)")
        logger.info("=" * 80)

        val status = terminalClient.getStatus()

        logger.info("Terminal Open: ${status.terminalOpen}")
        logger.info("Terminal Ready: ${status.terminalReady}")
        logger.info("Connection State: ${status.connectionState}")

        assertEquals(false, status.terminalOpen, "Terminal should not be open")
        assertEquals(false, status.terminalReady, "Terminal should not be ready")
        assertEquals("DISCONNECTED", status.connectionState, "Terminal should be disconnected")
    }

    @Test
    @Order(3)
    fun `test openTerminal with real hardware`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Open Terminal")
        logger.info("=" * 80)

        val response = terminalClient.openTerminal()

        logger.info("Success: ${response.success}")
        logger.info("Message: ${response.message}")
        logger.info("Error: ${response.error}")

        assertTrue(response.success, "Terminal should open successfully: ${response.error}")
        assertNotNull(response.message, "Success message should be present")
    }

    @Test
    @Order(4)
    fun `test getHealth after opening terminal`() {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Get Health (After Opening)")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        val health = terminalClient.getHealth()

        logger.info("Health Status: ${health.status}")
        logger.info("Config Loaded: ${health.configLoaded}")

        assertTrue(health.configLoaded, "Configuration should be loaded")
        assertEquals("healthy", health.status, "Terminal should be healthy when open")
    }

    @Test
    @Order(5)
    fun `test getStatus after opening terminal`() {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Get Status (After Opening)")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        val status = terminalClient.getStatus()

        logger.info("Terminal Open: ${status.terminalOpen}")
        logger.info("Terminal Ready: ${status.terminalReady}")
        logger.info("Connection State: ${status.connectionState}")

        assertTrue(status.terminalOpen, "Terminal should be open")
        assertTrue(status.terminalReady, "Terminal should be ready")
        assertEquals("CONNECTED", status.connectionState, "Terminal should be connected")
    }

    @Test
    @Order(6)
    fun `test purchase with real card - 1 NOK`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Purchase Transaction (1 NOK)")
        logger.info("=" * 80)
        logger.info("⚠️  PLEASE TAP YOUR CARD ON THE TERMINAL WITHIN 60 SECONDS")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        // Attempt purchase of 1 NOK (100 øre)
        val request = TerminalPurchaseRequest(
            amountMinor = 100,  // 1 NOK in øre
            operatorId = "TEST",
            currency = "NOK"
        )

        val response = terminalClient.purchase(request)

        logger.info("\n" + "-" * 80)
        logger.info("PURCHASE RESULT:")
        logger.info("-" * 80)
        logger.info("Success: ${response.success}")
        logger.info("Call Result: ${response.callResult}")
        logger.info("Response Code: ${response.responseCode}")
        logger.info("Duration: ${response.durationMs}ms")
        logger.info("Last Display Text: ${response.lastDisplayText}")
        logger.info("Error: ${response.error}")
        if (response.printTextSanitized != null) {
            logger.info("\nReceipt:\n${response.printTextSanitized}")
        }
        logger.info("-" * 80)

        if (!response.success) {
            logger.warn("⚠️  Purchase was not successful. This could be due to:")
            logger.warn("   - Timeout (no card tapped within 60 seconds)")
            logger.warn("   - Card declined")
            logger.warn("   - Terminal communication error")
            logger.warn("   Error: ${response.error}")
        } else {
            assertEquals(1, response.callResult, "Call result should be 1 for approved")
            assertNotNull(response.responseCode, "Response code should be present")
        }
    }

    @Test
    @Order(7)
    fun `test reversal of last transaction`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Reversal of Last Transaction")
        logger.info("=" * 80)
        logger.info("⚠️  This will attempt to reverse the previous purchase")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        // First do a purchase
        logger.info("Performing purchase to reverse...")
        val purchaseRequest = TerminalPurchaseRequest(
            amountMinor = 100,  // 1 NOK
            operatorId = "TEST"
        )
        val purchaseResponse = terminalClient.purchase(purchaseRequest)

        if (purchaseResponse.success) {
            logger.info("Purchase succeeded, now attempting reversal...")

            val reversalResponse = terminalClient.reversal()

            logger.info("\n" + "-" * 80)
            logger.info("REVERSAL RESULT:")
            logger.info("-" * 80)
            logger.info("Success: ${reversalResponse.success}")
            logger.info("Call Result: ${reversalResponse.callResult}")
            logger.info("Response Code: ${reversalResponse.responseCode}")
            logger.info("Error: ${reversalResponse.error}")
            logger.info("-" * 80)

            // Reversal might fail if purchase failed or was already reversed
            if (reversalResponse.success) {
                assertEquals(1, reversalResponse.callResult, "Reversal call result should be 1 for approved")
            } else {
                logger.warn("Reversal failed: ${reversalResponse.error}")
            }
        } else {
            logger.warn("Skipping reversal test - purchase was not successful: ${purchaseResponse.error}")
        }
    }

    @Test
    @Order(8)
    fun `test purchase timeout scenario`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Purchase Timeout (DO NOT TAP CARD)")
        logger.info("=" * 80)
        logger.info("⚠️  DO NOT TAP YOUR CARD - Testing timeout scenario")
        logger.info("   Timeout set to 5 seconds for this test")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        // Set a shorter timeout for this test
        config.timeouts.purchaseTimeoutMs = 5_000L

        val request = TerminalPurchaseRequest(
            amountMinor = 100,
            operatorId = "TEST"
        )

        val response = terminalClient.purchase(request)

        logger.info("\n" + "-" * 80)
        logger.info("TIMEOUT TEST RESULT:")
        logger.info("-" * 80)
        logger.info("Success: ${response.success}")
        logger.info("Error: ${response.error}")
        logger.info("-" * 80)

        // Should timeout
        assertEquals(false, response.success, "Purchase should fail due to timeout")
        assertNotNull(response.error, "Error message should be present")

        // Reset timeout to normal
        config.timeouts.purchaseTimeoutMs = 60_000L
    }

    @Test
    @Order(9)
    fun `test closeTerminal`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Close Terminal")
        logger.info("=" * 80)

        // First open the terminal
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Terminal should open: ${openResponse.error}")

        val response = terminalClient.closeTerminal()

        logger.info("Success: ${response.success}")
        logger.info("Message: ${response.message}")
        logger.info("Error: ${response.error}")

        assertTrue(response.success, "Terminal should close successfully: ${response.error}")
        assertNotNull(response.message, "Success message should be present")
    }

    @Test
    @Order(10)
    fun `test workflow - open and purchase only`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Workflow - Open and Purchase")
        logger.info("=" * 80)
        logger.info("⚠️  PLEASE TAP YOUR CARD WHEN PROMPTED")
        logger.info("=" * 80)

        // Step 1: Open
        logger.info("\n[STEP 1/2] Opening terminal...")
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Step 1 failed - Terminal open: ${openResponse.error}")
        logger.info("✓ Terminal opened successfully")

        // Step 2: Purchase
        logger.info("\n[STEP 2/2] Performing purchase (1 NOK)...")
        logger.info("⚠️  PLEASE TAP YOUR CARD NOW")
        val purchaseRequest = TerminalPurchaseRequest(
            amountMinor = 100,  // 1 NOK in øre
            operatorId = "TEST"
        )
        val purchaseResponse = terminalClient.purchase(purchaseRequest)

        if (purchaseResponse.success) {
            logger.info("✓ Purchase completed successfully")
            logger.info("  Amount: ${purchaseRequest.amountMinor / 100.0} NOK")
            logger.info("  Result: ${purchaseResponse.callResult}")
            logger.info("  Duration: ${purchaseResponse.durationMs}ms")
        } else {
            logger.warn("✗ Purchase failed or timed out: ${purchaseResponse.error}")
        }

        logger.info("\n" + "=" * 80)
        logger.info("WORKFLOW COMPLETED (Terminal remains open)")
        logger.info("=" * 80)
    }

    @Test
    @Order(11)
    fun `test full workflow - open, purchase, reversal, close`() = runTest {
        logger.info("\n" + "=" * 80)
        logger.info("TEST: Full Workflow - Open, Purchase, Reversal, Close")
        logger.info("=" * 80)
        logger.info("⚠️  PLEASE TAP YOUR CARD WHEN PROMPTED")
        logger.info("=" * 80)

        // Step 1: Open
        logger.info("\n[STEP 1/4] Opening terminal...")
        val openResponse = terminalClient.openTerminal()
        assertTrue(openResponse.success, "Step 1 failed - Terminal open: ${openResponse.error}")
        logger.info("✓ Terminal opened successfully")

        // Step 2: Purchase
        logger.info("\n[STEP 2/4] Performing purchase (50 NOK)...")
        logger.info("⚠️  PLEASE TAP YOUR CARD NOW")
        val purchaseRequest = TerminalPurchaseRequest(
            amountMinor = 5_000,  // 50 NOK in øre
            operatorId = "TEST"
        )
        val purchaseResponse = terminalClient.purchase(purchaseRequest)

        if (purchaseResponse.success) {
            logger.info("✓ Purchase completed successfully")
            logger.info("  Amount: ${purchaseRequest.amountMinor / 100.0} NOK")
            logger.info("  Result: ${purchaseResponse.callResult}")
            logger.info("  Duration: ${purchaseResponse.durationMs}ms")

            // Step 3: Reversal
            logger.info("\n[STEP 3/4] Reversing the purchase...")
            val reversalResponse = terminalClient.reversal()

            if (reversalResponse.success) {
                logger.info("✓ Reversal completed successfully")
                logger.info("  Result: ${reversalResponse.callResult}")
                logger.info("  Response Code: ${reversalResponse.responseCode}")
            } else {
                logger.warn("✗ Reversal failed: ${reversalResponse.error}")
            }
        } else {
            logger.warn("✗ Purchase failed or timed out: ${purchaseResponse.error}")
            logger.warn("  Skipping reversal step")
        }

        // Step 4: Close
        logger.info("\n[STEP 4/4] Closing terminal...")
        val closeResponse = terminalClient.closeTerminal()
        assertTrue(closeResponse.success, "Step 4 failed - Terminal close: ${closeResponse.error}")
        logger.info("✓ Terminal closed successfully")

        logger.info("\n" + "=" * 80)
        logger.info("FULL WORKFLOW COMPLETED")
        logger.info("=" * 80)
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)
