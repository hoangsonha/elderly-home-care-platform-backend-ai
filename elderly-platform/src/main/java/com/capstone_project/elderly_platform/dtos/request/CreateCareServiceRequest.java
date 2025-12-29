package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCareServiceRequest {
    @NotNull(message = "Please enter elderly id")
    UUID elderlyProfileId;

    @NotNull(message = "Please enter caregiver id")
    UUID caregiverProfileId;

    LocationRequest location;

    @NotNull(message = "Please enter work date")

    @NotNull(message = "Work date must not be null")
    @FutureOrPresent(message = "Work date must be today or in the future")
    LocalDate workDate;

    Integer startHour;
    Integer startMinute;

    @NotNull(message = "Please enter service package id")
    UUID servicePackageId;

    String note;
}
