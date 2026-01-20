package no.cloudberries.lpg.service.pump

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PumpAuthorizationRepository : JpaRepository<PumpAuthorization, UUID> {
    
    /**
     * Finn alle PENDING autorisasjoner for en gitt dispenser.
     * Headless bruker denne til å finne autorisasjoner som skal aktiveres.
     */
    fun findByDispenserAddressAndStatus(
        dispenserAddress: Int, 
        status: AuthorizationStatus
    ): List<PumpAuthorization>
    
    /**
     * Finn alle PENDING autorisasjoner (uavhengig av dispenser).
     * Brukes av headless til å se om noen dispensere venter på UNBLOCK.
     */
    fun findByStatus(status: AuthorizationStatus): List<PumpAuthorization>
    
    /**
     * Finn aktiv autorisasjon for en dispenser (PENDING, AUTHORIZED, PUMPING eller STOPPED).
     */
    fun findFirstByDispenserAddressAndStatusInOrderByCreatedAtDesc(
        dispenserAddress: Int,
        statuses: List<AuthorizationStatus>
    ): PumpAuthorization?
    
    /**
     * Finn siste autorisasjon for en dispenser (uansett status).
     */
    fun findFirstByDispenserAddressOrderByCreatedAtDesc(dispenserAddress: Int): PumpAuthorization?
    
    /**
     * Tell ventende autorisasjoner.
     */
    fun countByStatus(status: AuthorizationStatus): Long
}
