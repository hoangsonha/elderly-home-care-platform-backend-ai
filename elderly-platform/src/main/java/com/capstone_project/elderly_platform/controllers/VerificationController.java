package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CaregiverProfileVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.QualificationVerificationRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverVerificationResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/verifications")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification", description = "Operations related to caregiver profile verification. Only accessible by ADMIN role")
public class VerificationController {

    private final ProfileService profileService;

    @Operation(summary = "Get pending verification caregivers", description = "Get list of caregivers waiting for profile verification (isVerified = false). "
            +
            "Includes full caregiver profile, account information, and qualifications. Only accessible by ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/caregivers/pending")
    public ResponseEntity<ObjectResponse> getPendingVerificationCaregivers() {
        try {
            List<CaregiverVerificationResponseDTO> caregivers = profileService.getPendingVerificationCaregivers();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get pending verification caregivers successfully",
                            caregivers));
        } catch (Exception e) {
            log.error("Error getting pending verification caregivers", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to get pending verification caregivers: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Approve or reject caregiver profile status", description = "Approve or reject a caregiver profile (basic information verification). "
            +
            "If action is 'REJECT', rejectionReason is required. " +
            "If action is 'APPROVE', rejectionReason is optional. " +
            "Only accessible by ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/caregivers/{caregiverProfileId}")
    public ResponseEntity<ObjectResponse> verifyCaregiverProfileStatus(
            @Parameter(description = "Caregiver profile ID to verify") @PathVariable UUID caregiverProfileId,
            @Valid @RequestBody CaregiverProfileVerificationRequest request) {
        try {
            CaregiverVerificationResponseDTO result = profileService.verifyCaregiverProfileStatus(caregiverProfileId,
                    request);
            String message = "APPROVE".equalsIgnoreCase(request.getAction())
                    ? "Caregiver profile approved successfully"
                    : "Caregiver profile rejected successfully";
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", message, result));
        } catch (Exception e) {
            log.error("Error verifying caregiver profile status", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to verify caregiver profile status: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Approve or reject qualification", description = "Approve or reject a specific qualification (certificate). "
            +
            "If action is 'REJECT', rejectionReason is required. " +
            "If action is 'APPROVE', rejectionReason is optional. " +
            "After all qualifications are reviewed, is_needed_review_certificate will be set to false. " +
            "Only accessible by ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/qualifications/{qualificationId}")
    public ResponseEntity<ObjectResponse> verifyQualification(
            @Parameter(description = "Qualification ID to verify") @PathVariable UUID qualificationId,
            @Valid @RequestBody QualificationVerificationRequest request) {
        try {
            CaregiverVerificationResponseDTO result = profileService.verifyQualification(qualificationId, request);
            String message = "APPROVE".equalsIgnoreCase(request.getAction())
                    ? "Qualification approved successfully"
                    : "Qualification rejected successfully";
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", message, result));
        } catch (Exception e) {
            log.error("Error verifying qualification", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to verify qualification: " + e.getMessage(), null));
        }
    }
}
