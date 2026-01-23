package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Optional<Payout> findByPayoutIdAndDeletedIsFalse(UUID id);
    Payout findByPayoutCode(String payoutCode);
    
    @Query("SELECT p FROM Payout p WHERE p.caregiverProfile.caregiverProfileId = :caregiverProfileId " +
            "AND p.deleted = false AND p.careService.status = :status " +
            "ORDER BY p.serviceDate DESC, p.createdAt DESC")
    List<Payout> findByCaregiverProfileAndCompletedCareServiceOrderByServiceDateDesc(
            @Param("caregiverProfileId") UUID caregiverProfileId,
            @Param("status") EnumCareServiceStatusType status);
}























