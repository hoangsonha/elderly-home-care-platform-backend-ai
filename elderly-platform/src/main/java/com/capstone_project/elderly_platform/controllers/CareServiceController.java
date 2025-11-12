package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.CareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementExistException;
import com.capstone_project.elderly_platform.services.CareServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/care-services")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Care Service", description = "Operations related to care service management")
public class CareServiceController {

    private final CareServiceService careServiceService;

     /**
     * Method create care service booking for elderly
     *
     * @param careServiceRequest param basic for booking
     * @return booking or null
     */
    @Operation(summary = "Create care service", description = "Create care service booking for elderly")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping("")
    public ResponseEntity<ObjectResponse> createCareService(@Valid @RequestBody CareServiceRequest careServiceRequest) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.createCareService(careServiceRequest);
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Create care service successfully", careServiceResponseDTO));
        } catch (BadRequestException e) {
            log.error("Error creating care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (ElementExistException e) {
            log.error("Error creating care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

}
