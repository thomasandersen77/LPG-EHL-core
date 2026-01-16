package no.cloudberries.lpg.api.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import no.cloudberries.lpg.service.model.DispenserStatus
import no.cloudberries.lpg.service.model.Transaction
import no.cloudberries.lpg.service.repository.DispenserStatusRepository
import no.cloudberries.lpg.service.repository.TransactionRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class ApiIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var dispenserStatusRepository: DispenserStatusRepository

    @BeforeEach
    fun setupRestAssured() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
        
        // Clean database before each test
        transactionRepository.deleteAll()
        dispenserStatusRepository.deleteAll()
    }

    @Test
    fun `should get health status without authentication`() {
        RestAssured.given()
            .`when`()
            .get("/actuator/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }

    @Test
    fun `should return 401 when accessing transactions without token`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/transactions")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should get empty transactions list with valid token`() {
        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/transactions")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("content", hasSize<Any>(0))
            .body("totalElements", equalTo(0))
    }

    @Test
    fun `should get transaction by ID`() {
        // Create test transaction
        val transaction = Transaction(
            dispenserAddress = 1,
            nozzleNumber = 1,
            volumeDeciliters = 500,
            amountOre = 7950,
            pricePerLiter = BigDecimal("15.90"),
            timestamp = LocalDateTime.now()
        )
        val saved = transactionRepository.save(transaction)

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/transactions/${saved.transactionId}")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("transactionId", equalTo(saved.transactionId.toString()))
            .body("dispenserAddress", equalTo(1))
            .body("nozzleNumber", equalTo(1))
            .body("volumeLiters", equalTo(50.0f))
            .body("amountKr", equalTo(79.50f))
    }

    @Test
    fun `should return 404 for non-existent transaction`() {
        val randomId = UUID.randomUUID()
        
        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/transactions/$randomId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should get transactions with pagination`() {
        // Create 5 test transactions
        repeat(5) { i ->
            transactionRepository.save(
                Transaction(
                    dispenserAddress = 1,
                    nozzleNumber = 1,
                    volumeDeciliters = 100 + i,
                    amountOre = 1000 + i * 10,
                    timestamp = LocalDateTime.now().minusMinutes(i.toLong())
                )
            )
        }

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .queryParam("page", 0)
            .queryParam("size", 3)
            .`when`()
            .get("/api/v1/transactions")
            .then()
            .statusCode(200)
            .body("content", hasSize<Any>(3))
            .body("totalElements", equalTo(5))
            .body("totalPages", equalTo(2))
            .body("currentPage", equalTo(0))
    }

    @Test
    fun `should filter transactions by dispenser address`() {
        // Create transactions for different dispensers
        transactionRepository.save(
            Transaction(
                dispenserAddress = 1,
                nozzleNumber = 1,
                volumeDeciliters = 100,
                amountOre = 1000,
                timestamp = LocalDateTime.now()
            )
        )
        transactionRepository.save(
            Transaction(
                dispenserAddress = 2,
                nozzleNumber = 1,
                volumeDeciliters = 200,
                amountOre = 2000,
                timestamp = LocalDateTime.now()
            )
        )

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .queryParam("dispenserAddress", 1)
            .`when`()
            .get("/api/v1/transactions")
            .then()
            .statusCode(200)
            .body("content", hasSize<Any>(1))
            .body("content[0].dispenserAddress", equalTo(1))
    }

    @Test
    fun `should get all dispensers`() {
        // Create test dispenser status
        dispenserStatusRepository.save(
            DispenserStatus(
                address = 1,
                state = "IDLE",
                lastActive = LocalDateTime.now()
            )
        )

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/dispensers")
            .then()
            .statusCode(200)
            .body("$", hasSize<Any>(1))
            .body("[0].dispenserAddress", equalTo(1))
            .body("[0].state", equalTo("IDLE"))
    }

    @Test
    fun `should get dispenser by address`() {
        dispenserStatusRepository.save(
            DispenserStatus(
                address = 5,
                state = "DELIVERING",
                lastActive = LocalDateTime.now()
            )
        )

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/dispensers/5")
            .then()
            .statusCode(200)
            .body("dispenserAddress", equalTo(5))
            .body("state", equalTo("DELIVERING"))
    }

    @Test
    fun `should return 404 for non-existent dispenser`() {
        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/dispensers/999")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should get transaction count`() {
        // Create 3 transactions
        repeat(3) {
            transactionRepository.save(
                Transaction(
                    dispenserAddress = 1,
                    nozzleNumber = 1,
                    volumeDeciliters = 100,
                    amountOre = 1000,
                    timestamp = LocalDateTime.now()
                )
            )
        }

        RestAssured.given()
            .header(authHeader().first, authHeader().second)
            .`when`()
            .get("/api/v1/transactions/count")
            .then()
            .statusCode(200)
            .body("count", equalTo(3))
    }
}
