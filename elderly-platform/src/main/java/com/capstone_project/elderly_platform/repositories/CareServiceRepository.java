package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, UUID> {
    CareService findByCareServiceIdAndDeletedIsFalse(UUID id);
    CareService findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    /**
     * Finds all care services with status PENDING_CAREGIVER that have already passed their deadline.
     * 
     * This is used to handle edge cases where:
     * - Server/Redis restarted at the exact moment expiration should happen
     * - Redis keys are lost but database still has status = PENDING_CAREGIVER
     * - Deadline has passed but status hasn't been updated to EXPIRED
     * 
     * Used by:
     * - ExpiredCareServiceRecoveryService (on startup)
     * - ExpiredCareServiceChecker (scheduled backup check)
     *
     * @param status The status to filter (typically PENDING_CAREGIVER)
     * @param now    Current timestamp
     * @return List of care services that should be expired (status = PENDING_CAREGIVER but deadline < now)
     */
    @Query("SELECT cs FROM CareService cs WHERE cs.status = :status " +
            "AND cs.caregiverResponseDeadline IS NOT NULL " +
            "AND cs.caregiverResponseDeadline < :now " +
            "AND cs.deleted = false")
    List<CareService> findExpiredCareServices(@Param("status") EnumCareServiceStatusType status,
                                               @Param("now") LocalDateTime now);

    /**
     * Finds all pending care services that need to be rescheduled in Redis.
     * Used for recovery after Redis restart.
     *
     * @param status The status to filter (typically PENDING_CAREGIVER)
     * @param now    Current timestamp
     * @return List of pending care services with future deadlines
     */
    @Query("SELECT cs FROM CareService cs WHERE cs.status = :status " +
            "AND cs.caregiverResponseDeadline IS NOT NULL " +
            "AND cs.caregiverResponseDeadline > :now " +
            "AND cs.deleted = false")
    List<CareService> findPendingCareServicesForRecovery(@Param("status") EnumCareServiceStatusType status,
                                                          @Param("now") LocalDateTime now);
}





