package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class TerminalClientWireMockTest {
    private val objectMapper = ObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private lateinit var wireMockProcess: Process
    private lateinit var client: SimulatedTerminalClient
    private var wireMockPort: Int = 0

    @BeforeEach
    fun setUp() {
        wireMockPort = findFreePort()
        val wireMockJar = resolveWireMockJar()
        val rootDir = resolveWireMockRootDir()
        wireMockProcess = ProcessBuilder(
            "java",
            "-jar",
            wireMockJar.toString(),
            "--port",
            wireMockPort.toString(),
            "--root-dir",
            rootDir.toString()
        )
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        waitForWireMock()
        client = SimulatedTerminalClient("http://localhost:$wireMockPort")
    }

    @AfterEach
    fun tearDown() {
        wireMockProcess.destroy()
        wireMockProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        if (wireMockProcess.isAlive) {
            wireMockProcess.destroyForcibly()
        }
    }

    @Test
    fun openTerminalThenPurchaseUsesCorrectFlow() {
        val openResponse = client.openTerminal()
        val purchaseResponse = client.purchase(
            TerminalPurchaseRequest(
                amountMinor = 100,
                optionalData = "WireMock Test",
                clientRequestId = "wiremock-capture-001"
            )
        )

        assertThat(openResponse.success).isTrue()
        assertThat(openResponse.message).isEqualTo("Terminal opened")
        assertThat(purchaseResponse.success).isTrue()
        assertThat(purchaseResponse.operationId).isEqualTo("op-purchase-mock-001")

        val requestUrls = requestLogUrls()
        assertThat(requestUrls).contains("/v1/terminal/open", "/v1/payments/purchase")
    }

    @Test
    fun purchaseReturnsTerminalNotReadyWhenStubbed() {
        registerPurchaseNotReadyStub()

        val openResponse = client.openTerminal()
        assertThat(openResponse.success).isTrue()

        val purchaseResponse = client.purchase(
            TerminalPurchaseRequest(
                amountMinor = 250,
                optionalData = "WireMock Error Test",
                clientRequestId = "wiremock-capture-error"
            )
        )

        assertThat(purchaseResponse.success).isFalse()
        assertThat(purchaseResponse.errorCode).isEqualTo("terminal_not_ready")
    }

    private fun resolveWireMockJar(): Path {
        val jarPath = Path.of("..", "wiremock-standalone-3.3.1.jar").normalize().toAbsolutePath()
        require(Files.exists(jarPath)) { "WireMock jar not found at $jarPath" }
        return jarPath
    }

    private fun resolveWireMockRootDir(): Path {
        val rootDir = Path.of("src", "test", "resources", "wiremock").toAbsolutePath()
        require(Files.exists(rootDir)) { "WireMock root dir not found at $rootDir" }
        return rootDir
    }

    private fun waitForWireMock() {
        val deadline = System.currentTimeMillis() + 30000  // Increased to 30 seconds
        var lastException: Exception? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:$wireMockPort/__admin/mappings"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    return
                }
            } catch (e: Exception) {
                lastException = e
                Thread.sleep(500)  // Increased sleep interval
            }
        }
        // Capture WireMock output for debugging
        val output = wireMockProcess.inputStream.bufferedReader().readText()
        error("WireMock did not start within timeout. Last exception: $lastException. Output: $output")
    }

    private fun registerPurchaseNotReadyStub() {
        val body = """
            {
              "priority": 1,
              "request": {
                "method": "POST",
                "urlPath": "/v1/payments/purchase"
              },
              "response": {
                "status": 503,
                "headers": {
                  "Content-Type": "application/json"
                },
                "jsonBody": {
                  "success": false,
                  "operationId": null,
                  "callResult": 0,
                  "error": "Terminal is not ready for operations",
                  "errorCode": "terminal_not_ready"
                }
              }
            }
        """.trimIndent()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$wireMockPort/__admin/mappings"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(5))
            .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun requestLogUrls(): List<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$wireMockPort/__admin/requests"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val node = objectMapper.readTree(response.body())
        return node.path("requests")
            .mapNotNull { it.path("request").path("url").takeIf(JsonNode::isTextual)?.asText() }
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }
}
