package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmationCareServiceRequest {
    @NotNull(message = "Please enter care service id")
    UUID careServiceId;

    String note;
}
