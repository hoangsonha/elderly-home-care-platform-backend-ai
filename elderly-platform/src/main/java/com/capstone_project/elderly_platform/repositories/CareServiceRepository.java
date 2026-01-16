package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, UUID> {
        @EntityGraph(attributePaths = { "workSchedule", "workSchedule.workTasks" })
        CareService findByCareServiceIdAndDeletedIsFalse(UUID id);

        @EntityGraph(attributePaths = { "workSchedule", "workSchedule.workTasks" })
        CareService findByBookingCodeAndDeletedIsFalse(String bookingCode);

        CareService findByBookingCode(String bookingCode);

        boolean existsByBookingCode(String bookingCode);

        // Find all care services by seeker (with optional status filter and sorting)
        List<CareService> findByCareSeekerProfileAndDeletedIsFalse(CareSeekerProfile careSeekerProfile, Sort sort);

        List<CareService> findByCareSeekerProfileAndStatusAndDeletedIsFalse(CareSeekerProfile careSeekerProfile,
                        EnumCareServiceStatusType status, Sort sort);

        // Find all care services by seeker with work date filter
        List<CareService> findByCareSeekerProfileAndWorkDateAndDeletedIsFalse(CareSeekerProfile careSeekerProfile,
                        LocalDate workDate, Sort sort);

        List<CareService> findByCareSeekerProfileAndWorkDateAndStatusAndDeletedIsFalse(
                        CareSeekerProfile careSeekerProfile, LocalDate workDate, EnumCareServiceStatusType status,
                        Sort sort);

        // Find all care services by caregiver (with optional status filter and sorting)
        List<CareService> findByCaregiverProfileAndDeletedIsFalse(CaregiverProfile caregiverProfile, Sort sort);

        List<CareService> findByCaregiverProfileAndStatusAndDeletedIsFalse(CaregiverProfile caregiverProfile,
                        EnumCareServiceStatusType status, Sort sort);

        // Find all care services by caregiver with work date filter
        List<CareService> findByCaregiverProfileAndWorkDateAndDeletedIsFalse(CaregiverProfile caregiverProfile,
                        LocalDate workDate, Sort sort);

        List<CareService> findByCaregiverProfileAndWorkDateAndStatusAndDeletedIsFalse(CaregiverProfile caregiverProfile,
                        LocalDate workDate, EnumCareServiceStatusType status, Sort sort);

        /**
         * Finds all care services with status PENDING_CAREGIVER that have already
         * passed their deadline.
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
         * @return List of care services that should be expired (status =
         *         PENDING_CAREGIVER but deadline < now)
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

        @Query("SELECT COUNT(cs) FROM CareService cs WHERE cs.servicePackage.servicePackageId = :servicePackageId " +
                        "AND cs.deleted = false")
        Long countByServicePackageIdAndDeletedFalse(@Param("servicePackageId") UUID servicePackageId);

        @Query("SELECT COUNT(cs) FROM CareService cs WHERE cs.deleted = false")
        Long countTotalBookings();

        @Query("SELECT COALESCE(SUM(cs.totalPrice), 0) FROM CareService cs WHERE cs.deleted = false")
        Double sumTotalRevenue();

        @Query("SELECT COUNT(cs) FROM CareService cs WHERE cs.status = :status AND cs.deleted = false")
        Long countByStatusAndDeletedFalse(@Param("status") EnumCareServiceStatusType status);

        /**
         * Find all care services for a caregiver within a date range
         * Used to check conflicts when updating free schedule
         * 
         * @param caregiverProfile Caregiver profile
         * @param startDate Start date (inclusive)
         * @param endDate End date (inclusive)
         * @return List of care services in the date range
         */
        @Query("SELECT cs FROM CareService cs WHERE cs.caregiverProfile = :caregiverProfile " +
                        "AND cs.workDate >= :startDate " +
                        "AND cs.workDate <= :endDate " +
                        "AND cs.deleted = false " +
                        "ORDER BY cs.workDate ASC, cs.startTime ASC")
        List<CareService> findByCaregiverProfileAndWorkDateBetweenAndDeletedIsFalse(
                        @Param("caregiverProfile") CaregiverProfile caregiverProfile,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

}
