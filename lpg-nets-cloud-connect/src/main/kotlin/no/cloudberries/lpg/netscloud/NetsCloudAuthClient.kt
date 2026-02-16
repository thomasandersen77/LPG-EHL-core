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
        logger.debug("   Base URL: ${config.baseUrl}")
        logger.debug("   Username: ${config.username}")
        
        val response = httpClient.post("${config.baseUrl}/v1/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "username" to config.username,
                "password" to config.password
            ))
        }
        
        if (response.status != HttpStatusCode.OK) {
            val errorBody = runCatching { response.body<String>() }.getOrNull()
            throw NetsCloudAuthException("Login failed: ${response.status} - $errorBody")
        }
        
        val loginResponse = response.body<NetsLoginResponse>()
        logger.info("✅ Login successful!")
        logger.debug("   Token: ${loginResponse.token.take(20)}...")
        logger.debug("   Terminals: ${loginResponse.terminals}")
        
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
