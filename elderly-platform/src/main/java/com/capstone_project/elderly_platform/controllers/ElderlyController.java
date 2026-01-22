package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.UpdateElderlyProfileRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequestMapping("/api/v1/elderly")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Elderly", description = "Operations related to elderly profile management")
public class ElderlyController {

    private final ProfileService profileService;

    @Operation(summary = "Get elderly profile by ID", description = "Retrieve an elderly profile by its ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    public ResponseEntity<ObjectResponse> getElderlyProfileById(
            @Parameter(description = "Elderly profile ID") @PathVariable("id") UUID id) {
        try {
            ElderlyProfileResponseDTO elderlyProfile = profileService.getElderlyProfileById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Elderly profile retrieved successfully", elderlyProfile));
        } catch (ElementNotFoundException e) {
            log.error("Elderly profile not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting elderly profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get elderly profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update elderly profile (with avatar)", description = "Update an elderly profile with optional avatar. Only accessible by CARE_SEEKER role who owns the profile. Use multipart/form-data when uploading avatar. Note: In Swagger UI, for the 'data' part, you must manually set Content-Type to 'application/json' in the request.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectResponse> updateElderlyProfile(
            @Parameter(description = "Elderly profile ID") @PathVariable("id") UUID id,
            @Parameter(description = "JSON data containing elderly profile information. IMPORTANT: Set Content-Type to 'application/json' in Swagger UI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) @RequestPart(value = "data", required = true) @Valid UpdateElderlyProfileRequest request,
            @Parameter(description = "Avatar image file (optional)", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)) @RequestPart(value = "avatar", required = false) MultipartFile avatarFile) {
        try {
            ElderlyProfileResponseDTO updatedProfile = profileService.updateElderlyProfile(id, request, avatarFile);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Elderly profile updated successfully", updatedProfile));
        } catch (ElementNotFoundException e) {
            log.error("Elderly profile not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating elderly profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to update elderly profile: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update elderly profile (without avatar)", description = "Update an elderly profile without avatar. Only accessible by CARE_SEEKER role who owns the profile. Use application/json when no avatar is needed.")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PutMapping(value = "/{id}/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectResponse> updateElderlyProfileWithoutAvatar(
            @Parameter(description = "Elderly profile ID") @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateElderlyProfileRequest request) {
        try {
            ElderlyProfileResponseDTO updatedProfile = profileService.updateElderlyProfile(id, request, null);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Elderly profile updated successfully", updatedProfile));
        } catch (ElementNotFoundException e) {
            log.error("Elderly profile not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating elderly profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to update elderly profile: " + e.getMessage(), null));
        }
    }
}
