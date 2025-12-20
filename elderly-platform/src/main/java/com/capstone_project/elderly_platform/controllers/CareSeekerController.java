package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Create elderly profile", description = "Create a new elderly profile for the current care seeker. Only accessible by CARE_SEEKER role")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping(value = "/elderly-profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> createElderlyProfile(
            @RequestPart("data") @Valid CreateElderlyProfileRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatarFile) {
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
}
