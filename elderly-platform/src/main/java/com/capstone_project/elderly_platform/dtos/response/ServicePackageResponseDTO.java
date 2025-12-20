package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServicePackageResponseDTO {
    String servicePackageId;
    String packageName;
    String description;
    Integer durationHours;
    String packageType;
    Double price;
    String note;
    String serviceIncluded;
    String status;
    List<ServiceTaskResponseDTO> serviceTasks;
    Long totalCareServices;
}
