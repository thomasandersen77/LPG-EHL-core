package no.cloudberries.lpg.payment

import no.cloudberries.lpg.service.payment.Payment
import no.cloudberries.lpg.service.payment.PaymentGateway
import no.cloudberries.lpg.service.payment.PaymentMethod
import no.cloudberries.lpg.service.payment.PaymentRequest
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentGateway: PaymentGateway
) {

    data class StartPaymentRequest(
        val amountCents: Long,
        val method: PaymentMethod,
        val reference: String,
        val metadata: Map<String, String> = emptyMap()
    )

    @PostMapping
    fun startPayment(@RequestBody body: StartPaymentRequest): Payment {
        val request = PaymentRequest(
            amountCents = body.amountCents,
            method = body.method,
            reference = body.reference,
            metadata = body.metadata
        )
        return paymentGateway.startPayment(request)
    }

    @GetMapping("/{id}")
    fun getPayment(@PathVariable id: UUID): Payment? =
        paymentGateway.getPayment(id)
}
