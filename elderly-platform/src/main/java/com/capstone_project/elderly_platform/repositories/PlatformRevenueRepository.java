package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.PlatformRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformRevenueRepository extends JpaRepository<PlatformRevenue, UUID> {
    Optional<PlatformRevenue> findByPlatformRevenueId(UUID id);
}








