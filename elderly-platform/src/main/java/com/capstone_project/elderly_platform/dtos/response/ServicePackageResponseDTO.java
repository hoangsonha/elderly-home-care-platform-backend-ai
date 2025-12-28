package com.capstone_project.elderly_platform.dtos.response;

import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
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
    QualificationRequirements qualification;
    String status;
    List<ServiceTaskResponseDTO> serviceTasks;
    Long totalCareServices;
}
