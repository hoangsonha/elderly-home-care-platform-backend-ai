package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/v1/care-seekers")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Care Seeker", description = "Operations related to care seeker management")
public class CareSeekerController {

    private final ProfileService profileService;

    @Operation(summary = "Get my elderly profiles", description = "Retrieve all active elderly profiles created by the current care seeker. Only accessible by CARE_SEEKER role")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @GetMapping("/elderly-profiles")
    public ResponseEntity<ObjectResponse> getMyElderlyProfiles() {
        try {
            List<ElderlyProfileResponseDTO> elderlyProfiles = profileService.getElderlyProfilesByCurrentCareSeeker();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Elderly profiles retrieved successfully", elderlyProfiles));
        } catch (Exception e) {
            log.error("Error getting elderly profiles", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get elderly profiles: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Create elderly profile (with avatar)", description = "Create a new elderly profile for the current care seeker with optional avatar. Only accessible by CARE_SEEKER role. Use multipart/form-data when uploading avatar. Note: In Swagger UI, for the 'data' part, you must manually set Content-Type to 'application/json' in the request.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping(value = "/elderly-profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> createElderlyProfile(
            @Parameter(description = "JSON data containing elderly profile information. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart(value = "data", required = true) @Valid CreateElderlyProfileRequest request,
            @Parameter(description = "Avatar image file (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "avatar", required = false) MultipartFile avatarFile) {
        try {
            ElderlyProfileResponseDTO createdProfile = profileService.createElderlyProfile(request, avatarFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Elderly profile created successfully", createdProfile));
        } catch (Exception e) {
            log.error("Error creating elderly profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to create elderly profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Create elderly profile (without avatar)", description = "Create a new elderly profile for the current care seeker without avatar. Only accessible by CARE_SEEKER role. Use application/json when no avatar is needed.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping(value = "/elderly-profiles/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectResponse> createElderlyProfileWithoutAvatar(
            @Valid @RequestBody CreateElderlyProfileRequest request) {
        try {
            ElderlyProfileResponseDTO createdProfile = profileService.createElderlyProfile(request, null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Elderly profile created successfully", createdProfile));
        } catch (Exception e) {
            log.error("Error creating elderly profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to create elderly profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Create care seeker profile (with avatar)", description = "Create a new care seeker profile for the current user with optional avatar. Only accessible by CARE_SEEKER role. Use multipart/form-data when uploading avatar.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> createCareSeekerProfile(
            @RequestPart("data") @Valid CreateCareSeekerProfileRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatarFile) {
        try {
            CareSeekerProfileResponseDTO createdProfile = profileService.createCareSeekerProfile(request, avatarFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Care seeker profile created successfully", createdProfile));
        } catch (Exception e) {
            log.error("Error creating care seeker profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to create care seeker profile: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Create care seeker profile (without avatar)", description = "Create a new care seeker profile for the current user without avatar. Only accessible by CARE_SEEKER role. Use application/json when no avatar is needed.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping(value = "/profile/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectResponse> createCareSeekerProfileWithoutAvatar(
            @Valid @RequestBody CreateCareSeekerProfileRequest request) {
        try {
            CareSeekerProfileResponseDTO createdProfile = profileService.createCareSeekerProfile(request, null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Care seeker profile created successfully", createdProfile));
        } catch (Exception e) {
            log.error("Error creating care seeker profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to create care seeker profile: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Get my care seeker profile", description = "Get current care seeker's profile with all related information including account, elderly profiles, etc. Only accessible by CARE_SEEKER role.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @GetMapping("/profile")
    public ResponseEntity<ObjectResponse> getMyCareSeekerProfile() {
        try {
            com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileDetailResponseDTO profile = profileService
                    .getMyCareSeekerProfile();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Get care seeker profile successfully", profile));
        } catch (ElementNotFoundException e) {
            log.error("Error getting care seeker profile", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting care seeker profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get care seeker profile: " + e.getMessage(), null));
        }
    }
}
