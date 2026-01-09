package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.CaregiverProfileVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateCareSeekerProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.CreateElderlyProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.QualificationVerificationRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverProfileRequest;
import com.capstone_project.elderly_platform.dtos.request.UpdateCaregiverQualificationsRequest;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CaregiverVerificationResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileDetailResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.CareSeekerProfileResponseDTO;
import com.capstone_project.elderly_platform.dtos.response.ElderlyProfileResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    List<CaregiverProfileResponseDTO> getAllCaregivers();

    List<ElderlyProfileResponseDTO> getElderlyProfilesByCurrentCareSeeker();

    ElderlyProfileResponseDTO createElderlyProfile(CreateElderlyProfileRequest request, MultipartFile avatarFile);
    
    CareSeekerProfileResponseDTO createCareSeekerProfile(CreateCareSeekerProfileRequest request,
            MultipartFile avatarFile);

    CaregiverProfileResponseDTO createCaregiverProfile(UpdateCaregiverProfileRequest request, MultipartFile avatarFile,
            List<MultipartFile> credentialFiles, MultipartFile citizenIdFrontImage, MultipartFile citizenIdBackImage);

    CaregiverProfileResponseDTO updateCaregiverProfile(UpdateCaregiverProfileRequest request, MultipartFile avatarFile,
            List<MultipartFile> credentialFiles, MultipartFile citizenIdFrontImage, MultipartFile citizenIdBackImage);

    CaregiverProfileResponseDTO updateCaregiverQualifications(UpdateCaregiverQualificationsRequest request,
            List<MultipartFile> credentialFiles);

    List<CaregiverVerificationResponseDTO> getPendingVerificationCaregivers();

    CaregiverVerificationResponseDTO verifyCaregiverProfileStatus(UUID caregiverProfileId,
            CaregiverProfileVerificationRequest request);

    CaregiverVerificationResponseDTO verifyQualification(UUID qualificationId,
            QualificationVerificationRequest request);
    
    CaregiverProfileDetailResponseDTO getMyCaregiverProfile();
    
    CareSeekerProfileDetailResponseDTO getMyCareSeekerProfile();
}
