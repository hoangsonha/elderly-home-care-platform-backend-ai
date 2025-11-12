package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateSystemConfigRequest {
    @NotNull(message = "Config value is required")
    @NotBlank(message = "Config value cannot be blank")
    String value;

    String changeReason; // Optional reason for the change
}

