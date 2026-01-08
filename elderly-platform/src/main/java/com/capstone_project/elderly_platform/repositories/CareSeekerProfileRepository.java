package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.CareSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareSeekerProfileRepository extends JpaRepository<CareSeekerProfile, UUID> {
    CareSeekerProfile findByCareSeekerProfileIdAndDeletedIsFalse(UUID id);
    
    CareSeekerProfile findByAccount_AccountIdAndDeletedIsFalse(UUID accountId);
    
    @Query("SELECT DISTINCT c FROM CareSeekerProfile c " +
            "LEFT JOIN FETCH c.account " +
            "LEFT JOIN FETCH c.elderlyProfiles e " +
            "WHERE c.account.accountId = :accountId " +
            "AND c.deleted = false " +
            "AND (e.deleted = false OR e IS NULL)")
    CareSeekerProfile findByAccountIdWithAccountAndElderlyProfiles(UUID accountId);
}









