package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateServicePackageRequest;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageListResponse;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ServicePackageUsageResponse;

import java.util.List;
import java.util.UUID;

public interface ServicePackageService {
    
    ServicePackageResponseDTO createServicePackage(CreateServicePackageRequest request);
    
    ServicePackageResponseDTO updateServicePackage(UUID id, UpdateServicePackageRequest request);
    
    ServicePackageResponseDTO getServicePackageById(UUID id);
    
    ServicePackageListResponse getAllServicePackages();
    
    List<ServicePackageResponseDTO> getAllActiveServicePackages();
    
    void deleteServicePackage(UUID id);
    
    void restoreServicePackage(UUID id);
    
    ServicePackageUsageResponse getServicePackageUsage(UUID id);
}

