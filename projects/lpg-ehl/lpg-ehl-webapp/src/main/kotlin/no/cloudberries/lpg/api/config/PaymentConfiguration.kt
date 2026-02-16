package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.service.payment.PaymentGateway
import no.cloudberries.lpg.service.payment.MockPaymentGateway
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Payment Gateway Configuration
 * 
 * Ensures a PaymentGateway bean is always available.
 * Uses MockPaymentGateway as the default implementation for all profiles.
 */
@Configuration
class PaymentConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(PaymentGateway::class)
    fun paymentGateway(): PaymentGateway {
        return MockPaymentGateway()
    }
}
