package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.QualificationTypeResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
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

    // private final AircraftService aircraftService;
    //
    // @Value("${application.default-current-page}")
    // private int defaultCurrentPage;
    //
    // @Value("${application.default-page-size}")
    // private int defaultPageSize;
    //
    // /**
    // * Method get all air-crafts have status is active
    // *
    // * @param currentPage currentOfThePage
    // * @param pageSize numberOfElement
    // * @return list or empty
    // */
    // @Operation(summary = "Get all air-crafts active", description = "Retrieves
    // all air-crafts have status is active")
    // @GetMapping("/aircraft-active")
    // public ResponseEntity<PagingResponse>
    // getAllAircraftActive(@RequestParam(value = "currentPage", required = false)
    // Integer currentPage,
    // @RequestParam(value = "pageSize", required = false) Integer pageSize) {
    // int resolvedCurrentPage = (currentPage != null) ? currentPage :
    // defaultCurrentPage;
    // int resolvedPageSize = (pageSize != null) ? pageSize : defaultPageSize;
    // PagingResponse results =
    // aircraftService.getAircraftsActive(resolvedCurrentPage, resolvedPageSize);
    // List<?> data = (List<?>) results.getData();
    // return ResponseEntity.status(!data.isEmpty() ? HttpStatus.OK :
    // HttpStatus.BAD_REQUEST).body(results);
    // }
    //
    // /**
    // * Method search air-crafts active with name and sortBy
    // *
    // * @param currentPage currentOfThePage
    // * @param pageSize numberOfElement
    // * @param code code of aircraft to search
    // * @param model sortBy model with aircraftType
    // * @param manufacturer sortBy manufacturer with aircraftType
    // * @return list or empty
    // */
    // @Operation(summary = "Search air-crafts active", description = "Retrieves all
    // air-crafts active are filtered by code, model and manufacturer")
    // @GetMapping("/search-aricraft-active")
    // public ResponseEntity<PagingResponse>
    // searchAircraftActive(@RequestParam(value = "currentPage", required = false)
    // Integer currentPage,
    // @RequestParam(value = "pageSize", required = false) Integer pageSize,
    // @RequestParam(value = "code", required = false, defaultValue = "") String
    // code,
    // @RequestParam(value = "model", required = false, defaultValue = "") String
    // model,
    // @RequestParam(value = "manufacturer", required = false, defaultValue = "")
    // String manufacturer) {
    // int resolvedCurrentPage = (currentPage != null) ? currentPage :
    // defaultCurrentPage;
    // int resolvedPageSize = (pageSize != null) ? pageSize : defaultPageSize;
    //
    // PagingResponse results =
    // aircraftService.searchAircraftsActive(resolvedCurrentPage, resolvedPageSize,
    // code, model, manufacturer);
    // List<?> data = (List<?>) results.getData();
    // return ResponseEntity.status(!data.isEmpty() ? HttpStatus.OK :
    // HttpStatus.BAD_REQUEST).body(results);
    // }
    //
    // /**
    // * Method get aircraft by aircraft id
    // *
    // * @param id idOfAircraft
    // * @return list or empty
    // */
    // @Operation(summary = "Get aircraft by aircraft id", description = "Retrieves
    // aircraft by aircraft id")
    //// @PreAuthorize("hasRole('USER') or hasRole('STAFF') or hasRole('ADMIN')")
    // @GetMapping("/{id}/active")
    // public ResponseEntity<ObjectResponse>
    // getAircraftByIDActive(@PathVariable("id") UUID id) {
    // AircraftResponseDTO aircraft = aircraftService.findByIdActive(id);
    // return aircraft != null ?
    // ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get
    // aircraft by ID successfully", aircraft)) :
    // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail",
    // "Get aircraft by ID failed", null));
    // }

    /**
     * Match caregivers using AI matching service
     * 
     * @param request MatchCaregiverRequest with DISEASE request details
     * @return MatchCaregiverResponse with matched caregivers
     */
    @Operation(summary = "Match caregivers using AI", description = "Match caregivers using AI matching service based on care requirements")
    @PostMapping("/match-caregivers")
    public ResponseEntity<?> matchCaregivers(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received caregiver matching request");

            Map<String, Object> response = aiMatchingService.matchCaregivers(request);

            return ResponseEntity.ok(response);
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
