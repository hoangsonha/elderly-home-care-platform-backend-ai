package com.capstone_project.elderly_platform.dtos.request;

import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.validators.ValidDurationHours;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateServicePackageRequest {
    
    String packageName;
    
    String description;
    
    @ValidDurationHours
    Integer durationHours;
    
    EnumServicePackageType packageType;
    
    @Positive(message = "Price must be positive")
    Double price;
    
    String note;
    
    EnumActivationStatusType status;
    
    @Valid
    List<ServiceTaskItemRequest> serviceTasks;
}

