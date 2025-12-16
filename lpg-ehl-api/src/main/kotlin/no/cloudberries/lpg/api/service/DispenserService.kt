package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.api.dto.DispenserStatusResponse
import no.cloudberries.lpg.api.repository.DispenserStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class DispenserService(
    private val dispenserStatusRepository: DispenserStatusRepository
) {

    fun getAllDispensers(): List<DispenserStatusResponse> {
        return dispenserStatusRepository.findAll()
            .map { DispenserStatusResponse.from(it) }
    }

    fun getDispenserStatus(address: Int): DispenserStatusResponse? {
        return dispenserStatusRepository.findById(address)
            .map { DispenserStatusResponse.from(it) }
            .orElse(null)
    }

    fun getActiveDispensers(minutesSinceLastSeen: Long = 60): List<DispenserStatusResponse> {
        val cutoffTime = LocalDateTime.now().minusMinutes(minutesSinceLastSeen)
        return dispenserStatusRepository.findActiveDispensers(cutoffTime)
            .map { DispenserStatusResponse.from(it) }
    }

    fun dispenserExists(address: Int): Boolean {
        return dispenserStatusRepository.existsByAddress(address)
    }
}
