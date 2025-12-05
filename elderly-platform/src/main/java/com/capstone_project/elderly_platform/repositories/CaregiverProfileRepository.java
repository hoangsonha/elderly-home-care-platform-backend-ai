package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CaregiverProfileRepository extends JpaRepository<CaregiverProfile, UUID> {
    CaregiverProfile findByCaregiverProfileIdAndDeletedIsFalse(UUID id);
    
    CaregiverProfile findByAccount_AccountIdAndDeletedIsFalse(UUID accountId);
    
    java.util.List<CaregiverProfile> findByDeletedFalse();
}









