package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.dtos.request.ConfirmationCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.exceptions.BadRequestException;
import com.capstone_project.elderly_platform.exceptions.ElementExistException;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.services.CareServiceService;
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
     * @param createCareServiceRequest param basic for booking
     * @return booking or null
     */
    @Operation(summary = "Create care service", description = "Create care service booking for elderly")
    @PreAuthorize("hasRole('CARE_SEEKER')")
    @PostMapping("")
    public ResponseEntity<ObjectResponse> createCareService(@Valid @RequestBody CreateCareServiceRequest createCareServiceRequest) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.createCareService(createCareServiceRequest);
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

    /**
     * Method caregiver accept care service booking for elderly from care seeker
     *
     * @param confirmationCareServiceRequest param basic for booking
     * @return booking or null
     */
    @Operation(summary = "Accept care service from caregiver", description = "Caregiver accept care service booking for elderly from care eeeker")
    @PreAuthorize("hasRole('CAREGIVER')")
    @PostMapping("/accept-care-service-from-caregiver")
    public ResponseEntity<ObjectResponse> acceptCareServiceFromCaregiver(@Valid @RequestBody ConfirmationCareServiceRequest confirmationCareServiceRequest) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.acceptCareServiceFromCaregiver(confirmationCareServiceRequest);
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Accept care service successfully", careServiceResponseDTO));
        } catch (BadRequestException e) {
            log.error("Error accept care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (ElementExistException e) {
            log.error("Error accept care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error accept care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Method caregiver or care seeker decline care service booking for elderly
     *
     * @param confirmationCareServiceRequest param basic for booking
     * @return booking or null
     */
    @Operation(summary = "Decline care service from caregiver or care seeker", description = "Caregiver or care seeker decline care service booking for elderly")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @PostMapping("/decline-care-service")
    public ResponseEntity<ObjectResponse> declineCareService(@Valid @RequestBody ConfirmationCareServiceRequest confirmationCareServiceRequest) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.declineCareService(confirmationCareServiceRequest);
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Accept care service successfully", careServiceResponseDTO));
        } catch (ElementExistException e) {
            log.error("Error accept care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error accept care service", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get care service detail by ID
     *
     * @param careServiceId ID of the care service
     * @return care service details
     */
    @Operation(summary = "Get care service by ID", description = "Get care service detail by care service ID")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER') or hasRole('ADMIN')")
    @GetMapping("/{careServiceId}")
    public ResponseEntity<ObjectResponse> getCareServiceById(
            @Parameter(description = "Care service ID", required = true)
            @PathVariable UUID careServiceId) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.getCareServiceById(careServiceId);
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get care service successfully", careServiceResponseDTO));
        } catch (ElementNotFoundException e) {
            log.error("Error getting care service by ID", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting care service by ID", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get care service detail by booking code
     *
     * @param bookingCode Booking code of the care service
     * @return care service details
     */
    @Operation(summary = "Get care service by booking code", description = "Get care service detail by booking code")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER') or hasRole('ADMIN')")
    @GetMapping("/booking-code/{bookingCode}")
    public ResponseEntity<ObjectResponse> getCareServiceByBookingCode(
            @Parameter(description = "Booking code", required = true)
            @PathVariable String bookingCode) {
        try {
            CareServiceResponseDTO careServiceResponseDTO = careServiceService.getCareServiceByBookingCode(bookingCode);
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", "Get care service successfully", careServiceResponseDTO));
        } catch (ElementNotFoundException e) {
            log.error("Error getting care service by booking code", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting care service by booking code", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

    /**
     * Get all care services for the current user (seeker or caregiver)
     * Sorted by created date (newest first) with optional status filter
     *
     * @param status Optional status filter
     * @return list of care services
     */
    @Operation(summary = "Get my care services", description = "Get all care services for current user (seeker or caregiver) with optional status filter. Default sort: newest first")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/my-care-services")
    public ResponseEntity<ObjectResponse> getMyCareServices(
            @Parameter(description = "Optional status filter (WAITING_PAYMENT, PENDING_CAREGIVER, CAREGIVER_APPROVED, IN_PROGRESS, COMPLETED_WAITING_REVIEW, COMPLETED, CANCELLED, EXPIRED)")
            @RequestParam(required = false) EnumCareServiceStatusType status) {
        try {
            List<CareServiceResponseDTO> careServices = careServiceService.getMyCareServices(status);
            String message = status != null 
                    ? String.format("Get %d care services with status %s successfully", careServices.size(), status)
                    : String.format("Get %d care services successfully", careServices.size());
            return ResponseEntity.status(HttpStatus.OK).body(new ObjectResponse("Success", message, careServices));
        } catch (ElementNotFoundException e) {
            log.error("Error getting my care services", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (BadRequestException e) {
            log.error("Error getting my care services", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error getting my care services", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ObjectResponse("Fail", e.getMessage(), null));
        }
    }

}
