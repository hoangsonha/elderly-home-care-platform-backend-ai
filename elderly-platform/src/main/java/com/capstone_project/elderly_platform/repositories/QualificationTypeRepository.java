package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.QualificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualificationTypeRepository extends JpaRepository<QualificationType, UUID> {
    QualificationType findByQualificationTypeIdAndDeletedIsFalse(UUID id);

    List<QualificationType> findByDeletedFalse();

    List<QualificationType> findByIsActiveTrueAndDeletedFalse();

    QualificationType findByTypeNameAndDeletedIsFalse(String typeName);
}








