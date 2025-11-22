package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.ConfirmationCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;

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

    // Get all care services for current user (seeker or caregiver) with optional status filter
    List<CareServiceResponseDTO> getMyCareServices(EnumCareServiceStatusType status);
}
