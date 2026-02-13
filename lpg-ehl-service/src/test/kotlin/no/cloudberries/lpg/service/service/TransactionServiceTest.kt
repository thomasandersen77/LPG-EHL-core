package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.service.pump.PumpAuthorization
import no.cloudberries.lpg.service.pump.PumpAuthorizationService
import no.cloudberries.lpg.service.transaction.Transaction
import no.cloudberries.lpg.service.transaction.TransactionRepository
import no.cloudberries.lpg.service.transaction.TransactionService
import no.cloudberries.lpg.service.transaction.TransactionSyncService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class TransactionServiceTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transactionSyncService: TransactionSyncService
    private lateinit var authorizationService: PumpAuthorizationService
    private lateinit var transactionService: TransactionService

    @BeforeEach
    fun setup() {
        transactionRepository = mock()
        transactionSyncService = mock()
        authorizationService = mock()
        transactionService = TransactionService(transactionRepository, transactionSyncService, authorizationService)
    }

    @Test
    fun `updatePaymentStatus completes STOPPED authorization when paid`() {
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            transactionId = transactionId,
            dispenserAddress = 1,
            nozzleNumber = 1,
            volumeDeciliters = 0,
            amountOre = 0
        )

        whenever(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction))
        whenever(transactionRepository.save(any<Transaction>())).thenReturn(transaction)
        whenever(
            authorizationService.completeStoppedAuthorizationByTransaction(
                transactionId,
                "CARD",
                "PAYMENT_UPDATED"
            )
        ).thenReturn(null)
        whenever(
            authorizationService.completeStoppedAuthorizationByDispenser(
                1,
                "CARD",
                "PAYMENT_UPDATED"
            )
        ).thenReturn(mock<PumpAuthorization>())

        transactionService.updatePaymentStatus(transactionId, "CARD", "PAID")

        verify(authorizationService).completeStoppedAuthorizationByTransaction(
            transactionId,
            "CARD",
            "PAYMENT_UPDATED"
        )
        verify(authorizationService).completeStoppedAuthorizationByDispenser(
            1,
            "CARD",
            "PAYMENT_UPDATED"
        )
    }

    @Test
    fun `markTransactionPaid completes authorization via transaction id`() {
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            transactionId = transactionId,
            dispenserAddress = 2,
            nozzleNumber = 1,
            volumeDeciliters = 0,
            amountOre = 0
        )

        whenever(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction))
        whenever(transactionRepository.save(any<Transaction>())).thenReturn(transaction)
        whenever(
            authorizationService.completeStoppedAuthorizationByTransaction(
                transactionId,
                "CARD",
                "MARK_TRANSACTION_PAID"
            )
        ).thenReturn(mock<PumpAuthorization>())

        transactionService.markTransactionPaid(transactionId, "CARD")

        verify(authorizationService).completeStoppedAuthorizationByTransaction(
            transactionId,
            "CARD",
            "MARK_TRANSACTION_PAID"
        )
        verify(authorizationService, never()).completeStoppedAuthorizationByDispenser(
            eq(2),
            eq("CARD"),
            eq("MARK_TRANSACTION_PAID")
        )
    }
}