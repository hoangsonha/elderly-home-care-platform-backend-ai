package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.WorkSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    @EntityGraph(attributePaths = {"careService", "careService.careSeekerProfile", "careService.careSeekerProfile.account", 
                                    "caregiverProfile", "caregiverProfile.account"})
    Optional<WorkSchedule> findByWorkScheduleIdAndDeletedIsFalse(UUID id);
}























