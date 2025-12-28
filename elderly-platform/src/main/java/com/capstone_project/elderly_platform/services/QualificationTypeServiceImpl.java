package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.response.QualificationTypeResponseDTO;
import com.capstone_project.elderly_platform.pojos.QualificationType;
import com.capstone_project.elderly_platform.repositories.QualificationTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualificationTypeServiceImpl implements QualificationTypeService {

    private final QualificationTypeRepository qualificationTypeRepository;

    @Transactional(readOnly = true)
    @Override
    public List<QualificationTypeResponseDTO> getAllActiveQualificationTypes() {

        List<QualificationType> qualificationTypes = qualificationTypeRepository.findByIsActiveTrueAndDeletedFalse();

        return qualificationTypes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<QualificationTypeResponseDTO> getAllQualificationTypes() {

        List<QualificationType> qualificationTypes = qualificationTypeRepository.findByDeletedFalse();

        return qualificationTypes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private QualificationTypeResponseDTO toDTO(QualificationType qualificationType) {
        if (qualificationType == null) {
            return null;
        }

        return QualificationTypeResponseDTO.builder()
                .qualificationTypeId(qualificationType.getQualificationTypeId())
                .typeName(qualificationType.getTypeName())
                .description(qualificationType.getDescription())
                .isActive(qualificationType.getIsActive())
                .build();
    }
}


