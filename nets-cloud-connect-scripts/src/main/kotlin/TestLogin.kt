import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/**
 * STEP 1: Test HTTP Login til Nets Cloud Connect
 *
 * Credentials fra Nets (e-post fra Jannick):
 *   Username: cranberries_shared
 *   Password: Gf&DW*8-IN7Lx6pE
 */

fun main() = runBlocking {
    println("━".repeat(70))
    println("🔐 STEP 1: Test Login til Nets Cloud Connect")
    println("━".repeat(70))
    println()

    val baseUrl = System.getenv("NETS_CLOUD_URL") ?: "https://connectcloud.aws.nets.eu"
    val username = System.getenv("NETS_USERNAME") ?: "cranberries_shared"
    val password = System.getenv("NETS_PASSWORD") ?: "Gf&DW*8-IN7Lx6pE"

    println("Base URL:  $baseUrl")
    println("Username:  $username")
    println("Password:  ${password.take(3)}***${password.takeLast(3)}")
    println()

    val client = HttpClient(CIO) {
        expectSuccess = false
    }

    try {
        println("🔌 Kobler til Nets Cloud Connect...")
        println("   POST $baseUrl/v1/login")
        println()

        val response: HttpResponse = client.post("$baseUrl/v1/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }

        val statusCode = response.status.value
        val body = response.bodyAsText()

        println("📊 Respons:")
        println("   Status: $statusCode ${response.status.description}")
        println("   Body: $body")
        println()

        when (statusCode) {
            200 -> {
                println("✅ SUCCESS!")
                println("   Du har fått JWT token!")
                println()

                if (body.contains("token")) {
                    val tokenMatch = Regex(""""token"\s*:\s*"([^"]+)"""").find(body)
                    if (tokenMatch != null) {
                        val token = tokenMatch.groupValues[1]
                        println("🎫 Token (første 50 tegn):")
                        println("   ${token.take(50)}...")
                        println()
                        println("✅ Klar for neste steg: WebSocket-tilkobling!")
                    }
                } else {
                    println("⚠️  Fant ikke 'token' i responsen")
                }
            }
            401 -> {
                println("❌ UNAUTHORIZED")
                println("   Feil brukernavn eller passord!")
            }
            403 -> {
                println("❌ FORBIDDEN")
                println("   Du har ikke tilgang til denne terminalen")
            }
            404 -> {
                println("❌ NOT FOUND")
                println("   Login-endepunktet finnes ikke")
            }
            else -> {
                println("⚠️  Uventet statuskode: $statusCode")
            }
        }

    } catch (e: Exception) {
        println("💥 EXCEPTION:")
        println("   ${e.message}")
        println()
        e.printStackTrace()
    } finally {
        client.close()
    }

    println()
    println("━".repeat(70))
}
