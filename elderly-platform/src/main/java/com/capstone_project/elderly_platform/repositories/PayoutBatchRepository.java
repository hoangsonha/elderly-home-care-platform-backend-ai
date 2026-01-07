package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.PayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, UUID> {
    Optional<PayoutBatch> findByPayoutBatchIdAndDeletedIsFalse(UUID id);
    PayoutBatch findByBatchCode(String batchCode);
    
    @Query("SELECT pb FROM PayoutBatch pb WHERE pb.caregiverProfile.caregiverProfileId = :caregiverProfileId " +
            "AND pb.payoutYear = :year AND pb.payoutMonth = :month AND pb.deleted = false")
    Optional<PayoutBatch> findByCaregiverProfileAndYearAndMonth(
            @Param("caregiverProfileId") java.util.UUID caregiverProfileId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}























