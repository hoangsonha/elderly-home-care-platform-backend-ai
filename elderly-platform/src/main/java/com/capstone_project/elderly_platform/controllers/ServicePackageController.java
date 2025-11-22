package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CreateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.ServicePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/service-packages")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service Package", description = "Operations related to service package management")
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    @Operation(summary = "Create service package", description = "Create a new service package")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    public ResponseEntity<ObjectResponse> createServicePackage(
            @Valid @RequestBody CreateServicePackageRequest request) {
        try {
            ServicePackageResponseDTO response = servicePackageService.createServicePackage(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ObjectResponse("Success", "Service package created successfully", response));
        } catch (Exception e) {
            log.error("Error creating service package", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to create service package: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Update service package", description = "Update an existing service package")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ObjectResponse> updateServicePackage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateServicePackageRequest request) {
        try {
            ServicePackageResponseDTO response = servicePackageService.updateServicePackage(id, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Service package updated successfully", response));
        } catch (ElementNotFoundException e) {
            log.error("Service package not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating service package", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to update service package: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get service package by ID", description = "Retrieve a service package by its ID")
    @GetMapping("/{id}")
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

    @Operation(summary = "Get all service packages", description = "Retrieve all service packages (not deleted)")
    @GetMapping("")
    public ResponseEntity<ObjectResponse> getAllServicePackages() {
        try {
            List<ServicePackageResponseDTO> response = servicePackageService.getAllServicePackages();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Service packages retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting all service packages", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get service packages: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get all active service packages", description = "Retrieve all active service packages")
    @GetMapping("/active")
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

    @Operation(summary = "Delete service package", description = "Soft delete a service package (mark as deleted)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ObjectResponse> deleteServicePackage(@PathVariable("id") UUID id) {
        try {
            servicePackageService.deleteServicePackage(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Service package deleted successfully", null));
        } catch (ElementNotFoundException e) {
            log.error("Service package not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting service package", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to delete service package: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Restore service package", description = "Restore a soft deleted service package")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ObjectResponse> restoreServicePackage(@PathVariable("id") UUID id) {
        try {
            servicePackageService.restoreServicePackage(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Service package restored successfully", null));
        } catch (ElementNotFoundException e) {
            log.error("Service package not found", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ObjectResponse("Failed", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error restoring service package", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to restore service package: " + e.getMessage(), null));
        }
    }
}
