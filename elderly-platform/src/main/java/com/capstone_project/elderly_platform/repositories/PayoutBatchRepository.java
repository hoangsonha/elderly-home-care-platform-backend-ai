package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.PayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, UUID> {
    Optional<PayoutBatch> findByPayoutBatchIdAndDeletedIsFalse(UUID id);
    PayoutBatch findByBatchCode(String batchCode);
}










