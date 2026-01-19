package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.externals.MatchCaregiverByElderlyRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.QualificationTypeResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.ProfileService;
import com.capstone_project.elderly_platform.services.QualificationTypeService;
import com.capstone_project.elderly_platform.services.ServicePackageService;
import com.capstone_project.elderly_platform.services.externals.ai.AIMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api/v1/public")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Api", description = "Operations related to public endpoints")
public class PublicController {

    private final AIMatchingService aiMatchingService;
    private final ServicePackageService servicePackageService;
    private final ProfileService profileService;
    private final QualificationTypeService qualificationTypeService;

    @Operation(summary = "Get all active service packages", description = "Retrieve all active service packages")
    @GetMapping("/service-package/active")
    public ResponseEntity<ObjectResponse> getAllActiveServicePackages() {
        try {
            List<ServicePackageResponseDTO> response = servicePackageService.getAllActiveServicePackages();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Active service packages retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting active service packages", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get active service packages: " + e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Get service package by ID", description = "Retrieve a service package by its ID")
    @GetMapping("/service-package/{id}")
    public ResponseEntity<ObjectResponse> getServicePackageById(@PathVariable("id") UUID id) {
        try {
            ServicePackageResponseDTO response = servicePackageService.getServicePackageById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Service package retrieved successfully", response));
        } catch (ElementNotFoundException e) {
            log.error("Service package not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting service package", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get service package: " + e.getMessage(), null));
        }
    }

    /**
     * Match caregivers using AI matching service
     * 
     * @param request MatchCaregiverByElderlyRequest với elderlyProfileId, servicePackageId, workDate, startHour, startMinute
     * @return MatchCaregiverResponse with matched caregivers
     */
    @Operation(summary = "Match caregivers using AI", description = "Match caregivers using AI matching service based on elderly profile and service package")
    @PostMapping("/match-caregivers")
    public ResponseEntity<?> matchCaregivers(@RequestBody MatchCaregiverByElderlyRequest request) {
        try {
            log.info("Received caregiver matching request for elderly profile: {}", request.getElderlyProfileId());

            Map<String, Object> response = aiMatchingService.matchCaregiversByElderly(request);

            return ResponseEntity.ok(response);
        } catch (ElementNotFoundException | BadRequestException e) {
            log.error("Error matching caregivers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error matching caregivers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to match caregivers: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get all caregivers", description = "Retrieve all active caregiver profiles")
    @GetMapping("/caregivers")
    public ResponseEntity<ObjectResponse> getAllCaregivers() {
        try {
            List<CaregiverProfileResponseDTO> caregivers = profileService.getAllCaregivers();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregivers retrieved successfully", caregivers));
        } catch (Exception e) {
            log.error("Error getting caregivers", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get caregivers: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get caregiver by ID", description = "Retrieve a caregiver profile by its ID")
    @GetMapping("/caregivers/{id}")
    public ResponseEntity<ObjectResponse> getCaregiverById(@PathVariable("id") UUID id) {
        try {
            CaregiverProfileResponseDTO caregiver = profileService.getCaregiverById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Caregiver retrieved successfully", caregiver));
        } catch (ElementNotFoundException e) {
            log.error("Caregiver not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting caregiver", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get caregiver: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get all active qualification types", description = "Retrieve all active qualification types (certificate types)")
    @GetMapping("/qualification-types")
    public ResponseEntity<ObjectResponse> getAllActiveQualificationTypes() {
        try {
            List<QualificationTypeResponseDTO> qualificationTypes = qualificationTypeService.getAllActiveQualificationTypes();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Qualification types retrieved successfully", qualificationTypes));
        } catch (Exception e) {
            log.error("Error getting qualification types", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get qualification types: " + e.getMessage(), null));
        }
    }

}
