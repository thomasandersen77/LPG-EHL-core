package no.cloudberries.lpg.service.terminal

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TerminalClientWireMockTest {
    private lateinit var wireMock: WireMockServer
    private lateinit var client: SimulatedTerminalClient

    @BeforeEach
    fun setUp() {
        wireMock = WireMockServer(wireMockConfig().dynamicPort())
        wireMock.start()
        configureFor("localhost", wireMock.port())
        registerDefaultStubs()
        client = SimulatedTerminalClient("http://localhost:${wireMock.port()}")
    }

    @AfterEach
    fun tearDown() {
        wireMock.stop()
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

        val requestUrls = wireMock.allServeEvents.map { it.request.url }
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

    private fun registerDefaultStubs() {
        stubFor(
            post(urlPathEqualTo("/v1/terminal/open")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"success":true,"message":"Terminal opened"}""")
            )
        )

        stubFor(
            post(urlPathEqualTo("/v1/payments/purchase")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "success": true,
                          "operationId": "op-purchase-mock-001",
                          "callResult": 1,
                          "responseCode": "00",
                          "printTextSanitized": "GODKJENT"
                        }
                        """.trimIndent()
                    )
            )
        )
    }

    private fun registerPurchaseNotReadyStub() {
        stubFor(
            post(urlPathEqualTo("/v1/payments/purchase"))
                .atPriority(1)
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "success": false,
                              "operationId": null,
                              "callResult": 0,
                              "error": "Terminal is not ready for operations",
                              "errorCode": "terminal_not_ready"
                            }
                            """.trimIndent()
                        )
                )
        )
    }
}
