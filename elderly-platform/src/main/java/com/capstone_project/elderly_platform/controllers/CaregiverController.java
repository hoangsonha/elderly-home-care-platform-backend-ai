package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverQualificationsRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/v1/caregivers")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Caregiver", description = "Operations related to caregiver profile management")
public class CaregiverController {

    private final ProfileService profileService;

    @Operation(summary = "Create caregiver profile", description = "Create a new caregiver profile with all information including personal details, location, schedule, credentials, CCCD/CMND, etc. "
            +
            "Use multipart/form-data. Note: In Swagger UI, for the 'data' part, you must manually set Content-Type to 'application/json'. "
            +
            "For credentials, upload files in order matching the credentials array in request body.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> createCaregiverProfile(
            @Parameter(description = "JSON data containing caregiver profile information. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart("data") @Valid UpdateCaregiverProfileRequest request,
            @Parameter(description = "Avatar image file (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "avatar", required = false) MultipartFile avatarFile,
            @Parameter(description = "Credential files (optional, must match order of credentials in request body)", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) @RequestPart(value = "credentialFiles", required = false) List<MultipartFile> credentialFiles,
            @Parameter(description = "CCCD/CMND front image (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "citizenIdFrontImage", required = false) MultipartFile citizenIdFrontImage,
            @Parameter(description = "CCCD/CMND back image (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "citizenIdBackImage", required = false) MultipartFile citizenIdBackImage) {
        try {
            // Validate credentials and files match
            if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
                if (credentialFiles == null || credentialFiles.size() != request.getCredentials().size()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail",
                                    "Number of credential files must match number of credentials. Expected: "
                                            + request.getCredentials().size() + ", got: "
                                            + (credentialFiles != null ? credentialFiles.size() : 0),
                                    null));
                }
            }

            CaregiverProfileResponseDTO createdProfile = profileService.createCaregiverProfile(request, avatarFile,
                    credentialFiles, citizenIdFrontImage, citizenIdBackImage);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Caregiver profile created successfully", createdProfile));
        } catch (ElementNotFoundException e) {
            log.error("Error creating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error creating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to create caregiver profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update caregiver profile", description = "Update caregiver profile with all information including personal details, location, schedule, CCCD/CMND, etc. "
            +
            "Note: To update qualifications (chứng chỉ), use the separate API: PUT /api/v1/caregivers/qualifications. "
            +
            "Use multipart/form-data. Note: In Swagger UI, for the 'data' part, you must manually set Content-Type to 'application/json'.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> updateCaregiverProfile(
            @Parameter(description = "JSON data containing caregiver profile information. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart("data") @Valid UpdateCaregiverProfileRequest request,
            @Parameter(description = "Avatar image file (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "avatar", required = false) MultipartFile avatarFile,
            @Parameter(description = "Credential files - DEPRECATED: Use PUT /api/v1/caregivers/qualifications instead", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE), hidden = true) @RequestPart(value = "credentialFiles", required = false) List<MultipartFile> credentialFiles,
            @Parameter(description = "CCCD/CMND front image (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "citizenIdFrontImage", required = false) MultipartFile citizenIdFrontImage,
            @Parameter(description = "CCCD/CMND back image (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "citizenIdBackImage", required = false) MultipartFile citizenIdBackImage) {
        try {
            // Note: credentialFiles parameter is ignored - use separate API for qualifications
            CaregiverProfileResponseDTO updatedProfile = profileService.updateCaregiverProfile(request, avatarFile,
                    null, citizenIdFrontImage, citizenIdBackImage);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregiver profile updated successfully", updatedProfile));
        } catch (ElementNotFoundException e) {
            log.error("Error updating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error updating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating caregiver profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to update caregiver profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update caregiver qualifications", description = "Update caregiver qualifications (chứng chỉ). This will replace all existing qualifications with new ones. "
            +
            "Use multipart/form-data. Note: In Swagger UI, for the 'data' part, you must manually set Content-Type to 'application/json'. "
            +
            "For credentials, upload files in order matching the qualifications array in request body.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PutMapping(value = "/qualifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> updateCaregiverQualifications(
            @Parameter(description = "JSON data containing qualifications information. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart("data") @Valid UpdateCaregiverQualificationsRequest request,
            @Parameter(description = "Credential files (required, must match order of qualifications in request body)", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) @RequestPart(value = "credentialFiles", required = false) List<MultipartFile> credentialFiles) {
        try {
            // Validate credentials and files match
            if (request.getQualifications() != null && !request.getQualifications().isEmpty()) {
                if (credentialFiles == null || credentialFiles.size() != request.getQualifications().size()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ObjectResponse("Fail",
                                    "Number of credential files must match number of qualifications. Expected: "
                                            + request.getQualifications().size() + ", got: "
                                            + (credentialFiles != null ? credentialFiles.size() : 0),
                                    null));
                }
            }

            CaregiverProfileResponseDTO updatedProfile = profileService.updateCaregiverQualifications(request,
                    credentialFiles);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregiver qualifications updated successfully", updatedProfile));
        } catch (ElementNotFoundException e) {
            log.error("Error updating caregiver qualifications", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error updating caregiver qualifications", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating caregiver qualifications", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to update caregiver qualifications: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get my caregiver profile", description = "Get current caregiver's profile with all related information including account, qualifications, etc. Only accessible by CAREGIVER role.")
    @PreAuthorize("hasRole('CAREGIVER')")
    @GetMapping("/profile")
    public ResponseEntity<ObjectResponse> getMyCaregiverProfile() {
        try {
            com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO profile = profileService
                    .getMyCaregiverProfile();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get caregiver profile successfully", profile));
        } catch (ElementNotFoundException e) {
            log.error("Error getting caregiver profile", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting caregiver profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Fail", "Failed to get caregiver profile: " + e.getMessage(), null));
        }
    }

}
