package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {
    List<CaregiverProfileResponseDTO> getAllCaregivers();

    List<ElderlyProfileResponseDTO> getElderlyProfilesByCurrentCareSeeker();

    ElderlyProfileResponseDTO createElderlyProfile(CreateElderlyProfileRequest request, MultipartFile avatarFile);
}
