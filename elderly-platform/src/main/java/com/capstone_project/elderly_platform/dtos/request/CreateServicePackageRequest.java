package com.capstone_project.elderly_platform.dtos.request;

import com.capstone_project.elderly_platform.enums.EnumServicePackageType;
import com.capstone_project.elderly_platform.validators.ValidDurationHours;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateServicePackageRequest {

    @NotBlank(message = "Package name is required")
    String packageName;

    String description;

    @NotNull(message = "Duration hours is required")
    @ValidDurationHours
    Integer durationHours;

    @NotNull(message = "Package type is required")
    EnumServicePackageType packageType;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    Double price;

    String note;

    @Valid
    List<ServiceTaskItemRequest> serviceTasks;
}
