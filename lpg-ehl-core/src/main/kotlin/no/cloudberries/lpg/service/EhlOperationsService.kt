package no.cloudberries.lpg.service

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

/**
 * High-level EHL protocol operations service.
 * 
 * This service provides domain-level operations that abstract away the low-level
 * EHL protocol details. It can be used by:
 * - REST API controllers
 * - CLI commands
 * - Any other client that needs to interact with dispensers
 * 
 * All operations are suspend functions to support coroutines.
 * 
 * ARCHITECTURE:
 * - No Spring dependencies (can be used anywhere)
 * - Returns rich domain objects (not raw packets)
 * - Comprehensive error handling with Result types
 * - Thread-safe via EhlCommunicator
 */
class EhlOperationsService(
    private val communicator: EhlCommunicator
) {
    private val logger = LoggerFactory.getLogger(EhlOperationsService::class.java)
    
    /**
     * Test connectivity to dispenser.
     * 
     * @param address Dispenser address (1-255)
     * @return OK packet if successful
     * @throws Exception if communication fails
     */
    suspend fun linetest(address: Int): EhlPacket {
        logger.debug("LINETEST for address $address")
        val packet = EhlPacket(address, EhlCommand.LINETEST)
        return communicator.sendAndReceive(packet)
    }
    
    /**
     * Query dispenser state.
     * 
     * @param address Dispenser address
     * @return State packet with state code in data[0]
     * @throws Exception if communication fails
     */
    suspend fun getState(address: Int): EhlPacket {
        logger.debug("STATE query for address $address")
        val packet = EhlPacket(address, EhlCommand.STATE)
        return communicator.sendAndReceive(packet)
    }
    
    /**
     * Query current volume and parse result.
     * 
     * @param address Dispenser address
     * @return VolumeResult with parsed volume in liters
     * @throws Exception if communication fails or parsing fails
     */
    suspend fun getVolume(address: Int): VolumeResult {
        logger.debug("VOLUME query for address $address")
        val packet = EhlPacket(address, EhlCommand.VOLUME)
        val response = communicator.sendAndReceive(packet)
        
        // Parse volume using VB6-compatible 5-byte ASCII format
        val volumeLitres = if (response.data.size == 5) {
            EhlDataParser.parseVolumeDataVb6(response.data)
        } else {
            throw IllegalStateException("Unexpected VOLUME data size: ${response.data.size} bytes")
        }
        
        return VolumeResult(
            volumeLitres = volumeLitres,
            pumpNumber = address,
            raw = response
        )
    }
    
    /**
     * Query price setting and parse result.
     * 
     * @param address Dispenser address
     * @return PriceResult with price in cents and kr
     * @throws Exception if communication fails or parsing fails
     */
    suspend fun getPrice(address: Int): PriceResult {
        logger.debug("PRICE query for address $address")
        val packet = EhlPacket(address, EhlCommand.PRICE)
        val response = communicator.sendAndReceive(packet)
        
        // Parse price (4 ASCII bytes, reversed order)
        val priceString = EhlDataParser.parsePriceData(response.data)  // e.g., "15.90"
        val priceCents = (priceString.replace(".", "").toInt())  // "15.90" -> 1590
        
        return PriceResult(
            pricePerLitreCents = priceCents,
            raw = response
        )
    }
    
    /**
     * Unblock dispenser to allow fuel delivery.
     * 
     * @param address Dispenser address
     * @return Result with Unit if successful, failure if error
     */
    suspend fun unblock(address: Int): Result<Unit> {
        logger.info("UNBLOCK for address $address")
        return try {
            val packet = EhlPacket(address, EhlCommand.UNBLOCK)
            val response = communicator.sendAndReceive(packet)
            
            if (response.command == EhlCommand.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unexpected response: ${response.command}"))
            }
        } catch (e: Exception) {
            logger.error("UNBLOCK failed for address $address: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Block dispenser to stop fuel delivery.
     * 
     * @param address Dispenser address
     * @return Result with Unit if successful, failure if error
     */
    suspend fun block(address: Int): Result<Unit> {
        logger.info("BLOCK for address $address")
        return try {
            val packet = EhlPacket(address, EhlCommand.BLOCK)
            val response = communicator.sendAndReceive(packet)
            
            if (response.command == EhlCommand.OK || response.command == EhlCommand.VOLUME) {
                // BLOCK often returns VOLUME as acknowledgment
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unexpected response: ${response.command}"))
            }
        } catch (e: Exception) {
            logger.error("BLOCK failed for address $address: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Query error status.
     * 
     * @param address Dispenser address
     * @return ErrorResult with error info
     * @throws Exception if communication fails
     */
    suspend fun getError(address: Int): ErrorResult {
        logger.debug("ERROR query for address $address")
        val packet = EhlPacket(address, EhlCommand.ERROR_QUERY)
        val response = communicator.sendAndReceive(packet)
        
        // Parse error data (VB6 format: 2 ASCII bytes)
        return if (response.data.size == 2) {
            val (mainCode, subCode) = EhlDataParser.parseErrorData(response.data)
            val hasError = mainCode != '0' || subCode != '0'
            
            ErrorResult(
                hasError = hasError,
                errorCode = if (hasError) "$mainCode$subCode".toIntOrNull() else null,
                errorDescription = if (hasError) "Error code: $mainCode$subCode" else null,
                raw = response
            )
        } else if (response.data.size == 1) {
            // Legacy format
            val errorCode = EhlDataParser.parseErrorDataLegacy(response.data)
            val hasError = errorCode != 0
            
            ErrorResult(
                hasError = hasError,
                errorCode = if (hasError) errorCode else null,
                errorDescription = if (hasError) "Error code: $errorCode" else null,
                raw = response
            )
        } else {
            throw IllegalStateException("Unexpected ERROR data size: ${response.data.size} bytes")
        }
    }
    
    /**
     * Query tank level.
     * 
     * @param address Dispenser address
     * @return TankResult with tank level percentage
     * @throws Exception if communication fails
     */
    suspend fun getTank(address: Int): TankResult {
        logger.debug("TANK query for address $address")
        val packet = EhlPacket(address, EhlCommand.TANK)
        val response = communicator.sendAndReceive(packet)
        
        // Parse tank data (1 byte: 0-100%)
        val tankLevel = if (response.data.isNotEmpty()) {
            response.data[0].toInt() and 0xFF
        } else {
            0
        }
        
        return TankResult(
            tankLevelPercent = tankLevel,
            raw = response
        )
    }
    
    /**
     * Run VB6 compatibility test sequence.
     * Tests the essential commands in order: LINETEST -> STATE -> VOLUME -> PRICE
     * Stops at first failure.
     * 
     * @param address Dispenser address
     * @return SequenceResult with test results
     */
    suspend fun runVb6Sequence(address: Int): SequenceResult {
        logger.info("🧪 Running VB6 sequence test for address $address")
        
        val steps = mutableListOf<SequenceStep>()
        var allPassed = true
        var failedAt: String? = null
        
        // Test 1: LINETEST
        val linetestDuration = measureTimeMillis {
            try {
                linetest(address)
                steps.add(SequenceStep("LINETEST", true, 0))
            } catch (e: Exception) {
                steps.add(SequenceStep("LINETEST", false, 0, e.message))
                allPassed = false
                failedAt = "LINETEST"
            }
        }
        steps.last().let { steps[steps.lastIndex] = it.copy(duration = linetestDuration) }
        
        if (!allPassed) {
            return SequenceResult(false, failedAt, 1, 4, steps)
        }
        
        // Test 2: STATE
        val stateDuration = measureTimeMillis {
            try {
                getState(address)
                steps.add(SequenceStep("STATE", true, 0))
            } catch (e: Exception) {
                steps.add(SequenceStep("STATE", false, 0, e.message))
                allPassed = false
                failedAt = "STATE"
            }
        }
        steps.last().let { steps[steps.lastIndex] = it.copy(duration = stateDuration) }
        
        if (!allPassed) {
            return SequenceResult(false, failedAt, 2, 4, steps)
        }
        
        // Test 3: VOLUME
        val volumeDuration = measureTimeMillis {
            try {
                getVolume(address)
                steps.add(SequenceStep("VOLUME", true, 0))
            } catch (e: Exception) {
                steps.add(SequenceStep("VOLUME", false, 0, e.message))
                allPassed = false
                failedAt = "VOLUME"
            }
        }
        steps.last().let { steps[steps.lastIndex] = it.copy(duration = volumeDuration) }
        
        if (!allPassed) {
            return SequenceResult(false, failedAt, 3, 4, steps)
        }
        
        // Test 4: PRICE
        val priceDuration = measureTimeMillis {
            try {
                getPrice(address)
                steps.add(SequenceStep("PRICE", true, 0))
            } catch (e: Exception) {
                steps.add(SequenceStep("PRICE", false, 0, e.message))
                allPassed = false
                failedAt = "PRICE"
            }
        }
        steps.last().let { steps[steps.lastIndex] = it.copy(duration = priceDuration) }
        
        val result = SequenceResult(
            allPassed = allPassed,
            failedAt = failedAt,
            testsRun = steps.size,
            totalTests = 4,
            steps = steps
        )
        
        if (result.allPassed) {
            logger.info("✅ VB6 sequence test PASSED - all 4 tests successful")
        } else {
            logger.warn("❌ VB6 sequence test FAILED at $failedAt")
        }
        
        return result
    }
}
