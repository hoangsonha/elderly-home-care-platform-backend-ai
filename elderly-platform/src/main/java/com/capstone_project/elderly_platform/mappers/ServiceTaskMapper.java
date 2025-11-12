package com.capstone_project.elderly_platform.mappers;

import com.capstone_project.elderly_platform.dtos.response.ServiceTaskResponseDTO;
import com.capstone_project.elderly_platform.pojos.ServiceTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceTaskMapper {

    public ServiceTaskResponseDTO toDTO(ServiceTask serviceTask) {
        if (serviceTask == null) {
            return null;
        }

        return ServiceTaskResponseDTO.builder()
                .serviceTaskId(serviceTask.getServiceTaskId() != null
                        ? serviceTask.getServiceTaskId().toString()
                        : null)
                .taskName(serviceTask.getTaskName())
                .description(serviceTask.getDescription())
                .status(serviceTask.getStatus() != null
                        ? serviceTask.getStatus().name()
                        : null)
                .build();
    }

    public List<ServiceTaskResponseDTO> toDTOList(List<ServiceTask> serviceTasks) {
        if (serviceTasks == null || serviceTasks.isEmpty()) {
            return List.of();
        }

        return serviceTasks.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

