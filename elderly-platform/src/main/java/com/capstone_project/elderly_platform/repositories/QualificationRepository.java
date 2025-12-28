package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, UUID> {
    Qualification findByQualificationIdAndDeletedIsFalse(UUID id);

    List<Qualification> findByDeletedFalse();

    List<Qualification> findByCaregiverProfile_CaregiverProfileIdAndDeletedIsFalse(UUID caregiverProfileId);

    List<Qualification> findByCaregiverProfile_CaregiverProfileIdAndIsVerifiedTrueAndDeletedIsFalse(UUID caregiverProfileId);

    List<Qualification> findByQualificationType_QualificationTypeIdAndDeletedIsFalse(UUID qualificationTypeId);
}





