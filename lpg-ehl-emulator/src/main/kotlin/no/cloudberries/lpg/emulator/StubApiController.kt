package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.emulator.api.LpgApiClient
import no.cloudberries.lpg.emulator.websocket.LogWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Emulator API endpoints for frontend.
 * 
 * Transaction queries are proxied to the real lpg-ehl-api service via HTTP,
 * ensuring the emulator uses the same PostgreSQL database as production.
 * Only loaded when emulator.standalone.enabled=true.
 */
@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class StubApiController(
    private val emulatorService: EmulatorService,
    private val logWebSocketHandler: LogWebSocketHandler,
    @Value("\${lpg-api.base-url:http://localhost:8080}") private val apiBaseUrl: String
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val logger = LoggerFactory.getLogger(StubApiController::class.java)
    
    // In-memory state for prices and road tax
    private var currentPrice = BigDecimal("15.90")
    private var roadTaxEnabled = true
    private var roadTaxAmount = BigDecimal("2.58")
    
    // ==========================================================================
    // CONFIG
    // ==========================================================================
    
    @GetMapping("/config/mode")
    fun getAppMode(): ResponseEntity<Map<String, String>> {
        logger.debug("📋 Config mode request")
        return ResponseEntity.ok(mapOf(
            "mode" to "LAB",
            "environment" to "emulator"
        ))
    }
    
    // ==========================================================================
    // DISPENSER STATE (legacy format for main UI)
    // ==========================================================================
    
    @GetMapping("/dispenser/state")
    fun getDispenserState(): ResponseEntity<Map<String, Any>> {
        val status = emulatorService.getPumpStatus()
        
        // Map emulator state to legacy format
        val legacyState = when (status.state) {
            "IDLE" -> "IDLE"
            "PUMPING" -> "DELIVERING"
            "STOPPED" -> "FINISHED"
            "AUTHORIZED" -> "READY"
            "PAYMENT_PENDING" -> "FINISHED"
            else -> status.state
        }
        
        return ResponseEntity.ok(mapOf(
            "state" to legacyState,
            "amountToPay" to status.amountKr,
            "litres" to status.volumeLitres,
            "pricePerLitre" to status.pricePerLitreKr,
            "includeRoadTax" to roadTaxEnabled,
            "cardModeActive" to false,
            "dayMode" to true,
            "stationCreditActive" to false,
            "connected" to true
        ))
    }
    
    @PostMapping("/dispenser/unblock")
    fun unblockDispenser(@RequestParam(defaultValue = "CASH") paymentType: String): ResponseEntity<Map<String, Any>> {
        logger.info("🔓 Legacy unblock request: paymentType=$paymentType")
        val result = emulatorService.unblockPump()
        return result.fold(
            onSuccess = { status ->
                ResponseEntity.ok(mapOf(
                    "state" to "DELIVERING",
                    "amountToPay" to status.amountKr,
                    "litres" to status.volumeLitres,
                    "pricePerLitre" to status.pricePerLitreKr,
                    "connected" to true
                ))
            },
            onFailure = { error ->
                ResponseEntity.status(409).body(mapOf(
                    "error" to (error.message ?: "Unblock failed")
                ))
            }
        )
    }
    
    @PostMapping("/dispenser/stop")
    fun stopDispenser(): ResponseEntity<Map<String, Any>> {
        logger.info("🛑 Legacy stop request")
        val result = emulatorService.blockPump()
        return result.fold(
            onSuccess = { status ->
                ResponseEntity.ok(mapOf(
                    "state" to "FINISHED",
                    "amountToPay" to status.amountKr,
                    "litres" to status.volumeLitres,
                    "pricePerLitre" to status.pricePerLitreKr,
                    "connected" to true
                ))
            },
            onFailure = { error ->
                ResponseEntity.status(500).body(mapOf(
                    "error" to (error.message ?: "Stop failed")
                ))
            }
        )
    }
    
    @PostMapping("/dispenser/reset")
    fun resetDispenser(): ResponseEntity<Map<String, Any>> {
        logger.info("🔄 Legacy reset request")
        emulatorService.reset()
        return ResponseEntity.ok(mapOf(
            "state" to "IDLE",
            "amountToPay" to 0.0,
            "litres" to 0.0,
            "pricePerLitre" to currentPrice.toDouble(),
            "connected" to true
        ))
    }
    
    @PostMapping("/dispenser/settle")
    fun settleDispenser(@RequestParam(defaultValue = "CARD") paymentMethod: String): ResponseEntity<Map<String, Any>> {
        logger.info("💳 Legacy settle request: method=$paymentMethod")
        val transaction = emulatorService.settleAndBroadcast(paymentMethod)
        return if (transaction != null) {
            ResponseEntity.ok(mapOf(
                "status" to "settled",
                "method" to paymentMethod,
                "liters" to transaction.liters,
                "amountNok" to transaction.amountNok
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "status" to "no_pending_transaction"
            ))
        }
    }
    
    // ==========================================================================
    // DISPENSER COMMANDS (EHL protocol stubs for DiagnosticsPage)
    // ==========================================================================
    
    @PostMapping("/dispenser/product-select")
    fun productSelect(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("📦 Product select for address $address (stub)")
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "address" to address,
            "message" to "Product selected (emulator stub)"
        ))
    }
    
    @PostMapping("/dispenser/program-price")
    fun programPrice(
        @RequestParam(defaultValue = "1") address: Int,
        @RequestParam(required = false) price: Double?
    ): ResponseEntity<Map<String, Any>> {
        val priceToUse = price ?: currentPrice.toDouble()
        logger.info("💰 Program price $priceToUse for address $address (stub)")
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "address" to address,
            "price" to priceToUse,
            "message" to "Price programmed (emulator stub)"
        ))
    }
    
    @PostMapping("/dispenser/program-amount")
    fun programAmount(
        @RequestParam(defaultValue = "1") address: Int,
        @RequestParam(required = false) amount: Double?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("💵 Program amount ${amount ?: "unlimited"} for address $address (stub)")
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "address" to address,
            "amount" to (amount ?: 0.0),
            "message" to "Amount programmed (emulator stub)"
        ))
    }
    
    @GetMapping("/dispenser/volume")
    fun getVolume(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any>> {
        val status = emulatorService.getPumpStatus()
        logger.debug("💧 Get volume for address $address")
        return ResponseEntity.ok(mapOf(
            "address" to address,
            "volume" to status.volumeLitres,
            "unit" to "liters"
        ))
    }
    
    @GetMapping("/dispenser/price")
    fun getPrice(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any>> {
        val status = emulatorService.getPumpStatus()
        logger.debug("💰 Get price for address $address")
        return ResponseEntity.ok(mapOf(
            "address" to address,
            "price" to status.pricePerLitreKr,
            "currency" to "NOK",
            "unit" to "per liter"
        ))
    }
    
    @GetMapping("/dispenser/tank")
    fun getTankStatus(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any>> {
        val status = emulatorService.getPumpStatus()
        logger.debug("⛽ Get tank status for address $address")
        return ResponseEntity.ok(mapOf(
            "address" to address,
            "state" to status.state,
            "volume" to status.volumeLitres,
            "amount" to status.amountKr,
            "price" to status.pricePerLitreKr,
            "tankLevel" to 85.0,  // Simulated tank level %
            "tankCapacity" to 1000.0  // Simulated capacity in liters
        ))
    }
    
    @GetMapping("/dispenser/error")
    fun getErrorStatus(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any?>> {
        logger.debug("⚠️ Get error status for address $address")
        return ResponseEntity.ok(mapOf(
            "address" to address,
            "hasError" to false,
            "errorCode" to 0,
            "errorMessage" to null,
            "lastErrorTime" to null
        ))
    }
    
    @PostMapping("/dispenser/linetest")
    fun lineTest(@RequestParam(defaultValue = "1") address: Int): ResponseEntity<Map<String, Any>> {
        logger.info("📡 Line test for address $address")
        return ResponseEntity.ok(mapOf(
            "address" to address,
            "success" to true,
            "responseTime" to 15,  // ms
            "message" to "Line test OK (emulator)"
        ))
    }
    
    // ==========================================================================
    // PRICES (matches PriceAdminPage.tsx expectations)
    // ==========================================================================
    
    @GetMapping("/prices")
    fun getPrices(): ResponseEntity<Map<String, Any>> {
        logger.debug("💰 Get prices request")
        val priceValue = currentPrice.toDouble()
        val priceExclVat = priceValue / 1.25  // 25% VAT
        return ResponseEntity.ok(mapOf(
            "displayPrice" to priceValue,
            "displayProductName" to "LPG Propan",
            "prices" to listOf(
                mapOf(
                    "productCode" to "LPG",
                    "productName" to "LPG Propan",
                    "pricePerLiter" to priceValue,
                    "pricePerLiterExclVat" to priceExclVat,
                    "vatRate" to 0.25,
                    "currency" to "NOK",
                    "lastUpdated" to Instant.now().toString()
                )
            )
        ))
    }
    
    @PostMapping("/prices/update")
    fun updatePrices(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        // Support both pricePerLitre and pricePerLiter
        val newPrice = (body["pricePerLiter"] ?: body["pricePerLitre"])?.toString()?.toBigDecimalOrNull()
        if (newPrice != null) {
            currentPrice = newPrice
            // IMPORTANT: Update the emulator's price so it takes effect for new deliveries
            val priceCents = (newPrice.toDouble() * 100).toInt()
            emulatorService.updatePrice(priceCents)
            logger.info("💰 Price updated to $currentPrice kr/L (emulator notified)")
            
            // Broadcast price update via WebSocket for real-time sync
            logWebSocketHandler.broadcastPriceUpdate(newPrice.toDouble())
        }
        val priceValue = currentPrice.toDouble()
        val priceExclVat = priceValue / 1.25
        return ResponseEntity.ok(mapOf(
            "displayPrice" to priceValue,
            "displayProductName" to "LPG Propan",
            "prices" to listOf(
                mapOf(
                    "productCode" to "LPG",
                    "productName" to "LPG Propan",
                    "pricePerLiter" to priceValue,
                    "pricePerLiterExclVat" to priceExclVat,
                    "vatRate" to 0.25,
                    "currency" to "NOK",
                    "lastUpdated" to Instant.now().toString()
                )
            ),
            "updated" to true
        ))
    }
    
    // ==========================================================================
    // ROAD TAX (matches PriceAdminPage.tsx expectations)
    // ==========================================================================
    
    @GetMapping("/road-tax")
    fun getRoadTax(): ResponseEntity<Map<String, Any>> {
        logger.debug("🛣️ Get road tax request")
        val taxKr = roadTaxAmount.toDouble()
        val taxOre = (taxKr * 100).toInt()
        return ResponseEntity.ok(mapOf(
            "taxPerLiterKr" to taxKr,
            "taxPerLiterOre" to taxOre,
            "enabled" to roadTaxEnabled,
            "description" to "Veitrafikkavgift"
        ))
    }
    
    @PostMapping("/road-tax/update")
    fun updateRoadTax(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        // Support taxPerLiterOre from frontend
        body["taxPerLiterOre"]?.toString()?.toIntOrNull()?.let { 
            roadTaxAmount = BigDecimal(it).divide(BigDecimal(100))
        }
        body["enabled"]?.toString()?.toBooleanStrictOrNull()?.let { roadTaxEnabled = it }
        body["amount"]?.toString()?.toBigDecimalOrNull()?.let { roadTaxAmount = it }
        logger.info("🛣️ Road tax updated: $roadTaxAmount kr/L")
        val taxKr = roadTaxAmount.toDouble()
        val taxOre = (taxKr * 100).toInt()
        return ResponseEntity.ok(mapOf(
            "taxPerLiterKr" to taxKr,
            "taxPerLiterOre" to taxOre,
            "enabled" to roadTaxEnabled,
            "updated" to true
        ))
    }
    
    // ==========================================================================
    // SYNC STATUS (matches sync.ts SyncStatusResponse)
    // ==========================================================================
    
    @GetMapping("/sync/status")
    fun getSyncStatus(): ResponseEntity<Map<String, Any?>> {
        logger.debug("🔄 Sync status request")
        return ResponseEntity.ok(mapOf(
            "pendingCount" to 0,
            "syncedCount" to 0,
            "failedCount" to 0,
            "lastSyncTime" to null,
            // Extra fields for UI compatibility
            "enabled" to false,
            "status" to "DISABLED",
            "message" to "Azure-synkronisering er ikke aktivert"
        ))
    }
    
    // ==========================================================================
    // TRANSACTIONS - Proxy to real lpg-ehl-api for PostgreSQL persistence
    // ==========================================================================
    
    /**
     * Proxy transaction requests to real API.
     * This ensures emulator mode uses the same PostgreSQL database as production.
     */
    private fun proxyGetToApi(path: String): ResponseEntity<String> {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$apiBaseUrl$path"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            ResponseEntity.status(response.statusCode())
                .header("Content-Type", "application/json")
                .body(response.body())
        } catch (e: Exception) {
            logger.error("❌ Failed to proxy request to API: ${e.message}")
            ResponseEntity.status(503).body("{\"error\": \"API not available\"}")
        }
    }
    
    @GetMapping("/transactions")
    fun getTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) paymentType: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) customerId: String?
    ): ResponseEntity<String> {
        logger.debug("📋 Get transactions - proxying to API: page=$page, size=$size")
        
        // Build query string
        val params = mutableListOf("page=$page", "size=$size")
        paymentType?.let { params.add("paymentType=$it") }
        from?.let { params.add("from=$it") }
        to?.let { params.add("to=$it") }
        customerId?.let { params.add("customerId=$it") }
        
        return proxyGetToApi("/api/v1/transactions?${params.joinToString("&")}")
    }
    
    @GetMapping("/transactions/count")
    fun getTransactionCount(): ResponseEntity<String> {
        logger.debug("📋 Get transaction count - proxying to API")
        return proxyGetToApi("/api/v1/transactions/count")
    }
    
    @GetMapping("/transactions/{id}")
    fun getTransaction(@PathVariable id: String): ResponseEntity<String> {
        logger.debug("📋 Get transaction: $id - proxying to API")
        return proxyGetToApi("/api/v1/transactions/$id")
    }
    
    @PatchMapping("/transactions/{id}/payment")
    fun updateTransactionPayment(
        @PathVariable id: String,
        @RequestParam paymentMethod: String,
        @RequestParam(defaultValue = "PAID") paymentStatus: String
    ): ResponseEntity<String> {
        logger.debug("📋 Update transaction payment: $id -> $paymentMethod/$paymentStatus - proxying to API")
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$apiBaseUrl/api/v1/transactions/$id/payment?paymentMethod=$paymentMethod&paymentStatus=$paymentStatus"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            ResponseEntity.status(response.statusCode())
                .header("Content-Type", "application/json")
                .body(response.body())
        } catch (e: Exception) {
            logger.error("❌ Failed to proxy payment update to API: ${e.message}")
            ResponseEntity.status(503).body("{\"error\": \"API not available\"}")
        }
    }
    
    // ==========================================================================
    // SYNC TRIGGER (for manual Azure sync button)
    // ==========================================================================
    
    @PostMapping("/sync/trigger")
    fun triggerSync(): ResponseEntity<Map<String, String>> {
        logger.info("🔄 Manual sync triggered (stub - no Azure in emulator)")
        return ResponseEntity.ok(mapOf(
            "status" to "skipped",
            "message" to "Azure sync not enabled in emulator mode"
        ))
    }
    
    // ==========================================================================
    // CREDIT ACCOUNTS (stub - returns empty list)
    // ==========================================================================
    
    @GetMapping("/credit/accounts")
    fun getCreditAccounts(): ResponseEntity<List<Any>> {
        logger.debug("🏦 Get credit accounts (stub)")
        return ResponseEntity.ok(emptyList())
    }
    
    @GetMapping("/credit/accounts/{id}")
    fun getCreditAccount(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.debug("🏦 Get credit account: $id")
        return ResponseEntity.notFound().build()
    }
    
    @PostMapping("/credit/accounts")
    fun createCreditAccount(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        logger.info("🏦 Create credit account: $body")
        return ResponseEntity.ok(mapOf(
            "id" to java.util.UUID.randomUUID().toString(),
            "customerName" to (body["customerName"] ?: "Test Kunde"),
            "customerNumber" to (body["customerNumber"] ?: "TEST001"),
            "balanceNok" to (body["initialBalanceNok"] ?: 0.0)
        ))
    }
    
    @GetMapping("/credit/accounts/{id}/transactions")
    fun getCreditAccountTransactions(@PathVariable id: String): ResponseEntity<List<Any>> {
        logger.debug("🏦 Get credit account transactions: $id")
        return ResponseEntity.ok(emptyList())
    }
    
    // ==========================================================================
    // REPORTS (stub - returns empty/zero data)
    // ==========================================================================
    
    @GetMapping("/reports/daily")
    fun getDailyReport(@RequestParam(required = false) date: String?): ResponseEntity<List<Map<String, Any>>> {
        logger.debug("📊 Get daily report: date=$date")
        val today = java.time.LocalDate.now().toString()
        return ResponseEntity.ok(listOf(
            mapOf(
                "summaryDate" to (date ?: today),
                "dispenserAddress" to 1,
                "transactionCount" to 0,
                "totalVolumeLiters" to 0.0,
                "totalAmountKr" to 0.0,
                "averagePricePerLiter" to currentPrice.toDouble()
            )
        ))
    }
    
    @GetMapping("/reports/period")
    fun getPeriodReport(
        @RequestParam from: String,
        @RequestParam to: String,
        @RequestParam(required = false) dispenserAddress: Int?
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("📊 Get period report: from=$from, to=$to")
        return ResponseEntity.ok(mapOf(
            "fromDate" to from,
            "toDate" to to,
            "dispenserAddress" to (dispenserAddress ?: 1),
            "totalTransactions" to 0,
            "totalVolumeLiters" to 0.0,
            "totalAmountKr" to 0.0,
            "averagePricePerLiter" to currentPrice.toDouble(),
            "dailySummaries" to emptyList<Any>()
        ))
    }
    
    // NOTE: Emulator endpoints (/api/v1/emulator/*) are in EmulatorController
}
