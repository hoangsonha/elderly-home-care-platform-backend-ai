package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCaregiverProfileRequest {

    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    String fullName;

    @JsonProperty("birth_year")
    Integer birthYear;

    String gender; // MALE, FEMALE, OTHER

    String phone;

    @JsonProperty("citizen_id")
    String citizenId; // Số CCCD/CMND

    @Valid
    LocationRequest location; // address, latitude, longitude

    @JsonProperty("service_radius_km")
    Double serviceRadiusKm;

    String bio;

    @JsonProperty("years_experience")
    Integer yearsExperience;

    @Valid
    @JsonProperty("free_schedule")
    FreeScheduleRequest freeSchedule;

    @Valid
    PreferencesRequest preferences;

    @Valid
    List<CredentialRequest> credentials;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FreeScheduleRequest {
        @JsonProperty("available_all_time")
        Boolean availableAllTime;

        @JsonProperty("booked_slots")
        List<BookedSlotRequest> bookedSlots;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BookedSlotRequest {
        @NotBlank(message = "Date is required")
        String date; // yyyy-MM-dd

        @NotBlank(message = "Start time is required")
        @JsonProperty("start_time")
        String startTime; // HH:mm

        @NotBlank(message = "End time is required")
        @JsonProperty("end_time")
        String endTime; // HH:mm
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PreferencesRequest {
        @JsonProperty("preferred_health_status")
        String preferredHealthStatus; // good, moderate, weak, null

        @JsonProperty("elderly_age_preference")
        AgeRangeRequest elderlyAgePreference; // min, max, null
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AgeRangeRequest {
        @Min(value = 0, message = "Min age must be >= 0")
        @JsonProperty("min_age")
        Integer minAge;

        @Min(value = 0, message = "Max age must be >= 0")
        @JsonProperty("max_age")
        Integer maxAge;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CredentialRequest {
        @JsonProperty("qualification_type_id")
        UUID qualificationTypeId;

        @JsonProperty("certificate_number")
        String certificateNumber;

        @JsonProperty("issuing_organization")
        String issuingOrganization;

        @JsonProperty("issue_date")
        LocalDate issueDate;

        @JsonProperty("expiry_date")
        LocalDate expiryDate;

        String notes;
    }
}

