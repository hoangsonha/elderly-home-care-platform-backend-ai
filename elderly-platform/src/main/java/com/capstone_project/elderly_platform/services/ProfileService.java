package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {
    List<CaregiverProfileResponseDTO> getAllCaregivers();

    List<ElderlyProfileResponseDTO> getElderlyProfilesByCurrentCareSeeker();

    ElderlyProfileResponseDTO createElderlyProfile(CreateElderlyProfileRequest request, MultipartFile avatarFile);
    
    CareSeekerProfileResponseDTO createCareSeekerProfile(CreateCareSeekerProfileRequest request, MultipartFile avatarFile);
    
    CaregiverProfileResponseDTO createCaregiverProfile(UpdateCaregiverProfileRequest request, MultipartFile avatarFile, List<MultipartFile> credentialFiles);
    
    CaregiverProfileResponseDTO updateCaregiverProfile(UpdateCaregiverProfileRequest request, MultipartFile avatarFile, List<MultipartFile> credentialFiles);
}
