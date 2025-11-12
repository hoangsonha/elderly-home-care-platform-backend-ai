package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServiceTaskResponseDTO;
import com.capstone_project.elderly_platform.pojos.ServicePackage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServicePackageMapper {

    private final ServiceTaskMapper serviceTaskMapper;

    public ServicePackageResponseDTO toDTO(ServicePackage servicePackage) {
        if (servicePackage == null) {
            return null;
        }

        // Map nested serviceTasks
        List<ServiceTaskResponseDTO> serviceTaskDTOs = null;
        if (servicePackage.getServiceTasks() != null) {
            serviceTaskDTOs = serviceTaskMapper.toDTOList(servicePackage.getServiceTasks());
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
                .serviceIncluded(null) // Not used, set to null
                .status(servicePackage.getStatus() != null
                        ? servicePackage.getStatus().name()
                        : null)
                .serviceTasks(serviceTaskDTOs)
                .build();
    }
}
