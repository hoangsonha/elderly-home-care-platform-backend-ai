package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareSeekerProfileRepository extends JpaRepository<CareSeekerProfile, UUID> {
    CareSeekerProfile findByCareSeekerProfileIdAndDeletedIsFalse(UUID id);
}




