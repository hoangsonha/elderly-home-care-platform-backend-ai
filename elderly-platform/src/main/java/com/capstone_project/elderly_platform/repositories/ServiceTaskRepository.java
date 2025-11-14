package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.ServiceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTaskRepository extends JpaRepository<ServiceTask, UUID> {
    Optional<ServiceTask> findByServiceTaskIdAndDeletedIsFalse(UUID id);
}








