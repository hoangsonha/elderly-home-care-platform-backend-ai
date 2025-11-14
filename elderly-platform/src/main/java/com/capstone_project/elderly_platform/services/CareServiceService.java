package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.ConfirmationCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;

public interface CareServiceService {
    CareServiceResponseDTO createCareService(CreateCareServiceRequest request);

    CareServiceResponseDTO acceptCareServiceFromCaregiver(ConfirmationCareServiceRequest request);

    CareServiceResponseDTO declineCareService(ConfirmationCareServiceRequest request);
}
