package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServiceTaskResponseDTO;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServicePackageMapper {

    private final ServiceTaskMapper serviceTaskMapper;
    private final ObjectMapper objectMapper;

    public ServicePackageResponseDTO toDTO(ServicePackage servicePackage) {
        if (servicePackage == null) {
            return null;
        }

        // Map nested serviceTasks (filter out deleted tasks)
        List<ServiceTaskResponseDTO> serviceTaskDTOs = null;
        if (servicePackage.getServiceTasks() != null) {
            List<com.capstone_project.elderly_platform.pojos.ServiceTask> activeTasks = 
                servicePackage.getServiceTasks().stream()
                    .filter(task -> !task.isDeleted())
                    .collect(java.util.stream.Collectors.toList());
            serviceTaskDTOs = serviceTaskMapper.toDTOList(activeTasks);
        }

        return ServicePackageResponseDTO.builder()
                .servicePackageId(servicePackage.getServicePackageId() != null
                        ? servicePackage.getServicePackageId().toString()
                        : null)
                .packageName(servicePackage.getPackageName())
                .description(servicePackage.getDescription())
                .durationHours(servicePackage.getDurationHours())
                .packageType(servicePackage.getPackageType() != null
                        ? servicePackage.getPackageType().name()
                        : null)
                .price(servicePackage.getPrice())
                .note(servicePackage.getNote())
                .qualification(parseQualification(servicePackage.getQualification()))
                .status(servicePackage.getStatus() != null
                        ? servicePackage.getStatus().name()
                        : null)
                .serviceTasks(serviceTaskDTOs)
                .totalCareServices(null) // Will be set in service layer
                .build();
    }

    /**
     * Parse qualification JSON string to QualificationRequirements object
     */
    private QualificationRequirements parseQualification(String qualificationJson) {
        if (qualificationJson == null || qualificationJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(qualificationJson, QualificationRequirements.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing qualification JSON: {}", qualificationJson, e);
            return null;
        }
    }
}
