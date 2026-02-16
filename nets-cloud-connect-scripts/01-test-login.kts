#!/usr/bin/env kotlin

/**
 * STEP 1: Test HTTP Login til Nets Cloud Connect
 * 
 * Dette scriptet tester om vi kan logge inn og få JWT token.
 * 
 * Credentials fra Nets (e-post fra Jannick):
 *   Username: cranberries_shared
 *   Password: Gf&DW*8-IN7Lx6pE
 * 
 * Endepunkt:
 *   PROD: https://connectcloud.aws.nets.eu/v1/login
 *   (alternativt: 3.33.230.243:6001 med TLS)
 */

@file:DependsOn("io.ktor:ktor-client-core:2.3.7")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.7")
@file:DependsOn("io.ktor:ktor-client-content-negotiation:2.3.7")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.9")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

println("━".repeat(70))
println("🔐 STEP 1: Test Login til Nets Cloud Connect")
println("━".repeat(70))
println()

// Konfigurasjon fra Nets e-post
val BASE_URL = System.getenv("NETS_CLOUD_URL") ?: "https://connectcloud.aws.nets.eu"
val USERNAME = System.getenv("NETS_USERNAME") ?: "cranberries_shared"
val PASSWORD = System.getenv("NETS_PASSWORD") ?: "Gf&DW*8-IN7Lx6pE"

println("Base URL:  $BASE_URL")
println("Username:  $USERNAME")
println("Password:  ${PASSWORD.take(3)}***${PASSWORD.takeLast(3)}")
println()

val client = HttpClient(CIO) {
    expectSuccess = false
}

runBlocking {
    try {
        println("🔌 Kobler til Nets Cloud Connect...")
        println("   POST $BASE_URL/v1/login")
        println()
        
        val response: HttpResponse = client.post("$BASE_URL/v1/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$USERNAME","password":"$PASSWORD"}""")
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
                
                // Prøv å parse token (JSON)
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
                    println("   Sjekk formatet på responsen ovenfor")
                }
            }
            401 -> {
                println("❌ UNAUTHORIZED")
                println("   Feil brukernavn eller passord!")
                println()
                println("   Sjekk credentials:")
                println("   - Username: $USERNAME")
                println("   - Password: (sjekk at det er riktig)")
            }
            403 -> {
                println("❌ FORBIDDEN")
                println("   Du har ikke tilgang til denne terminalen")
            }
            404 -> {
                println("❌ NOT FOUND")
                println("   Login-endepunktet finnes ikke")
                println("   Sjekk URL: $BASE_URL/v1/login")
            }
            else -> {
                println("⚠️  Uventet statuskode: $statusCode")
                println("   Se respons ovenfor for detaljer")
            }
        }
        
    } catch (e: Exception) {
        println("💥 EXCEPTION:")
        println("   ${e.message}")
        println()
        e.printStackTrace()
        
        println()
        println("🔍 Feilsøking:")
        println("   1. Sjekk at du har internett-tilgang")
        println("   2. Test med curl:")
        println("      curl -v -X POST $BASE_URL/v1/login \\")
        println("        -H \"Content-Type: application/json\" \\")
        println("        -d '{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}'")
    } finally {
        client.close()
    }
}

println()
println("━".repeat(70))
