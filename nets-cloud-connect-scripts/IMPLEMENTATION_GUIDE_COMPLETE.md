# Nets Cloud Connect - Complete Implementation Guide

**Dato:** 2026-02-15  
**Forfatter:** WARP  
**Status:** Komplett guide for begge løsninger

---

## 📋 Innhold

1. [Oversikt](#oversikt)
2. [Løsning A: Kotlin-bibliotek (Anbefalt)](#løsning-a-kotlin-bibliotek-anbefalt)
3. [Løsning B: C# Mono Server](#løsning-b-c-mono-server)
4. [Sammenligning](#sammenligning)
5. [Valg av løsning](#valg-av-løsning)

---

## Oversikt

Du har to mulige implementasjoner for Nets Cloud Connect integrasjon:

| Løsning | Type | Plattform | Integrasjon | Kompleksitet |
|---------|------|-----------|-------------|--------------|
| **A** | Kotlin-bibliotek | JVM | Direkte i lpg-ehl-service | Lav |
| **B** | C# Mono Server | .NET/Mono | REST API via HTTP | Middels |

**Begge løsninger:**
- ✅ Bruker samme Nets Cloud Connect API (HTTP + WebSocket)
- ✅ Implementerer samme OpenAPI-spesifikasjon
- ✅ Støtter Open, Purchase, Reversal, Admin-kommandoer
- ✅ Har allerede testscripts som fungerer mot production

---

# Løsning A: Kotlin-bibliotek (Anbefalt)

## 🎯 Oversikt

Implementer Nets Cloud Connect som et **Kotlin-bibliotek** integrert direkte i **lpg-ehl-service**.

### Arkitektur

```
┌──────────────────────────────────────────────────────────────────┐
│  lpg-ehl-service (Kotlin + Spring Boot)                          │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ TerminalClient (interface)                                  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                      ▲                                            │
│                      │ implements                                 │
│  ┌───────────────────┴──────────────────────────────────────┐   │
│  │ NetsCloudConnectTerminalClient                            │   │
│  │                                                            │   │
│  │  Uses:                                                     │   │
│  │  - NetsCloudAuthClient (HTTP login → JWT)                 │   │
│  │  - NetsCloudWebSocketClient (WSS with bearer token)       │   │
│  │  - NetsMessageBuilder (XML/JSON message creation)         │   │
│  │  - NetsResponseParser (parse Dfs13* responses)            │   │
│  └────────────────────────────────────────────────────────────┘  │
│                      │                                            │
│                      │ Ktor HTTP + WebSockets                     │
│                      ▼                                            │
│              [Nets Cloud Connect]                                 │
│         https://connectcloud.aws.nets.eu                          │
│         wss://connectcloud.aws.nets.eu/ws/json                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📦 Steg 1: Opprett ny modul

### 1.1 Modulstruktur

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl

# Opprett ny modul
mkdir -p lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud
mkdir -p lpg-nets-cloud-connect/src/main/resources
mkdir -p lpg-nets-cloud-connect/src/test/kotlin/no/cloudberries/lpg/netscloud
```

### 1.2 Lag pom.xml

```bash
cat > lpg-nets-cloud-connect/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>no.cloudberries.lpg</groupId>
        <artifactId>lpg-ehl</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>lpg-nets-cloud-connect</artifactId>
    <name>LPG Nets Cloud Connect</name>
    <description>Nets Cloud Connect terminal client implementation</description>

    <dependencies>
        <!-- Core module (for TerminalClient interface) -->
        <dependency>
            <groupId>no.cloudberries.lpg</groupId>
            <artifactId>lpg-ehl-service</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- Ktor for HTTP + WebSockets -->
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-core-jvm</artifactId>
            <version>2.3.7</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-cio-jvm</artifactId>
            <version>2.3.7</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-websockets-jvm</artifactId>
            <version>2.3.7</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-content-negotiation-jvm</artifactId>
            <version>2.3.7</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-serialization-kotlinx-json-jvm</artifactId>
            <version>2.3.7</version>
        </dependency>

        <!-- Kotlin Coroutines -->
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-core</artifactId>
            <version>1.7.3</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-reactor</artifactId>
            <version>1.7.3</version>
        </dependency>

        <!-- Spring Boot (for configuration) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-test</artifactId>
            <version>1.7.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF
```

### 1.3 Oppdater parent pom.xml

```bash
# Legg til modul i lpg-ehl/pom.xml
# Under <modules> section, legg til:
#     <module>lpg-nets-cloud-connect</module>
```

---

## 🔧 Steg 2: Konfigurasjon

### 2.1 Lag NetsCloudConnectConfig.kt

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsCloudConnectConfig.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "nets-cloud-connect")
data class NetsCloudConnectConfig(
    var baseUrl: String = "https://connectcloud.aws.nets.eu",
    var username: String = "",
    var password: String = "",
    var terminalId: String = "",
    var websocket: WebSocketConfig = WebSocketConfig(),
    var timeouts: TimeoutConfig = TimeoutConfig()
)

data class WebSocketConfig(
    var pingIntervalMs: Long = 20000,
    var reconnectDelayMs: Long = 5000,
    var maxReconnectAttempts: Int = 10
)

data class TimeoutConfig(
    var loginTimeoutMs: Long = 10000,
    var openTerminalTimeoutMs: Long = 30000,
    var purchaseTimeoutMs: Long = 120000,
    var reversalTimeoutMs: Long = 60000
)
EOF
```

### 2.2 Lag application.yaml eksempel

```bash
cat > lpg-nets-cloud-connect/src/main/resources/application-nets-cloud.yaml << 'EOF'
terminal:
  provider: nets-cloud-connect

nets-cloud-connect:
  base-url: https://connectcloud.aws.nets.eu
  username: ${NETS_USERNAME:cloudberries_shared}
  password: ${NETS_PASSWORD}
  terminal-id: ${NETS_TERMINAL_ID}
  
  websocket:
    ping-interval-ms: 20000
    reconnect-delay-ms: 5000
    max-reconnect-attempts: 10
    
  timeouts:
    login-timeout-ms: 10000
    open-terminal-timeout-ms: 30000
    purchase-timeout-ms: 120000
    reversal-timeout-ms: 60000
EOF
```

---

## 🧩 Steg 3: Implementer komponenter

### 3.1 NetsCloudAuthClient.kt

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsCloudAuthClient.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NetsCloudAuthClient(
    private val config: NetsCloudConnectConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    suspend fun login(): NetsLoginResponse {
        logger.info("Logging in to Nets Cloud Connect...")
        
        val response = httpClient.post("${config.baseUrl}/v1/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "username" to config.username,
                "password" to config.password
            ))
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw NetsCloudAuthException("Login failed: ${response.status}")
        }
        
        val loginResponse = response.body<NetsLoginResponse>()
        logger.info("Login successful. Token: ${loginResponse.token.take(20)}...")
        
        return loginResponse
    }
    
    fun close() {
        httpClient.close()
    }
}

@Serializable
data class NetsLoginResponse(
    val token: String,
    val username: String,
    val terminals: List<String>
)

class NetsCloudAuthException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
EOF
```

### 3.2 NetsCloudWebSocketClient.kt

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsCloudWebSocketClient.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

class NetsCloudWebSocketClient(
    private val baseUrl: String,
    private val token: String,
    private val config: NetsCloudConnectConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val httpClient = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = config.websocket.pingIntervalMs
        }
    }
    
    private var session: DefaultClientWebSocketSession? = null
    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    private var isConnected = false
    
    suspend fun connect() {
        if (isConnected) {
            logger.debug("Already connected to WebSocket")
            return
        }
        
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        logger.info("Connecting to WebSocket: $wsUrl")
        
        try {
            httpClient.webSocket(
                request = {
                    url.takeFrom(wsUrl)
                    headers.append(HttpHeaders.Authorization, "bearer $token")
                }
            ) {
                session = this
                isConnected = true
                logger.info("✅ WebSocket connected!")
                
                // Start listening for incoming messages
                launch { listenForMessages() }
                
                // Keep session alive
                while (isActive && isConnected) {
                    delay(1000)
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket connection failed", e)
            isConnected = false
            throw NetsCloudWebSocketException("Failed to connect", e)
        }
    }
    
    private suspend fun listenForMessages() {
        try {
            for (frame in session!!.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val message = frame.readText()
                        logger.debug("📨 Received TEXT: ${message.take(100)}...")
                        messageChannel.send(message)
                    }
                    is Frame.Binary -> {
                        val message = frame.readBytes().decodeToString()
                        logger.debug("📨 Received BINARY: ${message.take(100)}...")
                        messageChannel.send(message)
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket closed: ${frame.readReason()}")
                        isConnected = false
                        break
                    }
                    else -> { /* Ignore Ping/Pong */ }
                }
            }
        } catch (e: Exception) {
            logger.error("Error listening for messages", e)
            isConnected = false
        }
    }
    
    suspend fun sendMessage(json: String) {
        if (!isConnected || session == null) {
            throw IllegalStateException("WebSocket not connected")
        }
        
        logger.debug("📤 Sending: ${json.take(100)}...")
        session!!.send(Frame.Text(json))
    }
    
    suspend fun receiveMessage(timeoutMs: Long = 30000): String? {
        return withTimeoutOrNull(timeoutMs) {
            messageChannel.receive()
        }
    }
    
    fun isConnected(): Boolean = isConnected
    
    suspend fun close() {
        isConnected = false
        session?.close()
        httpClient.close()
        messageChannel.close()
    }
}

class NetsCloudWebSocketException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
EOF
```

### 3.3 NetsMessageBuilder.kt

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsMessageBuilder.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class NetsMessageBuilder(
    private val config: NetsCloudConnectConfig
) {
    
    fun buildOpenRequest(): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "${config.terminalId}",
                "VersionNumber": "1"
              }
            },
            "Open": {}
          }
        }
        """.trimIndent()
    }
    
    fun buildPurchaseRequest(
        amountMinor: Int,
        operatorId: String = "0000"
    ): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "${config.terminalId}",
                "VersionNumber": "1"
              }
            },
            "Dfs13TransferAmount": {
              "TransactionType": "48",
              "OperId": "$operatorId",
              "Amount1": "$amountMinor",
              "Amount2": "0",
              "Amount3": "0",
              "Type2": "48",
              "Type3": "48",
              "HostData": "",
              "OptionalData": ""
            }
          }
        }
        """.trimIndent()
    }
    
    fun buildReversalRequest(): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "${config.terminalId}",
                "VersionNumber": "1"
              }
            },
            "Dfs13Reversal": {}
          }
        }
        """.trimIndent()
    }
    
    fun buildCloseRequest(): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "${config.terminalId}",
                "VersionNumber": "1"
              }
            },
            "Close": {}
          }
        }
        """.trimIndent()
    }
    
    private fun generateEcrId(): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "POS-$timestamp-$suffix"
    }
}
EOF
```

### 3.4 NetsResponseParser.kt

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsResponseParser.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NetsResponseParser {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun isTerminalReady(message: String): Boolean =
        message.contains("Dfs13TerminalReady") || message.contains("ALREADY_OPEN")
    
    fun isTransactionComplete(message: String): Boolean =
        message.contains("Dfs13LocalMode")
    
    fun isDisplayText(message: String): Boolean =
        message.contains("Dfs13DisplayText")
    
    fun isPrintText(message: String): Boolean =
        message.contains("Dfs13PrintText")
    
    fun isError(message: String): Boolean =
        message.contains("Dfs13Error") || message.contains("Error")
    
    fun parseLocalMode(message: String): Dfs13LocalMode? {
        try {
            val resultMatch = Regex(""""Result"\s*:\s*"(\d+)"""").find(message)
            val amountMatch = Regex(""""TotalAmount"\s*:\s*"(\d+)"""").find(message)
            val responseCodeMatch = Regex(""""ResponseCode"\s*:\s*"(\w+)"""").find(message)
            
            return if (resultMatch != null) {
                Dfs13LocalMode(
                    result = resultMatch.groupValues[1].toInt(),
                    totalAmount = amountMatch?.groupValues?.get(1)?.toLongOrNull(),
                    responseCode = responseCodeMatch?.groupValues?.get(1)
                )
            } else null
        } catch (e: Exception) {
            logger.error("Failed to parse LocalMode", e)
            return null
        }
    }
    
    fun parseDisplayText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
            ?.replace("\\r", "")
            ?.replace("\\n", "")
    }
    
    fun parsePrintText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
    }
    
    fun parseError(message: String): String? {
        val match = Regex(""""ErrorCode"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
    }
}

data class Dfs13LocalMode(
    val result: Int,         // 1 = Godkjent, 2 = Avvist
    val totalAmount: Long?,
    val responseCode: String?
)
EOF
```

### 3.5 NetsCloudConnectTerminalClient.kt (hovedklassen)

```bash
cat > lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/NetsCloudConnectTerminalClient.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.cloudberries.lpg.service.terminal.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-connect")
class NetsCloudConnectTerminalClient(
    private val config: NetsCloudConnectConfig,
    private val authClient: NetsCloudAuthClient,
    private val messageBuilder: NetsMessageBuilder,
    private val responseParser: NetsResponseParser
) : TerminalClient {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val operationLock = Mutex()
    
    private var webSocketClient: NetsCloudWebSocketClient? = null
    private var isTerminalOpen = false
    
    override fun openTerminal(): TerminalSimpleResponse = runBlocking {
        operationLock.withLock {
            try {
                logger.info("Opening Nets Cloud Connect terminal...")
                
                // 1. Login and get JWT token
                val loginResponse = authClient.login()
                
                // 2. Connect WebSocket
                webSocketClient = NetsCloudWebSocketClient(
                    config.baseUrl,
                    loginResponse.token,
                    config
                )
                webSocketClient!!.connect()
                
                // 3. Send Open command
                val openRequest = messageBuilder.buildOpenRequest()
                webSocketClient!!.sendMessage(openRequest)
                
                // 4. Wait for TerminalReady
                var attempts = 0
                while (attempts < 5) {
                    val message = webSocketClient!!.receiveMessage(
                        config.timeouts.openTerminalTimeoutMs
                    )
                    
                    if (message == null) {
                        return@runBlocking TerminalSimpleResponse(
                            success = false,
                            error = "Timeout waiting for TerminalReady"
                        )
                    }
                    
                    if (responseParser.isTerminalReady(message)) {
                        isTerminalOpen = true
                        logger.info("✅ Terminal ready!")
                        return@runBlocking TerminalSimpleResponse(
                            success = true,
                            message = "Terminal opened successfully"
                        )
                    }
                    
                    if (responseParser.isError(message)) {
                        val error = responseParser.parseError(message)
                        return@runBlocking TerminalSimpleResponse(
                            success = false,
                            error = "Terminal error: $error"
                        )
                    }
                    
                    attempts++
                }
                
                TerminalSimpleResponse(
                    success = false,
                    error = "Failed to open terminal after $attempts attempts"
                )
                
            } catch (e: Exception) {
                logger.error("Failed to open terminal", e)
                TerminalSimpleResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse = runBlocking {
        operationLock.withLock {
            if (!isTerminalOpen) {
                return@runBlocking TerminalOperationResponse(
                    success = false,
                    error = "Terminal not open"
                )
            }
            
            try {
                logger.info("Starting purchase: ${request.amountMinor} øre")
                
                // 1. Send Purchase command
                val purchaseRequest = messageBuilder.buildPurchaseRequest(
                    request.amountMinor,
                    request.operatorId
                )
                webSocketClient!!.sendMessage(purchaseRequest)
                
                // 2. Accumulate responses
                val displayTexts = mutableListOf<String>()
                val printTexts = mutableListOf<String>()
                
                // 3. Wait for transaction complete
                val startTime = System.currentTimeMillis()
                val timeout = config.timeouts.purchaseTimeoutMs
                
                while ((System.currentTimeMillis() - startTime) < timeout) {
                    val message = webSocketClient!!.receiveMessage(timeout)
                    
                    if (message == null) {
                        return@runBlocking TerminalOperationResponse(
                            success = false,
                            error = "Timeout waiting for transaction result"
                        )
                    }
                    
                    // Accumulate display/print text
                    if (responseParser.isDisplayText(message)) {
                        responseParser.parseDisplayText(message)?.let { displayTexts.add(it) }
                    }
                    if (responseParser.isPrintText(message)) {
                        responseParser.parsePrintText(message)?.let { printTexts.add(it) }
                    }
                    
                    // Check for completion
                    if (responseParser.isTransactionComplete(message)) {
                        val localMode = responseParser.parseLocalMode(message)
                        
                        return@runBlocking TerminalOperationResponse(
                            success = localMode?.result == 1,
                            callResult = localMode?.result,
                            responseCode = localMode?.responseCode,
                            printTextSanitized = printTexts.joinToString("\n"),
                            lastDisplayText = displayTexts.lastOrNull(),
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    }
                    
                    // Check for error
                    if (responseParser.isError(message)) {
                        val error = responseParser.parseError(message)
                        return@runBlocking TerminalOperationResponse(
                            success = false,
                            error = "Terminal error: $error"
                        )
                    }
                }
                
                TerminalOperationResponse(
                    success = false,
                    error = "Transaction timeout"
                )
                
            } catch (e: Exception) {
                logger.error("Purchase failed", e)
                TerminalOperationResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse = runBlocking {
        operationLock.withLock {
            try {
                logger.info("Starting reversal...")
                
                val reversalRequest = messageBuilder.buildReversalRequest()
                webSocketClient!!.sendMessage(reversalRequest)
                
                // Wait for response
                val message = webSocketClient!!.receiveMessage(config.timeouts.reversalTimeoutMs)
                
                if (message == null) {
                    return@runBlocking TerminalOperationResponse(
                        success = false,
                        error = "Timeout waiting for reversal result"
                    )
                }
                
                if (responseParser.isTransactionComplete(message)) {
                    val localMode = responseParser.parseLocalMode(message)
                    return@runBlocking TerminalOperationResponse(
                        success = localMode?.result == 1,
                        callResult = localMode?.result,
                        responseCode = localMode?.responseCode
                    )
                }
                
                TerminalOperationResponse(
                    success = false,
                    error = "Reversal failed"
                )
                
            } catch (e: Exception) {
                logger.error("Reversal failed", e)
                TerminalOperationResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun closeTerminal(): TerminalSimpleResponse = runBlocking {
        try {
            logger.info("Closing terminal...")
            
            webSocketClient?.close()
            webSocketClient = null
            isTerminalOpen = false
            
            TerminalSimpleResponse(
                success = true,
                message = "Terminal closed"
            )
            
        } catch (e: Exception) {
            logger.error("Failed to close terminal", e)
            TerminalSimpleResponse(
                success = false,
                error = "Exception: ${e.message}"
            )
        }
    }
    
    override fun getHealth(): TerminalHealthResponse {
        return TerminalHealthResponse(
            status = if (isTerminalOpen) "healthy" else "unhealthy",
            configLoaded = true
        )
    }
    
    override fun getStatus(): TerminalStatusResponse {
        return TerminalStatusResponse(
            terminalOpen = isTerminalOpen,
            terminalReady = isTerminalOpen,
            connectionState = if (webSocketClient?.isConnected() == true) "CONNECTED" else "DISCONNECTED"
        )
    }
}
EOF
```

---

## 🧪 Steg 4: Testing

### 4.1 Unit test for Auth

```bash
cat > lpg-nets-cloud-connect/src/test/kotlin/no/cloudberries/lpg/netscloud/NetsCloudAuthClientTest.kt << 'EOF'
package no.cloudberries.lpg.netscloud

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "NETS_PASSWORD", matches = ".+")
class NetsCloudAuthClientTest {
    
    @Test
    fun `should login and get JWT token`() = runBlocking {
        val config = NetsCloudConnectConfig(
            baseUrl = "https://connectcloud.aws.nets.eu",
            username = "cloudberries_shared",
            password = System.getenv("NETS_PASSWORD")
        )
        
        val authClient = NetsCloudAuthClient(config)
        
        val response = authClient.login()
        
        assertNotNull(response.token)
        assertTrue(response.token.isNotEmpty())
        assertEquals("cloudberries_shared", response.username)
        assertTrue(response.terminals.isNotEmpty())
        
        authClient.close()
    }
}
EOF
```

---

## 🔌 Steg 5: Integrasjon med lpg-ehl-service

### 5.1 Legg til dependency

```xml
<!-- I lpg-ehl-service/pom.xml -->
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-nets-cloud-connect</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 5.2 Konfigurer miljøvariabler

```bash
export NETS_USERNAME=cloudberries_shared
export NETS_PASSWORD="B8PnVjmVq-SMM9QD"
export NETS_TERMINAL_ID="42696609"
```

### 5.3 Kjør med Nets Cloud Connect

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service
mvn spring-boot:run -Dspring-boot.run.profiles=nets-cloud
```

---

## ✅ Fordeler med Løsning A

✅ **Ingen separat server** - Alt kjører i lpg-ehl-service  
✅ **Type-safe Kotlin** - Full compile-time safety  
✅ **Spring Boot native** - Ingen ekstra runtime  
✅ **Testbart** - Kan mockes enkelt  
✅ **Samme API** - Gjenbruker `TerminalClient`-interface  
✅ **Lavere kompleksitet** - Færre moving parts  

---

# Løsning B: C# Mono Server

## 🎯 Oversikt

Implementer en **separat .NET/Mono server** som eksponerer REST API og kommuniserer med Nets Cloud Connect.

### Arkitektur

```
┌─────────────────────────────────────────────────────────┐
│  lpg-ehl-service (Kotlin)                               │
│                                                          │
│  ┌────────────────────────────────────┐                 │
│  │ TerminalClient (interface)         │                 │
│  └────────────────────────────────────┘                 │
│                    ▲                                     │
│                    │ implements                          │
│  ┌─────────────────┴──────────────────┐                 │
│  │ NetsCloudHttpTerminalClient        │                 │
│  │ (REST client)                      │                 │
│  └────────────────────────────────────┘                 │
│                    │                                     │
│                    │ HTTP REST API                       │
│                    ▼                                     │
└──────────────────────────────────────────────────────────┘
                     │
                     │
┌────────────────────▼────────────────────────────────────┐
│  PaymentTerminalNetsCloudMonoServer (C# / .NET)        │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │ HttpServer (ASP.NET Core / HTTP.sys)            │   │
│  │                                                  │   │
│  │  Endpoints:                                      │   │
│  │  - POST /api/terminal/open                       │   │
│  │  - POST /api/terminal/purchase                   │   │
│  │  - POST /api/terminal/reversal                   │   │
│  │  - POST /api/terminal/close                      │   │
│  │  - GET  /api/terminal/health                     │   │
│  │  - GET  /api/terminal/status                     │   │
│  └──────────────────────────────────────────────────┘   │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────┐    │
│  │ TerminalService                                 │    │
│  │  - ConnectCloudAdapter                          │    │
│  │  - OperationLock (concurrency)                  │    │
│  │  - EventStore (persistence)                     │    │
│  └─────────────────────────────────────────────────┘    │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────┐    │
│  │ ConnectCloudWebSocketClient                     │    │
│  │  - ConnectCloudAuthClient (HTTP login → JWT)   │    │
│  │  - WebSocket client (WSS)                       │    │
│  └─────────────────────────────────────────────────┘    │
│                    │                                     │
│                    │ HTTPS + WSS                         │
│                    ▼                                     │
│              [Nets Cloud Connect]                       │
│         https://connectcloud.aws.nets.eu                │
│         wss://connectcloud.aws.nets.eu/ws/json          │
└──────────────────────────────────────────────────────────┘
```

---

## 📦 Steg 1: C# Mono Server Setup

### 1.1 Krav

```bash
# Installer .NET SDK (hvis ikke allerede)
# macOS:
brew install dotnet-sdk

# Verifiser
dotnet --version  # Skal være 7.0 eller nyere
```

### 1.2 Prosjektstruktur

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/PaymentTerminalNetsCloudMonoServer

# Struktur (allerede eksisterer):
# PaymentTerminalNetsCloudMonoServer/
# ├── Program.cs
# ├── Configuration/
# │   └── ServerConfig.cs
# ├── ConnectCloud/
# │   ├── ConnectCloudAuthClient.cs
# │   ├── ConnectCloudWebSocketClient.cs
# │   ├── NetsRequest.cs
# │   └── NetsResponse.cs
# ├── Services/
# │   ├── ITerminalService.cs
# │   ├── TerminalService.cs
# │   ├── ConnectCloudAdapter.cs
# │   └── OperationLock.cs
# ├── Models/
# │   ├── Requests.cs
# │   └── Responses.cs
# └── PaymentTerminalNetsCloudMonoServer.csproj
```

---

## 🔧 Steg 2: Konfigurasjon

### 2.1 appsettings.json

```bash
cat > PaymentTerminalNetsCloudMonoServer/appsettings.json << 'EOF'
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "AllowedHosts": "*",
  "Server": {
    "Port": 8090,
    "BaseUrl": "http://0.0.0.0:8090"
  },
  "NetsCloudConnect": {
    "BaseUrl": "https://connectcloud.aws.nets.eu",
    "Username": "${NETS_USERNAME}",
    "Password": "${NETS_PASSWORD}",
    "TerminalId": "${NETS_TERMINAL_ID}",
    "WebSocket": {
      "PingIntervalMs": 20000,
      "ReconnectDelayMs": 5000,
      "MaxReconnectAttempts": 10
    },
    "Timeouts": {
      "LoginTimeoutMs": 10000,
      "OpenTerminalTimeoutMs": 30000,
      "PurchaseTimeoutMs": 120000,
      "ReversalTimeoutMs": 60000
    }
  }
}
EOF
```

### 2.2 Miljøvariabler

```bash
export NETS_USERNAME=cloudberries_shared
export NETS_PASSWORD="B8PnVjmVq-SMM9QD"
export NETS_TERMINAL_ID="42696609"
```

---

## 🧩 Steg 3: OpenAPI Spesifikasjon

### 3.1 Lag OpenAPI spec

```bash
cat > PaymentTerminalNetsCloudMonoServer/openapi.yaml << 'EOF'
openapi: 3.0.3
info:
  title: Payment Terminal Nets Cloud Connect API
  version: 1.0.0
  description: REST API for Nets Cloud Connect payment terminal operations

servers:
  - url: http://localhost:8090
    description: Local development server

paths:
  /api/terminal/open:
    post:
      summary: Open payment terminal
      operationId: openTerminal
      responses:
        '200':
          description: Terminal opened successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalSimpleResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/terminal/purchase:
    post:
      summary: Perform card payment
      operationId: purchase
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TerminalPurchaseRequest'
      responses:
        '200':
          description: Purchase completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalOperationResponse'

  /api/terminal/reversal:
    post:
      summary: Reverse last transaction
      operationId: reversal
      parameters:
        - name: operationId
          in: query
          schema:
            type: string
      responses:
        '200':
          description: Reversal completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalOperationResponse'

  /api/terminal/close:
    post:
      summary: Close terminal connection
      operationId: closeTerminal
      responses:
        '200':
          description: Terminal closed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalSimpleResponse'

  /api/terminal/health:
    get:
      summary: Health check
      operationId: getHealth
      responses:
        '200':
          description: Health status
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalHealthResponse'

  /api/terminal/status:
    get:
      summary: Get terminal status
      operationId: getStatus
      responses:
        '200':
          description: Terminal status
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalStatusResponse'

components:
  schemas:
    TerminalPurchaseRequest:
      type: object
      required:
        - amountMinor
      properties:
        amountMinor:
          type: integer
          description: Amount in øre (minor units)
          example: 100
        operatorId:
          type: string
          default: "0000"
        currency:
          type: string
          default: "NOK"
        optionalData:
          type: string
        clientRequestId:
          type: string

    TerminalSimpleResponse:
      type: object
      properties:
        success:
          type: boolean
        message:
          type: string
        error:
          type: string

    TerminalOperationResponse:
      type: object
      properties:
        success:
          type: boolean
        operationId:
          type: string
        callResult:
          type: integer
        responseCode:
          type: string
        printTextSanitized:
          type: string
        lastDisplayText:
          type: string
        durationMs:
          type: integer
        error:
          type: string

    TerminalHealthResponse:
      type: object
      properties:
        status:
          type: string
        configLoaded:
          type: boolean

    TerminalStatusResponse:
      type: object
      properties:
        terminalOpen:
          type: boolean
        terminalReady:
          type: boolean
        connectionState:
          type: string
        lastError:
          type: string

    ErrorResponse:
      type: object
      properties:
        error:
          type: string
        timestamp:
          type: string
EOF
```

---

## 🚀 Steg 4: Bygg og kjør

### 4.1 Bygg prosjektet

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/PaymentTerminalNetsCloudMonoServer

# Restore og bygg
dotnet restore
dotnet build
```

### 4.2 Kjør serveren

```bash
# Kjør med miljøvariabler
NETS_USERNAME=cloudberries_shared \
NETS_PASSWORD="B8PnVjmVq-SMM9QD" \
NETS_TERMINAL_ID="42696609" \
dotnet run
```

### 4.3 Test med curl

```bash
# Health check
curl http://localhost:8090/api/terminal/health

# Open terminal
curl -X POST http://localhost:8090/api/terminal/open

# Purchase (1 krone)
curl -X POST http://localhost:8090/api/terminal/purchase \
  -H "Content-Type: application/json" \
  -d '{"amountMinor":100,"operatorId":"0001"}'

# Close terminal
curl -X POST http://localhost:8090/api/terminal/close
```

---

## 🔌 Steg 5: Kotlin HTTP Client

### 5.1 Lag NetsCloudHttpTerminalClient.kt

```bash
cat > lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/netscloud/NetsCloudHttpTerminalClient.kt << 'EOF'
package no.cloudberries.lpg.service.terminal.netscloud

import no.cloudberries.lpg.service.terminal.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import org.springframework.web.client.getForObject

@Configuration
@ConfigurationProperties(prefix = "nets-cloud-http-client")
data class NetsCloudHttpClientConfig(
    var baseUrl: String = "http://localhost:8090"
)

@Component
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-http")
class NetsCloudHttpTerminalClient(
    private val config: NetsCloudHttpClientConfig
) : TerminalClient {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()
    
    override fun openTerminal(): TerminalSimpleResponse {
        logger.info("Opening terminal via HTTP...")
        return restTemplate.postForObject(
            "${config.baseUrl}/api/terminal/open",
            null,
            TerminalSimpleResponse::class.java
        ) ?: TerminalSimpleResponse(success = false, error = "No response")
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        logger.info("Starting purchase via HTTP: ${request.amountMinor} øre")
        return restTemplate.postForObject(
            "${config.baseUrl}/api/terminal/purchase",
            request,
            TerminalOperationResponse::class.java
        ) ?: TerminalOperationResponse(success = false, error = "No response")
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse {
        logger.info("Starting reversal via HTTP...")
        val url = if (operationId != null) {
            "${config.baseUrl}/api/terminal/reversal?operationId=$operationId"
        } else {
            "${config.baseUrl}/api/terminal/reversal"
        }
        return restTemplate.postForObject(
            url,
            null,
            TerminalOperationResponse::class.java
        ) ?: TerminalOperationResponse(success = false, error = "No response")
    }
    
    override fun closeTerminal(): TerminalSimpleResponse {
        logger.info("Closing terminal via HTTP...")
        return restTemplate.postForObject(
            "${config.baseUrl}/api/terminal/close",
            null,
            TerminalSimpleResponse::class.java
        ) ?: TerminalSimpleResponse(success = false, error = "No response")
    }
    
    override fun getHealth(): TerminalHealthResponse {
        return restTemplate.getForObject(
            "${config.baseUrl}/api/terminal/health",
            TerminalHealthResponse::class.java
        ) ?: TerminalHealthResponse(status = "unhealthy", configLoaded = false)
    }
    
    override fun getStatus(): TerminalStatusResponse {
        return restTemplate.getForObject(
            "${config.baseUrl}/api/terminal/status",
            TerminalStatusResponse::class.java
        ) ?: TerminalStatusResponse(
            terminalOpen = false,
            terminalReady = false
        )
    }
}
EOF
```

### 5.2 Konfigurer i application.yaml

```yaml
terminal:
  provider: nets-cloud-http

nets-cloud-http-client:
  base-url: http://localhost:8090
```

---

## ✅ Fordeler med Løsning B

✅ **Uavhengig prosess** - Kan kjøre separat fra hovedapplikasjonen  
✅ **Enklere debugging** - Kan teste C# server uavhengig  
✅ **Språkagnostisk** - Kan brukes fra hvilken som helst klient  
✅ **OpenAPI-spec** - Klar REST API-kontrakt  
✅ **Gjenbrukbar** - Kan brukes av MinLPG og andre prosjekter  

---

# Sammenligning

| Aspekt | Løsning A (Kotlin) | Løsning B (C# Mono) |
|--------|-------------------|---------------------|
| **Kompleksitet** | Lav | Middels |
| **Deployment** | 1 prosess | 2 prosesser |
| **Språk** | Kotlin | C# + Kotlin |
| **Dependencies** | Ktor | .NET + Ktor |
| **Latency** | Lavere (in-process) | Høyere (HTTP) |
| **Testbarhet** | Enkel (mock interface) | Krever kjørende server |
| **Vedlikehold** | Enklere | Mer komplekst |
| **Gjenbrukbarhet** | Kun Kotlin/JVM | Alle språk (REST) |
| **Type safety** | Full (compile-time) | Mindre (HTTP DTO) |
| **Feilhåndtering** | Enkel | Krever HTTP error codes |
| **Monitoring** | Spring Actuator | Separat monitoring |

---

# Valg av løsning

## 🎯 Anbefaling: Løsning A (Kotlin-bibliotek)

**Hvorfor:**

1. **Enklere arkitektur** - Færre moving parts, lavere kompleksitet
2. **Bedre performance** - In-process, ingen HTTP overhead
3. **Type-safe** - Full compile-time checking
4. **Enklere deployment** - Kun én prosess å administrere
5. **Spring Boot native** - Følger eksisterende patterns
6. **Samme kodebase** - Mindre vedlikehold

**Når bruke Løsning B:**

- ✅ Du trenger språkagnostisk API
- ✅ Du vil dele terminaltjeneste mellom flere applikasjoner
- ✅ Du trenger isolasjon mellom betalingslogikk og hovedapplikasjon
- ✅ Du allerede har C# expertise i teamet

---

## 🚀 Quick Start

### Løsning A (Anbefalt)

```bash
# 1. Bygg ny modul
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn clean install -pl lpg-nets-cloud-connect

# 2. Konfigurer miljøvariabler
export NETS_USERNAME=cloudberries_shared
export NETS_PASSWORD="B8PnVjmVq-SMM9QD"
export NETS_TERMINAL_ID="42696609"

# 3. Kjør med Nets Cloud Connect
cd lpg-ehl-service
mvn spring-boot:run -Dspring-boot.run.profiles=nets-cloud
```

### Løsning B (Alternativ)

```bash
# 1. Start C# server
cd /Users/tandersen/git/NorgesGass/lpg-ehl/PaymentTerminalNetsCloudMonoServer
NETS_USERNAME=cloudberries_shared \
NETS_PASSWORD="B8PnVjmVq-SMM9QD" \
NETS_TERMINAL_ID="42696609" \
dotnet run

# 2. Kjør Kotlin-applikasjon (ny terminal)
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service
mvn spring-boot:run -Dspring-boot.run.profiles=nets-cloud-http
```

---

## 📋 Implementasjons-sjekkliste

### Løsning A: Kotlin-bibliotek

- [ ] Opprett modul: `lpg-nets-cloud-connect/`
- [ ] Implementer `NetsCloudAuthClient`
- [ ] Implementer `NetsCloudWebSocketClient`
- [ ] Implementer `NetsMessageBuilder`
- [ ] Implementer `NetsResponseParser`
- [ ] Implementer `NetsCloudConnectTerminalClient`
- [ ] Skriv unit tests
- [ ] Integrer med lpg-ehl-service
- [ ] Test mot production (1 krone)
- [ ] Dokumenter

**Estimert tid:** 3-5 dager

### Løsning B: C# Mono Server

- [ ] Fullfør C# server (allerede påbegynt)
- [ ] Implementer alle endpoints
- [ ] Lag OpenAPI spec
- [ ] Test C# server standalone
- [ ] Implementer `NetsCloudHttpTerminalClient` (Kotlin)
- [ ] Integrer med lpg-ehl-service
- [ ] Setup deployment (systemd/Docker)
- [ ] Test end-to-end
- [ ] Dokumenter

**Estimert tid:** 5-7 dager

---

## 🔗 Relaterte Dokumenter

- `nets-cloud-connect-scripts/README.md` - Test-scripts
- `nets-cloud-connect-scripts/KOTLIN_LIBRARY_IMPLEMENTATION_PLAN.md` - Detaljert plan for Løsning A
- `PaymentTerminalNetsCloudMonoServer/README.md` - C# server dokumentasjon
- `lpg-ehl-service/README_Service.md` - Service-dokumentasjon
- `lpg-ehl-core/WARP.md` - Core-dokumentasjon

---

**Sist oppdatert:** 2026-02-15  
**Neste steg:** Velg løsning og start implementering! 🚀
