package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CareServiceRequest;
import com.capstone_project.elderly_platform.dtos.response.CareServiceResponseDTO;

public interface CareServiceService {
    CareServiceResponseDTO createCareService(CareServiceRequest request);
}
