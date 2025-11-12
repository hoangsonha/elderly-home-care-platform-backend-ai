package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.CareService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, UUID> {
    CareService findByCareServiceIdAndDeletedIsFalse(UUID id);
    CareService findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);
}




