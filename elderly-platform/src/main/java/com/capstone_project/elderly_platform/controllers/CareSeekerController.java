package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.services.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
