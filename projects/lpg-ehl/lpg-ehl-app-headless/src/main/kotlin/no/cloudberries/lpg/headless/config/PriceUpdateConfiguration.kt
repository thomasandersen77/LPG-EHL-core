package no.cloudberries.lpg.headless.config

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

@Configuration
class PriceUpdateConfiguration {

    private val logger = LoggerFactory.getLogger(PriceUpdateConfiguration::class.java)

    @Bean
    fun priceUpdateCallback(ehlCommunicator: EhlCommunicator): (Int, BigDecimal) -> Unit {
        return { address, pricePerLiter ->
            logger.info("Executing price update for address $address with price $pricePerLiter")

            // EhlCommunicator functions are suspendable, so we need runBlocking here
            kotlinx.coroutines.runBlocking {
                try {
                    // Step 1: Send PRODUCT_SELECT (0xC3 / 195)
                    val productSelectByte: Byte = 0x30
                    val productSelectPacket = EhlPacket(
                        address = address,
                        command = EhlCommand.PRODUCT_SELECT,
                        data = byteArrayOf(productSelectByte)
                    )

                    logger.debug("Step 1: Sending PRODUCT_SELECT to address $address")
                    try {
                        ehlCommunicator.sendAndReceive(productSelectPacket)
                    } catch (e: Exception) {
                        logger.warn("PRODUCT_SELECT failed or timed out: ${e.message}. Proceeding anyway...")
                    }
                    
                    // Step 2: Send PROG_PRC (0xA9 / 169)
                    // Format: 4 ASCII digits, LSB first.
                    val priceString = "%.2f".format(pricePerLiter) // "15.90"
                    val digits = priceString.replace(".", "") // "1590"
                    
                    val priceData = ByteArray(4)
                    if (digits.length >= 4) {
                        // LSB first: digits[3], digits[2], digits[1], digits[0]
                        priceData[0] = digits[3].code.toByte()
                        priceData[1] = digits[2].code.toByte()
                        priceData[2] = digits[1].code.toByte()
                        priceData[3] = digits[0].code.toByte()
                    }

                    val progPrcPacket = EhlPacket(
                        address = address,
                        command = EhlCommand.PROG_PRC,
                        data = priceData
                    )

                    logger.debug("Step 2: Sending PROG_PRC to address $address with data ${priceData.contentToString()}")
                    ehlCommunicator.sendAndReceive(progPrcPacket)
                    
                    logger.info("Price update sequence completed for address $address")

                } catch (e: Exception) {
                    logger.error("Failed to update price for address $address", e)
                }
            }
        }
    }
}
