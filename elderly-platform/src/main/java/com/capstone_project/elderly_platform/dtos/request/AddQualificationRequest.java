package com.capstone_project.elderly_platform.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddQualificationRequest {

    @NotNull(message = "Qualification type ID is required")
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
