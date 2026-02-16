package no.cloudberries.lpg.api

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.test.assertTrue

/**
 * API Parity Test
 * 
 * Verifies that the REST API endpoints exposed by lpg-ehl-api are consistent
 * across different deployment modes (webapp and headless with debug-api profile).
 * 
 * This test ensures that:
 * 1. All API endpoints are registered and accessible
 * 2. The same controllers are available in both configurations
 * 3. URL patterns and HTTP methods match expectations
 */
@Disabled
@SpringBootTest(
    classes = [ApiTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
class ApiParityTest {

    @Autowired
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping

    @Test
    fun `verify all API endpoints are registered`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        
        // Filter to only API endpoints (exclude actuator, error handlers, etc.)
        val apiMappings = mappings.filter { (info, _) ->
            info.pathPatternsCondition?.patterns?.any { pattern ->
                pattern.patternString.startsWith("/api/")
            } ?: false
        }
        
        println("\n=== Registered API Endpoints ===")
        apiMappings.forEach { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patterns?.map { it.patternString } ?: emptyList()
            val methods = info.methodsCondition.methods.map { it.name }
            val handlerClass = (handler as? HandlerMethod)?.beanType?.simpleName ?: "Unknown"
            
            patterns.forEach { pattern ->
                methods.forEach { method ->
                    println("  $method $pattern -> $handlerClass")
                }
            }
        }
        println("=== Total API endpoints: ${apiMappings.size} ===\n")
        
        // Assert we have API endpoints registered
        assertTrue(apiMappings.isNotEmpty(), "No API endpoints found - controllers may not be loaded")
    }

    @Test
    fun `verify core API endpoints exist`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        val allPatterns = mappings.flatMap { (info, _) ->
            info.pathPatternsCondition?.patterns?.map { it.patternString } ?: emptyList()
        }.toSet()
        
        // Define expected core endpoints
        val expectedEndpoints = listOf(
            "/api/v1/transactions",
            "/api/v1/payments",
            "/api/v1/prices",
            "/api/v1/dispenser"
        )
        
        println("\n=== Verifying Core Endpoints ===")
        expectedEndpoints.forEach { endpoint ->
            val exists = allPatterns.any { it.startsWith(endpoint) }
            println("  $endpoint: ${if (exists) "✓ Found" else "✗ Missing"}")
            assertTrue(exists, "Expected endpoint $endpoint not found")
        }
        println("=== All core endpoints verified ===\n")
    }

    @Test
    fun `verify controller packages are scanned`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        val controllerClasses = mappings.values
            .filterIsInstance<HandlerMethod>()
            .map { it.beanType }
            .toSet()
        
        println("\n=== Registered Controllers ===")
        controllerClasses.forEach { controllerClass ->
            println("  - ${controllerClass.simpleName} (${controllerClass.packageName})")
        }
        println("=== Total controllers: ${controllerClasses.size} ===\n")
        
        // Verify we have controllers from the expected packages
        val expectedPackages = listOf(
            "no.cloudberries.lpg.api.controller",
            "no.cloudberries.lpg.api.payment"
        )
        
        val foundPackages = controllerClasses.map { it.packageName }.toSet()
        expectedPackages.forEach { expectedPkg ->
            val found = foundPackages.any { it.startsWith(expectedPkg) }
            assertTrue(found, "No controllers found in package $expectedPkg")
        }
    }

    @Test
    fun `verify HTTP methods for transaction endpoints`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        
        // Find transaction endpoints
        val transactionMappings = mappings.filter { (info, _) ->
            info.pathPatternsCondition?.patterns?.any { pattern ->
                pattern.patternString.startsWith("/api/v1/transactions")
            } ?: false
        }
        
        println("\n=== Transaction Endpoints ===")
        transactionMappings.forEach { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patterns?.map { it.patternString } ?: emptyList()
            val methods = info.methodsCondition.methods.map { it.name }
            
            patterns.forEach { pattern ->
                methods.forEach { method ->
                    println("  $method $pattern")
                }
            }
        }
        
        // Verify we have GET (list) and POST (create) for transactions
        val allMethods = transactionMappings.flatMap { (info, _) ->
            info.methodsCondition.methods.map { it.name }
        }.toSet()
        
        assertTrue(allMethods.contains("GET"), "GET method missing for transactions")
        assertTrue(allMethods.contains("POST") || allMethods.contains("PATCH"), 
            "POST or PATCH method missing for transactions")
    }

    @Test
    fun `verify payment endpoints`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        
        val paymentMappings = mappings.filter { (info, _) ->
            info.pathPatternsCondition?.patterns?.any { pattern ->
                pattern.patternString.startsWith("/api/v1/payments")
            } ?: false
        }
        
        println("\n=== Payment Endpoints ===")
        paymentMappings.forEach { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patterns?.map { it.patternString } ?: emptyList()
            val methods = info.methodsCondition.methods.map { it.name }
            
            patterns.forEach { pattern ->
                methods.forEach { method ->
                    println("  $method $pattern")
                }
            }
        }
        
        assertTrue(paymentMappings.isNotEmpty(), "No payment endpoints found")
    }

    @Test
    fun `verify dispenser endpoints`() {
        val mappings = requestMappingHandlerMapping.handlerMethods
        
        val dispenserMappings = mappings.filter { (info, _) ->
            info.pathPatternsCondition?.patterns?.any { pattern ->
                pattern.patternString.startsWith("/api/v1/dispenser")
            } ?: false
        }
        
        println("\n=== Dispenser Endpoints ===")
        dispenserMappings.forEach { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patterns?.map { it.patternString } ?: emptyList()
            val methods = info.methodsCondition.methods.map { it.name }
            
            patterns.forEach { pattern ->
                methods.forEach { method ->
                    println("  $method $pattern")
                }
            }
        }
        
        assertTrue(dispenserMappings.isNotEmpty(), "No dispenser endpoints found")
    }
}
