package no.cloudberries.lpg.headless

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Prints curl commands for all API endpoints after the application starts.
 * 
 * Only active when the debug-api profile is enabled.
 * Useful for quickly testing API endpoints without needing to reference documentation.
 */
@Component
@Profile("debug-api")
class CurlCommandPrinter(
    @Value("\${server.port:8080}") private val port: Int
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val baseUrl = "http://localhost:$port"
        
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("🔧 DEBUG API ENDPOINTS - CURL COMMANDS")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
        
        // Serial Debug Endpoints
        logger.info("📡 SERIAL DEBUG ENDPOINTS:")
        logger.info("  # List all available serial ports")
        logger.info("  curl $baseUrl/api/debug/serial/ports")
        logger.info("")
        logger.info("  # Check serial communication health")
        logger.info("  curl '$baseUrl/api/debug/serial/health?address=1'")
        logger.info("")
        logger.info("  # Get serial connection status")
        logger.info("  curl $baseUrl/api/debug/serial/status")
        logger.info("")
        logger.info("  # Auto-detect parity mode")
        logger.info("  curl -X POST '$baseUrl/api/debug/serial/auto-detect?port=/dev/ttyUSB0&address=1'")
        logger.info("")
        logger.info("  # Smart scan for working configuration")
        logger.info("  curl -X POST '$baseUrl/api/debug/serial/smart-scan?timeoutMs=1000&stopOnFirst=true'")
        logger.info("")
        logger.info("  # Scan for responding addresses")
        logger.info("  curl -X POST '$baseUrl/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=64&baud=9600&parity=NONE&timeoutMs=500'")
        logger.info("")
        
        // Configuration Endpoints
        logger.info("⚙️  CONFIGURATION ENDPOINTS:")
        logger.info("  # Get application mode")
        logger.info("  curl $baseUrl/api/v1/config/mode")
        logger.info("")
        logger.info("  # Get hardware mode (LAB/FIELD)")
        logger.info("  curl $baseUrl/api/v1/config/hardware-mode")
        logger.info("")
        
        // Dispenser Endpoints
        logger.info("⛽ DISPENSER ENDPOINTS:")
        logger.info("  # List all dispensers")
        logger.info("  curl $baseUrl/api/v1/dispensers")
        logger.info("")
        logger.info("  # Get active dispensers")
        logger.info("  curl '$baseUrl/api/v1/dispensers/active?minutesSinceLastSeen=60'")
        logger.info("")
        logger.info("  # Get specific dispenser status")
        logger.info("  curl $baseUrl/api/v1/dispensers/1")
        logger.info("")
        logger.info("  # Get live dispenser status")
        logger.info("  curl $baseUrl/api/v1/dispensers/status")
        logger.info("")
        
        // Transaction Endpoints
        logger.info("💰 TRANSACTION ENDPOINTS:")
        logger.info("  # List transactions (paginated)")
        logger.info("  curl '$baseUrl/api/v1/transactions?page=0&size=50'")
        logger.info("")
        logger.info("  # Get transaction by ID")
        logger.info("  curl $baseUrl/api/v1/transactions/{id}")
        logger.info("")
        logger.info("  # Get unsynced transactions")
        logger.info("  curl '$baseUrl/api/v1/transactions/unsynced?limit=100'")
        logger.info("")
        logger.info("  # Count transactions")
        logger.info("  curl $baseUrl/api/v1/transactions/count")
        logger.info("")
        logger.info("  # Update payment status")
        logger.info("  curl -X PATCH '$baseUrl/api/v1/transactions/{id}/payment?paymentMethod=CARD&paymentStatus=PAID'")
        logger.info("")
        logger.info("  # Pay all pending transactions")
        logger.info("  curl -X POST '$baseUrl/api/v1/transactions/pay-all-pending?paymentMethod=CARD'")
        logger.info("")
        logger.info("  # Create transaction")
        logger.info("  curl -X POST $baseUrl/api/v1/transactions \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"dispenserAddress\":1,\"nozzleNumber\":1,\"volumeDeciliters\":100,\"amountOre\":1590,\"pricePerLiter\":15.90,\"paymentType\":\"CASH\",\"productCode\":\"LPG\",\"includesRoadTax\":true}'")
        logger.info("")
        
        // Price Endpoints
        logger.info("💵 PRICE ENDPOINTS:")
        logger.info("  # Get current prices")
        logger.info("  curl $baseUrl/api/v1/prices")
        logger.info("")
        logger.info("  # Update price")
        logger.info("  curl -X POST $baseUrl/api/v1/prices/update \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"pricePerLiter\":16.50}'")
        logger.info("")
        
        // Sync Endpoints
        logger.info("☁️  AZURE SYNC ENDPOINTS:")
        logger.info("  # Get sync status")
        logger.info("  curl $baseUrl/api/v1/sync/status")
        logger.info("")
        logger.info("  # Trigger manual sync")
        logger.info("  curl -X POST $baseUrl/api/v1/sync/trigger")
        logger.info("")
        logger.info("  # Retry specific sync item")
        logger.info("  curl -X POST $baseUrl/api/v1/sync/retry/{queueId}")
        logger.info("")
        logger.info("  # Get Azure queue messages")
        logger.info("  curl '$baseUrl/api/v1/sync/queue/messages?maxMessages=32'")
        logger.info("")
        logger.info("  # Get queue messages grouped by date")
        logger.info("  curl $baseUrl/api/v1/sync/queue/by-date")
        logger.info("")
        
        // Demo Dispenser Endpoints (only with local/demo profiles)
        logger.info("🧪 DEMO DISPENSER ENDPOINTS (local/demo profiles only):")
        logger.info("  # Get dispenser state")
        logger.info("  curl $baseUrl/api/v1/dispenser/state")
        logger.info("")
        logger.info("  # Start fuel delivery")
        logger.info("  curl -X POST '$baseUrl/api/v1/dispenser/unblock?paymentType=CASH'")
        logger.info("")
        logger.info("  # Stop delivery")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/stop")
        logger.info("")
        logger.info("  # Settle payment")
        logger.info("  curl -X POST '$baseUrl/api/v1/dispenser/settle?paymentMethod=CARD'")
        logger.info("")
        logger.info("  # Reset dispenser")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/reset")
        logger.info("")
        logger.info("  # Get current volume")
        logger.info("  curl '$baseUrl/api/v1/dispenser/volume?address=1'")
        logger.info("")
        logger.info("  # Get tank status")
        logger.info("  curl '$baseUrl/api/v1/dispenser/tank?address=1'")
        logger.info("")
        logger.info("  # Get current price")
        logger.info("  curl '$baseUrl/api/v1/dispenser/price?address=1'")
        logger.info("")
        logger.info("  # Communication line test")
        logger.info("  curl -X POST '$baseUrl/api/v1/dispenser/linetest?address=1'")
        logger.info("")
        logger.info("  # Get error status")
        logger.info("  curl '$baseUrl/api/v1/dispenser/error?address=1'")
        logger.info("")
        logger.info("  # Select product")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/product-select \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"address\":1,\"product\":\"0x30\"}'")
        logger.info("")
        logger.info("  # Program price")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/program-price \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"address\":1,\"priceKrPerLiter\":\"15.90\"}'")
        logger.info("")
        logger.info("  # Program amount preset")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/program-amount \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"address\":1,\"amountOre\":50000}'")
        logger.info("")
        logger.info("  # Program volume preset")
        logger.info("  curl -X POST $baseUrl/api/v1/dispenser/program-volume \\")
        logger.info("    -H 'Content-Type: application/json' \\")
        logger.info("    -d '{\"address\":1,\"volumeLiters\":25.0}'")
        logger.info("")
        
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("💡 TIP: Copy and paste these commands to test the API")
        logger.info("💡 TIP: Use jq for pretty JSON output: curl ... | jq")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
    }
}
