package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Optional<Payout> findByPayoutIdAndDeletedIsFalse(UUID id);
    Payout findByPayoutCode(String payoutCode);
}








