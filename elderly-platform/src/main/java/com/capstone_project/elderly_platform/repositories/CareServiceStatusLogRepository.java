package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.CareServiceStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareServiceStatusLogRepository extends JpaRepository<CareServiceStatusLog, UUID> {
    Optional<CareServiceStatusLog> findByCareServiceStatusLogIdAndDeletedIsFalse(UUID id);
}










