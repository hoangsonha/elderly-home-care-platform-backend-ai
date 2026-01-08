package com.capstone_project.elderly_platform.dtos.response;

import com.capstone_project.elderly_platform.dtos.QualificationRequirements;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageEligibilityResponse {
    private String servicePackageId;
    private String packageName;
    private String description;
    private Integer durationHours;
    private String packageType;
    private Double price;
    private String note;
    private QualificationRequirements qualification;
    private String status;
    private List<ServiceTaskResponseDTO> serviceTasks;
    private Long totalCareServices;
    private Boolean isEligible; // Caregiver có đủ yêu cầu để làm package này hay không
}

