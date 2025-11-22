package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.ElderlyProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElderlyProfileRepository extends JpaRepository<ElderlyProfile, UUID> {
    ElderlyProfile findByElderlyProfileIdAndDeletedIsFalse(UUID id);
}























