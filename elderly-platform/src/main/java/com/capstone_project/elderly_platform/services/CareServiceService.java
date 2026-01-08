package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.ConfirmationCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCareServiceStatusRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageEligibilityResponse;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CareServiceService {
    CareServiceResponseDTO createCareService(CreateCareServiceRequest request);

    CareServiceResponseDTO acceptCareServiceFromCaregiver(ConfirmationCareServiceRequest request);

    CareServiceResponseDTO declineCareService(ConfirmationCareServiceRequest request);

    // Get care service detail by ID
    CareServiceResponseDTO getCareServiceById(UUID careServiceId);

    // Get care service detail by booking code
    CareServiceResponseDTO getCareServiceByBookingCode(String bookingCode);

    // Get all care services for current user (seeker or caregiver) with optional status and work date filter
    List<CareServiceResponseDTO> getMyCareServices(EnumCareServiceStatusType status, LocalDate workDate);

    CareService updateStatus(UUID careServiceId, UpdateCareServiceStatusRequest request);
    
    // Check caregiver eligibility for all active service packages
    // If caregiverId is provided, checks that caregiver's eligibility
    // If not provided, checks current user's eligibility (only for CAREGIVER role)
    List<ServicePackageEligibilityResponse> checkCaregiverEligibilityForServicePackages(UUID caregiverId);
}
